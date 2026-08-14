package pk.vexel.financepassport

import android.app.Application
import pk.vexel.financepassport.core.database.DatabaseProvider
import pk.vexel.financepassport.core.database.FinanceRepository

class PassportApplication : Application() {
    val repository by lazy { FinanceRepository(DatabaseProvider.get(this)) }
}
