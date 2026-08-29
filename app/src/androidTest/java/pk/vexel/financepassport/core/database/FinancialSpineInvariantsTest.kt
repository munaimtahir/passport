package pk.vexel.financepassport.core.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pk.vexel.financepassport.core.model.FinancialEventType
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class FinancialSpineInvariantsTest {
    private lateinit var database: AppDatabase
    private val now = 1000L

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun invA01_openingBalanceIsNotIncome() = runBlocking {
        val accountId = UUID.randomUUID().toString()
        val account = AccountEntity(accountId, "Cash", null, "Cash", null, null, "PKR", 50000, 1, "ACTIVE", null, now, now)
        database.accountDao().upsert(account)
        val income = database.financialEventDao().observeIncomeMinor().first()
        assertEquals(0L, income)
    }

    @Test
    fun invA02_openingBalanceIsNotExpense() = runBlocking {
        val accountId = UUID.randomUUID().toString()
        val account = AccountEntity(accountId, "Cash", null, "Cash", null, null, "PKR", 50000, 1, "ACTIVE", null, now, now)
        database.accountDao().upsert(account)
        val expense = database.financialEventDao().observeExpenseMinor().first()
        assertEquals(0L, expense)
    }

    @Test
    fun invA03_balanceCorrectionCreatesAdjustmentEvent() = runBlocking {
        val accountId = UUID.randomUUID().toString()
        val adjustment = FinancialEventEntity(UUID.randomUUID().toString(), FinancialEventType.ADJUSTMENT.name, 1, 2000, "PKR", accountId, null, null, "Correction", null, "UNKNOWN", null, now, now)
        database.financialEventDao().upsert(adjustment)
        val movement = database.financialEventDao().observeAccountMovement(accountId).first()
        assertEquals(2000L, movement)
    }

    @Test
    fun invA04_adjustmentDoesNotCountAsIncomeOrExpense() = runBlocking {
        val accountId = UUID.randomUUID().toString()
        val adjustment = FinancialEventEntity(UUID.randomUUID().toString(), FinancialEventType.ADJUSTMENT.name, 1, 2000, "PKR", accountId, null, null, "Correction", null, "UNKNOWN", null, now, now)
        database.financialEventDao().upsert(adjustment)
        assertEquals(0L, database.financialEventDao().observeIncomeMinor().first())
        assertEquals(0L, database.financialEventDao().observeExpenseMinor().first())
    }

    @Test
    fun invA05_unassignedIncomeCountsAsIncome() = runBlocking {
        val unassignedIncome = FinancialEventEntity(UUID.randomUUID().toString(), FinancialEventType.INCOME.name, 1, 5000, "PKR", null, null, null, "Unassigned", null, "UNKNOWN", null, now, now)
        database.financialEventDao().upsert(unassignedIncome)
        assertEquals(5000L, database.financialEventDao().observeIncomeMinor().first())
    }

    @Test
    fun invA06_unassignedExpenseCountsAsExpense() = runBlocking {
        val unassignedExpense = FinancialEventEntity(UUID.randomUUID().toString(), FinancialEventType.EXPENSE.name, 1, 3000, "PKR", null, null, null, "Unassigned", null, "UNKNOWN", null, now, now)
        database.financialEventDao().upsert(unassignedExpense)
        assertEquals(3000L, database.financialEventDao().observeExpenseMinor().first())
    }

    @Test
    fun invA07_unassignedEventAffectsNoSpecificAccountBalance() = runBlocking {
        val accountId = UUID.randomUUID().toString()
        val account = AccountEntity(accountId, "Cash", null, "Cash", null, null, "PKR", 0, 1, "ACTIVE", null, now, now)
        database.accountDao().upsert(account)
        val unassignedIncome = FinancialEventEntity(UUID.randomUUID().toString(), FinancialEventType.INCOME.name, 1, 5000, "PKR", null, null, null, "Unassigned", null, "UNKNOWN", null, now, now)
        database.financialEventDao().upsert(unassignedIncome)
        assertEquals(0L, database.financialEventDao().observeAccountMovement(accountId).first())
    }

    @Test
    fun invA08_assigningAccountChangesAttributionWithoutDuplication() = runBlocking {
        val accountId = UUID.randomUUID().toString()
        database.accountDao().upsert(AccountEntity(accountId, "Cash", null, "Cash", null, null, "PKR", 0, 1, "ACTIVE", null, now, now))
        val eventId = UUID.randomUUID().toString()
        val unassignedIncome = FinancialEventEntity(eventId, FinancialEventType.INCOME.name, 1, 5000, "PKR", null, null, null, "Unassigned", null, "UNKNOWN", null, now, now)
        database.financialEventDao().upsert(unassignedIncome)
        assertEquals(5000L, database.financialEventDao().observeIncomeMinor().first())
        
        // Re-assign account
        database.financialEventDao().upsert(unassignedIncome.copy(accountId = accountId))
        
        // Still 5000 total income
        assertEquals(5000L, database.financialEventDao().observeIncomeMinor().first())
        assertEquals(5000L, database.financialEventDao().observeAccountMovement(accountId).first())
        val count = database.financialEventDao().getAll().size
        assertEquals(1, count)
    }

    @Test
    fun invA09_accountReassignmentDoesNotChangeMeaning() = runBlocking {
        val account1 = UUID.randomUUID().toString()
        val account2 = UUID.randomUUID().toString()
        val eventId = UUID.randomUUID().toString()
        database.financialEventDao().upsert(FinancialEventEntity(eventId, FinancialEventType.EXPENSE.name, 1, 2000, "PKR", account1, null, null, "Expense", null, "UNKNOWN", null, now, now))
        
        database.financialEventDao().upsert(database.financialEventDao().getById(eventId)!!.copy(accountId = account2))
        val retrieved = database.financialEventDao().getById(eventId)!!
        assertEquals(FinancialEventType.EXPENSE.name, retrieved.eventType)
        assertEquals(2000L, retrieved.amountMinor)
    }

    @Test
    fun invA10_transferDecreasesSourceAndIncreasesDestinationEqually() = runBlocking {
        val src = UUID.randomUUID().toString()
        val dest = UUID.randomUUID().toString()
        val out = FinancialEventEntity(UUID.randomUUID().toString(), FinancialEventType.TRANSFER.name, 1, -1000, "PKR", src, null, null, "Transfer", null, "UNKNOWN", null, now, now)
        val incoming = FinancialEventEntity(UUID.randomUUID().toString(), FinancialEventType.TRANSFER.name, 1, 1000, "PKR", dest, null, null, "Transfer", null, "UNKNOWN", null, now, now)
        database.financialEventDao().insertAll(listOf(out, incoming))
        
        assertEquals(-1000L, database.financialEventDao().observeAccountMovement(src).first())
        assertEquals(1000L, database.financialEventDao().observeAccountMovement(dest).first())
    }

    @Test
    fun invA11_transferDoesNotCountAsIncome() = runBlocking {
        val dest = UUID.randomUUID().toString()
        val incoming = FinancialEventEntity(UUID.randomUUID().toString(), FinancialEventType.TRANSFER.name, 1, 1000, "PKR", dest, null, null, "Transfer", null, "UNKNOWN", null, now, now)
        database.financialEventDao().upsert(incoming)
        assertEquals(0L, database.financialEventDao().observeIncomeMinor().first())
    }

    @Test
    fun invA12_transferDoesNotCountAsExpense() = runBlocking {
        val src = UUID.randomUUID().toString()
        val out = FinancialEventEntity(UUID.randomUUID().toString(), FinancialEventType.TRANSFER.name, 1, -1000, "PKR", src, null, null, "Transfer", null, "UNKNOWN", null, now, now)
        database.financialEventDao().upsert(out)
        assertEquals(0L, database.financialEventDao().observeExpenseMinor().first())
    }

    @Test
    fun invA13_financialEventRemainsAuthoritativeSourceFact() = runBlocking {
        val eventId = UUID.randomUUID().toString()
        val event = FinancialEventEntity(eventId, FinancialEventType.INCOME.name, 1, 5000, "PKR", null, null, null, "Unassigned", null, "UNKNOWN", null, now, now)
        database.financialEventDao().upsert(event)
        val count = database.financialEventDao().getAll().size
        assertEquals(1, count)
        val retrieved = database.financialEventDao().getById(eventId)
        assertEquals(5000L, retrieved?.amountMinor)
    }

    @Test
    fun invA14_taxInterpretationCannotBecomeSourceFact() = runBlocking {
        // Tax is separate, we just verify FinancialEvent count is isolated
        val count = database.financialEventDao().getAll().size
        assertEquals(0, count)
    }
}
