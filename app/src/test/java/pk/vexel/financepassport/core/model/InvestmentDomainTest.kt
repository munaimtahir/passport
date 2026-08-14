package pk.vexel.financepassport.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import pk.vexel.financepassport.core.database.InvestmentEventEntity

class InvestmentDomainTest {
    @Test fun averageCostSellProducesTraceableGain() {
        val events = listOf(
            InvestmentEventEntity("b", "manual", "PSX Fund", "BUY", 1, 10, 1_000, 0, 0, "PKR"),
            InvestmentEventEntity("s", "manual", "PSX Fund", "SELL", 2, 5, 700, 0, 0, "PKR"),
        )
        val position = calculateInvestmentPosition("PSX Fund", events)
        assertEquals(5, position.quantityMinor)
        assertEquals(500, position.costBasisMinor)
        assertEquals(200, position.realizedGainLossMinor)
    }

    @Test fun dividendExcludesWithholdingFromRecordedIncome() {
        val event = InvestmentEventEntity("d", "manual", "PSX Fund", "DIVIDEND", 1, null, 1_000, 20, 100, "PKR")
        assertEquals(880, calculateInvestmentPosition("PSX Fund", listOf(event)).incomeMinor)
    }
}
