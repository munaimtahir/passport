package pk.vexel.financepassport.core.security

import android.content.Context
import androidx.room.Room
import pk.vexel.financepassport.core.database.AppDatabase
import pk.vexel.financepassport.core.database.DatabaseProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class LiveRestoreService(private val context: Context, private val packageService: BackupPackageService = BackupPackageService(), private val databaseName: String = "passport.db") {
    fun restore(encryptedPackage: ByteArray, password: CharArray): BackupManifest {
        val staging = File(context.cacheDir, "restore-staging-${System.currentTimeMillis()}")
            val live = context.getDatabasePath(databaseName)
        val previous = File(context.filesDir, "passport-before-restore.db")
        try {
            val manifest = packageService.restore(encryptedPackage, password, staging)
            val stagedDatabase = File(staging, "database.snapshot")
            require(stagedDatabase.length() > 0) { "Backup database is empty" }
            DatabaseProvider.close()
            if (live.exists()) Files.copy(live.toPath(), previous.toPath(), StandardCopyOption.REPLACE_EXISTING)
            File(live.path + "-wal").delete()
            File(live.path + "-shm").delete()
            val replacement = File(context.cacheDir, "passport-replacement.db")
            Files.copy(stagedDatabase.toPath(), replacement.toPath(), StandardCopyOption.REPLACE_EXISTING)
            Files.move(replacement.toPath(), live.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            validateDatabase(live)
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
}
