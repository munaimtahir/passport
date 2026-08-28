package pk.vexel.financepassport.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
import org.junit.Assert.assertTrue
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.junit.rules.TestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pk.vexel.financepassport.MainActivity

/**
 * Confirms Sprint 24's unified shell exposes Home / Bills / Money / History while later finance
 * workspaces remain outside primary navigation.
 */
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
    fun launchShowsTheUtilityBillTrackerShell() {
        unlockIfNeeded()
        composeRule.onNodeWithText("Vexel Finance Passport").assertIsDisplayed()
        composeRule.onNodeWithText("Dashboard").assertIsDisplayed()
    }

    @Test
    fun homeBillsMoneyAndHistoryAreReachableAndDeferredSurfacesAreAbsent() {
        unlockIfNeeded()

        composeRule.onNodeWithText("Bills", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Utility Connections").assertIsDisplayed()

        composeRule.onNodeWithText("Money", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("money-list").assertIsDisplayed()

        composeRule.onNodeWithText("History", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Global Bill & Payment History").assertIsDisplayed()

        composeRule.onNodeWithText("Home", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Dashboard").assertIsDisplayed()

        listOf("Wealth", "Tax & Records", "Tax", "Vault", "Net Worth", "Investments", "Loans", "Receivables", "Goals").forEach { hidden ->
            assertTrue(
                "'$hidden' must not be reachable from the main navigation",
                composeRule.onAllNodesWithText(hidden, useUnmergedTree = true).fetchSemanticsNodes().isEmpty(),
            )
        }
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
