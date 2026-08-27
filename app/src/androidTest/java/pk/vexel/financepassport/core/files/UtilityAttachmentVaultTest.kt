package pk.vexel.financepassport.core.files

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pk.vexel.financepassport.core.database.AppDatabase
import pk.vexel.financepassport.core.database.FinanceRepository
import pk.vexel.financepassport.core.security.KeystoreCryptoService
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class UtilityAttachmentVaultTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var vault: UtilityAttachmentVault
    private lateinit var context: Context
    private lateinit var testTempDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = FinanceRepository(db)
        
        val testCrypto = KeystoreCryptoService("vexel_passport_test_alias_" + UUID.randomUUID())
        vault = UtilityAttachmentVault(context, repository, testCrypto)

        testTempDir = File(context.cacheDir, "test_attachments").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        db.close()
        testTempDir.deleteRecursively()
    }

    @Test
    fun importEncryptsAndDecryptsAttachmentProof() = runBlocking {
        val fileContent = "This is a dummy payment receipt for electricity bill."
        val tempFile = File(testTempDir, "receipt.pdf")
        FileOutputStream(tempFile).use { it.write(fileContent.toByteArray()) }
        val uri = Uri.fromFile(tempFile)

        val linkedId = UUID.randomUUID().toString()
        val attachment = vault.import(uri, linkedId, "PAYMENT_PROOF")

        assertNotNull(attachment.id)
        assertEquals(linkedId, attachment.linkedId)
        assertEquals("receipt.pdf", attachment.displayName)
        assertEquals("application/pdf", attachment.mimeType)
        assertTrue(attachment.sizeBytes > 0)
        assertNotNull(attachment.fileHash)

        val encryptedFile = File(attachment.storagePath)
        assertTrue(encryptedFile.exists())
        val rawEncrypted = encryptedFile.readBytes()
        assertTrue(rawEncrypted.size > fileContent.length)

        val decrypted = vault.decrypt(attachment)
        assertEquals(fileContent, String(decrypted))

        var exceptionThrown = false
        try {
            vault.import(uri, linkedId, "PAYMENT_PROOF")
        } catch (e: Exception) {
            exceptionThrown = true
            assertTrue(e.message?.contains("already been attached") == true)
        }
        assertTrue("Expected duplicate attachment to fail", exceptionThrown)

        vault.delete(attachment)
        assertTrue(!encryptedFile.exists())
    }
}
