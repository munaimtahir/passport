package pk.vexel.financepassport.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrateV2ToV7PreservesExistingTablesAndAddsOfficialRecordsIssuesAndRecurringItems() {
        helper.createDatabase("migration-v2", 2).apply {
            execSQL("INSERT INTO user_profiles (id, displayName, baseCurrency, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('user', 'Demo', 'PKR', 1, 1)")
            close()
        }

        helper.runMigrationsAndValidate(
            "migration-v2",
            7,
            true,
            DatabaseProvider.MIGRATION_2_3,
            DatabaseProvider.MIGRATION_3_4,
            DatabaseProvider.MIGRATION_4_5,
            DatabaseProvider.MIGRATION_5_6,
            DatabaseProvider.MIGRATION_6_7,
        ).use { database ->
            database.query("SELECT COUNT(*) FROM user_profiles").use { cursor ->
                cursor.moveToFirst()
                check(cursor.getInt(0) == 1)
            }
            database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'official_records'").use { cursor ->
                check(cursor.moveToFirst())
            }
            database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'tax_issues'").use { cursor ->
                check(cursor.moveToFirst())
            }
            database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'recurring_items'").use { cursor ->
                check(cursor.moveToFirst())
            }
        }
    }

    @Test
    fun migrateV7ToV8PreservesExistingGoalsAndAddsBudgetsWithBackfilledDefaults() {
        helper.createDatabase("migration-v7", 7).apply {
            execSQL("INSERT INTO goals (id, title, goalType, targetAmountMinor, targetDateEpochDay, status) VALUES ('goal', 'Car', 'CUSTOM', 500000, NULL, 'OPEN')")
            execSQL("INSERT INTO recurring_items (id, title, eventType, amountMinor, currency, accountId, category, frequency, nextDueDateEpochDay, status, autoCreateDraft, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('rec', 'Rent', 'EXPENSE', 50000, 'PKR', 'acc', NULL, 'MONTHLY', 19000, 'ACTIVE', 1, 1, 1)")
            close()
        }

        helper.runMigrationsAndValidate("migration-v7", 8, true, DatabaseProvider.MIGRATION_7_8).use { database ->
            database.query("SELECT currentAmountMinor FROM goals WHERE id = 'goal'").use { cursor ->
                cursor.moveToFirst()
                check(cursor.getLong(0) == 0L) { "Pre-existing goals must backfill currentAmountMinor to zero" }
            }
            database.query("SELECT anchorDayOfMonth FROM recurring_items WHERE id = 'rec'").use { cursor ->
                cursor.moveToFirst()
                check(cursor.getInt(0) == 1) { "Pre-existing recurring items must backfill anchorDayOfMonth to 1" }
            }
            database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'budgets'").use { cursor ->
                check(cursor.moveToFirst())
            }
        }
    }

    @Test
    fun migrateV8ToV9AddsTaxMappingsTable() {
        helper.createDatabase("migration-v8", 8).apply {
            execSQL("INSERT INTO tax_years (id, jurisdictionCode, yearLabel, startDateEpochDay, endDateEpochDay, rulesetVersion, status) VALUES ('PK-2026', 'PK', '2026', 0, 365, 'pk-structural-1', 'OPEN')")
            execSQL("INSERT INTO tax_items (id, taxYearId, sourceType, sourceId, taxEventType, dateEpochDay, grossAmountMinor, taxWithheldMinor, currency, description, reviewState, evidenceState, exclusionReason, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('item-1', 'PK-2026', 'financial_event', 'event-1', 'EMPLOYMENT_INCOME', 10, 100000, NULL, 'PKR', 'Salary', 'CAPTURED', 'REQUESTED', NULL, 1, 1)")
            close()
        }

        helper.runMigrationsAndValidate("migration-v8", 9, true, DatabaseProvider.MIGRATION_8_9).use { database ->
            database.query("SELECT COUNT(*) FROM tax_items WHERE id = 'item-1'").use { cursor ->
                cursor.moveToFirst()
                check(cursor.getInt(0) == 1) { "Pre-existing tax items must survive the migration" }
            }
            database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'tax_mappings'").use { cursor ->
                check(cursor.moveToFirst())
            }
            database.execSQL("INSERT INTO tax_mappings (id, taxItemId, rulesetVersion, taxEventType, sectionCode, categoryCode, source, overrideReason, supersededByMappingId, createdAtEpochMillis) VALUES ('mapping-1', 'item-1', 'pk-structural-1', 'EMPLOYMENT_INCOME', 'INCOME', 'EMPLOYMENT_INCOME', 'SYSTEM_GENERATED', NULL, NULL, 1)")
            database.query("SELECT COUNT(*) FROM tax_mappings WHERE taxItemId = 'item-1'").use { cursor ->
                cursor.moveToFirst()
                check(cursor.getInt(0) == 1) { "A mapping row must be insertable against a pre-existing tax item" }
            }
        }
    }

    @Test
    fun migrateV9ToV10AddsWealthSnapshotsTable() {
        helper.createDatabase("migration-v9", 9).apply {
            execSQL("INSERT INTO tax_years (id, jurisdictionCode, yearLabel, startDateEpochDay, endDateEpochDay, rulesetVersion, status) VALUES ('PK-2026', 'PK', '2026', 0, 365, 'pk-structural-1', 'OPEN')")
            close()
        }

        helper.runMigrationsAndValidate("migration-v9", 10, true, DatabaseProvider.MIGRATION_9_10).use { database ->
            database.query("SELECT COUNT(*) FROM tax_years WHERE id = 'PK-2026'").use { cursor ->
                cursor.moveToFirst()
                check(cursor.getInt(0) == 1) { "Pre-existing tax years must survive the migration" }
            }
            database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'wealth_snapshots'").use { cursor ->
                check(cursor.moveToFirst())
            }
            database.execSQL("INSERT INTO wealth_snapshots (id, taxYearId, kind, snapshotDateEpochDay, liquidFundsMinor, investmentsValueMinor, assetsValueMinor, receivablesValueMinor, liabilitiesValueMinor, netWealthMinor, createdAtEpochMillis) VALUES ('snap-1', 'PK-2026', 'OPENING', 0, 0, 0, 0, 0, 0, 0, 1)")
            database.query("SELECT COUNT(*) FROM wealth_snapshots WHERE taxYearId = 'PK-2026'").use { cursor ->
                cursor.moveToFirst()
                check(cursor.getInt(0) == 1) { "A snapshot row must be insertable against a pre-existing tax year" }
            }
        }
    }

    @Test
    fun migrateV10ToV11AddsIncomeSourcesTableAndFinancialEventLink() {
        helper.createDatabase("migration-v10", 10).apply {
            execSQL("INSERT INTO accounts (id, name, institution, accountType, maskedIdentifier, encryptedSensitiveIdentifier, currency, openingBalanceMinor, openingBalanceDateEpochDay, status, notes, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('acct-1', 'Salary account', NULL, 'BANK', NULL, NULL, 'PKR', 0, 0, 'ACTIVE', NULL, 1, 1)")
            execSQL("INSERT INTO financial_events (id, eventType, dateEpochDay, amountMinor, currency, accountId, category, description, notes, taxRelevance, deletedAtEpochMillis, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('event-1', 'INCOME', 0, 50000, 'PKR', 'acct-1', NULL, 'Pre-existing salary', NULL, 'UNKNOWN', NULL, 1, 1)")
            close()
        }

        helper.runMigrationsAndValidate("migration-v10", 11, true, DatabaseProvider.MIGRATION_10_11).use { database ->
            database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'income_sources'").use { cursor ->
                check(cursor.moveToFirst()) { "income_sources table must exist after migration" }
            }
            database.query("SELECT incomeSourceId FROM financial_events WHERE id = 'event-1'").use { cursor ->
                cursor.moveToFirst()
                check(cursor.isNull(0)) { "Pre-existing events must default incomeSourceId to NULL, not lose the row" }
            }
            database.execSQL("INSERT INTO income_sources (id, name, sourceType, payerOrEmployer, status, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('src-1', 'Day job', 'EMPLOYMENT', 'Acme Co', 'ACTIVE', 1, 1)")
            database.execSQL("UPDATE financial_events SET incomeSourceId = 'src-1' WHERE id = 'event-1'")
            database.query("SELECT incomeSourceId FROM financial_events WHERE id = 'event-1'").use { cursor ->
                cursor.moveToFirst()
                check(cursor.getString(0) == "src-1") { "incomeSourceId must be settable against a real income source row" }
            }
        }
    }

    @Test
    fun migrateV11ToV12AddsLiabilityStructuralFields() {
        helper.createDatabase("migration-v11", 11).apply {
            execSQL("INSERT INTO liabilities (id, type, title, lender, originalAmountMinor, outstandingAmountMinor, currency, startDateEpochDay, dueDateEpochDay, status) VALUES ('liab-1', 'OTHER', 'Pre-existing loan', NULL, 100000, 100000, 'PKR', 0, NULL, 'ACTIVE')")
            close()
        }

        helper.runMigrationsAndValidate("migration-v11", 12, true, DatabaseProvider.MIGRATION_11_12).use { database ->
            database.query("SELECT interestRateBps, installmentAmountMinor FROM liabilities WHERE id = 'liab-1'").use { cursor ->
                cursor.moveToFirst()
                check(cursor.isNull(0) && cursor.isNull(1)) { "Pre-existing liabilities must default the new columns to NULL, not lose the row" }
            }
            database.execSQL("UPDATE liabilities SET interestRateBps = 1250, installmentAmountMinor = 25000 WHERE id = 'liab-1'")
            database.query("SELECT interestRateBps, installmentAmountMinor FROM liabilities WHERE id = 'liab-1'").use { cursor ->
                cursor.moveToFirst()
                check(cursor.getInt(0) == 1250 && cursor.getLong(1) == 25000L) { "New columns must be settable against a real pre-existing row" }
            }
        }
    }
}
