package pk.vexel.financepassport.core.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pk.vexel.financepassport.core.database.BillAttachmentEntity
import pk.vexel.financepassport.core.database.AccountEntity
import pk.vexel.financepassport.core.database.DatabaseProvider
import pk.vexel.financepassport.core.database.FinanceRepository
import pk.vexel.financepassport.core.database.MonthlyBillOccurrenceEntity
import pk.vexel.financepassport.core.database.PaymentRecordEntity
import pk.vexel.financepassport.core.database.UtilityBillProfileEntity
import java.io.File
import java.time.LocalDate
import java.util.UUID

/**
 * End-to-end coverage for the manual, local, single-file backup/restore feature — every utility
 * profile, monthly occurrence, payment, and bill/payment-proof attachment must round-trip through
 * a create-backup -> data-loss -> restore-backup cycle, matching how a user would recover after
 * reinstalling the app or resetting their device. Runs against the real DatabaseProvider-managed
 * "passport.db" and the real on-disk utility_vault directory (the same wiring MoreDialog's
 * "Create Encrypted Backup" / "Restore Encrypted Backup" buttons use), not an isolated in-memory
 * database, since LiveRestoreService itself is hard-wired to that singleton.
 */
@RunWith(AndroidJUnit4::class)
class UtilityBackupRestoreDeviceTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val password = "correct horse battery".toCharArray()

    @After
    fun tearDown() {
        DatabaseProvider.close()
    }

    @Test
    fun utilityProfileOccurrencePaymentAndAttachmentSurviveABackupAndRestoreCycle() = runBlocking {
        DatabaseProvider.close()
        context.getDatabasePath("passport.db").delete()
        File(context.getDatabasePath("passport.db").path + "-wal").delete()
        File(context.getDatabasePath("passport.db").path + "-shm").delete()
        File(context.filesDir, "utility_vault").deleteRecursively()

        var db = DatabaseProvider.get(context)
        var repository = FinanceRepository(db)

        val today = LocalDate.now()
        val account = AccountEntity("backup-account", "HBL Personal", "HBL", "BANK", null, null, "PKR", 100_000_00, today.toEpochDay(), "ACTIVE", null, 1, 1, "Personal / Home")
        db.accountDao().upsert(account)
        val profile = UtilityBillProfileEntity(
            id = UUID.randomUUID().toString(), name = "Backup Test Electric", category = "Electricity",
            referenceNumber = "REF-BK-1", issueDayAnchor = 1, dueDayAnchor = 5,
            recurrenceStartMonth = today.toString().substring(0, 7), status = "ACTIVE",
            provider = "TestCo", customCategoryName = null, locationLabel = "Home",
            connectionIdentifier = null, notes = null, reminderPreference = "Enabled",
            createdAtEpochMillis = System.currentTimeMillis(), updatedAtEpochMillis = System.currentTimeMillis(),
        )
        repository.addUtilityProfile(profile)

        val occurrence = MonthlyBillOccurrenceEntity(
            id = UUID.randomUUID().toString(), profileId = profile.id, billingYear = today.year,
            billingMonth = today.monthValue, expectedIssueDateEpochDay = today.toEpochDay(),
            expectedDueDateEpochDay = today.plusDays(5).toEpochDay(), actualIssueDateEpochDay = null,
            actualDueDateEpochDay = null, amountMinor = 250000, status = "Paid", notes = null,
            creationSource = "Automatic", createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        repository.addMonthlyOccurrence(occurrence)

        val payment = PaymentRecordEntity(
            id = UUID.randomUUID().toString(), occurrenceId = occurrence.id, amountPaidMinor = 250000,
            paymentDateEpochDay = today.toEpochDay(), paymentMode = "Cash", bankName = null,
            transactionReference = null, notes = null, createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(), accountId = account.id,
        )
        repository.addPayment(payment)

        val vaultDir = File(context.filesDir, "utility_vault").apply { mkdirs() }
        val attachmentBytes = "fake encrypted payment proof bytes".toByteArray()
        val attachmentFile = File(vaultDir, "${UUID.randomUUID()}.enc").apply { writeBytes(attachmentBytes) }
        val attachment = BillAttachmentEntity(
            id = UUID.randomUUID().toString(), linkedId = occurrence.id, attachmentType = "PAYMENT_PROOF",
            storagePath = attachmentFile.absolutePath, displayName = "receipt.pdf", mimeType = "application/pdf",
            sizeBytes = attachmentBytes.size.toLong(), fileHash = null, createdAtEpochMillis = System.currentTimeMillis(),
        )
        repository.addAttachment(attachment)
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { }

        val backupFile = repository.createEncryptedBackupFile(context, password)
        val backupBytes = backupFile.readBytes()
        assertTrue("Backup file should be non-trivial in size", backupBytes.size > 100)
        backupFile.delete()

        // Simulate real data loss (reinstall / device reset): the profile row cascades away the
        // occurrence and payment; the attachment row and its on-disk file are removed separately,
        // exactly as if the app's storage had simply been wiped.
        repository.deleteUtilityProfile(profile.id)
        repository.deleteAttachment(attachment.id)
        attachmentFile.delete()
        assertEquals(0, db.utilityBillDao().getAll().size)
        assertEquals(0, db.monthlyBillOccurrenceDao().getAll().size)
        assertEquals(0, db.paymentRecordDao().getAll().size)
        assertTrue(!attachmentFile.exists())

        LiveRestoreService(context).restore(backupBytes, password)

        db = DatabaseProvider.get(context)
        repository = FinanceRepository(db)

        val restoredProfiles = db.utilityBillDao().getAll()
        assertEquals(1, restoredProfiles.size)
        assertEquals(profile.name, restoredProfiles[0].name)
        assertEquals(profile.referenceNumber, restoredProfiles[0].referenceNumber)

        val restoredOccurrences = db.monthlyBillOccurrenceDao().getByProfile(profile.id)
        assertEquals(1, restoredOccurrences.size)
        assertEquals("Paid", restoredOccurrences[0].status)
        assertEquals(250000L, restoredOccurrences[0].amountMinor)

        val restoredPayment = db.paymentRecordDao().getForOccurrence(occurrence.id)
        assertNotNull(restoredPayment)
        assertEquals(250000L, restoredPayment?.amountPaidMinor)
        assertEquals(account.id, restoredPayment?.accountId)
        val restoredEvent = restoredPayment?.financialEventId?.let { db.financialEventDao().getById(it) }
        assertNotNull(restoredEvent)
        assertEquals("EXPENSE", restoredEvent?.eventType)
        assertEquals(account.id, restoredEvent?.accountId)

        val restoredAttachments = db.billAttachmentDao().getForLinkedEntity(occurrence.id)
        assertEquals(1, restoredAttachments.size)
        val restoredAttachmentFile = File(restoredAttachments[0].storagePath)
        assertTrue("Restored attachment file should exist on disk at its recorded storagePath", restoredAttachmentFile.exists())
        assertArrayEquals(attachmentBytes, restoredAttachmentFile.readBytes())
    }
}
