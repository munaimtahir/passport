package pk.vexel.financepassport.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GoalMathTest {
    private val today = LocalDate.of(2026, 1, 1)

    @Test fun withoutTargetDateOnlyReportsPercent() {
        val progress = calculateGoalProgress(currentAmountMinor = 25_000, targetAmountMinor = 100_000, targetDateEpochDay = null, today = today)
        assertEquals(25, progress.progressPercent)
        assertNull(progress.monthsRemaining)
        assertNull(progress.requiredMonthlySavingsMinor)
        assertTrue(!progress.isAchieved)
    }

    @Test fun achievedGoalReportsNoRemainingWork() {
        val progress = calculateGoalProgress(currentAmountMinor = 100_000, targetAmountMinor = 100_000, targetDateEpochDay = today.toEpochDay(), today = today)
        assertEquals(100, progress.progressPercent)
        assertTrue(progress.isAchieved)
        assertNull(progress.requiredMonthlySavingsMinor)
    }

    @Test fun evenlyDivisibleRemainderSpreadsAcrossMonths() {
        val targetDate = today.plusMonths(6)
        val progress = calculateGoalProgress(currentAmountMinor = 0, targetAmountMinor = 120_000, targetDateEpochDay = targetDate.toEpochDay(), today = today)
        assertEquals(6, progress.monthsRemaining)
        assertEquals(20_000L, progress.requiredMonthlySavingsMinor)
    }

    @Test fun unevenRemainderRoundsUpSoTheGoalIsNeverUnderfunded() {
        val targetDate = today.plusMonths(3)
        val progress = calculateGoalProgress(currentAmountMinor = 0, targetAmountMinor = 100_000, targetDateEpochDay = targetDate.toEpochDay(), today = today)
        assertEquals(3, progress.monthsRemaining)
        assertEquals(33_334L, progress.requiredMonthlySavingsMinor) // ceiling of 100000/3
    }

    @Test fun overdueTargetDateRequiresTheFullRemainderImmediately() {
        val targetDate = today.minusYears(1)
        val progress = calculateGoalProgress(currentAmountMinor = 40_000, targetAmountMinor = 100_000, targetDateEpochDay = targetDate.toEpochDay(), today = today)
        assertEquals(0, progress.monthsRemaining)
        assertEquals(60_000L, progress.requiredMonthlySavingsMinor)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveTarget() {
        calculateGoalProgress(currentAmountMinor = 0, targetAmountMinor = 0, targetDateEpochDay = null, today = today)
    }
}
