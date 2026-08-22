package pk.vexel.financepassport.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pk.vexel.financepassport.PassportApplication

/**
 * Phase 6 regression coverage: deleting a document must not leave a tax item's evidence state
 * dangling as ATTACHED once its only supporting document is gone, and expiring
 * documents/official records must schedule a real persisted reminder.
 */
@RunWith(AndroidJUnit4::class)
class DocumentLifecycleDeviceTest {
    private val application get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PassportApplication

    private fun insertDocument(app: PassportApplication): DocumentEntity {
        val id = "doc-${UUID.randomUUID()}"
        val file = File(app.filesDir, "vault/$id.enc").apply { parentFile?.mkdirs(); writeBytes(byteArrayOf(1, 2, 3)) }
        val entity = DocumentEntity(id, "Evidence $id", "Test", id, "application/pdf", 3L, file.absolutePath, "sha-$id", null, Instant.now().toEpochMilli())
        return entity
    }

    private fun insertTaxItem(app: PassportApplication, evidenceState: String): TaxItemEntity {
        val id = "tax-${UUID.randomUUID()}"
        val now = Instant.now().toEpochMilli()
        return TaxItemEntity(id, "PK-2026", "financial_event", "src-$id", "OTHER_INCOME", LocalDate.now().toEpochDay(), 10000L, null, "PKR", "Test item", "REVIEWED", evidenceState, null, now, now)
    }

    @Test
    fun deletingDocumentRevertsAttachedEvidenceStateInsteadOfLeavingItDangling() = runBlocking {
        val app = application
        val document = insertDocument(app)
        val taxItem = insertTaxItem(app, evidenceState = "ATTACHED")
        app.repository.database.documentDao().insert(document)
        app.repository.database.taxItemDao().insertIfAbsent(taxItem)
        app.repository.linkDocument(document.id, "tax_item", taxItem.id)
        assertEquals(1, app.repository.documentDependencyCount(document.id))

        app.repository.deleteDocument(document.id)

        val reloaded = app.repository.database.taxItemDao().getById(taxItem.id)
        assertNotNull("tax item must survive document deletion", reloaded)
        assertEquals("evidence state must revert off ATTACHED, not dangle", "REQUESTED", reloaded!!.evidenceState)
        assertTrue("document row must be gone", app.repository.database.documentDao().getAll().none { it.id == document.id })
        assertTrue("document links must be gone", app.repository.database.documentLinkDao().getForDocument(document.id).isEmpty())
    }

    @Test
    fun documentDependencyCountReflectsCurrentLinks() = runBlocking {
        val app = application
        val document = insertDocument(app)
        val itemA = insertTaxItem(app, evidenceState = "REQUESTED")
        val itemB = insertTaxItem(app, evidenceState = "REQUESTED")
        app.repository.database.documentDao().insert(document)
        app.repository.database.taxItemDao().insertIfAbsent(itemA)
        app.repository.database.taxItemDao().insertIfAbsent(itemB)

        assertEquals(0, app.repository.documentDependencyCount(document.id))
        app.repository.linkDocument(document.id, "tax_item", itemA.id)
        app.repository.linkDocument(document.id, "tax_item", itemB.id)
        assertEquals(2, app.repository.documentDependencyCount(document.id))

        app.repository.deleteDocument(document.id)
    }

    @Test
    fun documentExpiryReminderIsPersistedAsAnOpenCalendarItem() = runBlocking {
        val app = application
        val document = insertDocument(app)
        app.repository.database.documentDao().insert(document)
        val expiry = LocalDate.now().plusDays(30)

        app.repository.scheduleDocumentExpiryReminder(app, document.id, document.title, expiry.toEpochDay())

        val calendarItem = app.repository.database.calendarDao().getById("document-expiry-${document.id}")
        assertNotNull("expiry must create a persisted calendar item", calendarItem)
        assertEquals("DOCUMENT_EXPIRY", calendarItem!!.kind)
        assertEquals("OPEN", calendarItem.status)
        assertEquals(document.id, calendarItem.linkedEntityId)

        app.repository.deleteDocument(document.id)
    }

    @Test
    fun officialRecordWithoutExpiryDoesNotScheduleAReminder() = runBlocking {
        val app = application
        val before = app.repository.database.officialRecordDao().getAll().size

        app.repository.addOfficialRecord(app, "CNIC/NICOP", "No-expiry record ${UUID.randomUUID()}", null)

        val records = app.repository.database.officialRecordDao().getAll()
        assertEquals(before + 1, records.size)
        val created = records.last()
        assertNull(created.expiryDateEpochDay)
        assertNull(app.repository.database.calendarDao().getById("official-record-expiry-${created.id}"))
    }
}
