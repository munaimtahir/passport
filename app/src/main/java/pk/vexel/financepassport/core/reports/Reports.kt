package pk.vexel.financepassport.core.reports

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import pk.vexel.financepassport.core.export.ExportSnapshot

data class FinancialReport(val title: String, val generatedAt: String, val lines: List<String>)

class ReportGenerator {
    fun netWorth(snapshot: ExportSnapshot, generatedAt: String): FinancialReport {
        val assets = snapshot.assets.sumOf { it.currentEstimatedValueMinor }
        val liabilities = snapshot.liabilities.sumOf { it.outstandingAmountMinor }
        return FinancialReport("Net Worth Statement", generatedAt, listOf("Assets: PKR ${assets / 100}", "Liabilities: PKR ${liabilities / 100}", "Net worth: PKR ${(assets - liabilities) / 100}"))
    }

    fun incomeExpense(snapshot: ExportSnapshot, generatedAt: String): FinancialReport {
        val income = snapshot.events.filter { it.eventType == "INCOME" }.sumOf { it.amountMinor }
        val expense = snapshot.events.filter { it.eventType == "EXPENSE" }.sumOf { it.amountMinor }
        return FinancialReport("Income & Expense Report", generatedAt, listOf("Income: PKR ${income / 100}", "Expense: PKR ${expense / 100}", "Net movement: PKR ${(income - expense) / 100}"))
    }

    fun assetStatement(snapshot: ExportSnapshot, generatedAt: String) = FinancialReport("Asset Statement", generatedAt, snapshot.assets.flatMap { listOf("${it.title}: PKR ${it.currentEstimatedValueMinor / 100}", "Source asset ${it.id}; acquisition PKR ${it.acquisitionCostMinor / 100}; status ${it.status}") }.ifEmpty { listOf("No assets recorded") })
    fun liabilityStatement(snapshot: ExportSnapshot, generatedAt: String) = FinancialReport("Liability Statement", generatedAt, snapshot.liabilities.flatMap { listOf("${it.title}: PKR ${it.outstandingAmountMinor / 100}", "Source liability ${it.id}; original PKR ${it.originalAmountMinor / 100}; status ${it.status}") }.ifEmpty { listOf("No liabilities recorded") })
    fun cashFlowSummary(snapshot: ExportSnapshot, generatedAt: String) = incomeExpense(snapshot, generatedAt).copy(title = "Cash Flow Summary")
    fun investmentSummary(snapshot: ExportSnapshot, generatedAt: String) = FinancialReport("Investment Summary", generatedAt, snapshot.investments.groupBy { it.securityName }.flatMap { (security, events) -> listOf("$security: ${events.size} event(s), gross PKR ${events.sumOf { it.grossAmountMinor } / 100}", "Sources: ${events.joinToString { "${it.id} (${it.type})" }}") }.ifEmpty { listOf("No investment events recorded") })
    fun receivablesReport(snapshot: ExportSnapshot, generatedAt: String) = FinancialReport("Receivables Report", generatedAt, snapshot.receivables.flatMap { listOf("${it.title}: outstanding PKR ${it.outstandingAmountMinor / 100}", "Source receivable ${it.id}; counterparty ${it.counterparty}; status ${it.status}") }.ifEmpty { listOf("No receivables recorded") })
    fun annualFinancialSummary(snapshot: ExportSnapshot, generatedAt: String) = FinancialReport("Annual Financial Summary", generatedAt, netWorth(snapshot, generatedAt).lines + incomeExpense(snapshot, generatedAt).lines)
    fun taxPreparationSummary(snapshot: ExportSnapshot, generatedAt: String) = FinancialReport("Tax Preparation Summary", generatedAt, listOf("Tax items: ${snapshot.taxItems.size}", "Gross captured: PKR ${snapshot.taxItems.sumOf { it.grossAmountMinor ?: 0 } / 100}", "Documents: ${snapshot.documents.size}") + snapshot.taxItems.map { "Source ${it.sourceType}/${it.sourceId}: ${it.taxEventType} PKR ${(it.grossAmountMinor ?: 0) / 100} (${it.reviewState})" })
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
