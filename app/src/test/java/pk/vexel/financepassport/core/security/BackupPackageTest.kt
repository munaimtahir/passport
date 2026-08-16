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

    @Test fun streamingPackageRestoresWithoutLoadingDocumentBytesIntoPackageApi() {
        val root = Files.createTempDirectory("passport-streaming").toFile()
        val database = File(root, "database.db").apply { writeText("database") }
        val document = File(root, "a.enc").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val output = File(root, "backup.bin")
        val staging = File(root, "staging")
        val service = BackupPackageService(PortableBackupCrypto(100_000))
        val manifest = service.createStreaming(database, listOf(BackupDiskFile("documents/a.enc", document)), "0.1", 8, "password".toCharArray(), 3, output)
        val restored = service.restore(output.readBytes(), "password".toCharArray(), staging)
        assertEquals(8, manifest.schemaVersion)
        assertEquals(1, restored.documentCount)
        assertTrue(File(staging, "documents/a.enc").readBytes().contentEquals(document.readBytes()))
        root.deleteRecursively()
    }
}
