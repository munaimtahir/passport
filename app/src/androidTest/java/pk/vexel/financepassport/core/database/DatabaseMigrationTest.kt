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
}
