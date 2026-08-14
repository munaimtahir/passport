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
}
