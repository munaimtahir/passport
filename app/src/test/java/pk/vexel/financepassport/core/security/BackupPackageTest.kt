package pk.vexel.financepassport.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class BackupPackageTest {
    @Test fun packageRestoresDatabaseAndDocumentsIntoStaging() {
        val staging = Files.createTempDirectory("passport-restore").toFile()
        val service = BackupPackageService(PortableBackupCrypto(100_000))
        val result = service.create("database".toByteArray(), listOf(BackupFile("documents/a.enc", byteArrayOf(1, 2))), "0.1", 2, "password".toCharArray(), recordCount = 3)
        val manifest = service.restore(result.payload, "password".toCharArray(), staging)
        assertEquals(3, manifest.recordCount)
        assertEquals(1, manifest.documentCount)
        assertTrue(manifest.createdAtEpochMillis > 1_000_000_000_000L)
        assertTrue(File(staging, "database.snapshot").readBytes().contentEquals("database".toByteArray()))
        assertTrue(File(staging, "documents/a.enc").exists())
    }
}
