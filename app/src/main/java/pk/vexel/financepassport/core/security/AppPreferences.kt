package pk.vexel.financepassport.core.security

import android.content.Context

/** Local, non-sensitive app preferences: onboarding completion and privacy-mode masking. */
class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("passport_app_prefs", Context.MODE_PRIVATE)

    fun isOnboardingComplete(): Boolean = preferences.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    fun setOnboardingComplete(complete: Boolean) { preferences.edit().putBoolean(KEY_ONBOARDING_COMPLETE, complete).apply() }

    fun isPrivacyModeEnabled(): Boolean = preferences.getBoolean(KEY_PRIVACY_MODE, false)
    fun setPrivacyMode(enabled: Boolean) { preferences.edit().putBoolean(KEY_PRIVACY_MODE, enabled).apply() }

    private companion object {
        const val KEY_ONBOARDING_COMPLETE = "onboardingComplete"
        const val KEY_PRIVACY_MODE = "privacyModeEnabled"
    }
}
