package pk.vexel.financepassport.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecurringScheduleTest {
    @Test fun weeklyAdvancesBySevenDays() {
        val next = advanceRecurringDueDate(LocalDate.of(2026, 1, 1), RecurringFrequency.WEEKLY, anchorDayOfMonth = 1)
        assertEquals(LocalDate.of(2026, 1, 8), next)
    }

    @Test fun monthlyClampsToMonthEndWhenAnchorExceedsMonthLength() {
        // 2026 is not a leap year, so a rule anchored on the 31st clamps to Feb 28.
        val next = advanceRecurringDueDate(LocalDate.of(2026, 1, 31), RecurringFrequency.MONTHLY, anchorDayOfMonth = 31)
        assertEquals(LocalDate.of(2026, 2, 28), next)
    }

    @Test fun monthlyReturnsToTheAnchorDayOnceAMonthAllowsItAgain() {
        // A rule that was clamped to Feb 28 must land back on the 31st in March, not drift permanently to the 28th.
        val afterFebruary = advanceRecurringDueDate(LocalDate.of(2026, 2, 28), RecurringFrequency.MONTHLY, anchorDayOfMonth = 31)
        assertEquals(LocalDate.of(2026, 3, 31), afterFebruary)
    }

    @Test fun quarterlyClampsToShorterMonth() {
        val next = advanceRecurringDueDate(LocalDate.of(2026, 1, 31), RecurringFrequency.QUARTERLY, anchorDayOfMonth = 31)
        assertEquals(LocalDate.of(2026, 4, 30), next)
    }

    @Test fun yearlyClampsLeapDayInNonLeapYear() {
        val next = advanceRecurringDueDate(LocalDate.of(2024, 2, 29), RecurringFrequency.YEARLY, anchorDayOfMonth = 29)
        assertEquals(LocalDate.of(2025, 2, 28), next)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidAnchorDay() {
        advanceRecurringDueDate(LocalDate.of(2026, 1, 1), RecurringFrequency.MONTHLY, anchorDayOfMonth = 32)
    }
}
