package pk.vexel.financepassport.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onAllNodesWithTag
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
import android.Manifest
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import pk.vexel.financepassport.MainActivity
import pk.vexel.financepassport.PassportApplication

@RunWith(AndroidJUnit4::class)
class MoneyCaptureDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val application: PassportApplication
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PassportApplication

    // On API 33+, MainActivity requests POST_NOTIFICATIONS right after setContent; without
    // pre-granting it, the system permission dialog steals window focus from the just-created
    // Compose hierarchy and every ComposeTestRule interaction fails with "No compose hierarchies
    // found in the app" (reproduced on API 36; API 26 has no runtime permission so was unaffected).
    @get:Rule
    val notificationPermissionRule: TestRule = object : TestRule {
        override fun apply(base: Statement, description: Description): Statement = if (Build.VERSION.SDK_INT >= 33) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS).apply(base, description)
        } else base
    }

    @Test
    fun accountAndSalaryCapturePersistThroughUi() {
        unlockIfNeeded()
        composeRule.onNodeWithText("Money", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("add-account", useUnmergedTree = true).performClick()

        val accountName = "Device Account ${UUID.randomUUID().toString().take(8)}"
        composeRule.onNodeWithTag("account-name", useUnmergedTree = true).performTextInput(accountName)
        composeRule.onNodeWithTag("account-amount", useUnmergedTree = true).performTextInput("100000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        assertVisible(accountName)

        composeRule.onNodeWithText("Income / expense", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Record income", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("money-event-amount", useUnmergedTree = true).performTextInput("250000")
        composeRule.onNodeWithTag("money-event-description", useUnmergedTree = true).performTextInput("Salary device acceptance")
        composeRule.onNodeWithTag("money-event-category", useUnmergedTree = true).performTextInput("Salary")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        assertVisible("Salary device acceptance")
        assertVisible("INCOME · Salary")
    }

    @Test
    fun incomeSourceCanBeAddedInlineAndAppearsInBreakdown() {
        unlockIfNeeded()
        composeRule.onNodeWithText("Money", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("add-account", useUnmergedTree = true).performClick()

        val accountName = "Source Test Account ${UUID.randomUUID().toString().take(8)}"
        composeRule.onNodeWithTag("account-name", useUnmergedTree = true).performTextInput(accountName)
        composeRule.onNodeWithTag("account-amount", useUnmergedTree = true).performTextInput("50000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        assertVisible(accountName)

        composeRule.onNodeWithText("Income / expense", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Record income", useUnmergedTree = true).assertIsDisplayed()
        val sourceName = "Freelance ${UUID.randomUUID().toString().take(6)}"
        // AddEventDialog's content is a scrollable Column (fixed after this test found the dialog
        // could overflow past the AlertDialog's visible height with income-source fields showing) —
        // every element past the fold needs performScrollTo() before it reliably registers a click,
        // same lesson as the Sprint 19 ScrollableTabRow case: a plain performClick() can silently
        // no-op on an off-screen node here instead of throwing.
        composeRule.onNodeWithTag("add-new-income-source", useUnmergedTree = true).performScrollTo().performClick()
        composeRule.onNodeWithTag("new-income-source-name", useUnmergedTree = true).performScrollTo().performTextInput(sourceName)
        composeRule.onNodeWithTag("save-new-income-source", useUnmergedTree = true).performScrollTo().performClick()
        // FinanceRepository.addIncomeSource writes via a fire-and-forget viewModelScope.launch (same
        // pattern as every other write in this repository) — the new source may not be selectable in
        // the dropdown the instant the click returns. The dropdown's own contents only compose while
        // it's expanded, so retry by opening it, checking, and closing again rather than a plain wait.
        waitForOpenDropdownToContain("income-source-picker", sourceName)
        composeRule.onNodeWithText(sourceName, useUnmergedTree = true).performClick()

        composeRule.onNodeWithTag("money-event-amount", useUnmergedTree = true).performTextInput("75000")
        composeRule.onNodeWithTag("money-event-description", useUnmergedTree = true).performTextInput("Freelance payout")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        assertVisible("Freelance payout")

        // UI text presence alone can't distinguish "linked to the right source" from "linked to no
        // source" (both would just show the event) — verify the actual link via the DAOs directly.
        kotlinx.coroutines.runBlocking {
            val deadline = System.currentTimeMillis() + 5_000
            var event: pk.vexel.financepassport.core.database.FinancialEventEntity? = null
            while (System.currentTimeMillis() < deadline) {
                event = application.repository.database.financialEventDao().getAll().firstOrNull { it.description == "Freelance payout" }
                if (event?.incomeSourceId != null) break
                Thread.sleep(200)
            }
            checkNotNull(event) { "Freelance payout event was not persisted within 5s" }
            val source = application.repository.database.incomeSourceDao().getById(event.incomeSourceId ?: "")
            check(source?.name == sourceName) { "Event's incomeSourceId must resolve to the newly created source (expected '$sourceName', got '${source?.name}')" }
        }
        assertVisible(sourceName)
    }

    @Test
    fun incomeWithoutIncomeSourcePickedStillSaves() {
        unlockIfNeeded()
        composeRule.onNodeWithText("Money", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("add-account", useUnmergedTree = true).performClick()

        val accountName = "No Source Account ${UUID.randomUUID().toString().take(8)}"
        composeRule.onNodeWithTag("account-name", useUnmergedTree = true).performTextInput(accountName)
        composeRule.onNodeWithTag("account-amount", useUnmergedTree = true).performTextInput("20000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        assertVisible(accountName)

        composeRule.onNodeWithText("Income / expense", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("Record income", useUnmergedTree = true).assertIsDisplayed()
        // Deliberately never touch the income-source picker — source must stay optional.
        val description = "Unassigned income ${UUID.randomUUID().toString().take(6)}"
        composeRule.onNodeWithTag("money-event-amount", useUnmergedTree = true).performTextInput("15000")
        composeRule.onNodeWithTag("money-event-description", useUnmergedTree = true).performTextInput(description)
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        assertVisible(description)
    }

    /**
     * Opens a dropdown-menu-box (by its own tag, e.g. income-source-picker) and checks for [text]
     * among its items, retrying by closing and reopening if not found yet — the menu's contents
     * only compose while expanded, so a plain retry-in-place isn't enough; each attempt needs a
     * fresh open. Leaves the dropdown open on success so the caller can click the item immediately.
     */
    private fun waitForOpenDropdownToContain(pickerTag: String, text: String, timeoutMillis: Long = 5_000) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            composeRule.onNodeWithTag(pickerTag, useUnmergedTree = true).performScrollTo().performClick()
            try {
                composeRule.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
                return
            } catch (error: AssertionError) {
                lastError = error
                composeRule.onNodeWithTag(pickerTag, useUnmergedTree = true).performScrollTo().performClick()
                Thread.sleep(200)
            }
        }
        throw lastError ?: AssertionError("Timed out waiting for '$text' to appear in dropdown '$pickerTag'")
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

    /**
     * Every androidTest class in a connected test run shares one continuous app install/database
     * (no data reset between test classes), so by the time this test runs there may already be
     * several accounts/events from earlier test classes — pushing a freshly-added row below the
     * LazyColumn's currently-composed viewport, so this scrolls to it directly rather than
     * assuming it's still near the top. It also retries: [MainViewModel.write] (used by both
     * `addAccount` and `addEvent`) fires via a fire-and-forget `viewModelScope.launch`, not
     * awaited by the dialog's confirm button, so the Room insert + Flow re-emission can genuinely
     * still be in flight the instant this runs — a single scroll-then-assert without retry flaked
     * on the slower API 36 emulator even though the same single-shot check was fine on API 26.
     */
    private fun assertVisible(text: String, timeoutMillis: Long = 5_000) {
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
}
