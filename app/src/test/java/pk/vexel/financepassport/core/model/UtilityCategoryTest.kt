package pk.vexel.financepassport.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UtilityCategoryTest {
    @Test fun legacyCategoriesMapToCanonicalTaxonomy() {
        assertEquals(UtilityCategory.MOBILE_TELEPHONE, UtilityCategory.fromStored("Telephone"))
        assertEquals(UtilityCategory.MOBILE_TELEPHONE, UtilityCategory.fromStored("mobile"))
        assertEquals(UtilityCategory.INTERNET, UtilityCategory.fromStored("Broadband"))
        assertEquals(UtilityCategory.OTHER, UtilityCategory.fromStored("Unknown custom service"))
    }

    @Test fun requiredCanonicalCategoriesAreSelectable() {
        assertEquals(
            listOf("Electricity", "Gas", "Water", "Internet", "Mobile / Telephone", "Subscription / Service", "Other"),
            UtilityCategory.selectable.map { it.label },
        )
    }
}
