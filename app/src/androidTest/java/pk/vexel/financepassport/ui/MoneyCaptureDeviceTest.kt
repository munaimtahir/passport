package pk.vexel.financepassport.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pk.vexel.financepassport.MainActivity

@RunWith(AndroidJUnit4::class)
class MoneyCaptureDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun accountAndSalaryCapturePersistThroughUi() {
        unlockIfNeeded()
        composeRule.onNodeWithText("Money", useUnmergedTree = true).performClick()
        composeRule.onNodeWithContentDescription("Add").performClick()

        val accountName = "Device Account ${UUID.randomUUID().toString().take(8)}"
        val accountFields = composeRule.onAllNodes(hasSetTextAction())
        accountFields[0].performTextInput(accountName)
        accountFields[1].performTextInput("100000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText(accountName).assertIsDisplayed()

        composeRule.onNodeWithText("Income / expense", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Record income", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("money-event-amount", useUnmergedTree = true).performTextInput("250000")
        composeRule.onNodeWithTag("money-event-description", useUnmergedTree = true).performTextInput("Salary device acceptance")
        composeRule.onNodeWithTag("money-event-category", useUnmergedTree = true).performTextInput("Salary")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        assertVisible("Salary device acceptance")
        assertVisible("INCOME · Salary")
    }

    private fun unlockIfNeeded() {
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

    private fun assertVisible(text: String) {
        composeRule.onNode(hasScrollAction(), useUnmergedTree = true)
            .performScrollToNode(hasText("Activity"))
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }
}
