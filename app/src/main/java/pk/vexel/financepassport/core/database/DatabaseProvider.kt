package pk.vexel.financepassport.core.database

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "passport.db")
            .addMigrations(*ALL_MIGRATIONS)
            .build().also { instance = it }
    }

    /** The one migration chain used by production, tests, validation, and restore opens. */
    val ALL_MIGRATIONS: Array<Migration> by lazy {
        arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
    }

    fun close() { synchronized(this) { instance?.close(); instance = null } }

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS assets (id TEXT NOT NULL PRIMARY KEY, type TEXT NOT NULL, title TEXT NOT NULL, acquisitionDateEpochDay INTEGER NOT NULL, acquisitionCostMinor INTEGER NOT NULL, currentEstimatedValueMinor INTEGER NOT NULL, currency TEXT NOT NULL, ownershipPercent INTEGER NOT NULL, disposalDateEpochDay INTEGER, disposalValueMinor INTEGER, status TEXT NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_assets_status ON assets(status)")
            db.execSQL("CREATE TABLE IF NOT EXISTS liabilities (id TEXT NOT NULL PRIMARY KEY, type TEXT NOT NULL, title TEXT NOT NULL, lender TEXT, originalAmountMinor INTEGER NOT NULL, outstandingAmountMinor INTEGER NOT NULL, currency TEXT NOT NULL, startDateEpochDay INTEGER NOT NULL, dueDateEpochDay INTEGER, status TEXT NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_liabilities_status ON liabilities(status)")
            db.execSQL("CREATE TABLE IF NOT EXISTS investment_events (id TEXT NOT NULL PRIMARY KEY, investmentAccountId TEXT NOT NULL, securityName TEXT NOT NULL, type TEXT NOT NULL, dateEpochDay INTEGER NOT NULL, quantityMinor INTEGER, grossAmountMinor INTEGER NOT NULL, feesMinor INTEGER NOT NULL, taxWithheldMinor INTEGER NOT NULL, currency TEXT NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_investment_events_dateEpochDay ON investment_events(dateEpochDay)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_investment_events_investmentAccountId ON investment_events(investmentAccountId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS receivables (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, counterparty TEXT NOT NULL, originalAmountMinor INTEGER NOT NULL, outstandingAmountMinor INTEGER NOT NULL, dueDateEpochDay INTEGER, status TEXT NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_receivables_status ON receivables(status)")
            db.execSQL("CREATE TABLE IF NOT EXISTS goals (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, goalType TEXT NOT NULL, targetAmountMinor INTEGER NOT NULL, targetDateEpochDay INTEGER, status TEXT NOT NULL)")
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS tax_annual_drafts (id TEXT NOT NULL PRIMARY KEY, taxYearId TEXT NOT NULL, draftVersion INTEGER NOT NULL, rulesetVersion TEXT NOT NULL, generatedAtEpochMillis INTEGER NOT NULL, status TEXT NOT NULL, issueCount INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tax_annual_drafts_taxYearId ON tax_annual_drafts(taxYearId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS tax_draft_lines (id TEXT NOT NULL PRIMARY KEY, draftId TEXT NOT NULL, sectionCode TEXT NOT NULL, categoryCode TEXT NOT NULL, amountMinor INTEGER NOT NULL, currency TEXT NOT NULL, sourceIdsJson TEXT NOT NULL, calculation TEXT NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tax_draft_lines_draftId ON tax_draft_lines(draftId)")
            db.execSQL("CREATE TABLE IF NOT EXISTS wealth_reconciliations (id TEXT NOT NULL PRIMARY KEY, taxYearId TEXT NOT NULL, openingWealthMinor INTEGER NOT NULL, inflowsMinor INTEGER NOT NULL, expenditureMinor INTEGER NOT NULL, outflowsMinor INTEGER NOT NULL, adjustmentsMinor INTEGER NOT NULL, expectedClosingMinor INTEGER NOT NULL, recordedClosingMinor INTEGER NOT NULL, unexplainedDifferenceMinor INTEGER NOT NULL, calculation TEXT NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_wealth_reconciliations_taxYearId ON wealth_reconciliations(taxYearId)")
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS calendar_items (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, kind TEXT NOT NULL, dueAtEpochMillis INTEGER NOT NULL, linkedEntityId TEXT, status TEXT NOT NULL, reminderMinutesBefore INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_calendar_items_dueAtEpochMillis ON calendar_items(dueAtEpochMillis)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_calendar_items_status ON calendar_items(status)")
        }
    }

    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS official_records (id TEXT NOT NULL PRIMARY KEY, recordType TEXT NOT NULL, title TEXT NOT NULL, maskedIdentifier TEXT, encryptedIdentifier BLOB, issueDateEpochDay INTEGER, expiryDateEpochDay INTEGER, linkedDocumentId TEXT, status TEXT NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_official_records_recordType ON official_records(recordType)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_official_records_expiryDateEpochDay ON official_records(expiryDateEpochDay)")
        }
    }

    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS tax_issues (id TEXT NOT NULL PRIMARY KEY, draftId TEXT NOT NULL, code TEXT NOT NULL, title TEXT NOT NULL, explanation TEXT NOT NULL, sourceId TEXT, status TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tax_issues_draftId ON tax_issues(draftId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_tax_issues_sourceId ON tax_issues(sourceId)")
        }
    }

    val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS recurring_items (id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, eventType TEXT NOT NULL, amountMinor INTEGER NOT NULL, currency TEXT NOT NULL, accountId TEXT NOT NULL, category TEXT, frequency TEXT NOT NULL, nextDueDateEpochDay INTEGER NOT NULL, status TEXT NOT NULL, autoCreateDraft INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_items_status ON recurring_items(status)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_items_nextDueDateEpochDay ON recurring_items(nextDueDateEpochDay)")
        }
    }

    val MIGRATION_7_8: Migration = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE goals ADD COLUMN currentAmountMinor INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE recurring_items ADD COLUMN anchorDayOfMonth INTEGER NOT NULL DEFAULT 1")
            db.execSQL("CREATE TABLE IF NOT EXISTS budgets (id TEXT NOT NULL PRIMARY KEY, category TEXT NOT NULL, monthlyLimitMinor INTEGER NOT NULL, currency TEXT NOT NULL, status TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_budgets_category ON budgets(category)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_budgets_status ON budgets(status)")
        }
    }
}
