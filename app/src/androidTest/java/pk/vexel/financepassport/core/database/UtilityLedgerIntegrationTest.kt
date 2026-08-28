package pk.vexel.financepassport.core.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pk.vexel.financepassport.core.model.FinancialEventType
import pk.vexel.financepassport.core.reports.ReportGenerator
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class UtilityLedgerIntegrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val repository = FinanceRepository(database)

    @After fun close() = database.close()

    private suspend fun fixture(): Triple<AccountEntity, MonthlyBillOccurrenceEntity, PaymentRecordEntity> {
        repository.addAccount("HBL Personal", "BANK", 100_000_00, context = "Personal / Home")
        val account = database.accountDao().getAll().single()
        val profile = UtilityBillProfileEntity("utility", "Home Electricity", "Electricity", "demo-ref", 15, 27, "2026-08", "ACTIVE", "Demo Power", null, "Home", null, null, "DISABLED", 1, 1)
        val occurrence = MonthlyBillOccurrenceEntity("occurrence", profile.id, 2026, 8, LocalDate.of(2026, 8, 15).toEpochDay(), LocalDate.of(2026, 8, 27).toEpochDay(), null, null, 20_000_00, "Pending", null, "Automatic", 1, 1)
        database.utilityBillDao().upsert(profile)
        database.monthlyBillOccurrenceDao().upsert(occurrence)
        val payment = PaymentRecordEntity("payment", occurrence.id, 20_000_00, LocalDate.of(2026, 8, 28).toEpochDay(), "Bank Transfer", "HBL", "demo", "Paid locally", 1, 1, account.id)
        return Triple(account, occurrence, payment)
    }

    @Test fun paymentCreatesExactlyOneTraceableExpenseAndChangesBalance() = runBlocking {
        val (account, occurrence, payment) = fixture()
        repository.addEvent(FinancialEventType.INCOME, 50_000_00, account.id, "Income")
        repository.addEvent(FinancialEventType.EXPENSE, 10_000_00, account.id, "Manual expense")
        repository.addPayment(payment)
        repository.addPayment(payment.copy(id = "duplicate-action"))

        val storedPayment = database.paymentRecordDao().getForOccurrence(occurrence.id)!!
        val linkedEvent = database.financialEventDao().getById(storedPayment.financialEventId!!)!!
        assertEquals(FinancialEventType.EXPENSE.name, linkedEvent.eventType)
        assertEquals(20_000_00L, linkedEvent.amountMinor)
        assertEquals(payment.paymentDateEpochDay, linkedEvent.dateEpochDay)
        assertEquals(account.id, linkedEvent.accountId)
        assertEquals("Utilities", linkedEvent.category)
        assertTrue(linkedEvent.notes!!.contains("Occurrence: ${occurrence.id}"))
        assertEquals(storedPayment, database.paymentRecordDao().getForFinancialEvent(linkedEvent.id))
        assertEquals(3, database.financialEventDao().getAll().size)
        assertEquals(120_000_00L, account.openingBalanceMinor + repository.accountMovement(account.id).first())

        val report = ReportGenerator().incomeExpense(repository.exportSnapshot(), "test")
        assertTrue(report.lines.any { it.contains("Expense: PKR 30,000") })
    }

    @Test fun editingMovesSingleExpenseAndDeletingRemovesLedgerConsequence() = runBlocking {
        val (firstAccount, occurrence, payment) = fixture()
        repository.addAccount("Cash", "CASH", 0, context = "Personal / Home")
        val secondAccount = database.accountDao().getAll().first { it.id != firstAccount.id }
        repository.addPayment(payment)
        val originalEventId = database.paymentRecordDao().getForOccurrence(occurrence.id)!!.financialEventId!!

        repository.updatePayment(payment.id, 18_000_00, LocalDate.of(2026, 8, 29).toEpochDay(), "Cash", secondAccount.id, null, null, "Edited", 2)
        val edited = database.financialEventDao().getById(originalEventId)
        assertNotNull(edited)
        assertEquals(18_000_00L, edited!!.amountMinor)
        assertEquals(secondAccount.id, edited.accountId)
        assertEquals(100_000_00L, firstAccount.openingBalanceMinor + repository.accountMovement(firstAccount.id).first())
        assertEquals(-18_000_00L, repository.accountMovement(secondAccount.id).first())
        assertEquals(1, database.financialEventDao().getAll().size)

        repository.deletePayment(payment.id)
        assertEquals(null, database.paymentRecordDao().getForOccurrence(occurrence.id))
        assertEquals(null, database.financialEventDao().getById(originalEventId))
        assertNotNull(database.monthlyBillOccurrenceDao().getById(occurrence.id))
    }

    @Test fun profileDeletionRemovesLinkedEventsAndAttachmentMetadata() = runBlocking {
        val (_, occurrence, payment) = fixture()
        repository.addPayment(payment)
        database.billAttachmentDao().insert(BillAttachmentEntity("proof", payment.id, "PAYMENT_PROOF", "/tmp/proof.enc", "proof.pdf", "application/pdf", 1, "hash", 1, "PAYMENT"))
        repository.deleteUtilityProfile("utility")
        assertTrue(database.financialEventDao().getAll().isEmpty())
        assertTrue(database.paymentRecordDao().getAll().isEmpty())
        assertTrue(database.billAttachmentDao().getAll().isEmpty())
        assertTrue(database.monthlyBillOccurrenceDao().getAll().none { it.id == occurrence.id })
    }
}
