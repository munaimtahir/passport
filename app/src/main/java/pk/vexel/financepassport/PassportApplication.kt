package pk.vexel.financepassport

import android.app.Application
import pk.vexel.financepassport.core.database.DatabaseProvider
import pk.vexel.financepassport.core.database.FinanceRepository
import pk.vexel.financepassport.core.calendar.RecurringWorkScheduler

class PassportApplication : Application() {
    val repository by lazy { FinanceRepository(DatabaseProvider.get(this)) }

    override fun onCreate() {
        super.onCreate()
        RecurringWorkScheduler.schedule(this)
    }
}
