package pk.vexel.financepassport.core.reports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pk.vexel.financepassport.core.database.AccountEntity
import pk.vexel.financepassport.core.database.AssetEntity
import pk.vexel.financepassport.core.database.FinancialEventEntity
import pk.vexel.financepassport.core.database.InvestmentEventEntity
import pk.vexel.financepassport.core.database.TaxItemEntity
import pk.vexel.financepassport.core.export.ExportSnapshot
import pk.vexel.financepassport.core.model.calculateFinancialPosition

class ReportsTest {
    @Test fun netWorthReportUsesCanonicalAssetsAndLiabilities() {
        val snapshot = ExportSnapshot(emptyList(), emptyList(), listOf(AssetEntity("a", "OTHER", "Car", 1, 10000, 12000, "PKR", 100, null, null, "ACTIVE")), emptyList(), emptyList(), emptyList())
        assertTrue(ReportGenerator().netWorth(snapshot, "now").lines.any { it.contains("Net worth: PKR 120") })
    }

    @Test fun netWorthReportMatchesCanonicalFinancialPositionIndependently() {
        val account = AccountEntity("acc", "Bank", null, "SAVINGS", null, null, "PKR", 500_00, 1, "ACTIVE", null, 1, 1)
        val income = FinancialEventEntity("in", "INCOME", 10, 1_000_00, "PKR", "acc", null, null, "Salary", null, "UNKNOWN", null, 1, 1)
        val asset = AssetEntity("a", "OTHER", "Car", 1, 10_000_00, 12_000_00, "PKR", 100, null, null, "ACTIVE")
        val snapshot = ExportSnapshot(listOf(account), listOf(income), listOf(asset), emptyList(), emptyList(), emptyList())

        val expected = calculateFinancialPosition(account.openingBalanceMinor, income.amountMinor, snapshot.assets, snapshot.liabilities, snapshot.investments, snapshot.receivables, income.amountMinor, 0L)
        val report = ReportGenerator().netWorth(snapshot, "now")

        assertTrue(report.lines.any { it == "Net worth: PKR ${"%,d".format(expected.netWorthMinor / 100)}" })
        assertEquals(expected, ReportGenerator().canonicalPosition(snapshot))
    }

    @Test fun reportAmountsUseGroupedPkrFormattingNotRawDivision() {
        val asset = AssetEntity("a", "OTHER", "Property", 1, 1_000_000_00, 1_234_567_00, "PKR", 100, null, null, "ACTIVE")
        val snapshot = ExportSnapshot(emptyList(), emptyList(), listOf(asset), emptyList(), emptyList(), emptyList())
        val report = ReportGenerator().assetStatement(snapshot, "now")
        assertTrue(report.lines.any { it.contains("PKR 1,234,567") })
        assertTrue(report.lines.none { it.contains("123456700") })
    }

    @Test fun reportCatalogProducesTraceableSections() {
        val report = ReportGenerator()
        val snapshot = ExportSnapshot(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertTrue(report.assetStatement(snapshot, "now").title.contains("Asset"))
        assertTrue(report.liabilityStatement(snapshot, "now").title.contains("Liability"))
        assertTrue(report.cashFlowSummary(snapshot, "now").title.contains("Cash Flow"))
        assertTrue(report.investmentSummary(snapshot, "now").title.contains("Investment"))
        assertTrue(report.receivablesReport(snapshot, "now").title.contains("Receivable"))
        assertTrue(report.annualFinancialSummary(snapshot, "now").lines.isNotEmpty())
        assertTrue(report.taxPreparationSummary(snapshot, "now").lines.isNotEmpty())
        assertTrue(report.evidenceChecklist(snapshot, "now").lines.isNotEmpty())
    }

    @Test fun dateRangeFiltersCanonicalReportInputsWithoutMutatingSnapshot() {
        val event = FinancialEventEntity("in", "INCOME", 10, 100, "PKR", null, null, null, "In range", null, "UNKNOWN", null, 1, 1)
        val outside = event.copy(id = "out", dateEpochDay = 99, description = "Outside")
        val snapshot = ExportSnapshot(
            emptyList(), listOf(event, outside), emptyList(), emptyList(),
            listOf(TaxItemEntity("tax", "year", "manual", "source", "OTHER_INCOME", 10, 100, null, "PKR", "In range", "CAPTURED", "NONE", null, 1, 1)),
            emptyList(),
            listOf(InvestmentEventEntity("inv", "manual", "Security", "BUY", 10, 1, 100, 0, 0, "PKR")),
        )
        val ranged = snapshot.forDateRange(1, 20)
        assertEquals(1, ranged.events.size)
        assertEquals(1, ranged.taxItems.size)
        assertEquals(1, ranged.investments.size)
        assertEquals(2, snapshot.events.size)
    }

    @Test fun cashFlowUsesCashEffectsAndDoesNotCallTransfersExpenses() {
        fun event(id: String, type: String, amount: Long, cash: Long) = FinancialEventEntity(id = id, eventType = type, dateEpochDay = 1, amountMinor = amount, currency = "PKR", accountId = "a", contextId = null, category = null, description = id, notes = null, taxRelevance = "UNKNOWN", deletedAtEpochMillis = null, createdAtEpochMillis = 1, updatedAtEpochMillis = 1, cashEffectMinor = cash)
        val income = event("income", "INCOME", 1_000, 1_000)
        val transfer = event("transfer", "TRANSFER", -500, -500)
        val expense = event("expense", "EXPENSE", 200, -200)
        val report = ReportGenerator().cashFlowSummary(ExportSnapshot(emptyList(), listOf(income, transfer, expense), emptyList(), emptyList(), emptyList(), emptyList()), "now")
        assertTrue(report.lines.any { it.startsWith("Net cash movement: PKR 3") })
        assertTrue(report.lines.none { it.startsWith("Expense:") })
        assertTrue(report.lines.any { it.startsWith("Transfers excluded") })
    }
}
