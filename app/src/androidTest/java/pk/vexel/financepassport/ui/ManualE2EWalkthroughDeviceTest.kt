package pk.vexel.financepassport.ui

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import pk.vexel.financepassport.MainActivity

/**
 * Phase 10 manual-E2E-walkthrough item: chains onboarding through account capture, an income
 * event, wealth capture, manual tax capture, annual draft generation and report preview in one
 * continuous session on a real device/emulator process — no other test in this suite exercises
 * that full chain end to end (each other class covers one slice in isolation).
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

    @Test
    fun fullSessionWalkthroughFromOnboardingThroughReportPreview() {
        unlockIfNeeded()

        // Money: account + income event
        composeRule.onNodeWithText("Money", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("add-account", useUnmergedTree = true).performClick()
        val accountName = "E2E Account ${UUID.randomUUID().toString().take(8)}"
        composeRule.onNodeWithTag("account-name", useUnmergedTree = true).performTextInput(accountName)
        composeRule.onNodeWithTag("account-amount", useUnmergedTree = true).performTextInput("50000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        assertVisible(accountName)

        composeRule.onNodeWithText("Income / expense", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("money-event-amount", useUnmergedTree = true).performTextInput("25000")
        val incomeDescription = "E2E salary ${UUID.randomUUID().toString().take(6)}"
        composeRule.onNodeWithTag("money-event-description", useUnmergedTree = true).performTextInput(incomeDescription)
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        assertVisible(incomeDescription)

        // Wealth: asset + liability
        composeRule.onNodeWithText("Wealth", useUnmergedTree = true).performClick()
        val assetName = "E2E Asset ${UUID.randomUUID().toString().take(8)}"
        composeRule.onAllNodesWithText("Add", useUnmergedTree = true)[0].performClick()
        val assetFields = composeRule.onAllNodes(hasSetTextAction())
        assetFields[0].performTextInput(assetName)
        assetFields[1].performTextInput("30000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        assertVisible(assetName)

        val liabilityName = "E2E Liability ${UUID.randomUUID().toString().take(8)}"
        composeRule.onAllNodesWithText("Add", useUnmergedTree = true)[0].performClick()
        composeRule.onNodeWithTag("wealth-mode-LIABILITY", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("wealth-name", useUnmergedTree = true).performTextInput(liabilityName)
        composeRule.onNodeWithTag("wealth-amount", useUnmergedTree = true).performTextInput("5000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        assertVisible(liabilityName)

        // Tax & Records: manual tax item + annual draft
        composeRule.onNodeWithText("Tax & Records", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Add tax item", useUnmergedTree = true).performClick()
        val taxFields = composeRule.onAllNodes(hasSetTextAction())
        taxFields[0].performTextInput("OTHER_INCOME")
        val taxDescription = "E2E tax item ${UUID.randomUUID().toString().take(6)}"
        taxFields[1].performTextInput(taxDescription)
        taxFields[2].performTextInput("1000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        assertVisible(taxDescription)

        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasText("Prepare draft"))
        composeRule.onNodeWithText("Prepare draft", useUnmergedTree = true).performClick()
        assertVisible("draft v", substring = true)

        // Report preview (top app bar "More" menu) — dialog buttons aren't inside a scrollable
        // container, so use the non-scrolling waiter rather than assertVisible.
        composeRule.onNodeWithContentDescription("More", useUnmergedTree = true).performClick()
        waitAndClick("Preview net-worth report")
        waitFor("Export as PDF")
        // Both the "More" dialog and the report-preview dialog it opened have their own "Close"
        // button live in the tree at once (previewReport is local state inside MoreDialog); either
        // one dismisses the whole stack, so a single click here is sufficient.
        composeRule.onAllNodesWithText("Close", useUnmergedTree = true)[0].performClick()
    }

    private fun dismissOnboardingIfPresent() {
        while (composeRule.onAllNodesWithTag("onboarding-next").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("onboarding-next").performClick()
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

    /** See MoneyCaptureDeviceTest/WealthCaptureDeviceTest for why this retries: writes are fire-and-forget. */
    private fun assertVisible(text: String, timeoutMillis: Long = 8_000, substring: Boolean = false) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
                    .performScrollToNode(hasText(text, substring = substring))
                composeRule.onNode(hasText(text, substring = substring), useUnmergedTree = true).assertIsDisplayed()
                return
            } catch (error: Throwable) {
                lastError = error
                Thread.sleep(200)
            }
        }
        throw AssertionError("Timed out waiting for '$text' to appear", lastError)
    }

    /** For text not inside a scrollable container (e.g. dialog buttons/labels). */
    private fun waitFor(text: String, timeoutMillis: Long = 8_000) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                composeRule.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
                return
            } catch (error: Throwable) {
                lastError = error
                Thread.sleep(200)
            }
        }
        throw AssertionError("Timed out waiting for '$text' to appear", lastError)
    }

    private fun waitAndClick(text: String, timeoutMillis: Long = 8_000) {
        waitFor(text, timeoutMillis)
        composeRule.onNodeWithText(text, useUnmergedTree = true).performClick()
    }
}
