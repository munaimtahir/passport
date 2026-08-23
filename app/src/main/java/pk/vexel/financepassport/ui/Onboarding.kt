package pk.vexel.financepassport.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pk.vexel.financepassport.core.security.AppPreferences

private data class OnboardingPage(val title: String, val body: String)

private val onboardingPages = listOf(
    OnboardingPage(
        "Welcome to Vexel Finance Passport",
        "A private, offline-first record of your financial life: accounts, wealth, documents and continuous tax capture, kept only on this device.",
    ),
    OnboardingPage(
        "Your privacy, your device",
        "There is no login, no bank or FBR credentials, no ads and no tracking. Nothing leaves this device unless you export or back it up yourself.",
    ),
    OnboardingPage(
        "Base currency: PKR",
        "Amounts are recorded in whole Pakistani rupees by default. Next you will create a private PIN to protect your local records.",
    ),
)

/** Shows a one-time onboarding flow before [content]; skipped once [AppPreferences.isOnboardingComplete] is true. */
@Composable
fun OnboardingGate(preferences: AppPreferences, content: @Composable () -> Unit) {
    // Plain remember, not rememberSaveable: AppPreferences is already the durable source of
    // truth for this flag. Caching it in the saved-instance-state Bundle would make it survive
    // Activity#recreate() (rotation, or the deliberate post-delete-all recreate below), which
    // would then keep showing the stale pre-delete value instead of re-reading the cleared prefs.
    var complete by remember { mutableStateOf(preferences.isOnboardingComplete()) }
    if (complete) {
        content()
    } else {
        OnboardingFlow { preferences.setOnboardingComplete(true); complete = true }
    }
}

@Composable
private fun OnboardingFlow(onFinished: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(0) }
    val current = onboardingPages[page]
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(current.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("onboarding-title"))
        Text(current.body, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (page > 0) {
                OutlinedButton(onClick = { page -= 1 }, modifier = Modifier.weight(1f)) { Text("Back") }
            }
            Button(
                onClick = { if (page < onboardingPages.lastIndex) page += 1 else onFinished() },
                modifier = Modifier.weight(1f).testTag("onboarding-next"),
            ) { Text(if (page < onboardingPages.lastIndex) "Next" else "Continue to PIN setup") }
        }
    }
}
