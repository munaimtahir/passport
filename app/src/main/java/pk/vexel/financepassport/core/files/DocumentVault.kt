package pk.vexel.financepassport.core.files

import android.content.Context
import android.net.Uri
import pk.vexel.financepassport.core.database.DocumentEntity
import pk.vexel.financepassport.core.database.FinanceRepository
import pk.vexel.financepassport.core.security.EncryptedBytes
import pk.vexel.financepassport.core.security.KeystoreCryptoService
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

data class ImportedDocument(val metadata: DocumentEntity, val encryptedFile: File)

class DocumentVault(private val context: Context, private val repository: FinanceRepository, private val crypto: KeystoreCryptoService = KeystoreCryptoService()) {
    suspend fun import(uri: Uri, title: String, category: String, expiryDateEpochDay: Long? = null): ImportedDocument {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: error("The selected file has no supported MIME type")
        require(mime in SUPPORTED_MIME) { "Supported documents are PDF, JPEG, PNG or WebP" }
        val bytes = resolver.openInputStream(uri)?.use { stream -> stream.readBytes().also { require(it.size <= MAX_BYTES) { "Document is larger than 20 MB" } } } ?: error("Unable to read selected document")
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
        val existing = repository.database.documentDao().getAll().firstOrNull { it.sha256 == digest }
        require(existing == null) { "This exact file is already in the vault as \"${existing?.title}\"" }
        val id = UUID.randomUUID().toString()
        val directory = File(context.filesDir, "vault").apply { mkdirs() }
        val file = File(directory, "$id.enc")
        val encrypted = crypto.encrypt(bytes, digest.toByteArray())
        file.outputStream().use { it.write(encrypted.nonce); it.write(encrypted.ciphertext) }
        val metadata = DocumentEntity(id, title.ifBlank { "Imported document" }, category, id, mime, bytes.size.toLong(), file.absolutePath, digest, expiryDateEpochDay, Instant.now().toEpochMilli())
        repository.database.documentDao().insert(metadata)
        return ImportedDocument(metadata, file)
    }

    fun decrypt(document: DocumentEntity): ByteArray {
        val bytes = File(document.localEncryptedPath).readBytes()
        require(bytes.size > 12) { "Encrypted document is corrupt" }
        return crypto.decrypt(EncryptedBytes(bytes.copyOfRange(0, 12), bytes.copyOfRange(12, bytes.size)), document.sha256.toByteArray())
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private companion object { const val MAX_BYTES = 20 * 1024 * 1024; val SUPPORTED_MIME = setOf("application/pdf", "image/jpeg", "image/png", "image/webp") }
}
