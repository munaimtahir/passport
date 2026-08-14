package pk.vexel.financepassport.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pk.vexel.financepassport.MainActivity

@RunWith(AndroidJUnit4::class)
class WealthCaptureDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun assetAndLiabilityCaptureUpdateNetWealthSurface() {
        unlockIfNeeded()
        composeRule.onNodeWithText("Wealth", useUnmergedTree = true).performClick()

        val assetName = "Device Asset ${UUID.randomUUID().toString().take(8)}"
        composeRule.onAllNodesWithText("Add", useUnmergedTree = true).fetchSemanticsNodes().first()
        composeRule.onAllNodesWithText("Add", useUnmergedTree = true)[0].performClick()
        val assetFields = composeRule.onAllNodes(hasSetTextAction())
        assetFields[0].performTextInput(assetName)
        assetFields[1].performTextInput("50000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText(assetName).assertIsDisplayed()

        val liabilityName = "Device Liability ${UUID.randomUUID().toString().take(8)}"
        composeRule.onAllNodesWithText("Add", useUnmergedTree = true)[0].performClick()
        composeRule.onNodeWithText("LIAB", useUnmergedTree = true).performClick()
        val liabilityFields = composeRule.onAllNodes(hasSetTextAction())
        liabilityFields[0].performTextInput(liabilityName)
        liabilityFields[1].performTextInput("10000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText(liabilityName).assertIsDisplayed()
        composeRule.onNodeWithText("Recorded net wealth").assertIsDisplayed()
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
}
