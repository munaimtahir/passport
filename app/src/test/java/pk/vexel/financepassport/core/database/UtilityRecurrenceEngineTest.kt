package pk.vexel.financepassport.core.database

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class UtilityRecurrenceEngineTest {

    @Test
    fun testDateCalculationStandard() {
        // Issue day 15, Due day 27. August 2026.
        val (issueDate, dueDate) = UtilityRecurrenceEngine.calculateDates(2026, 8, 15, 27)
        assertEquals(LocalDate.of(2026, 8, 15), issueDate)
        assertEquals(LocalDate.of(2026, 8, 27), dueDate)
    }

    @Test
    fun testLeapYearClamping() {
        // Issue day 29 in Feb 2024 (Leap year)
        val (issueLeap, _) = UtilityRecurrenceEngine.calculateDates(2024, 2, 29, 29)
        assertEquals(LocalDate.of(2024, 2, 29), issueLeap)

        // Issue day 29 in Feb 2026 (Non-leap year)
        val (issueNonLeap, _) = UtilityRecurrenceEngine.calculateDates(2026, 2, 29, 29)
        assertEquals(LocalDate.of(2026, 2, 28), issueNonLeap)

        // Issue day 31 in Feb (clamped to end of month)
        val (issueClamped, _) = UtilityRecurrenceEngine.calculateDates(2026, 2, 31, 31)
        assertEquals(LocalDate.of(2026, 2, 28), issueClamped)
    }

    @Test
    fun testDueDayBeforeIssueDayRollover() {
        // Issue day 25, Due day 5. August 2026.
        // Due date must roll over to September 5th.
        val (issueDate, dueDate) = UtilityRecurrenceEngine.calculateDates(2026, 8, 25, 5)
        assertEquals(LocalDate.of(2026, 8, 25), issueDate)
        assertEquals(LocalDate.of(2026, 9, 5), dueDate)
    }

    @Test
    fun testDeriveStatusTransitions() {
        val issueDate = LocalDate.of(2026, 8, 15)
        val dueDate = LocalDate.of(2026, 8, 27)

        // Expected: today is before issue date
        assertEquals(
            "Expected",
            UtilityRecurrenceEngine.deriveStatus(issueDate, dueDate, isPaid = false, isSkipped = false, today = LocalDate.of(2026, 8, 10))
        )

        // Pending: today is on or after issue date, but before due-soon window (e.g. 18 Aug)
        assertEquals(
            "Pending",
            UtilityRecurrenceEngine.deriveStatus(issueDate, dueDate, isPaid = false, isSkipped = false, today = LocalDate.of(2026, 8, 18))
        )

        // Due soon: today is within 5 days of due date (e.g. 23 Aug)
        assertEquals(
            "Due soon",
            UtilityRecurrenceEngine.deriveStatus(issueDate, dueDate, isPaid = false, isSkipped = false, today = LocalDate.of(2026, 8, 23))
        )

        // Overdue: today is after due date (e.g. 28 Aug)
        assertEquals(
            "Overdue",
            UtilityRecurrenceEngine.deriveStatus(issueDate, dueDate, isPaid = false, isSkipped = false, today = LocalDate.of(2026, 8, 28))
        )

        // Paid: overrides all, even if overdue
        assertEquals(
            "Paid",
            UtilityRecurrenceEngine.deriveStatus(issueDate, dueDate, isPaid = true, isSkipped = false, today = LocalDate.of(2026, 8, 28))
        )

        // Skipped: overrides all, even if overdue
        assertEquals(
            "Skipped",
            UtilityRecurrenceEngine.deriveStatus(issueDate, dueDate, isPaid = false, isSkipped = true, today = LocalDate.of(2026, 8, 28))
        )
    }
}
