package pk.vexel.financepassport.core.model

import pk.vexel.financepassport.core.database.TaxItemEntity

/**
 * Workflow-completeness signals for the current set of tax items — never a statement of tax
 * correctness or filing readiness. Shared by Home and the Tax workspace so both surfaces agree.
 */
data class TaxReadiness(
    val totalItemCount: Int,
    val evidencePendingCount: Int,
    val unmappedCount: Int,
    val duplicateGroupCount: Int,
) {
    val evidenceResolvedCount: Int get() = totalItemCount - evidencePendingCount
}

fun calculateTaxReadiness(items: List<TaxItemEntity>): TaxReadiness = TaxReadiness(
    totalItemCount = items.size,
    evidencePendingCount = items.count { it.evidenceState == "NONE" || it.evidenceState == "REQUESTED" },
    unmappedCount = items.count { it.reviewState == "NEEDS_CLASSIFICATION" || it.taxEventType == "OTHER_TAX_EVENT" },
    duplicateGroupCount = items.groupBy { Triple(it.dateEpochDay, it.grossAmountMinor, it.currency) }.values.count { group -> group.size > 1 },
)
