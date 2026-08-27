package pk.vexel.financepassport.core.calendar

import android.Manifest
import android.app.NotificationManager
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import java.util.UUID
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.rules.TestRule
import org.junit.runners.model.Statement

/**
 * Phase 10 notification-delivery item: prior phases only confirmed POST_NOTIFICATIONS was
 * grantable (permission dialogs, GrantPermissionRule workarounds for focus-steal). This actually
 * schedules a real ReminderScheduler work request with zero delay through the production
 * WorkManager instance and polls the real NotificationManager for the resulting notification,
 * proving delivery end to end rather than just permission state.
 */
@RunWith(AndroidJUnit4::class)
class NotificationDeliveryDeviceTest {
    @get:Rule
    val notificationPermissionRule: TestRule = object : TestRule {
        override fun apply(base: Statement, description: Description): Statement = if (Build.VERSION.SDK_INT >= 33) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS).apply(base, description)
        } else base
    }

    @Test
    fun scheduledReminderActuallyPostsANotification() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val id = "notif-delivery-${UUID.randomUUID()}"
        val title = "Vexel device-test reminder"

        ReminderScheduler(context).schedule(
            id = id,
            dueAtEpochMillis = System.currentTimeMillis(),
            reminderMinutesBefore = 0,
            title = title,
            body = "Delivery proof",
        )

        val manager = context.getSystemService(NotificationManager::class.java)
        val expectedId = id.hashCode()
        val deadline = System.currentTimeMillis() + 20_000
        var delivered = false
        while (System.currentTimeMillis() < deadline) {
            if (manager.activeNotifications.any { it.id == expectedId }) {
                delivered = true
                break
            }
            Thread.sleep(300)
        }
        assertTrue(
            "Expected notification id=$expectedId to be posted by WorkManager within 20s; active ids were " +
                manager.activeNotifications.map { it.id },
            delivered,
        )

        manager.cancel(expectedId)
    }
}
