package pk.vexel.financepassport.core.reports

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import pk.vexel.financepassport.core.export.ExportSnapshot
import pk.vexel.financepassport.core.model.FinancialPosition
import pk.vexel.financepassport.core.model.PkrMoneyInput
import pk.vexel.financepassport.core.model.calculateFinancialPosition

data class FinancialReport(val title: String, val generatedAt: String, val lines: List<String>)

class ReportGenerator {
    private fun pkr(minor: Long) = PkrMoneyInput.formatMinorUnits(minor)

    /**
     * The same canonical financial position used by Home/Wealth (see [FinancialPosition]),
     * recomputed from a point-in-time [ExportSnapshot] instead of the live Flow so every report
     * agrees with the rest of the app rather than recalculating its own net worth.
     */
    fun canonicalPosition(snapshot: ExportSnapshot): FinancialPosition {
        val activeAccountIds = snapshot.accounts.filter { it.status == "ACTIVE" }.map { it.id }.toSet()
        val liveEvents = snapshot.events.filter { it.deletedAtEpochMillis == null }
        val openingBalanceMinor = snapshot.accounts.filter { it.id in activeAccountIds }.sumOf { it.openingBalanceMinor }
        val movementMinor = liveEvents.filter { it.accountId in activeAccountIds }.sumOf { if (it.eventType == "EXPENSE") -it.amountMinor else it.amountMinor }
        val incomeMinor = liveEvents.filter { it.eventType == "INCOME" }.sumOf { it.amountMinor }
        val expenseMinor = liveEvents.filter { it.eventType == "EXPENSE" }.sumOf { it.amountMinor }
        return calculateFinancialPosition(openingBalanceMinor, movementMinor, snapshot.assets, snapshot.liabilities, snapshot.investments, snapshot.receivables, incomeMinor, expenseMinor)
    }

    fun netWorth(snapshot: ExportSnapshot, generatedAt: String): FinancialReport {
        val position = canonicalPosition(snapshot)
        return FinancialReport(
            "Net Worth Statement",
            generatedAt,
            listOf(
                "Liquid funds: ${pkr(position.liquidFundsMinor)}",
                "Investments (cost basis): ${pkr(position.investmentsValueMinor)}",
                "Other assets: ${pkr(position.assetsValueMinor)}",
                "Receivables: ${pkr(position.receivablesValueMinor)}",
                "Total assets: ${pkr(position.totalAssetsMinor)}",
                "Liabilities: ${pkr(position.liabilitiesValueMinor)}",
                "Net worth: ${pkr(position.netWorthMinor)}",
            ),
        )
    }

    fun incomeExpense(snapshot: ExportSnapshot, generatedAt: String): FinancialReport {
        val liveEvents = snapshot.events.filter { it.deletedAtEpochMillis == null }
        val income = liveEvents.filter { it.eventType == "INCOME" }.sumOf { it.amountMinor }
        val expense = liveEvents.filter { it.eventType == "EXPENSE" }.sumOf { it.amountMinor }
        return FinancialReport("Income & Expense Report", generatedAt, listOf("Income: ${pkr(income)}", "Expense: ${pkr(expense)}", "Net movement: ${pkr(income - expense)}"))
    }

    fun assetStatement(snapshot: ExportSnapshot, generatedAt: String) = FinancialReport(
        "Asset Statement",
        generatedAt,
        snapshot.assets.flatMap { listOf("${it.title}: ${pkr(it.currentEstimatedValueMinor)}", "Source asset ${it.id}; acquisition ${pkr(it.acquisitionCostMinor)}; status ${it.status}") }.ifEmpty { listOf("No assets recorded") },
    )

    fun liabilityStatement(snapshot: ExportSnapshot, generatedAt: String) = FinancialReport(
        "Liability Statement",
        generatedAt,
        snapshot.liabilities.flatMap { listOf("${it.title}: ${pkr(it.outstandingAmountMinor)}", "Source liability ${it.id}; original ${pkr(it.originalAmountMinor)}; status ${it.status}") }.ifEmpty { listOf("No liabilities recorded") },
    )

    fun cashFlowSummary(snapshot: ExportSnapshot, generatedAt: String) = incomeExpense(snapshot, generatedAt).copy(title = "Cash Flow Summary")

    fun investmentSummary(snapshot: ExportSnapshot, generatedAt: String) = FinancialReport(
        "Investment Summary",
        generatedAt,
        listOf("Recorded cost basis (no live market price feed): ${pkr(canonicalPosition(snapshot).investmentsValueMinor)}") +
            snapshot.investments.groupBy { it.securityName }.flatMap { (security, events) -> listOf("$security: ${events.size} event(s), gross ${pkr(events.sumOf { it.grossAmountMinor })}", "Sources: ${events.joinToString { "${it.id} (${it.type})" }}") }.ifEmpty { listOf("No investment events recorded") },
    )

    fun receivablesReport(snapshot: ExportSnapshot, generatedAt: String) = FinancialReport(
        "Receivables Report",
        generatedAt,
        snapshot.receivables.flatMap { listOf("${it.title}: outstanding ${pkr(it.outstandingAmountMinor)}", "Source receivable ${it.id}; counterparty ${it.counterparty}; status ${it.status}") }.ifEmpty { listOf("No receivables recorded") },
    )

    fun annualFinancialSummary(snapshot: ExportSnapshot, generatedAt: String) = FinancialReport("Annual Financial Summary", generatedAt, netWorth(snapshot, generatedAt).lines + incomeExpense(snapshot, generatedAt).lines)

    fun taxPreparationSummary(snapshot: ExportSnapshot, generatedAt: String) = FinancialReport(
        "Tax Preparation Summary",
        generatedAt,
        listOf("Tax items: ${snapshot.taxItems.size}", "Gross captured: ${pkr(snapshot.taxItems.sumOf { it.grossAmountMinor ?: 0 })}", "Documents: ${snapshot.documents.size}") +
            snapshot.taxItems.map { "Source ${it.sourceType}/${it.sourceId}: ${it.taxEventType} ${pkr(it.grossAmountMinor ?: 0)} (${it.reviewState})" },
    )

    fun evidenceChecklist(snapshot: ExportSnapshot, generatedAt: String) = FinancialReport("Evidence Checklist", generatedAt, listOf("Documents attached: ${snapshot.documents.size}", "Tax items requiring review: ${snapshot.taxItems.count { it.evidenceState != "ATTACHED" }}"))

    fun writePdf(report: FinancialReport, output: OutputStream) {
        val document = PdfDocument()
        report.lines.chunked(26).ifEmpty { listOf(emptyList()) }.forEachIndexed { pageIndex, lines ->
            val page = document.startPage(PdfDocument.PageInfo.Builder(612, 792, pageIndex + 1).create())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 16f }
            page.canvas.drawText(report.title, 48f, 64f, paint)
            paint.textSize = 10f
            page.canvas.drawText("Generated ${report.generatedAt} · Page ${pageIndex + 1}", 48f, 84f, paint)
            lines.forEachIndexed { index, line -> page.canvas.drawText(line, 48f, 120f + index * 24f, paint) }
            document.finishPage(page)
        }
        document.writeTo(output)
        document.close()
    }
}
