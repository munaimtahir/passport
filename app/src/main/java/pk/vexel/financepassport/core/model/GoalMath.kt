package pk.vexel.financepassport.core.model

import java.time.LocalDate
import java.time.Period

data class GoalProgress(
    val progressPercent: Int,
    val monthsRemaining: Int?,
    val requiredMonthlySavingsMinor: Long?,
    val isAchieved: Boolean,
)

/**
 * Deterministic goal-progress calculation. When [targetDateEpochDay] is absent, only the
 * saved-vs-target percentage is meaningful; there is no deadline to derive a monthly figure from.
 */
fun calculateGoalProgress(
    currentAmountMinor: Long,
    targetAmountMinor: Long,
    targetDateEpochDay: Long?,
    today: LocalDate = LocalDate.now(),
): GoalProgress {
    require(targetAmountMinor > 0) { "Goal target amount must be positive" }
    require(currentAmountMinor >= 0) { "Goal current amount cannot be negative" }

    val progressPercent = ((currentAmountMinor.coerceAtMost(targetAmountMinor) * 100) / targetAmountMinor).toInt()
    val remaining = (targetAmountMinor - currentAmountMinor).coerceAtLeast(0)

    if (targetDateEpochDay == null || remaining == 0L) {
        return GoalProgress(progressPercent, null, null, remaining == 0L)
    }

    val targetDate = LocalDate.ofEpochDay(targetDateEpochDay)
    val monthsRemaining = Period.between(today, targetDate).let { it.years * 12 + it.months }.coerceAtLeast(0)
    // A goal due this month or overdue still needs the full remainder covered in one installment.
    val divisor = monthsRemaining.coerceAtLeast(1)
    val requiredMonthly = (remaining + divisor - 1) / divisor // ceiling division: never understate the required contribution

    return GoalProgress(progressPercent, monthsRemaining, requiredMonthly, false)
}
