package pk.vexel.financepassport.core.security

import android.content.Context
import androidx.room.Room
import pk.vexel.financepassport.core.database.AppDatabase
import pk.vexel.financepassport.core.database.DatabaseProvider
import pk.vexel.financepassport.core.database.UtilityRecurrenceEngine
import pk.vexel.financepassport.core.database.FinanceRepository
import java.time.LocalDate
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

class LiveRestoreService(private val context: Context, private val packageService: BackupPackageService = BackupPackageService(), private val databaseName: String = "passport.db") {
    suspend fun restore(encryptedPackage: ByteArray, password: CharArray): BackupManifest {
        val staging = File(context.cacheDir, "restore-staging-${System.currentTimeMillis()}")
            val live = context.getDatabasePath(databaseName)
        val previous = File(context.filesDir, "passport-before-restore.db")
        try {
            val manifest = packageService.restore(encryptedPackage, password, staging)
            val stagedDatabase = File(staging, "database.snapshot")
            require(stagedDatabase.length() > 0) { "Backup database is empty" }
            // Validate the staged database and every referenced byte before touching the live
            // database. A valid SQLite file with missing evidence is not a successful restore.
            validateDatabase(stagedDatabase)
            validateStagedDocuments(stagedDatabase, staging)
            DatabaseProvider.close()
            if (live.exists()) Files.copy(live.toPath(), previous.toPath(), StandardCopyOption.REPLACE_EXISTING)
            File(live.path + "-wal").delete()
            File(live.path + "-shm").delete()
            val replacement = File(context.cacheDir, "passport-replacement.db")
            Files.copy(stagedDatabase.toPath(), replacement.toPath(), StandardCopyOption.REPLACE_EXISTING)
            Files.move(replacement.toPath(), live.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            val vault = File(context.filesDir, "vault").apply { mkdirs() }
            File(staging, "documents").listFiles()?.forEach { staged ->
                if (staged.isFile) Files.copy(staged.toPath(), File(vault, staged.name).toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            // Utility bill/payment attachments are bundled under documents/utility/ (see
            // FinanceRepository.createEncryptedBackup{,File}) and restore into their own vault
            // directory, matching where UtilityAttachmentVault stores them at capture time.
            val utilityVault = File(context.filesDir, "utility_vault").apply { mkdirs() }
            File(staging, "documents/utility").listFiles()?.forEach { staged ->
                if (staged.isFile) Files.copy(staged.toPath(), File(utilityVault, staged.name).toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            // Bill status is a derived projection of the restored occurrence/payment graph. Rebuild
            // it before returning so restore cannot expose a paid occurrence as due when another
            // process/test previously left a stale status in the live database.
            val restoredDb = DatabaseProvider.get(context)
            UtilityRecurrenceEngine.reconcileAll(restoredDb, LocalDate.now())
            FinanceRepository(restoredDb).reconcileCalendarProjection()
            return manifest
        } catch (failure: Throwable) {
            if (previous.exists()) Files.copy(previous.toPath(), live.toPath(), StandardCopyOption.REPLACE_EXISTING)
            File(live.path + "-wal").delete()
            File(live.path + "-shm").delete()
            throw failure
        } finally {
            staging.deleteRecursively()
            previous.delete()
        }
    }

    private fun validateDatabase(file: File) {
        val validationName = databaseName.removeSuffix(".db") + "-restore-validation.db"
        val validationFile = context.getDatabasePath(validationName)
        Files.copy(file.toPath(), validationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        val validation = Room.databaseBuilder(context, AppDatabase::class.java, validationName)
            .addMigrations(*DatabaseProvider.ALL_MIGRATIONS)
            .build()
        try { validation.openHelper.writableDatabase } finally { validation.close(); validationFile.delete() }
    }

    private suspend fun validateStagedDocuments(file: File, staging: File) {
        val validationName = databaseName.removeSuffix(".db") + "-document-validation.db"
        val validationFile = context.getDatabasePath(validationName)
        Files.copy(file.toPath(), validationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        val validation = Room.databaseBuilder(context, AppDatabase::class.java, validationName)
            .addMigrations(*DatabaseProvider.ALL_MIGRATIONS).build()
        try {
            val documents = validation.documentDao().getAll()
            documents.forEach { document ->
                val staged = File(staging, "documents/${File(document.localEncryptedPath).name}")
                require(staged.isFile) { "Backup is missing document bytes for ${document.id}" }
                require(decryptedHash(staged.readBytes(), document.sha256) == document.sha256) { "Document hash mismatch for ${document.id}" }
            }
            validation.billAttachmentDao().getAll().forEach { attachment ->
                val staged = File(staging, "documents/utility/${File(attachment.storagePath).name}")
                require(staged.isFile) { "Backup is missing bill attachment bytes for ${attachment.id}" }
                attachment.fileHash?.let { require(decryptedHash(staged.readBytes(), it) == it) { "Bill attachment hash mismatch for ${attachment.id}" } }
            }
            validation.documentLinkDao().getAll().forEach { link ->
                require(documents.any { it.id == link.documentId }) { "Document link ${link.id} references a missing document" }
            }
        } finally { validation.close(); validationFile.delete() }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun decryptedHash(bytes: ByteArray, expectedHash: String): String {
        require(bytes.size > 12) { "Encrypted evidence is corrupt" }
        val clear = KeystoreCryptoService().decrypt(EncryptedBytes(bytes.copyOfRange(0, 12), bytes.copyOfRange(12, bytes.size)), expectedHash.toByteArray())
        return sha256(clear)
    }
}
