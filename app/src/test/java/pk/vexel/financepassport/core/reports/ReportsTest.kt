package pk.vexel.financepassport.core.reports

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pk.vexel.financepassport.core.database.AssetEntity
import pk.vexel.financepassport.core.database.FinancialEventEntity
import pk.vexel.financepassport.core.database.InvestmentEventEntity
import pk.vexel.financepassport.core.database.TaxItemEntity
import pk.vexel.financepassport.core.export.ExportSnapshot

class ReportsTest {
    @Test fun netWorthReportUsesCanonicalAssetsAndLiabilities() {
        val snapshot = ExportSnapshot(emptyList(), emptyList(), listOf(AssetEntity("a", "OTHER", "Car", 1, 10000, 12000, "PKR", 100, null, null, "ACTIVE")), emptyList(), emptyList(), emptyList())
        assertTrue(ReportGenerator().netWorth(snapshot, "now").lines.any { it.contains("Net worth: PKR 120") })
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
        val event = FinancialEventEntity("in", "INCOME", 10, 100, "PKR", null, null, "In range", null, "UNKNOWN", null, 1, 1)
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
}
