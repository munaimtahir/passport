package pk.vexel.financepassport.ui

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assertIsDisplayed
import android.Manifest
import android.os.Build
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

@RunWith(AndroidJUnit4::class)
class RecurringDraftDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // See MoneyCaptureDeviceTest for why this is needed on API 33+ (POST_NOTIFICATIONS system
    // dialog otherwise steals focus from the Compose hierarchy right after MainActivity launches).
    @get:Rule
    val notificationPermissionRule: TestRule = object : TestRule {
        override fun apply(base: Statement, description: Description): Statement = if (Build.VERSION.SDK_INT >= 33) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS).apply(base, description)
        } else base
    }

    @Test
    fun recurringDraftCreatesReminderWithoutFinancialEvent() {
        unlockIfNeeded()
        composeRule.onNodeWithText("Money", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Recurring drafts", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add").performClick()
        composeRule.onNodeWithTag("account-name", useUnmergedTree = true).performTextInput("Recurring Test Account")
        composeRule.onNodeWithTag("account-amount", useUnmergedTree = true).performTextInput("100000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        // Every androidTest class in a connected run shares one continuous app install/database,
        // so earlier test classes may have already added accounts — scroll to the new one directly
        // rather than assuming a short, fixed-length list, and retry: MainViewModel.addAccount
        // writes via a fire-and-forget viewModelScope.launch, not awaited by the dialog's confirm
        // button, so the Room insert + Flow re-emission can genuinely still be in flight here.
        scrollToAndAssertVisible("Recurring Test Account")
        composeRule.onNode(hasScrollAction(), useUnmergedTree = true).performScrollToNode(hasTestTag("add-recurring"))
        composeRule.onNodeWithTag("add-recurring", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Add recurring draft", useUnmergedTree = true).assertIsDisplayed()

        val title = "Monthly draft ${UUID.randomUUID().toString().take(8)}"
        composeRule.onNodeWithTag("recurring-title", useUnmergedTree = true).performTextInput(title)
        composeRule.onNodeWithTag("recurring-amount", useUnmergedTree = true).performTextInput("3000")
        composeRule.onNodeWithTag("recurring-category", useUnmergedTree = true).performTextInput("Household")
        composeRule.onNodeWithTag("recurring-delay", useUnmergedTree = true).performTextInput("1")
        composeRule.onNodeWithText("Save draft", useUnmergedTree = true).performClick()
        // MainViewModel.addRecurringItem also writes via a fire-and-forget viewModelScope.launch,
        // not awaited by the dialog's confirm button — same race as the account save above.
        scrollToAndAssertVisible(title)
        composeRule.onNodeWithText("Next draft reminder:", substring = true).assertIsDisplayed()
    }

    private fun scrollToAndAssertVisible(text: String, timeoutMillis: Long = 5_000) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                composeRule.onNode(hasScrollAction(), useUnmergedTree = true).performScrollToNode(hasText(text))
                composeRule.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
                return
            } catch (error: AssertionError) {
                lastError = error
                Thread.sleep(200)
            }
        }
        throw lastError ?: AssertionError("Timed out waiting for '$text' to appear")
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
}
