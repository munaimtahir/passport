package pk.vexel.financepassport

import android.app.Application
import pk.vexel.financepassport.core.database.DatabaseProvider
import pk.vexel.financepassport.core.database.FinanceRepository
import pk.vexel.financepassport.core.calendar.RecurringWorkScheduler
import pk.vexel.financepassport.core.security.AppPreferences
import androidx.work.Configuration
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PassportApplication : Application(), Configuration.Provider {
    val repository by lazy { FinanceRepository(DatabaseProvider.get(this)) }
    val preferences by lazy { AppPreferences(this) }

    override fun onCreate() {
        super.onCreate()
        // Keep startup deterministic in cold app and instrumentation processes. Some Android
        // test/device configurations do not run the Startup provider before Application.onCreate.
        runCatching { WorkManager.getInstance(this) }
            .onFailure { WorkManager.initialize(this, workManagerConfiguration) }
        RecurringWorkScheduler.schedule(this)
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { repository.reconcileCalendarProjection() }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}
