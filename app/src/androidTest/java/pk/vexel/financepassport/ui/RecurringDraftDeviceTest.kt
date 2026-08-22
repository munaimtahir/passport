package pk.vexel.financepassport.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pk.vexel.financepassport.MainActivity

@RunWith(AndroidJUnit4::class)
class RecurringDraftDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun recurringDraftCreatesReminderWithoutFinancialEvent() {
        unlockIfNeeded()
        composeRule.onNodeWithText("Money", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Recurring drafts", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add").performClick()
        val accountFields = composeRule.onAllNodes(hasSetTextAction())
        accountFields[0].performTextInput("Recurring Test Account")
        accountFields[1].performTextInput("100000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Recurring Test Account", useUnmergedTree = true).assertIsDisplayed()
        repeat(6) { composeRule.onNodeWithTag("money-list", useUnmergedTree = true).performTouchInput { swipeUp() } }
        composeRule.onNodeWithTag("add-recurring", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Add recurring draft", useUnmergedTree = true).assertIsDisplayed()

        val title = "Monthly draft ${UUID.randomUUID().toString().take(8)}"
        composeRule.onNodeWithTag("recurring-title", useUnmergedTree = true).performTextInput(title)
        composeRule.onNodeWithTag("recurring-amount", useUnmergedTree = true).performTextInput("3000")
        composeRule.onNodeWithTag("recurring-category", useUnmergedTree = true).performTextInput("Household")
        composeRule.onNodeWithTag("recurring-delay", useUnmergedTree = true).performTextInput("1")
        composeRule.onNodeWithText("Save draft", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.onNodeWithText("Next draft reminder:", substring = true).assertIsDisplayed()
    }

    private fun dismissOnboardingIfPresent() {
        while (composeRule.onAllNodesWithTag("onboarding-next").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("onboarding-next").performClick()
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
