package pk.vexel.financepassport.core.calendar

import android.Manifest
import android.app.NotificationManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.room.Room
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import pk.vexel.financepassport.core.database.AppDatabase
import pk.vexel.financepassport.core.database.FinanceRepository
import pk.vexel.financepassport.core.database.RecurringItemEntity
import pk.vexel.financepassport.core.model.FinancialEventType

@RunWith(AndroidJUnit4::class)
class ReminderDeviceTest {
    @get:Rule
    val notificationPermissionRule: TestRule = object : TestRule {
        override fun apply(base: Statement, description: Description): Statement = if (Build.VERSION.SDK_INT >= 33) {
            GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS).apply(base, description)
        } else base
    }

    @Test
    fun immediateReminderRunsAndPostsNotification() {
        runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = "device-reminder-${UUID.randomUUID()}"
        NotificationHelper.ensureChannel(context)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(workDataOf(ReminderWorker.KEY_ID to id, ReminderWorker.KEY_TITLE to "Device test reminder", ReminderWorker.KEY_BODY to "Synthetic reminder"))
            .build()
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork("reminder-$id", ExistingWorkPolicy.REPLACE, request)
        val deadline = System.currentTimeMillis() + 20_000L
        var state: androidx.work.WorkInfo.State? = null
        while (System.currentTimeMillis() < deadline) {
            state = workManager.getWorkInfoById(request.id).get(5, TimeUnit.SECONDS)?.state
            if (state == androidx.work.WorkInfo.State.SUCCEEDED || state == androidx.work.WorkInfo.State.FAILED) break
            kotlinx.coroutines.delay(250)
        }
        assertEquals(androidx.work.WorkInfo.State.SUCCEEDED, state)
        val notifications = context.getSystemService(NotificationManager::class.java).activeNotifications
        if (Build.VERSION.SDK_INT >= 33) assertTrue(notifications.any { it.id == id.hashCode() })
        workManager.cancelUniqueWork("reminder-$id")
        }
    }

    @Test
    fun reschedulePersistsNewDueTimeAndReplacesWork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val repository = FinanceRepository(database)
        repository.addCalendarItem(context, "Review", "REVIEW", 5)
        val item = database.calendarDao().getById(database.openHelper.writableDatabase.query("SELECT id FROM calendar_items LIMIT 1").use { cursor -> cursor.moveToFirst(); cursor.getString(0) }) ?: error("Reminder missing")
        repository.rescheduleCalendarItem(context, item.id, 90)
        val updated = database.calendarDao().getById(item.id) ?: error("Reminder missing after reschedule")
        assertTrue(updated.dueAtEpochMillis > item.dueAtEpochMillis)
        assertEquals("OPEN", updated.status)
        WorkManager.getInstance(context).cancelUniqueWork("reminder-${item.id}")
        database.close()
    }

    @Test
    fun snoozePushesFromTheItemsOwnDueTimeRatherThanFromNow() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val repository = FinanceRepository(database)
        repository.addCalendarItem(context, "Renew CNIC", "OFFICIAL_RECORD", 5)
        val item = database.calendarDao().getById(database.openHelper.writableDatabase.query("SELECT id FROM calendar_items LIMIT 1").use { cursor -> cursor.moveToFirst(); cursor.getString(0) }) ?: error("Reminder missing")
        repository.snoozeCalendarItem(context, item.id, 3)
        val snoozed = database.calendarDao().getById(item.id) ?: error("Reminder missing after snooze")
        assertEquals(item.dueAtEpochMillis + 3 * 86_400_000L, snoozed.dueAtEpochMillis)
        WorkManager.getInstance(context).cancelUniqueWork("reminder-${item.id}")
        database.close()
    }

    @Test
    fun processingDueRecurringItemRemindsWithoutCreatingConfirmedEventAndAdvances() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val repository = FinanceRepository(database)
        repository.addAccount("Main", "CASH", 100_000)
        val account = database.accountDao().getAll().single()
        val today = java.time.LocalDate.now()
        database.recurringItemDao().upsert(
            RecurringItemEntity(
                "rent", "Rent", FinancialEventType.EXPENSE.name, 50_000, "PKR", account.id, "Housing",
                "MONTHLY", today.toEpochDay(), "ACTIVE", true, 1, 1, anchorDayOfMonth = 31,
            )
        )

        repository.processDueRecurringItems(context)

        val createdEvent = database.financialEventDao().getAll().singleOrNull { it.description == "Rent" }
        assertTrue(createdEvent == null)
        val advanced = database.recurringItemDao().getAll().single()
        assertTrue(advanced.nextDueDateEpochDay > today.toEpochDay())
        assertEquals("ACTIVE", advanced.status)
        WorkManager.getInstance(context).cancelUniqueWork("reminder-recurring-rent")
        database.close()
    }
}
