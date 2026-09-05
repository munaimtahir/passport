package pk.vexel.financepassport.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import pk.vexel.financepassport.core.database.AssetEntity
import pk.vexel.financepassport.core.database.InvestmentEventEntity
import pk.vexel.financepassport.core.database.LiabilityEntity
import pk.vexel.financepassport.core.database.ReceivableEntity
import pk.vexel.financepassport.core.database.SimpleInvestmentEntity

class FinancialPositionTest {
    @Test fun deterministicFixtureProducesExpectedNetWorth() {
        // Liquid funds: opening 500_00 + net movement 120_00 = 620_00
        val assets = listOf(AssetEntity("a1", "PROPERTY", "House", 1, 10_000_00, 12_000_00, "PKR", 100, null, null, "ACTIVE"))
        val liabilities = listOf(LiabilityEntity("l1", "LOAN", "Car loan", "Bank", 5_000_00, 3_000_00, "PKR", 1, null, "ACTIVE"))
        val investments = listOf(
            InvestmentEventEntity("i1", "broker-1", "PSX Fund", "BUY", 1, 10, 1_000_00, 0, 0, "PKR"),
            InvestmentEventEntity("i2", "broker-1", "PSX Fund", "SELL", 2, 4, 500_00, 0, 0, "PKR"),
        )
        val receivables = listOf(ReceivableEntity("r1", "Loan to friend", "Ali", 200_00, 150_00, null, "OPEN"))

        val position = calculateFinancialPosition(
            accountsOpeningBalanceMinor = 500_00,
            accountsMovementMinor = 120_00,
            assets = assets,
            liabilities = liabilities,
            investments = investments,
            receivables = receivables,
            monthlyIncomeMinor = 300_00,
            monthlyExpenseMinor = 100_00,
        )

        // Investment cost basis after selling 4 of 10 units bought at 1_000_00: remaining 6 units at proportional cost = 600_00
        assertEquals(620_00L, position.liquidFundsMinor)
        assertEquals(600_00L, position.investmentsValueMinor)
        assertEquals(12_000_00L, position.assetsValueMinor)
        assertEquals(150_00L, position.receivablesValueMinor)
        assertEquals(3_000_00L, position.liabilitiesValueMinor)
        assertEquals(620_00L + 600_00L + 12_000_00L + 150_00L, position.totalAssetsMinor)
        assertEquals(position.totalAssetsMinor - 3_000_00L, position.netWorthMinor)
        assertEquals(300_00L, position.monthlyIncomeMinor)
        assertEquals(100_00L, position.monthlyExpenseMinor)
    }

    @Test fun emptyPortfolioIsZeroNetWorth() {
        val position = calculateFinancialPosition(0, 0, emptyList(), emptyList(), emptyList(), emptyList(), 0, 0)
        assertEquals(0L, position.netWorthMinor)
    }

    @Test fun excludedAndPartiallyOwnedAssetsUseDerivedContribution() {
        val assets = listOf(
            AssetEntity("included", "PROPERTY", "Shared home", 1, 20_000_000, 20_000_000, "PKR", 50, null, null, "ACTIVE"),
            AssetEntity("excluded", "VEHICLE", "Private vehicle", 1, 2_000_000, 2_000_000, "PKR", 100, null, null, "ACTIVE", includeInNetWorth = false),
        )
        val position = calculateFinancialPosition(0, 0, assets, emptyList(), emptyList(), emptyList(), 0, 0)
        assertEquals(10_000_000L, position.assetsValueMinor)
    }

    @Test fun simpleInvestmentValueIsIncludedWithoutIncomeOrExpenseDoubleCounting() {
        val simple = listOf(SimpleInvestmentEntity("td", "Term Deposit", "TERM_DEPOSIT", acquisitionDateEpochDay = 1, principalInvestedMinor = 500_000, currentEstimatedValueMinor = 525_000))
        val position = calculateFinancialPosition(1_000_000, 0, emptyList(), emptyList(), emptyList(), emptyList(), 0, 0, simple)
        assertEquals(525_000L, position.investmentsValueMinor)
        assertEquals(1_525_000L, position.netWorthMinor)
        assertEquals(0L, position.monthlyIncomeMinor)
        assertEquals(0L, position.monthlyExpenseMinor)
    }
}
