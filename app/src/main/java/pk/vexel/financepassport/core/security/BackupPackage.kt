package pk.vexel.financepassport.core.security

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupFile(val path: String, val bytes: ByteArray)
data class BackupDiskFile(val path: String, val file: File)
data class BackupManifest(val appVersion: String, val schemaVersion: Int, val createdAtEpochMillis: Long, val recordCount: Int, val documentCount: Int)
data class BackupPackage(val manifest: BackupManifest, val payload: ByteArray)

class BackupPackageService(private val crypto: PortableBackupCrypto = PortableBackupCrypto()) {
    fun createStreaming(database: File, documents: List<BackupDiskFile>, appVersion: String, schemaVersion: Int, password: CharArray, recordCount: Int, output: File): BackupManifest {
        require(database.isFile) { "Database snapshot is missing" }
        require(recordCount >= 0) { "Record count cannot be negative" }
        val manifest = BackupManifest(appVersion, schemaVersion, Instant.now().toEpochMilli(), recordCount, documents.size)
        val manifestJson = manifestJson(manifest).toByteArray(StandardCharsets.UTF_8)
        output.parentFile?.mkdirs()
        try {
            FileOutputStream(output).use { destination ->
                database.inputStream().use { databaseInput ->
                    crypto.encrypt(java.io.SequenceInputStream(java.io.ByteArrayInputStream(ByteArray(0)), ZipPayloadInputStream(manifestJson, databaseInput, documents)), destination, password)
                }
            }
            return manifest
        } catch (failure: Throwable) {
            output.delete()
            throw failure
        }
    }

    /** Builds ZIP entries directly into the encryption stream with a bounded copy buffer. */
    private class ZipPayloadInputStream(private val manifest: ByteArray, private val database: java.io.InputStream, private val documents: List<BackupDiskFile>) : java.io.InputStream() {
        private val pipe = java.io.PipedInputStream(64 * 1024)
        private val writer = java.io.PipedOutputStream(pipe)
        private val thread = Thread {
            ZipOutputStream(writer).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json")); zip.write(manifest); zip.closeEntry()
                zip.putNextEntry(ZipEntry("database.snapshot")); database.copyTo(zip, DEFAULT_BUFFER_SIZE); zip.closeEntry()
                documents.forEach { file ->
                    require(file.path.startsWith("documents/")) { "Backup file path must be relative" }
                    zip.putNextEntry(ZipEntry(file.path)); file.file.inputStream().use { it.copyTo(zip, DEFAULT_BUFFER_SIZE) }; zip.closeEntry()
                }
            }
        }.also { it.start() }
        override fun read(): Int = pipe.read()
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int = pipe.read(buffer, offset, length)
        override fun close() { pipe.close(); writer.close(); database.close() }
    }

    fun create(database: ByteArray, documents: List<BackupFile>, appVersion: String, schemaVersion: Int, password: CharArray, recordCount: Int = database.size): BackupPackage {
        require(recordCount >= 0) { "Record count cannot be negative" }
        val manifest = BackupManifest(appVersion, schemaVersion, Instant.now().toEpochMilli(), recordCount, documents.size)
        val manifestJson = manifestJson(manifest)
        val plain = ByteArrayOutputStream().also { output -> ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json")); zip.write(manifestJson.toByteArray(StandardCharsets.UTF_8)); zip.closeEntry()
            zip.putNextEntry(ZipEntry("database.snapshot")); zip.write(database); zip.closeEntry()
            documents.forEach { file -> require(file.path.startsWith("documents/")) { "Backup file path must be relative" }; zip.putNextEntry(ZipEntry(file.path)); zip.write(file.bytes); zip.closeEntry() }
        } }.toByteArray()
        return BackupPackage(manifest, crypto.encrypt(plain, password).bytes)
    }

    private fun manifestJson(manifest: BackupManifest) = "{\"appVersion\":\"${manifest.appVersion}\",\"schemaVersion\":${manifest.schemaVersion},\"createdAtEpochMillis\":${manifest.createdAtEpochMillis},\"recordCount\":${manifest.recordCount},\"documentCount\":${manifest.documentCount}}"

    fun restore(packageBytes: ByteArray, password: CharArray, stagingDirectory: File): BackupManifest {
        val decrypted = crypto.decrypt(BackupEnvelope(packageBytes), password)
        val files = mutableMapOf<String, ByteArray>()
        ZipInputStream(decrypted.inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && !entry.name.contains("..") && !entry.name.startsWith("/")) { "Unsafe backup entry" }
                require(files.put(entry.name, zip.readBytes()) == null) { "Duplicate backup entry" }
            }
        }
        val manifestBytes = files["manifest.json"] ?: error("Backup manifest is missing")
        val database = files["database.snapshot"] ?: error("Database snapshot is missing")
        require(manifestBytes.decodeToString().contains("\"schemaVersion\":")) { "Backup manifest is invalid" }
        stagingDirectory.mkdirs()
        File(stagingDirectory, "manifest.json").writeBytes(manifestBytes)
        File(stagingDirectory, "database.snapshot").writeBytes(database)
        files.filterKeys { it.startsWith("documents/") }.forEach { (path, bytes) -> File(stagingDirectory, path).apply { parentFile?.mkdirs(); writeBytes(bytes) } }
        fun number(name: String) = Regex("\\\"$name\\\"\\s*:\\s*(\\d+)").find(manifestBytes.decodeToString())?.groupValues?.get(1)
        fun int(name: String) = number(name)?.toIntOrNull() ?: 0
        fun long(name: String) = number(name)?.toLongOrNull() ?: 0L
        val appVersion = Regex("\\\"appVersion\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"").find(manifestBytes.decodeToString())?.groupValues?.get(1) ?: "restored"
        return BackupManifest(appVersion, int("schemaVersion"), long("createdAtEpochMillis"), int("recordCount"), int("documentCount"))
    }
}
