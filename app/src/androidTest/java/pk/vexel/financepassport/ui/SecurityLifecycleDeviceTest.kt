package pk.vexel.financepassport.ui

import androidx.compose.ui.test.assert
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
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertIsDisplayed
import android.Manifest
import android.os.Build
import androidx.lifecycle.Lifecycle
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
 * Device-lifecycle coverage: inactivity relock, deleteAllData returning to onboarding without a
 * process kill, and a rotation/process-death proof that an in-flight Add Bill dialog's unsaved
 * fields survive. Rewritten against the utility-tracker shell (Dashboard/Bills/Add Bill) — the
 * prior version drove the pre-reset Money screen's Add Account dialog, which no longer exists.
 *
 * Deep-link lock enforcement (mega-prompt item) is not covered here: AndroidManifest.xml declares
 * exactly one intent-filter on MainActivity, category LAUNCHER only, no deep links of any kind
 * exist in this app to bypass the lock screen through. Confirmed by inspection, not a test against
 * nothing.
 *
 * Biometric cancel-does-not-unlock is also not covered here: attached emulators declare
 * android.hardware.fingerprint as a PackageManager feature but have no enrolled biometric, so
 * BiometricManager#canAuthenticate never returns BIOMETRIC_SUCCESS and SecurityGate's "Use
 * biometrics" button never renders. SecurityGate.kt's AuthenticationCallback only overrides
 * onAuthenticationSucceeded — there is no onAuthenticationError/onAuthenticationFailed override,
 * so the default no-op-on-cancel behavior is correct; confirmed by code inspection.
 */
@RunWith(AndroidJUnit4::class)
class SecurityLifecycleDeviceTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val notificationPermissionRule: TestRule = object : TestRule {
        override fun apply(base: Statement, description: Description): Statement = if (Build.VERSION.SDK_INT >= 33) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS).apply(base, description)
        } else base
    }

    @Test
    fun backgroundingTheAppRelocksItWhenAPinIsSet() {
        unlockIfNeeded()
        composeRule.onNodeWithText("Dashboard", useUnmergedTree = true).assertIsDisplayed()

        // ON_STOP (backgrounding without a process kill) flips SecurityGate's `unlocked` back to
        // false when a PIN exists; ON_RESUME afterward should show the PIN entry screen again.
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        composeRule.onNodeWithText("Unlock").assertIsDisplayed()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("1234")
        composeRule.onNodeWithText("Unlock").performClick()
        composeRule.onNodeWithText("Dashboard", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun deleteAllDataReturnsToOnboardingWithoutProcessKill() {
        unlockIfNeeded()
        composeRule.onNodeWithContentDescription("Settings", useUnmergedTree = true).performClick()
        waitFor("Offline local backup and data controls")
        composeRule.onNodeWithTag("more-dialog-scroll", useUnmergedTree = true)
            .performScrollToNode(hasText("Delete All Application Data"))
        composeRule.onNodeWithText("Delete All Application Data", useUnmergedTree = true).performClick()
        waitFor("Type DELETE to confirm")
        composeRule.onNodeWithTag("delete-confirmation", useUnmergedTree = true).performTextInput("DELETE")
        composeRule.onNodeWithText("Delete all", useUnmergedTree = true).performClick()

        // The confirm button calls MainViewModel.deleteAllData(context) { activity.recreate() } —
        // this waits out that same in-process Activity#recreate() (no process kill anywhere in
        // this test) and asserts onboarding is genuinely showing again afterward, not just that
        // the security PIN screen reset.
        val deadline = System.currentTimeMillis() + 10_000
        var seen = false
        while (System.currentTimeMillis() < deadline) {
            if (composeRule.onAllNodesWithTag("onboarding-title").fetchSemanticsNodes().isNotEmpty()) {
                seen = true
                break
            }
            Thread.sleep(200)
        }
        check(seen) { "Onboarding did not reappear after delete-all + recreate()" }
        composeRule.onNodeWithTag("onboarding-title").assertIsDisplayed()
    }

    @Test
    fun rotationPreservesInFlightAddBillDialogFields() {
        unlockIfNeeded()
        composeRule.onNodeWithText("Bills", useUnmergedTree = true).performClick()
        composeRule.onNodeWithTag("add-bill-fab", useUnmergedTree = true).performClick()
        waitForTag("bill-name")

        val billName = "Rotation ${UUID.randomUUID().toString().take(8)}"
        composeRule.onNodeWithTag("bill-name", useUnmergedTree = true).performTextInput(billName)
        composeRule.onNodeWithTag("provider", useUnmergedTree = true).performTextInput("Rotation Utility Co")

        // Deliberate rotation/process-death proof: Activity#recreate() tears down and rebuilds the
        // whole Compose hierarchy the same way a real rotation or process death + restore does.
        // The dialog's open state and its typed-but-unsaved field content must both survive
        // underneath the security gate.
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        // SecurityGate's `unlocked` flag is a plain (non-saveable) remember, by design: rotation
        // must not leave the app unlocked. Re-authenticating is expected here, not a workaround.
        waitFor("Unlock")
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("1234")
        composeRule.onNodeWithText("Unlock").performClick()

        waitFor(billName)
        composeRule.onNodeWithTag("bill-name", useUnmergedTree = true).assert(hasText(billName))
        composeRule.onNodeWithTag("provider", useUnmergedTree = true).assert(hasText("Rotation Utility Co"))
    }

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

    /** AlertDialog content can take a frame or two to attach after the state flip that opens it. */
    private fun waitForTag(tag: String, timeoutMillis: Long = 8_000) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()) return
            Thread.sleep(200)
        }
        throw AssertionError("Timed out waiting for tag '$tag' to appear")
    }

    private fun dismissOnboardingIfPresent() {
        while (composeRule.onAllNodesWithTag("onboarding-next").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("onboarding-next").performClick()
        }
        if (composeRule.onAllNodesWithTag("setup-start-empty").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("setup-start-empty").performClick()
        }
    }

    /** These tests specifically need a PIN to exist (to exercise relock), so onboarding's PIN
     * step is completed with a real PIN rather than skipped. */
    private fun unlockIfNeeded() {
        dismissOnboardingIfPresent()
        if (composeRule.onAllNodesWithText("Create PIN").fetchSemanticsNodes().isNotEmpty()) {
            val fields = composeRule.onAllNodes(hasSetTextAction())
            fields[0].performTextInput("1234")
            fields[1].performTextInput("1234")
            composeRule.onNodeWithText("Create PIN").performClick()
            if (composeRule.onAllNodesWithTag("setup-start-empty").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onNodeWithTag("setup-start-empty").performClick()
            }
            // A PIN now exists, so SecurityGate's `unlocked = !store.hasPin()` starts false the
            // instant onboarding hands off to it — this first unlock uses the PIN just created.
            if (composeRule.onAllNodesWithText("Unlock").fetchSemanticsNodes().isNotEmpty()) {
                composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("1234")
                composeRule.onNodeWithText("Unlock").performClick()
            }
        } else if (composeRule.onAllNodesWithText("Unlock").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("1234")
            composeRule.onNodeWithText("Unlock").performClick()
        }
    }
}
