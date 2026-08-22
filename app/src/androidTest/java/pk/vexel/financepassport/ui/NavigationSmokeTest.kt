package pk.vexel.financepassport.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import android.Manifest
import android.os.Build
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.junit.rules.TestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pk.vexel.financepassport.MainActivity

@RunWith(AndroidJUnit4::class)
class NavigationSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val notificationPermissionRule: TestRule = object : TestRule {
        override fun apply(base: Statement, description: Description): Statement = if (Build.VERSION.SDK_INT >= 33) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS).apply(base, description)
        } else base
    }

    @Test
    fun launchShowsPassportSecurityOrApplicationSurface() {
        dismissOnboardingIfPresent()
        composeRule.onNodeWithText("Vexel Finance Passport").assertIsDisplayed()
    }

    @Test
    fun primaryDestinationsOpenTheirWorkspaces() {
        dismissOnboardingIfPresent()
        val createPin = composeRule.onAllNodesWithText("Create PIN").fetchSemanticsNodes().isNotEmpty()
        val pinFields = composeRule.onAllNodes(hasSetTextAction())
        if (createPin) {
            pinFields[0].performTextInput("1234")
            pinFields[1].performTextInput("1234")
            composeRule.onNodeWithText("Create PIN").performClick()
        } else if (composeRule.onAllNodesWithText("Unlock").fetchSemanticsNodes().isNotEmpty()) {
            pinFields[0].performTextInput("1234")
            composeRule.onNodeWithText("Unlock").performClick()
        }
        listOf("Money" to "Accounts", "Wealth" to "Recorded net wealth", "Tax & Records" to "Official records", "Vault" to "Evidence stays attached to structured records.").forEach { (label, heading) ->
            composeRule.onNodeWithText(label, useUnmergedTree = true).performClick()
            composeRule.onNodeWithText(heading, useUnmergedTree = true).assertIsDisplayed()
        }
        composeRule.onNodeWithContentDescription("More").assertIsDisplayed()
    }

    private fun dismissOnboardingIfPresent() {
        while (composeRule.onAllNodesWithTag("onboarding-next").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("onboarding-next").performClick()
        }
    }
}
