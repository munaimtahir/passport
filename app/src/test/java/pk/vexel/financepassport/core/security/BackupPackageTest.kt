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

    @Test fun manifestRoundTripsDocumentHashesAndRulesetVersion() {
        val staging = Files.createTempDirectory("passport-restore-manifest").toFile()
        val service = BackupPackageService(PortableBackupCrypto(100_000))
        val result = service.create(
            "database".toByteArray(), listOf(BackupFile("documents/a.enc", byteArrayOf(1, 2))), "0.1", 10, "password".toCharArray(),
            recordCount = 3, documentHashes = listOf("hash-a", "hash-b"), rulesetVersion = "pk-structural-1",
        )
        val manifest = service.restore(result.payload, "password".toCharArray(), staging)
        assertEquals(listOf("hash-a", "hash-b"), manifest.documentHashes)
        assertEquals("pk-structural-1", manifest.rulesetVersion)
    }

    @Test fun manifestParsingToleratesLegacyPackagesMissingHashOrRulesetFieldsEntirely() {
        // Hand-builds a package shaped like one created before documentHashes/rulesetVersion
        // existed in the manifest schema, to prove restore() doesn't require the new fields.
        val staging = Files.createTempDirectory("passport-restore-legacy").toFile()
        val legacyManifestJson = "{\"appVersion\":\"0.1\",\"schemaVersion\":8,\"createdAtEpochMillis\":1700000000000,\"recordCount\":3,\"documentCount\":1}"
        val plain = java.io.ByteArrayOutputStream().also { output ->
            java.util.zip.ZipOutputStream(output).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry("manifest.json")); zip.write(legacyManifestJson.toByteArray()); zip.closeEntry()
                zip.putNextEntry(java.util.zip.ZipEntry("database.snapshot")); zip.write("database".toByteArray()); zip.closeEntry()
                zip.putNextEntry(java.util.zip.ZipEntry("documents/a.enc")); zip.write(byteArrayOf(1, 2)); zip.closeEntry()
            }
        }.toByteArray()
        val envelope = PortableBackupCrypto(100_000).encrypt(plain, "password".toCharArray())

        val manifest = BackupPackageService(PortableBackupCrypto(100_000)).restore(envelope.bytes, "password".toCharArray(), staging)

        assertEquals(3, manifest.recordCount)
        assertEquals(emptyList<String>(), manifest.documentHashes)
        assertEquals(null, manifest.rulesetVersion)
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
