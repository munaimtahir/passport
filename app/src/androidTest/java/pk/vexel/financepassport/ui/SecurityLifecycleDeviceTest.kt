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
 * Phase 10 device-lifecycle items that were never previously scoped into any pass:
 * inactivity relock, deleteAllData returning to onboarding without a process kill, and a
 * deliberate rotation/process-death proof for the Phase 8 rememberSaveable conversions (the
 * connected suite otherwise only exercises Activity teardown incidentally, between test classes).
 *
 * Deep-link lock enforcement (mega-prompt item) is not covered here: AndroidManifest.xml declares
 * exactly one intent-filter on MainActivity, category LAUNCHER only, no deep links of any kind
 * exist in this app to bypass the lock screen through. Confirmed by inspection, not a test against
 * nothing.
 *
 * Biometric cancel-does-not-unlock is also not covered here: both attached emulators
 * (Android_26_Test, Android_15_Test) declare android.hardware.fingerprint as a PackageManager
 * feature, but neither resolves an android.settings.FINGERPRINT_ENROLL intent (no Settings
 * fingerprint-enrollment activity present in these system images) and dumpsys biometric/
 * fingerprint report no enrolled biometric. BiometricManager#canAuthenticate therefore never
 * returns BIOMETRIC_SUCCESS on this hardware, so SecurityGate's "Use biometrics" button never
 * renders and BiometricPrompt's cancel path is unreachable in this environment. The app-side
 * code for that path (SecurityGate.kt) only overrides onAuthenticationSucceeded — there is no
 * onAuthenticationError/onAuthenticationFailed override, so the default AuthenticationCallback
 * no-ops on cancel/failure and `unlocked` simply stays false, which is the correct behavior; this
 * was confirmed by code inspection rather than a live BiometricPrompt interaction.
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
    fun backgroundingTheAppRelocksIt() {
        unlockIfNeeded()
        composeRule.onNodeWithText("Money", useUnmergedTree = true).assertIsDisplayed()

        // ON_STOP (backgrounding without a process kill) flips SecurityGate's `unlocked` back to
        // false; ON_RESUME afterward should show the PIN entry screen again, not the app content.
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)

        composeRule.onNodeWithText("Unlock").assertIsDisplayed()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("1234")
        composeRule.onNodeWithText("Unlock").performClick()
        composeRule.onNodeWithText("Money", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun deleteAllDataReturnsToOnboardingWithoutProcessKill() {
        unlockIfNeeded()
        composeRule.onNodeWithContentDescription("More", useUnmergedTree = true).performClick()
        waitFor("Reports and local data controls")
        composeRule.onNodeWithTag("more-dialog-scroll", useUnmergedTree = true)
            .performScrollToNode(hasText("Delete all application data"))
        composeRule.onNodeWithText("Delete all application data", useUnmergedTree = true).performClick()
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
    fun rotationPreservesInFlightAddAccountDialogFields() {
        unlockIfNeeded()
        composeRule.onNodeWithText("Money", useUnmergedTree = true).performClick()
        composeRule.onNodeWithContentDescription("Add").performClick()

        val accountName = "Rotation ${UUID.randomUUID().toString().take(8)}"
        composeRule.onNodeWithTag("account-name", useUnmergedTree = true).performTextInput(accountName)
        composeRule.onNodeWithTag("account-institution", useUnmergedTree = true).performTextInput("Rotation Bank")

        // Deliberate rotation/process-death proof for the Phase 8 remember -> rememberSaveable
        // conversion: Activity#recreate() tears down and rebuilds the whole Compose hierarchy the
        // same way a real rotation or process death + restore does. The dialog's open state and
        // its typed-but-unsaved field content must both survive underneath the security gate.
        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        // SecurityGate's `unlocked` flag is a plain (non-saveable) remember, by design: rotation
        // must not leave the app unlocked. Re-authenticating is expected here, not a workaround.
        waitFor("Unlock")
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("1234")
        composeRule.onNodeWithText("Unlock").performClick()

        waitFor(accountName)
        composeRule.onNodeWithTag("account-name", useUnmergedTree = true).assert(hasText(accountName))
        composeRule.onNodeWithTag("account-institution", useUnmergedTree = true).assert(hasText("Rotation Bank"))
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

    private fun waitAndClick(text: String, timeoutMillis: Long = 8_000) {
        waitFor(text, timeoutMillis)
        composeRule.onNodeWithText(text, useUnmergedTree = true).performClick()
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
