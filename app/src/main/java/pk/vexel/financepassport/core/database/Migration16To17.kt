package pk.vexel.financepassport.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS position_snapshots (id TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL, snapshotDateEpochDay INTEGER NOT NULL, liquidFundsMinor INTEGER NOT NULL, investmentsValueMinor INTEGER NOT NULL, assetsValueMinor INTEGER NOT NULL, receivablesValueMinor INTEGER NOT NULL, liabilitiesValueMinor INTEGER NOT NULL, netWorthMinor INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_position_snapshots_snapshotDateEpochDay ON position_snapshots (snapshotDateEpochDay)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_position_snapshots_kind ON position_snapshots (kind)")
    }
}
