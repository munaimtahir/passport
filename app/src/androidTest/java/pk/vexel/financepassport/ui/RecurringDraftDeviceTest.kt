package pk.vexel.financepassport.ui

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
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import android.Manifest
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.rules.TestRule
import org.junit.runners.model.Statement
import pk.vexel.financepassport.MainActivity
import pk.vexel.financepassport.PassportApplication

@RunWith(AndroidJUnit4::class)
class RecurringDraftDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val application: PassportApplication
        get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PassportApplication

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
        composeRule.onNodeWithText("Bills & Recurring", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithTag("add-account", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("account-name", useUnmergedTree = true).performTextInput("Recurring Test Account")
        composeRule.onNodeWithTag("account-amount", useUnmergedTree = true).performTextInput("100000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        // Every androidTest class in a connected run shares one continuous app install/database,
        // so earlier test classes may have already added accounts — scroll to the new one directly
        // rather than assuming a short, fixed-length list, and retry: MainViewModel.addAccount
        // writes via a fire-and-forget viewModelScope.launch, not awaited by the dialog's confirm
        // button, so the Room insert + Flow re-emission can genuinely still be in flight here.
        scrollToAndAssertVisible("Recurring Test Account")
        composeRule.onNode(isVerticallyScrollable, useUnmergedTree = true).performScrollToNode(hasTestTag("add-recurring"))
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
        scrollToAndAssertVisible("Next due:", substring = true)
    }

    @Test
    fun markPaidRecordsEventImmediatelyAndAdvancesSchedule() = runBlocking {
        unlockIfNeeded()
        composeRule.onNodeWithText("Money", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("add-account", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("account-name", useUnmergedTree = true).performTextInput("Mark Paid Account")
        composeRule.onNodeWithTag("account-amount", useUnmergedTree = true).performTextInput("100000")
        composeRule.onNodeWithText("Save", useUnmergedTree = true).performClick()
        scrollToAndAssertVisible("Mark Paid Account")
        composeRule.onNode(isVerticallyScrollable, useUnmergedTree = true).performScrollToNode(hasTestTag("add-recurring"))
        composeRule.onNodeWithTag("add-recurring", useUnmergedTree = true).performClick()

        val title = "Electricity bill ${UUID.randomUUID().toString().take(8)}"
        composeRule.onNodeWithTag("recurring-title", useUnmergedTree = true).performTextInput(title)
        composeRule.onNodeWithTag("recurring-amount", useUnmergedTree = true).performTextInput("2500")
        composeRule.onNodeWithText("Expense", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("bill-category-Electricity", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("recurring-delay", useUnmergedTree = true).performTextInput("1")
        composeRule.onNodeWithText("Save draft", useUnmergedTree = true).performClick()
        scrollToAndAssertVisible(title)

        // Fire-and-forget write race (same class as every other MainViewModel write in this repo):
        // poll the DAO directly rather than assuming the insert has landed the instant Save returns.
        val recurringItem = waitForRecurringItemByTitle(title)
        val eventCountBefore = application.repository.database.financialEventDao().getAll().size
        val originalDueDate = recurringItem.nextDueDateEpochDay

        composeRule.onNodeWithTag("mark-paid-${recurringItem.id}", useUnmergedTree = true).performScrollTo().performClick()

        // Not a plain scrollToAndAssertVisible(title) here: Mark paid records a new financial event
        // whose description is the recurring item's own title, so after this click the title text
        // legitimately appears twice on screen (the recurring-item card and the new Activity row) —
        // asserting "exactly one" would be wrong, not flaky. The DAO checks below are the real proof.
        val deadline = System.currentTimeMillis() + 5_000
        var confirmed: pk.vexel.financepassport.core.database.RecurringItemEntity? = null
        while (System.currentTimeMillis() < deadline) {
            val current = application.repository.database.recurringItemDao().getById(recurringItem.id)
            if (current != null && current.nextDueDateEpochDay != originalDueDate) { confirmed = current; break }
            Thread.sleep(200)
        }
        checkNotNull(confirmed) { "Mark paid did not advance the recurring item's schedule within 5s" }
        val eventCountAfter = application.repository.database.financialEventDao().getAll().size
        check(eventCountAfter == eventCountBefore + 1) {
            "Mark paid must record exactly one new financial event (before=$eventCountBefore, after=$eventCountAfter)"
        }
    }

    private suspend fun waitForRecurringItemByTitle(title: String): pk.vexel.financepassport.core.database.RecurringItemEntity {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            application.repository.database.recurringItemDao().getAll().firstOrNull { it.title == title }?.let { return it }
            Thread.sleep(200)
        }
        error("Recurring item titled '$title' was not persisted within 5s")
    }

    // Sprint 23's Money-screen activity filter bar added a second, horizontally-scrollable region
    // (same shape of ambiguity as the Sprint 19 Wealth tab row) — plain hasScrollAction() now
    // matches both it and the vertical list and throws "found 2 nodes". Match on the vertical
    // scroll axis specifically so this still resolves to exactly the list.
    private val isVerticallyScrollable = SemanticsMatcher("isVerticallyScrollable") {
        it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) != null
    }

    private fun scrollToAndAssertVisible(text: String, timeoutMillis: Long = 5_000, substring: Boolean = false) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var lastError: Throwable? = null
        while (System.currentTimeMillis() < deadline) {
            try {
                composeRule.onNode(isVerticallyScrollable, useUnmergedTree = true).performScrollToNode(hasText(text, substring = substring))
                composeRule.onNodeWithText(text, substring = substring, useUnmergedTree = true).assertIsDisplayed()
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
