package pk.vexel.financepassport.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pk.vexel.financepassport.core.database.UtilityRecurrenceEngine
import java.time.LocalDate
import java.util.UUID

/**
 * Regression test for a real bug: recording a payment through the same two-write sequence the UI
 * uses (repository.addPayment + repository.updateMonthlyOccurrence, mirroring
 * MainViewModel.addPayment/updateMonthlyOccurrence) left the occurrence's status stuck at its
 * pre-payment value and silently deleted the just-inserted payment_records row. Root cause:
 * MonthlyBillOccurrenceDao.updateMonthlyOccurrence used to go through upsert() (@Insert(onConflict
 * = REPLACE)), which SQLite implements as DELETE-then-INSERT; payment_records has
 * onDelete = CASCADE on occurrenceId, so replacing an existing occurrence row cascade-deleted its
 * payment. Fixed by giving edits of an existing occurrence a real @Update DAO method instead.
 */
@RunWith(AndroidJUnit4::class)
class UtilityPaymentStatusDeviceTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = FinanceRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun recordingPaymentUpdatesOccurrenceStatusAndSurvivesTheReconcilePass() = runBlocking {
        val today = LocalDate.now()
        val profile = UtilityBillProfileEntity(
            id = UUID.randomUUID().toString(),
            name = "Test Electric",
            category = "Electricity",
            referenceNumber = "REF-1",
            issueDayAnchor = 1,
            dueDayAnchor = 5,
            recurrenceStartMonth = today.toString().substring(0, 7),
            status = "ACTIVE",
            provider = "TestCo",
            customCategoryName = null,
            locationLabel = "Home",
            connectionIdentifier = null,
            notes = null,
            reminderPreference = "Enabled",
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        repository.addUtilityProfile(profile)

        // Force the occurrence into a definitely-overdue state so the reconcile pass inside
        // addPayment has something real to (incorrectly, pre-fix) recompute against.
        val pastDueDate = today.minusDays(20)
        val occurrence = MonthlyBillOccurrenceEntity(
            id = UUID.randomUUID().toString(),
            profileId = profile.id,
            billingYear = pastDueDate.year,
            billingMonth = pastDueDate.monthValue,
            expectedIssueDateEpochDay = pastDueDate.minusDays(4).toEpochDay(),
            expectedDueDateEpochDay = pastDueDate.toEpochDay(),
            actualIssueDateEpochDay = null,
            actualDueDateEpochDay = null,
            amountMinor = null,
            status = "Overdue",
            notes = null,
            creationSource = "Automatic",
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
        repository.addMonthlyOccurrence(occurrence)

        repository.addAccount("Test Bank", "BANK", 100_000_00, context = "Personal / Home")
        val account = db.accountDao().getAll().first()

        // Mirrors PassportApp's "Save Payment" click handler exactly: insert the payment record,
        // then separately update the occurrence with status = "Paid" and the entered amount.
        val payment = PaymentRecordEntity(
            id = UUID.randomUUID().toString(),
            occurrenceId = occurrence.id,
            amountPaidMinor = 250000,
            paymentDateEpochDay = today.toEpochDay(),
            paymentMode = "Cash",
            bankName = null,
            transactionReference = null,
            notes = null,
            createdAtEpochMillis = System.currentTimeMillis(),
            updatedAtEpochMillis = System.currentTimeMillis(),
            accountId = account.id,
        )
        repository.addPayment(payment)
        // MainViewModel.addPayment also reconciles the profile right after inserting the payment;
        // reproduce that here since it's the step that was clobbering the other write.
        UtilityRecurrenceEngine.reconcileProfile(db, profile, today)
        repository.updateMonthlyOccurrence(
            occurrence.copy(
                amountMinor = 250000,
                status = "Paid",
                updatedAtEpochMillis = System.currentTimeMillis(),
            )
        )
        // Reconcile can also run after the occurrence update (e.g. from a later app-launch pass);
        // it must not undo a real payment.
        UtilityRecurrenceEngine.reconcileProfile(db, profile, today)

        val storedOccurrence = db.monthlyBillOccurrenceDao().getById(occurrence.id)
        assertEquals("Paid", storedOccurrence?.status)
        assertEquals(250000L, storedOccurrence?.amountMinor)

        val storedPayment = db.paymentRecordDao().getForOccurrence(occurrence.id)
        assertEquals(250000L, storedPayment?.amountPaidMinor)
        assertEquals(1, db.paymentRecordDao().getAll().size)
    }
}
