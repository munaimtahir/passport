package pk.vexel.financepassport.core.model

import java.time.LocalDate

enum class RecurringAmountMode { FIXED, VARIABLE }

enum class RecurringLifecycle { UPCOMING, DUE, OVERDUE, CONFIRMED, SKIPPED }

data class RecurringTemplate(
    val id: String,
    val title: String,
    val eventType: FinancialEventType,
    val amountMode: RecurringAmountMode,
    val expectedAmountMinor: Long?,
    val frequency: RecurringFrequency,
    val intervalCount: Int = 1,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val defaultAccountId: String? = null,
    val defaultContextId: String? = null,
    val defaultCategoryId: String? = null,
)

data class ExpectedOccurrence(
    val id: String,
    val templateId: String,
    val dueDate: LocalDate,
    val expectedAmountMinor: Long?,
    val status: RecurringLifecycle = RecurringLifecycle.UPCOMING,
    val confirmedEventId: String? = null,
)

fun RecurringTemplate.validate() {
    require(title.isNotBlank()) { "Recurring title is required" }
    require(intervalCount > 0) { "Recurring interval must be positive" }
    require(expectedAmountMinor == null || expectedAmountMinor > 0) { "Expected amount must be positive" }
    require(amountMode == RecurringAmountMode.VARIABLE || expectedAmountMinor != null) {
        "Fixed recurring entries require an expected amount"
    }
    require(endDate == null || !endDate.isBefore(startDate)) { "Recurring end date precedes start date" }
}

fun RecurringTemplate.firstOccurrence(): ExpectedOccurrence {
    validate()
    return ExpectedOccurrence(
        id = "$id:${startDate.toEpochDay()}",
        templateId = id,
        dueDate = startDate,
        expectedAmountMinor = expectedAmountMinor.takeIf { amountMode == RecurringAmountMode.FIXED },
    )
}

fun RecurringTemplate.nextOccurrence(current: ExpectedOccurrence): ExpectedOccurrence? {
    require(current.templateId == id) { "Occurrence belongs to another template" }
    val nextDate = advanceRecurringDueDate(current.dueDate, frequency, current.dueDate.dayOfMonth, intervalCount)
    if (endDate != null && nextDate.isAfter(endDate)) return null
    return ExpectedOccurrence(
        id = "$id:${nextDate.toEpochDay()}",
        templateId = id,
        dueDate = nextDate,
        expectedAmountMinor = expectedAmountMinor.takeIf { amountMode == RecurringAmountMode.FIXED },
    )
}

fun advanceRecurringDueDate(
    currentDueDate: LocalDate,
    frequency: RecurringFrequency,
    anchorDayOfMonth: Int,
    intervalCount: Int,
): LocalDate {
    require(intervalCount > 0) { "Recurring interval must be positive" }
    require(anchorDayOfMonth in 1..31) { "Anchor day of month must be between 1 and 31" }
    return when (frequency) {
        RecurringFrequency.WEEKLY -> currentDueDate.plusWeeks(intervalCount.toLong())
        RecurringFrequency.MONTHLY -> currentDueDate.plusMonths(intervalCount.toLong()).clampToAnchor(anchorDayOfMonth)
        RecurringFrequency.QUARTERLY -> currentDueDate.plusMonths(3L * intervalCount).clampToAnchor(anchorDayOfMonth)
        RecurringFrequency.YEARLY -> currentDueDate.plusYears(intervalCount.toLong()).clampToAnchor(anchorDayOfMonth)
    }
}

private fun LocalDate.clampToAnchor(anchorDayOfMonth: Int): LocalDate =
    withDayOfMonth(anchorDayOfMonth.coerceAtMost(month.length(isLeapYear)))
