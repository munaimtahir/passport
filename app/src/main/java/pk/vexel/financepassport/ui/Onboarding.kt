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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import pk.vexel.financepassport.PassportApplication
import pk.vexel.financepassport.core.model.PkrMoneyInput
import pk.vexel.financepassport.core.security.AppPreferences

private data class OnboardingPage(val title: String, val body: String)

private val onboardingPages = listOf(
    OnboardingPage(
        "Welcome to Vexel Finance Passport",
        "A private, offline-first personal finance diary: track daily expenses, bills, income, loans, receivables, savings and your overall net worth — all in one place, on this device. As a supporting benefit, it can also keep your records tax-ready automatically.",
    ),
    OnboardingPage(
        "Your privacy, your device",
        "There is no login, no bank or FBR credentials, no ads and no tracking. Nothing leaves this device unless you export or back it up yourself.",
    ),
    OnboardingPage(
        "Base currency: PKR",
        "Amounts are recorded in whole Pakistani rupees by default. Next, you can optionally add a starter account, then create a private PIN to protect your local records.",
    ),
)

/** Guided starter-setup account/asset options, in the order offered. */
private enum class SetupOption(val label: String, val accountType: String?) {
    BANK("Add a bank account", "BANK"),
    CASH("Add a cash account", "CASH"),
    INVESTMENT("Add an investment account", "INVESTMENT"),
    ASSET("Add a major asset", null),
}

/** Shows a one-time onboarding flow before [content]; skipped once [AppPreferences.isOnboardingComplete] is true. */
@Composable
fun OnboardingGate(preferences: AppPreferences, content: @Composable () -> Unit) {
    var complete by rememberSaveable { mutableStateOf(preferences.isOnboardingComplete()) }
    if (complete) {
        content()
    } else {
        OnboardingFlow { preferences.setOnboardingComplete(true); complete = true }
    }
}

@Composable
private fun OnboardingFlow(onFinished: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(0) }
    val setupPageIndex = onboardingPages.size
    if (page < onboardingPages.size) {
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
                    modifier = Modifier.weight(1f).testTag("onboarding-next"),
                ) { Text(if (page < onboardingPages.lastIndex) "Next" else "Continue") }
            }
        }
    } else {
        check(page == setupPageIndex) { "Unexpected onboarding page index $page" }
        GuidedSetupPage(onBack = { page -= 1 }, onDone = onFinished)
    }
}

/**
 * Optional starter-setup step shown after the informational onboarding pages and before PIN
 * creation: the user may add one real bank/cash/investment account or a major asset, or skip
 * straight to an empty diary. There is deliberately no seeded/demo data option here — every
 * record this screen can create is a real, user-entered record or nothing at all.
 */
@Composable
private fun GuidedSetupPage(onBack: () -> Unit, onDone: () -> Unit) {
    val application = LocalContext.current.applicationContext as PassportApplication
    val vm: MainViewModel = viewModel(factory = MainViewModelFactory(application.repository, application.preferences))
    var selected by rememberSaveable { mutableStateOf<SetupOption?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Set up your first record", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("onboarding-setup-title"))
        val option = selected
        if (option == null) {
            Text(
                "Optionally add a starting account or asset now, or start with an empty diary — you can always add more later.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SetupOption.entries.forEach { opt ->
                    OutlinedButton(
                        onClick = { selected = opt },
                        modifier = Modifier.fillMaxWidth().testTag("setup-${opt.name.lowercase()}"),
                    ) { Text(opt.label) }
                }
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(onClick = onDone, modifier = Modifier.weight(1f).testTag("setup-start-empty")) { Text("Start empty") }
            }
        } else {
            OutlinedTextField(
                name,
                { name = it },
                label = { Text(if (option == SetupOption.ASSET) "Asset name" else "Account name") },
                singleLine = true,
                modifier = Modifier.testTag("setup-name"),
            )
            OutlinedTextField(
                amount,
                { PkrMoneyInput.groupedInput(it)?.let { value -> amount = value } },
                label = { Text(if (option == SetupOption.ASSET) "Value (PKR, optional)" else "Opening balance (PKR, optional)") },
                singleLine = true,
                modifier = Modifier.testTag("setup-amount"),
            )
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { selected = null; name = ""; amount = "" }, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(
                    onClick = {
                        val minor = amount.takeIf { it.isNotBlank() }?.let { PkrMoneyInput.toMinorUnits(it) } ?: 0L
                        if (option == SetupOption.ASSET) vm.addAsset(name, minor) else vm.addAccount(name, option.accountType!!, minor)
                        onDone()
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.weight(1f).testTag("setup-save"),
                ) { Text("Save & finish") }
            }
        }
    }
}
