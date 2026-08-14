package pk.vexel.financepassport.core.security

import android.content.Context
import android.util.Base64

class PinStore(context: Context) {
    private val preferences = context.getSharedPreferences("passport_lock", Context.MODE_PRIVATE)

    fun hasPin(): Boolean = preferences.contains(KEY_SALT) && preferences.contains(KEY_DIGEST)

    fun save(record: PinRecord) {
        preferences.edit()
            .putString(KEY_SALT, Base64.encodeToString(record.salt, Base64.NO_WRAP))
            .putString(KEY_DIGEST, Base64.encodeToString(record.digest, Base64.NO_WRAP))
            .putInt(KEY_ITERATIONS, record.iterations)
            .apply()
    }

    fun verify(pin: CharArray): Boolean {
        if (!canAttempt()) return false
        val salt = preferences.getString(KEY_SALT, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val digest = preferences.getString(KEY_DIGEST, null)?.let { Base64.decode(it, Base64.NO_WRAP) } ?: return false
        val valid = PinVerifier.verify(pin, PinRecord(salt, digest, preferences.getInt(KEY_ITERATIONS, 120_000)))
        if (valid) clearFailures() else recordFailure()
        return valid
    }

    fun canAttempt(): Boolean {
        val failures = preferences.getInt(KEY_FAILURES, 0)
        val last = preferences.getLong(KEY_LAST_FAILURE, 0)
        val delay = (1L shl failures.coerceAtMost(6)) * 1_000L
        return System.currentTimeMillis() - last >= delay
    }

    private fun recordFailure() { preferences.edit().putInt(KEY_FAILURES, preferences.getInt(KEY_FAILURES, 0) + 1).putLong(KEY_LAST_FAILURE, System.currentTimeMillis()).apply() }
    private fun clearFailures() { preferences.edit().remove(KEY_FAILURES).remove(KEY_LAST_FAILURE).apply() }

    private companion object { const val KEY_SALT = "salt"; const val KEY_DIGEST = "digest"; const val KEY_ITERATIONS = "iterations"; const val KEY_FAILURES = "failures"; const val KEY_LAST_FAILURE = "lastFailure" }
}
