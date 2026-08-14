package pk.vexel.financepassport.core.model

import java.time.LocalDate

/** Frequencies supported by recurring items; must stay in sync with the validation set in FinanceRepository.addRecurringItem. */
enum class RecurringFrequency { WEEKLY, MONTHLY, QUARTERLY, YEARLY }

/**
 * Advances [currentDueDate] to its next occurrence for [frequency].
 *
 * For monthly-family frequencies the day-of-month is clamped against [anchorDayOfMonth] (the
 * day the schedule was originally anchored to) rather than against [currentDueDate]'s own day,
 * so a rule anchored on the 31st lands on Feb 28/29 and then correctly returns to the 31st in a
 * 31-day month, instead of permanently drifting to the 28th once it has been clamped once.
 */
fun advanceRecurringDueDate(currentDueDate: LocalDate, frequency: RecurringFrequency, anchorDayOfMonth: Int): LocalDate {
    require(anchorDayOfMonth in 1..31) { "Anchor day of month must be between 1 and 31" }
    return when (frequency) {
        RecurringFrequency.WEEKLY -> currentDueDate.plusWeeks(1)
        RecurringFrequency.MONTHLY -> currentDueDate.plusMonths(1).clampToAnchor(anchorDayOfMonth)
        RecurringFrequency.QUARTERLY -> currentDueDate.plusMonths(3).clampToAnchor(anchorDayOfMonth)
        RecurringFrequency.YEARLY -> currentDueDate.plusYears(1).clampToAnchor(anchorDayOfMonth)
    }
}

private fun LocalDate.clampToAnchor(anchorDayOfMonth: Int): LocalDate =
    withDayOfMonth(anchorDayOfMonth.coerceAtMost(month.length(isLeapYear)))
