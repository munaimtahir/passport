package pk.vexel.financepassport.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import pk.vexel.financepassport.core.database.TaxItemEntity

class TaxReadinessTest {
    private fun item(
        id: String,
        dateEpochDay: Long = 1,
        grossAmountMinor: Long? = 1_000_00,
        currency: String = "PKR",
        reviewState: String = "REVIEWED",
        evidenceState: String = "ATTACHED",
        taxEventType: String = "EMPLOYMENT_INCOME",
    ) = TaxItemEntity(id, "ty-1", "FINANCIAL_EVENT", "src-$id", taxEventType, dateEpochDay, grossAmountMinor, null, currency, "desc", reviewState, evidenceState, null, 0, 0)

    @Test fun countsEvidencePendingUnmappedAndDuplicateGroups() {
        val items = listOf(
            item("1", dateEpochDay = 1, grossAmountMinor = 100_00, evidenceState = "NONE"),
            item("2", dateEpochDay = 2, grossAmountMinor = 200_00, evidenceState = "REQUESTED"),
            item("3", dateEpochDay = 3, grossAmountMinor = 300_00, evidenceState = "ATTACHED"),
            item("4", dateEpochDay = 4, grossAmountMinor = 400_00, reviewState = "NEEDS_CLASSIFICATION"),
            item("5", dateEpochDay = 5, grossAmountMinor = 500_00, taxEventType = "OTHER_TAX_EVENT"),
            item("6", dateEpochDay = 10, grossAmountMinor = 500_00),
            item("7", dateEpochDay = 10, grossAmountMinor = 500_00),
        )

        val readiness = calculateTaxReadiness(items)

        assertEquals(7, readiness.totalItemCount)
        assertEquals(2, readiness.evidencePendingCount)
        assertEquals(5, readiness.evidenceResolvedCount)
        assertEquals(2, readiness.unmappedCount)
        assertEquals(1, readiness.duplicateGroupCount)
    }

    @Test fun emptyListIsFullyResolved() {
        val readiness = calculateTaxReadiness(emptyList())
        assertEquals(0, readiness.totalItemCount)
        assertEquals(0, readiness.evidencePendingCount)
        assertEquals(0, readiness.unmappedCount)
        assertEquals(0, readiness.duplicateGroupCount)
    }
}
