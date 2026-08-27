package pk.vexel.financepassport.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pk.vexel.financepassport.PassportApplication
import pk.vexel.financepassport.core.database.DocumentEntity
import pk.vexel.financepassport.core.security.KeystoreCryptoService

@RunWith(AndroidJUnit4::class)
class DocumentPreviewDeviceTest {
    private lateinit var application: PassportApplication
    private val documents = mutableListOf<DocumentEntity>()

    @Before
    fun setUp() {
        application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PassportApplication
    }

    @After
    fun tearDown() = runBlocking {
        documents.forEach { document ->
            application.repository.deleteDocument(document.id)
            File(document.localEncryptedPath).delete()
        }
    }

    @Test
    fun encryptedImageAndPdfRenderPreviews() = runBlocking {
        val imageBytes = ByteArrayOutputStream().also { output ->
            Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, output)
        }.toByteArray()
        val pdfBytes = ByteArrayOutputStream().also { output ->
            PdfDocument().apply {
                val page = startPage(PdfDocument.PageInfo.Builder(120, 120, 1).create())
                page.canvas.drawColor(android.graphics.Color.WHITE)
                finishPage(page)
                writeTo(output)
                close()
            }
        }.toByteArray()

        val image = store("preview.png", "image/png", imageBytes)
        val pdf = store("preview.pdf", "application/pdf", pdfBytes)
        assertTrue(renderDocumentPreview(application, image).width == 4)
        assertTrue(renderDocumentPreview(application, pdf).width > 0)
    }

    private suspend fun store(filename: String, mime: String, plaintext: ByteArray): DocumentEntity {
        val id = "preview-${UUID.randomUUID()}"
        val digest = MessageDigest.getInstance("SHA-256").digest(plaintext).joinToString("") { "%02x".format(it) }
        val file = File(application.filesDir, "vault/$id.enc").apply { parentFile?.mkdirs() }
        val encrypted = KeystoreCryptoService().encrypt(plaintext, digest.toByteArray())
        file.outputStream().use { it.write(encrypted.nonce); it.write(encrypted.ciphertext) }
        return DocumentEntity(id, filename, "Test", filename, mime, plaintext.size.toLong(), file.absolutePath, digest, null, Instant.now().toEpochMilli()).also {
            application.repository.database.documentDao().insert(it)
            documents += it
        }
    }
}
