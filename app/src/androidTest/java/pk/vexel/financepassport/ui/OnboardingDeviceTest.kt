package pk.vexel.financepassport.ui

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import pk.vexel.financepassport.MainActivity

/**
 * Covers the current utility-tracker onboarding flow: two informational pages, an optional PIN
 * step, and a "Get Started" page. Replaces a prior version of this file that tested a guided
 * account-setup step (bank/cash/investment account, major asset) that no longer exists — that
 * step belonged to the pre-reset full personal-finance app; onboarding is utility-only now.
 *
 * [skipPinReachesTheDashboardWithoutCreatingAPin] is a regression test for a real bug: skipping
 * PIN setup here used to still leave the user unable to relaunch without being forced into PIN
 * *creation* (SecurityGate relocked on every backgrounding regardless of whether a PIN existed,
 * and its own lock screen has no skip option) — see SecurityGate.kt's ON_STOP fix.
 */
@RunWith(AndroidJUnit4::class)
class OnboardingDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val notificationPermissionRule: TestRule = object : TestRule {
        override fun apply(base: Statement, description: Description): Statement = if (Build.VERSION.SDK_INT >= 33) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS).apply(base, description)
        } else base
    }

    @Test
    fun welcomeCopyDescribesAUtilityBillTracker() {
        skipIfOnboardingAlreadyComplete { return@skipIfOnboardingAlreadyComplete }
        composeRule.onNodeWithText("Welcome to Vexel Finance Passport").assertIsDisplayed()
        composeRule.onNodeWithText(
            "A private, offline-first monthly utility bill tracker.",
            substring = true,
        ).assertIsDisplayed()
    }

    @Test
    fun skipPinReachesTheDashboardWithoutCreatingAPin() {
        skipIfOnboardingAlreadyComplete { return@skipIfOnboardingAlreadyComplete }
        advanceToPinStep()
        composeRule.onNodeWithTag("setup-skip-pin").performClick()
        composeRule.onNodeWithTag("setup-start-empty").assertIsDisplayed()
        composeRule.onNodeWithTag("setup-start-empty").performClick()
        composeRule.onNodeWithText("Dashboard").assertIsDisplayed()

        // No PIN was ever created, so a fresh SecurityGate composition (what a real relaunch
        // produces) must not demand one. Activity#recreate() rebuilds the Compose hierarchy the
        // same way rotation/process-death restoration does.
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        assertTrue(composeRule.onAllNodesWithText("Create PIN").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Dashboard").assertIsDisplayed()
    }

    @Test
    fun creatingAPinDuringOnboardingRequiresItOnRelaunch() {
        skipIfOnboardingAlreadyComplete { return@skipIfOnboardingAlreadyComplete }
        advanceToPinStep()
        composeRule.onNodeWithTag("setup-pin").performTextInput("1234")
        composeRule.onNodeWithTag("setup-confirm-pin").performTextInput("1234")
        composeRule.onNodeWithTag("setup-create-pin").performClick()
        composeRule.onNodeWithTag("setup-start-empty").performClick()
        // A PIN now exists, so SecurityGate's `unlocked = !store.hasPin()` starts false the
        // instant onboarding hands off to it — this first unlock uses the PIN just created.
        composeRule.onNodeWithText("Unlock").assertIsDisplayed()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("1234")
        composeRule.onNodeWithText("Unlock").performClick()
        composeRule.onNodeWithText("Dashboard").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Unlock").assertIsDisplayed()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("1234")
        composeRule.onNodeWithText("Unlock").performClick()
        composeRule.onNodeWithText("Dashboard").assertIsDisplayed()
    }

    @Test
    fun backNavigationReturnsFromPinStepToInfoPages() {
        skipIfOnboardingAlreadyComplete { return@skipIfOnboardingAlreadyComplete }
        advanceToPinStep()
        composeRule.onAllNodesWithText("Back")[0].performClick()
        composeRule.onNodeWithTag("onboarding-title").assertIsDisplayed()
    }

    /** Advances through the two informational pages by tapping onboarding-next each time. */
    private fun advanceToPinStep() {
        var guard = 0
        while (composeRule.onAllNodesWithTag("onboarding-next").fetchSemanticsNodes().isNotEmpty() && guard < 10) {
            composeRule.onNodeWithTag("onboarding-next").performClick()
            guard += 1
        }
        composeRule.onNodeWithTag("onboarding-pin-title").assertIsDisplayed()
    }

    /**
     * Test methods within this class share one app install; only the first method to run reaches
     * real onboarding. Later methods short-circuit rather than asserting against a screen that
     * has already completed.
     */
    private inline fun skipIfOnboardingAlreadyComplete(onSkip: () -> Unit) {
        if (composeRule.onAllNodesWithTag("onboarding-title").fetchSemanticsNodes().isEmpty()) {
            onSkip()
        }
    }
}
