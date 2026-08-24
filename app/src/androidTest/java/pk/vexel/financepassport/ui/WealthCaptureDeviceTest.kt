package pk.vexel.financepassport.ui

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
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
class WealthCaptureDeviceTest {
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
        scrollToAndAssertVisible(assetName)

        val liabilityName = "Device Liability ${UUID.randomUUID().toString().take(8)}"
        composeRule.onAllNodesWithText("Add", useUnmergedTree = true)[0].performClick()
        composeRule.onNodeWithText("LIAB", useUnmergedTree = true).performClick()
        val liabilityFields = composeRule.onAllNodes(hasSetTextAction())
        liabilityFields[0].performTextInput(liabilityName)
        liabilityFields[1].performTextInput("10000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        scrollToAndAssertVisible(liabilityName)
        composeRule.onNodeWithText("Recorded net wealth").assertIsDisplayed()
    }

    /**
     * Every androidTest class in a connected run shares one continuous app install/database, so
     * earlier test classes may have already populated several assets/liabilities — scroll to the
     * freshly-added one directly rather than assuming it's still near the top of the list. It also
     * retries: `MainViewModel.addAsset`/`addLiability` write via a fire-and-forget
     * `viewModelScope.launch`, not awaited by the dialog's confirm button, so the Room insert +
     * Flow re-emission can genuinely still be in flight the instant this runs.
     */
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
