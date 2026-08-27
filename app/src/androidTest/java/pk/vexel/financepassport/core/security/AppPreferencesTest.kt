package pk.vexel.financepassport.core.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppPreferencesTest {
    private fun freshPreferences(): AppPreferences {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("passport_app_prefs", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        return AppPreferences(context)
    }

    @Test
    fun onboardingIsIncompleteByDefaultAndPersistsOnceCompleted() {
        val preferences = freshPreferences()
        assertFalse(preferences.isOnboardingComplete())

        preferences.setOnboardingComplete(true)

        assertTrue(preferences.isOnboardingComplete())
        assertTrue(AppPreferences(InstrumentationRegistry.getInstrumentation().targetContext).isOnboardingComplete())
    }

    @Test
    fun privacyModeIsDisabledByDefaultAndTogglePersists() {
        val preferences = freshPreferences()
        assertFalse(preferences.isPrivacyModeEnabled())

        preferences.setPrivacyMode(true)

        assertTrue(preferences.isPrivacyModeEnabled())
        assertTrue(AppPreferences(InstrumentationRegistry.getInstrumentation().targetContext).isPrivacyModeEnabled())
    }
}
