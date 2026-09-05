package pk.vexel.financepassport.core.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurringDomainTest {
    @Test fun variableOccurrenceCarriesNoInventedAmount() {
        val template = RecurringTemplate("t", "Clinic", FinancialEventType.INCOME, RecurringAmountMode.VARIABLE, null, RecurringFrequency.MONTHLY, startDate = LocalDate.of(2026, 2, 1))
        assertNull(template.firstOccurrence().expectedAmountMinor)
    }

    @Test fun repeatedMonthlyScheduleHonorsIntervalAndEndDate() {
        val template = RecurringTemplate("t", "Rent", FinancialEventType.EXPENSE, RecurringAmountMode.FIXED, 100_00, RecurringFrequency.MONTHLY, intervalCount = 2, startDate = LocalDate.of(2026, 1, 31), endDate = LocalDate.of(2026, 5, 31))
        val first = template.firstOccurrence()
        val second = template.nextOccurrence(first)
        assertEquals(LocalDate.of(2026, 3, 31), second?.dueDate)
        val third = second?.let(template::nextOccurrence)
        assertEquals(LocalDate.of(2026, 5, 31), third?.dueDate)
        assertNull(third?.let(template::nextOccurrence))
    }

    @Test(expected = IllegalArgumentException::class)
    fun fixedTemplateWithoutAmountIsRejected() {
        RecurringTemplate("t", "Rent", FinancialEventType.EXPENSE, RecurringAmountMode.FIXED, null, RecurringFrequency.MONTHLY, startDate = LocalDate.now()).validate()
    }
}
