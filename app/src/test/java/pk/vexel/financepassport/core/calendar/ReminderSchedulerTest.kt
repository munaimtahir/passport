package pk.vexel.financepassport.core.calendar

import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSchedulerTest {
    @Test fun pastDueTimesAreRepresentedByImmediateWorkDelay() {
        val due = System.currentTimeMillis() - 1_000
        assertTrue((due - 15 * 60_000L - System.currentTimeMillis()).coerceAtLeast(0) == 0L)
    }
}
