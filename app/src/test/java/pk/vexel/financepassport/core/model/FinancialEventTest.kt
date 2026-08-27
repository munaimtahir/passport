package pk.vexel.financepassport.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FinancialEventTest {
    @Test fun transferDoesNotChangeIncomeOrExpenseTotals() {
        val events = listOf(
            FinancialEvent("income", FinancialEventType.INCOME, Money.pkr(1000), "a", 1, "Salary"),
            FinancialEvent("out", FinancialEventType.TRANSFER, Money.pkr(500), "a", 2, "Move"),
            FinancialEvent("in", FinancialEventType.TRANSFER, Money.pkr(500), "b", 2, "Move"),
        )
        TransferPair(events[1], events[2])
        assertEquals(Money.pkr(1000), events.incomeTotal())
        assertEquals(Money(MinorUnits(0)), events.expenseTotal())
    }
}
