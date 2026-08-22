package pk.vexel.financepassport.core.model

import pk.vexel.financepassport.core.database.BudgetEntity
import pk.vexel.financepassport.core.database.FinancialEventEntity
import java.time.LocalDate
import java.time.YearMonth

data class CategoryBudgetStatus(
    val category: String,
    val limitMinor: Long,
    val spentMinor: Long,
    val percentUsed: Int,
    val isOverBudget: Boolean,
    val isNearThreshold: Boolean,
)

private const val NEAR_THRESHOLD_PERCENT = 85

/**
 * Deterministic per-category budget usage for [month]. Only EXPENSE events for the matching,
 * non-deleted category within the month count toward spend; a category with no active budget is omitted.
 */
fun calculateCategoryBudgets(budgets: List<BudgetEntity>, events: List<FinancialEventEntity>, month: YearMonth): List<CategoryBudgetStatus> {
    val monthStart = month.atDay(1).toEpochDay()
    val monthEnd = month.atEndOfMonth().toEpochDay()
    val spendByCategory = events
        .asSequence()
        .filter { it.eventType == "EXPENSE" && it.deletedAtEpochMillis == null }
        .filter { it.dateEpochDay in monthStart..monthEnd }
        .filter { it.category != null }
        .groupBy { it.category!! }
        .mapValues { (_, categoryEvents) -> categoryEvents.sumOf { it.amountMinor } }

    return budgets
        .filter { it.status == "ACTIVE" }
        .map { budget ->
            val spent = spendByCategory[budget.category] ?: 0L
            val percentUsed = if (budget.monthlyLimitMinor == 0L) 0 else ((spent * 100) / budget.monthlyLimitMinor).toInt()
            CategoryBudgetStatus(
                category = budget.category,
                limitMinor = budget.monthlyLimitMinor,
                spentMinor = spent,
                percentUsed = percentUsed,
                isOverBudget = spent > budget.monthlyLimitMinor,
                isNearThreshold = percentUsed in NEAR_THRESHOLD_PERCENT..100,
            )
        }
}

fun LocalDate.toYearMonth(): YearMonth = YearMonth.of(year, month)
