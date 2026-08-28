package pk.vexel.financepassport.core.files

import android.content.Context
import android.net.Uri
import pk.vexel.financepassport.core.database.BillAttachmentEntity
import pk.vexel.financepassport.core.database.FinanceRepository
import pk.vexel.financepassport.core.security.EncryptedBytes
import pk.vexel.financepassport.core.security.KeystoreCryptoService
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class UtilityAttachmentVault(
    private val context: Context,
    private val repository: FinanceRepository,
    private val crypto: KeystoreCryptoService = KeystoreCryptoService()
) {
    suspend fun import(uri: Uri, linkedId: String, attachmentType: String): BillAttachmentEntity {
        val resolver = context.contentResolver
        // ContentResolver.getType() only reliably resolves content:// URIs (the normal SAF picker
        // result); it returns null for file:// URIs and can also return a generic
        // application/octet-stream from some picker sources. Fall back to the file extension via
        // MimeTypeMap so a real PDF/JPEG/PNG/WebP is never rejected just because the source
        // didn't populate a MIME type.
        val mime = resolver.getType(uri)
            ?.takeUnless { it == "application/octet-stream" }
            ?: android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase())
            ?: "application/octet-stream"
        val supportedMimes = setOf("application/pdf", "image/jpeg", "image/png", "image/webp")
        require(mime in supportedMimes) { "Supported attachments are PDF, JPEG, PNG or WebP" }
        
        // OpenableColumns.DISPLAY_NAME only resolves for content:// URIs backed by a
        // ContentProvider (the normal SAF picker result); fall back to the URI's own last path
        // segment (works for file:// URIs) before giving up on a real name entirely.
        val displayName = runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME)
                cursor.moveToFirst()
                cursor.getString(nameIndex)
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "attachment_${System.currentTimeMillis()}"

        val bytes = resolver.openInputStream(uri)?.use { stream ->
            stream.readBytes().also {
                require(it.size <= MAX_BYTES) { "Attachment is larger than 20 MB" }
            }
        } ?: error("Unable to read selected attachment")

        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
        
        // Check for duplicates
        val existing = repository.database.billAttachmentDao().getAll().firstOrNull { it.fileHash == digest }
        if (existing != null && existing.linkedId == linkedId) {
            error("This exact file has already been attached to this record.")
        }

        val id = UUID.randomUUID().toString()
        val directory = File(context.filesDir, "utility_vault").apply { mkdirs() }
        val file = File(directory, "$id.enc")
        
        val encrypted = crypto.encrypt(bytes, digest.toByteArray())
        file.outputStream().use {
            it.write(encrypted.nonce)
            it.write(encrypted.ciphertext)
        }

        val attachment = BillAttachmentEntity(
            id = id,
            linkedId = linkedId,
            attachmentType = attachmentType,
            storagePath = file.absolutePath,
            displayName = displayName,
            mimeType = mime,
            sizeBytes = bytes.size.toLong(),
            fileHash = digest,
            createdAtEpochMillis = System.currentTimeMillis(),
            linkedEntityType = if (attachmentType == "PAYMENT_PROOF") "PAYMENT" else "OCCURRENCE",
        )
        repository.addAttachment(attachment)
        return attachment
    }

    fun decrypt(attachment: BillAttachmentEntity): ByteArray {
        val file = File(attachment.storagePath)
        require(file.exists()) { "Encrypted attachment file does not exist." }
        val bytes = file.readBytes()
        require(bytes.size > 12) { "Encrypted attachment is corrupt" }
        val hash = attachment.fileHash ?: error("File hash missing from metadata")
        return crypto.decrypt(
            EncryptedBytes(bytes.copyOfRange(0, 12), bytes.copyOfRange(12, bytes.size)),
            hash.toByteArray()
        )
    }

    fun delete(attachment: BillAttachmentEntity) {
        val file = File(attachment.storagePath)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private companion object {
        const val MAX_BYTES = 20 * 1024 * 1024
    }
}
