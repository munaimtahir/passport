package pk.vexel.financepassport.core.security

import androidx.room.Room
import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.LocalDate
import java.time.Instant
import java.util.UUID
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking
import pk.vexel.financepassport.core.database.AccountEntity
import pk.vexel.financepassport.core.database.AppDatabase
import pk.vexel.financepassport.core.database.DatabaseProvider
import pk.vexel.financepassport.core.database.FinanceRepository
import pk.vexel.financepassport.core.database.DocumentEntity
import pk.vexel.financepassport.core.database.DocumentLinkEntity

@RunWith(AndroidJUnit4::class)
class BackupRestoreDeviceTest {
    @Test
    fun repositoryBackupUsesConsistentSqliteSnapshot() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val name = "repository-backup-${UUID.randomUUID()}.db"
            val database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            database.accountDao().upsert(AccountEntity(UUID.randomUUID().toString(), "Repository backup", null, "CASH", null, null, "PKR", 99, LocalDate.now().toEpochDay(), "ACTIVE", null, 1, 1))
            val payload = FinanceRepository(database).createEncryptedBackup(context, "backup-password".toCharArray())
            val staging = File(context.cacheDir, "repository-backup-check-${UUID.randomUUID()}")
            BackupPackageService().restore(payload, "backup-password".toCharArray(), staging)
            val raw = SQLiteDatabase.openDatabase(File(staging, "database.snapshot").path, null, SQLiteDatabase.OPEN_READONLY)
            raw.rawQuery("SELECT COUNT(*) FROM accounts", null).use { cursor -> cursor.moveToFirst(); assertEquals(1, cursor.getInt(0)) }
            raw.close()
            staging.deleteRecursively()
            database.close()
            context.getDatabasePath(name).delete()
            File(context.getDatabasePath(name).path + "-wal").delete()
            File(context.getDatabasePath(name).path + "-shm").delete()
        }
    }

    @Test
    fun realDatabaseSurvivesEncryptedBackupClearAndRestore() {
        runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "backup-device-${UUID.randomUUID()}.db"
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        val account = AccountEntity(UUID.randomUUID().toString(), "Restore account", null, "CASH", null, null, "PKR", 123_400, LocalDate.now().toEpochDay(), "ACTIVE", null, Instant.now().toEpochMilli(), Instant.now().toEpochMilli())
        database.accountDao().upsert(account)
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { }
        assertEquals(1, database.accountDao().getAll().size)
        val liveFile = context.getDatabasePath(name)
        val packageService = BackupPackageService(PortableBackupCrypto(100_000))
        database.close()
        val beforeRestore = SQLiteDatabase.openDatabase(liveFile.path, null, SQLiteDatabase.OPEN_READONLY)
        beforeRestore.rawQuery("SELECT COUNT(*) FROM accounts", null).use { cursor -> cursor.moveToFirst(); assertEquals(1, cursor.getInt(0)) }
        beforeRestore.close()
        val encrypted = packageService.create(liveFile.readBytes(), emptyList(), "0.1", 5, "restore-password".toCharArray(), recordCount = 1).payload
        liveFile.delete()
        File(liveFile.path + "-wal").delete()
        File(liveFile.path + "-shm").delete()

        val stagedCheck = File(context.cacheDir, "backup-staged-check-${UUID.randomUUID()}")
        packageService.restore(encrypted, "restore-password".toCharArray(), stagedCheck)
        val stagedRaw = SQLiteDatabase.openDatabase(File(stagedCheck, "database.snapshot").path, null, SQLiteDatabase.OPEN_READONLY)
        stagedRaw.rawQuery("SELECT COUNT(*) FROM accounts", null).use { cursor -> cursor.moveToFirst(); assertEquals(1, cursor.getInt(0)) }
        stagedRaw.close()
        stagedCheck.deleteRecursively()

        LiveRestoreService(context, packageService, name).restore(encrypted, "restore-password".toCharArray())
        val raw = SQLiteDatabase.openDatabase(liveFile.path, null, SQLiteDatabase.OPEN_READONLY)
        raw.rawQuery("SELECT COUNT(*) FROM accounts", null).use { cursor -> cursor.moveToFirst(); assertEquals(1, cursor.getInt(0)) }
        raw.close()
        val restored = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(DatabaseProvider.MIGRATION_4_5).build()
        assertEquals("Restore account", restored.accountDao().getAll().single().name)
        assertEquals(123_400, restored.accountDao().getAll().single().openingBalanceMinor)
        restored.close()
        File(context.getDatabasePath(name).path).delete()
        }
    }

    @Test
    fun populatedBackupRestoresEncryptedDocumentAndLinkGraph() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val name = "populated-backup-${UUID.randomUUID()}.db"
            val database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val repository = FinanceRepository(database)
            val account = AccountEntity(UUID.randomUUID().toString(), "Populated account", null, "CASH", null, null, "PKR", 500_000, LocalDate.now().toEpochDay(), "ACTIVE", null, 1, 1)
            database.accountDao().upsert(account)
            repository.addManualTaxItem("BANK_PROFIT", 25_000, "Bank certificate")
            repository.addManualTaxItem("TAX_WITHHELD", 2_500, "Withholding certificate")
            val taxItems = database.taxItemDao().getAll()
            val plaintext = "synthetic encrypted evidence".toByteArray()
            val hash = MessageDigest.getInstance("SHA-256").digest(plaintext).joinToString("") { "%02x".format(it) }
            val documentId = UUID.randomUUID().toString()
            val encrypted = KeystoreCryptoService().encrypt(plaintext, hash.toByteArray())
            val documentFile = File(context.filesDir, "vault/$documentId.enc").apply { parentFile?.mkdirs(); writeBytes(encrypted.nonce + encrypted.ciphertext) }
            database.documentDao().insert(DocumentEntity(documentId, "Bank evidence", "Tax", "bank.pdf", "application/pdf", plaintext.size.toLong(), documentFile.absolutePath, hash, null, 1))
            database.documentLinkDao().insert(DocumentLinkEntity(UUID.randomUUID().toString(), documentId, "tax_item", taxItems[0].id, "EVIDENCE"))
            database.documentLinkDao().insert(DocumentLinkEntity(UUID.randomUUID().toString(), documentId, "tax_item", taxItems[1].id, "EVIDENCE"))
            val payload = repository.createEncryptedBackup(context, "populated-password".toCharArray())
            val encryptedBytesBefore = documentFile.readBytes()
            database.close()
            context.getDatabasePath(name).delete()
            File(context.getDatabasePath(name).path + "-wal").delete()
            File(context.getDatabasePath(name).path + "-shm").delete()
            documentFile.delete()

            LiveRestoreService(context, BackupPackageService(), name).restore(payload, "populated-password".toCharArray())
            val restored = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(DatabaseProvider.MIGRATION_4_5).build()
            assertEquals(1, restored.accountDao().getAll().size)
            assertEquals(2, restored.taxItemDao().getAll().size)
            assertEquals(1, restored.documentDao().getAll().size)
            assertEquals(2, restored.documentLinkDao().getForDocument(documentId).size)
            val restoredFile = File(restored.documentDao().getAll().single().localEncryptedPath)
            assertEquals(hash, restored.documentDao().getAll().single().sha256)
            assertEquals(true, restoredFile.readBytes().contentEquals(encryptedBytesBefore))
            assertEquals(plaintext.contentToString(), KeystoreCryptoService().decrypt(EncryptedBytes(restoredFile.readBytes().copyOfRange(0, 12), restoredFile.readBytes().copyOfRange(12, restoredFile.readBytes().size)), hash.toByteArray()).contentToString())
            restored.close()
            context.getDatabasePath(name).delete()
            File(context.getDatabasePath(name).path + "-wal").delete()
            File(context.getDatabasePath(name).path + "-shm").delete()
            restoredFile.delete()
        }
    }
}
