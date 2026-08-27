package pk.vexel.financepassport.core.calendar

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import pk.vexel.financepassport.PassportApplication

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Vexel reminder"
        val body = inputData.getString(KEY_BODY) ?: "You have a scheduled financial review."
        NotificationHelper.ensureChannel(applicationContext)
        if (Build.VERSION.SDK_INT < 33 || applicationContext.checkSelfPermission("android.permission.POST_NOTIFICATIONS") == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(applicationContext).notify(inputData.getString(KEY_ID)?.hashCode() ?: title.hashCode(), NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(body).setAutoCancel(true).build())
        }
        return Result.success()
    }

    companion object { const val KEY_ID = "reminderId"; const val KEY_TITLE = "title"; const val KEY_BODY = "body" }
}

class RecurringProcessingWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val application = applicationContext as PassportApplication
        application.repository.processDueRecurringItems(applicationContext)
    }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
}

object RecurringWorkScheduler {
    const val UNIQUE_NAME = "recurring-draft-processing"
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecurringProcessingWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_NAME, androidx.work.ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

class ReminderScheduler(private val context: Context) {
    fun schedule(id: String, dueAtEpochMillis: Long, reminderMinutesBefore: Long, title: String = "Vexel reminder", body: String = "You have a scheduled financial review.") {
        val delay = (dueAtEpochMillis - reminderMinutesBefore * 60_000L - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>().setInputData(androidx.work.workDataOf(ReminderWorker.KEY_ID to id, ReminderWorker.KEY_TITLE to title, ReminderWorker.KEY_BODY to body)).setInitialDelay(delay, TimeUnit.MILLISECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork("reminder-$id", ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(id: String) { WorkManager.getInstance(context).cancelUniqueWork("reminder-$id") }
}

object NotificationHelper {
    const val CHANNEL_ID = "passport_reminders"
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Financial reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
    }
}
