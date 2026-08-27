package pk.vexel.financepassport

import android.os.Bundle
import android.os.Build
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import pk.vexel.financepassport.ui.OnboardingGate
import pk.vexel.financepassport.ui.PassportApp
import pk.vexel.financepassport.ui.SecurityGate
import pk.vexel.financepassport.ui.theme.PassportTheme
import pk.vexel.financepassport.core.calendar.NotificationHelper

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = (application as PassportApplication).preferences
        setContent {
            PassportTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    OnboardingGate(preferences) { SecurityGate { PassportApp() } }
                }
            }
        }
        NotificationHelper.ensureChannel(this)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7001)
        }
    }
}
