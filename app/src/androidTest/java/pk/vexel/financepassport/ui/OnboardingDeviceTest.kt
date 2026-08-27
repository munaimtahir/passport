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
import java.util.UUID
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import pk.vexel.financepassport.MainActivity

/**
 * Covers the guided-setup step added to onboarding (Sprint/phase item: guided account setup),
 * and confirms the welcome copy leads with the personal-finance-diary framing rather than
 * co-equal tax-capture language. Every androidTest class in a connectedAndroidTest run shares
 * one continuous app install (no reset between classes, per the existing Test Orchestrator
 * setup) — these tests are written to only assert onboarding-specific behavior and never assume
 * they are the very first test to touch the app process, matching the pattern used elsewhere in
 * this suite (see MoneyCaptureDeviceTest's own comment on this).
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
    fun welcomeCopyLeadsWithFinanceDiaryNotCoEqualTaxCapture() {
        skipIfOnboardingAlreadyComplete { return@skipIfOnboardingAlreadyComplete }
        composeRule.onNodeWithText(
            "A private, offline-first personal finance diary: track daily expenses, bills, income, loans, receivables, savings and your overall net worth — all in one place, on this device. As a supporting benefit, it can also keep your records tax-ready automatically.",
            substring = true,
        ).assertIsDisplayed()
        assertTrue(
            composeRule.onAllNodesWithText("accounts, wealth, documents and continuous tax capture", substring = true)
                .fetchSemanticsNodes().isEmpty(),
        )
    }

    @Test
    fun startEmptyFinishesOnboardingWithNoAccountsOrAssetsCreated() {
        skipIfOnboardingAlreadyComplete { return@skipIfOnboardingAlreadyComplete }
        advanceToGuidedSetup()
        composeRule.onNodeWithTag("setup-start-empty").performClick()
        // Onboarding is complete: either PIN creation or the unlocked app shell is now shown,
        // never the setup screen itself.
        assertTrue(composeRule.onAllNodesWithTag("setup-start-empty").fetchSemanticsNodes().isEmpty())
        finishSecurityGateIfPresent()
        composeRule.onNodeWithText("Money", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Accounts", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun addBankAccountOptionPersistsARealAccount() {
        skipIfOnboardingAlreadyComplete { return@skipIfOnboardingAlreadyComplete }
        advanceToGuidedSetup()
        composeRule.onNodeWithTag("setup-bank").performClick()
        val accountName = "Onboarding Bank ${UUID.randomUUID().toString().take(8)}"
        composeRule.onNodeWithTag("setup-name").performTextInput(accountName)
        composeRule.onNodeWithTag("setup-amount").performTextInput("5000")
        composeRule.onNodeWithTag("setup-save").performClick()
        finishSecurityGateIfPresent()
        composeRule.onNodeWithText("Money", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText(accountName, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun backNavigationReturnsFromGuidedSetupToInfoPages() {
        skipIfOnboardingAlreadyComplete { return@skipIfOnboardingAlreadyComplete }
        advanceToGuidedSetup()
        composeRule.onAllNodesWithText("Back").let { nodes ->
            if (nodes.fetchSemanticsNodes().isNotEmpty()) nodes[0].performClick()
        }
        composeRule.onNodeWithTag("onboarding-title").assertIsDisplayed()
    }

    /** Advances through the three informational pages by tapping onboarding-next each time. */
    private fun advanceToGuidedSetup() {
        var guard = 0
        while (composeRule.onAllNodesWithTag("onboarding-next").fetchSemanticsNodes().isNotEmpty() && guard < 10) {
            composeRule.onNodeWithTag("onboarding-next").performClick()
            guard += 1
        }
    }

    private fun finishSecurityGateIfPresent() {
        if (composeRule.onAllNodesWithText("Create PIN").fetchSemanticsNodes().isNotEmpty()) {
            val fields = composeRule.onAllNodes(hasSetTextAction())
            fields[0].performTextInput("1234")
            fields[1].performTextInput("1234")
            composeRule.onNodeWithText("Create PIN").performClick()
        } else if (composeRule.onAllNodesWithText("Unlock").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("1234")
            composeRule.onNodeWithText("Unlock").performClick()
        }
    }

    /**
     * Because androidTest classes share one app install in a full connectedAndroidTest run,
     * onboarding may already be complete by the time this class runs if an earlier test class
     * finished it first. These tests only exercise real onboarding behavior — if onboarding is
     * already done, [onSkip] short-circuits the test rather than asserting against a screen that
     * can no longer be reached (this class is still meaningful in isolation, e.g. `--tests
     * "*Onboarding*"` against a fresh install, which is how it is expected to be run first).
     */
    private inline fun skipIfOnboardingAlreadyComplete(onSkip: () -> Unit) {
        if (composeRule.onAllNodesWithTag("onboarding-title").fetchSemanticsNodes().isEmpty()) {
            onSkip()
        }
    }
}
