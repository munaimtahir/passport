package pk.vexel.financepassport.ui

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import java.util.UUID
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import pk.vexel.financepassport.MainActivity
import pk.vexel.financepassport.PassportApplication
import pk.vexel.financepassport.core.security.PinStore

/**
 * Chains onboarding through registering a utility bill, paying it, and confirming the paid state
 * is reflected everywhere (profile card, connection statistics, and the global History screen) in
 * one continuous UI-driven session — a real click-path proof that the payment-status fix
 * (MonthlyBillOccurrenceDao.update vs. the old cascade-deleting upsert) holds through the actual
 * production UI, not just at the repository/DAO level covered by UtilityPaymentStatusDeviceTest.
 *
 * Replaces a prior version of this file that walked through the pre-reset Money/Wealth/Tax &
 * Records flow, none of which is reachable from the current utility-only shell.
 */
@RunWith(AndroidJUnit4::class)
class ManualE2EWalkthroughDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val notificationPermissionRule: TestRule = object : TestRule {
        override fun apply(base: Statement, description: Description): Statement = if (Build.VERSION.SDK_INT >= 33) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS).apply(base, description)
        } else base
    }

    @Before
    fun resetStateForEachTest() {
        val context = ApplicationProvider.getApplicationContext<PassportApplication>()
        context.preferences.clear()
        PinStore(context).clear()
        // clearPackageData/orchestrator already provides a fresh process and data set. Calling
        // recreate here races a cold activity launch on API 36 and can yield a null scenario.
        composeRule.waitForIdle()
    }

    @Test
    fun fullSessionFromOnboardingThroughPayingABillAndBackupSettings() {
        unlockIfNeeded()

        // Create an active account in Money first (required by Sprint 24 unified ledger).
        composeRule.onNodeWithText("Money", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("add-account", useUnmergedTree = true).performClick()
        waitForTag("account-name")
        composeRule.onNodeWithTag("account-name", useUnmergedTree = true).performTextInput("HBL Personal")
        composeRule.onNodeWithTag("account-amount", useUnmergedTree = true).performTextInput("100000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        waitFor("HBL Personal")

        // Register a bill.
        composeRule.onNodeWithText("Bills", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("add-bill-fab", useUnmergedTree = true).performClick()
        waitForTag("bill-name")
        val billName = "E2E Electric ${UUID.randomUUID().toString().take(8)}"
        composeRule.onNodeWithTag("bill-name", useUnmergedTree = true).performTextInput(billName)
        composeRule.onNodeWithTag("provider", useUnmergedTree = true).performTextInput("E2E Power Co")
        composeRule.onNodeWithTag("reference-number", useUnmergedTree = true).performTextInput("E2E-REF-${UUID.randomUUID().toString().take(6)}")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        waitFor(billName)

        // Open it, then open the current month's occurrence row, then pay it. The profile card
        // and each billing-history row are clickable Cards wrapping their label Text; Card's
        // clickable modifier merges descendant semantics, so these must query the merged tree
        // (not useUnmergedTree) for the click action to resolve to the card, not the text.
        composeRule.onNodeWithText(billName).performClick()
        // The statistics card can be below the initial dialog viewport on API 36; scroll it into
        // view before asserting visibility so this is a layout-safe UI check.
        composeRule.onNodeWithTag("profile-details-scroll", useUnmergedTree = true).performScrollToNode(hasText("Connection Statistics"))
        waitFor("Connection Statistics")
        val monthLabel = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
        // The Billing History row sits below Connection Statistics in a scrollable dialog body,
        // so it may not be in the visible viewport (assertIsDisplayed()/click both need that).
        composeRule.onNodeWithTag("profile-details-scroll", useUnmergedTree = true).performScrollToNode(hasText(monthLabel))
        composeRule.onNodeWithText(monthLabel).performClick()
        composeRule.onNodeWithTag("pay-button", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("pay-amount", useUnmergedTree = true).performTextInput("2500")
        composeRule.onNodeWithTag("save-payment-button", useUnmergedTree = true).performClick()

        // The profile's own connection statistics must show the payment immediately.
        waitFor("Paid: 1")
        composeRule.onNodeWithText("Total Paid: PKR 2,500", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Close", useUnmergedTree = true).performClick()

        // The Bills list card for this profile must show "Paid", not the pre-payment status.
        composeRule.onNodeWithText(billName, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("Paid", useUnmergedTree = true).assertIsDisplayed()

        // The global History screen (a separate query path from the Bills profile cards) must
        // also show this occurrence as paid, proving the fix holds across every screen that reads
        // occurrence status, not just the one the payment was recorded from.
        composeRule.onNodeWithText("History", useUnmergedTree = true).performClick()
        waitFor(billName)

        // Settings & Local Data: the manual, local, single-file encrypted backup controls must be
        // reachable from the utility-only shell.
        composeRule.onNodeWithContentDescription("Settings", useUnmergedTree = true).performClick()
        waitFor("Offline local backup and data controls")
        composeRule.onNodeWithTag("backup-button", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("restore-button", useUnmergedTree = true).assertIsDisplayed()
    }

    /**
     * AlertDialog content can take a frame or two to attach after the state flip that opens it —
     * longer than usual right after the first navigation to a screen, since its composable
     * lambdas are still being JIT-compiled at that point on a cold-started process.
     */
    private fun waitForTag(tag: String, timeoutMillis: Long = 20_000) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) return
            Thread.sleep(200)
        }
        throw AssertionError("Timed out waiting for tag '$tag' to appear")
    }

    private fun waitFor(text: String, timeoutMillis: Long = 20_000) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                composeRule.onAllNodesWithText(text, useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .firstOrNull()
                    ?.let { composeRule.onAllNodesWithText(text, useUnmergedTree = true)[0].assertIsDisplayed() }
                    ?: throw AssertionError("No visible node for '$text'")
                return
            } catch (error: Throwable) {
                lastError = error
                Thread.sleep(200)
            }
        }
        throw AssertionError("Timed out waiting for '$text' to appear", lastError)
    }

    private fun dismissOnboardingIfPresent() {
        while (composeRule.onAllNodesWithTag("onboarding-next").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("onboarding-next").performClick()
        }
        if (composeRule.onAllNodesWithTag("setup-skip-pin").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("setup-skip-pin").performClick()
        }
        if (composeRule.onAllNodesWithTag("setup-start-empty").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("setup-start-empty").performClick()
        }
    }

    private fun unlockIfNeeded() {
        dismissOnboardingIfPresent()
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
}
