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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pk.vexel.financepassport.PassportApplication
import pk.vexel.financepassport.core.security.AppPreferences
import pk.vexel.financepassport.core.security.PinStore
import pk.vexel.financepassport.core.security.PinVerifier

private data class OnboardingPage(val title: String, val body: String)

private val onboardingPages = listOf(
    OnboardingPage(
        "Welcome to Vexel Finance Passport",
        "A private, offline-first monthly utility bill tracker.\n\nRegister recurring monthly utility bills once, automatically create each monthly bill occurrence, track unpaid obligations, and keep your payment history safe on this device."
    ),
    OnboardingPage(
        "Your privacy, your device",
        "There is no login, no bank credentials, no cloud synchronization, and no tracking. All records remain entirely on this device, backed by local Android Keystore encryption."
    )
)

/** Shows a one-time onboarding flow before [content]; skipped once [AppPreferences.isOnboardingComplete] is true. */
@Composable
fun OnboardingGate(preferences: AppPreferences, content: @Composable () -> Unit) {
    var complete by remember { mutableStateOf(preferences.isOnboardingComplete()) }
    if (complete) {
        content()
    } else {
        OnboardingFlow {
            preferences.setOnboardingComplete(true)
            complete = true
        }
    }
}

@Composable
private fun OnboardingFlow(onFinished: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(0) }
    val pinSetupPageIndex = onboardingPages.size
    val firstBillPageIndex = onboardingPages.size + 1

    when {
        page < onboardingPages.size -> {
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
                        onClick = { page += 1 },
                        modifier = Modifier.weight(1f).testTag("onboarding-next")
                    ) { Text("Continue") }
                }
            }
        }
        page == pinSetupPageIndex -> {
            OptionalPinSetupPage(
                onBack = { page -= 1 },
                onNext = { page += 1 }
            )
        }
        page == firstBillPageIndex -> {
            FirstBillSetupPage(
                onBack = { page -= 1 },
                onDone = onFinished
            )
        }
    }
}

@Composable
private fun OptionalPinSetupPage(onBack: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { PinStore(context) }
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Optional PIN Protection", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("onboarding-pin-title"))
        Text(
            "Create a private PIN to protect your local records. If you prefer not to use a PIN, you can skip this step and set it up later in settings.",
            style = MaterialTheme.typography.bodyLarge
        )
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(12) },
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("setup-pin")
        )
        OutlinedTextField(
            value = confirmation,
            onValueChange = { confirmation = it.filter(Char::isDigit).take(12) },
            label = { Text("Confirm PIN") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("setup-confirm-pin")
        )
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            OutlinedButton(onClick = onNext, modifier = Modifier.weight(1f).testTag("setup-skip-pin")) { Text("Skip PIN") }
            Button(
                onClick = {
                    runCatching {
                        require(pin.length >= 4) { "Use at least 4 digits." }
                        require(pin == confirmation) { "PINs do not match." }
                        store.save(PinVerifier.create(pin.toCharArray()))
                        onNext()
                    }.onFailure { message = it.message }
                },
                enabled = pin.length >= 4,
                modifier = Modifier.weight(1f).testTag("setup-create-pin")
            ) { Text("Create PIN") }
        }
    }
}

@Composable
private fun FirstBillSetupPage(onBack: () -> Unit, onDone: () -> Unit) {
    val application = LocalContext.current.applicationContext as PassportApplication
    // Save onboarding completed flag first, and let the dashboard offer "Add Bill"
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Get Started", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("onboarding-setup-title"))
        Text(
            "You are ready to begin tracking your bills. You can register your first utility connection, or skip to start with an empty dashboard.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onClick = onDone, modifier = Modifier.weight(1f).testTag("setup-start-empty")) { Text("Start Empty") }
        }
    }
}
