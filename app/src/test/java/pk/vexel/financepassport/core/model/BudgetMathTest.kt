package pk.vexel.financepassport.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pk.vexel.financepassport.core.database.BudgetEntity
import pk.vexel.financepassport.core.database.FinancialEventEntity
import java.time.LocalDate
import java.time.YearMonth

class BudgetMathTest {
    private val month = YearMonth.of(2026, 1)

    private fun budget(category: String, limitMinor: Long, status: String = "ACTIVE") =
        BudgetEntity("b-$category", category, limitMinor, "PKR", status, 0, 0)

    private fun expense(category: String?, amountMinor: Long, date: LocalDate = LocalDate.of(2026, 1, 15), deleted: Long? = null, type: String = "EXPENSE") =
        FinancialEventEntity("e-${category}-$amountMinor-${date}", type, date.toEpochDay(), amountMinor, "PKR", "acc", null, category, "desc", null, "NOT_RELEVANT", deleted, 0, 0)

    @Test fun computesPercentUsedForSpendWithinTheMonth() {
        val statuses = calculateCategoryBudgets(listOf(budget("Food", 10_000)), listOf(expense("Food", 4_000)), month)
        assertEquals(1, statuses.size)
        assertEquals(4_000L, statuses[0].spentMinor)
        assertEquals(40, statuses[0].percentUsed)
        assertTrue(!statuses[0].isOverBudget)
        assertTrue(!statuses[0].isNearThreshold)
    }

    @Test fun flagsNearThresholdAtEightyFivePercent() {
        val statuses = calculateCategoryBudgets(listOf(budget("Food", 10_000)), listOf(expense("Food", 8_500)), month)
        assertTrue(statuses[0].isNearThreshold)
        assertTrue(!statuses[0].isOverBudget)
    }

    @Test fun flagsOverBudgetWhenSpendExceedsLimit() {
        val statuses = calculateCategoryBudgets(listOf(budget("Food", 10_000)), listOf(expense("Food", 12_000)), month)
        assertTrue(statuses[0].isOverBudget)
        assertEquals(120, statuses[0].percentUsed)
    }

    @Test fun excludesEventsOutsideTheMonth() {
        val outside = expense("Food", 5_000, date = LocalDate.of(2026, 2, 1))
        val statuses = calculateCategoryBudgets(listOf(budget("Food", 10_000)), listOf(outside), month)
        assertEquals(0L, statuses[0].spentMinor)
    }

    @Test fun excludesDeletedEvents() {
        val deleted = expense("Food", 5_000, deleted = 123L)
        val statuses = calculateCategoryBudgets(listOf(budget("Food", 10_000)), listOf(deleted), month)
        assertEquals(0L, statuses[0].spentMinor)
    }

    @Test fun excludesNonExpenseEvents() {
        val income = expense("Food", 5_000, type = "INCOME")
        val statuses = calculateCategoryBudgets(listOf(budget("Food", 10_000)), listOf(income), month)
        assertEquals(0L, statuses[0].spentMinor)
    }

    @Test fun omitsInactiveBudgets() {
        val statuses = calculateCategoryBudgets(listOf(budget("Food", 10_000, status = "ARCHIVED")), listOf(expense("Food", 4_000)), month)
        assertTrue(statuses.isEmpty())
    }
}
