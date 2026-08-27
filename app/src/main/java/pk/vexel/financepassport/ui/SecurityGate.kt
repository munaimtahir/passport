package pk.vexel.financepassport.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import pk.vexel.financepassport.core.security.PinStore
import pk.vexel.financepassport.core.security.PinVerifier
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor
import androidx.core.content.ContextCompat
import androidx.activity.compose.LocalActivity

@Composable
fun SecurityGate(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val store = remember(context) { PinStore(context) }
    var unlocked by remember { mutableStateOf(!store.hasPin()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, unlocked) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) unlocked = false }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (unlocked) content() else PinScreen(store) { unlocked = true }
}

/**
 * A [TextToolbar] that never shows the copy/cut/paste/select-all bubble. Compose's
 * `PasswordVisualTransformation` only masks how the PIN *renders*; it does not stop a long-press
 * selection from copying the real, unmasked digits to the system clipboard, where any other app
 * (or a clipboard-history feature) can read them. Providing this in place of [LocalTextToolbar]
 * for just the PIN fields removes that path entirely, rather than relying on masking alone.
 */
private object NoClipboardTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden
    override fun showMenu(rect: Rect, onCopyRequested: (() -> Unit)?, onPasteRequested: (() -> Unit)?, onCutRequested: (() -> Unit)?, onSelectAllRequested: (() -> Unit)?) {
        // Intentionally does nothing: no copy/cut/paste/select-all menu for PIN entry fields.
    }
    override fun hide() {}
}

@Composable
private fun PinScreen(store: PinStore, onUnlock: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    val setup = !store.hasPin()
    val activity = LocalActivity.current
    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Vexel Finance Passport", style = MaterialTheme.typography.headlineMedium)
        Text(if (setup) "Create a private PIN. It stays on this device." else "Unlock your local financial records.")
        CompositionLocalProvider(LocalTextToolbar provides NoClipboardTextToolbar) {
            OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(12) }, label = { Text("PIN") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            if (setup) OutlinedTextField(confirmation, { confirmation = it.filter(Char::isDigit).take(12) }, label = { Text("Confirm PIN") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            runCatching {
                if (setup) {
                    require(pin.length >= 4) { "Use at least 4 digits." }
                    require(pin == confirmation) { "PINs do not match." }
                    store.save(PinVerifier.create(pin.toCharArray()))
                } else require(store.verify(pin.toCharArray())) { "Incorrect PIN." }
                onUnlock()
            }.onFailure { message = it.message }
        }) { Text(if (setup) "Create PIN" else "Unlock") }
        if (!setup && activity is FragmentActivity && BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
            Button(onClick = {
                val executor: Executor = ContextCompat.getMainExecutor(activity)
                BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onUnlock()
                }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Unlock Passport").setSubtitle("Use device security to unlock local records").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL).build())
            }) { Text("Use biometrics") }
        }
    }
}
