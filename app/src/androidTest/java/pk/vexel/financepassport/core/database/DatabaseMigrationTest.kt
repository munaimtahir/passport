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

    @Test
    fun migrateV12ToV13AddsUtilityBillTables() {
        helper.createDatabase("migration-v12", 12).apply {
            execSQL("INSERT INTO user_profiles (id, displayName, baseCurrency, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('user', 'Demo', 'PKR', 1, 1)")
            close()
        }

        helper.runMigrationsAndValidate("migration-v12", 13, true, DatabaseProvider.MIGRATION_12_13).use { database ->
            database.query("SELECT COUNT(*) FROM user_profiles").use { cursor ->
                cursor.moveToFirst()
                check(cursor.getInt(0) == 1) { "Pre-existing user profiles must survive migration" }
            }
            database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'utility_bill_profiles'").use { cursor ->
                check(cursor.moveToFirst()) { "utility_bill_profiles table must exist after migration" }
            }
            database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'monthly_bill_occurrences'").use { cursor ->
                check(cursor.moveToFirst()) { "monthly_bill_occurrences table must exist after migration" }
            }
            database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'payment_records'").use { cursor ->
                check(cursor.moveToFirst()) { "payment_records table must exist after migration" }
            }
            database.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'bill_attachments'").use { cursor ->
                check(cursor.moveToFirst()) { "bill_attachments table must exist after migration" }
            }
        }
    }

    @Test
    fun migrateV13ToV14PreservesUtilityAndFinanceRowsAndAddsLedgerLinks() {
        helper.createDatabase("migration-v13", 13).apply {
            execSQL("INSERT INTO accounts (id,name,institution,accountType,maskedIdentifier,encryptedSensitiveIdentifier,currency,openingBalanceMinor,openingBalanceDateEpochDay,status,notes,createdAtEpochMillis,updatedAtEpochMillis) VALUES ('account','HBL',NULL,'BANK',NULL,NULL,'PKR',10000000,0,'ACTIVE',NULL,1,1)")
            execSQL("INSERT INTO utility_bill_profiles VALUES ('profile','Home Electricity','Telephone','ref',15,27,'2026-08','ACTIVE',NULL,NULL,'Home',NULL,NULL,'DISABLED',1,1)")
            execSQL("INSERT INTO monthly_bill_occurrences VALUES ('occurrence','profile',2026,8,1,2,NULL,NULL,2000000,'Paid',NULL,'Automatic',1,1)")
            execSQL("INSERT INTO payment_records VALUES ('payment','occurrence',2000000,3,'Cash',NULL,NULL,NULL,1,1)")
            close()
        }

        helper.runMigrationsAndValidate("migration-v13", 14, true, DatabaseProvider.MIGRATION_13_14).use { database ->
            database.query("SELECT context FROM accounts WHERE id='account'").use { cursor ->
                check(cursor.moveToFirst() && cursor.isNull(0))
            }
            database.query("SELECT accountId, financialEventId FROM payment_records WHERE id='payment'").use { cursor ->
                check(cursor.moveToFirst() && cursor.isNull(0) && cursor.isNull(1))
            }
            database.query("SELECT category FROM utility_bill_profiles WHERE id='profile'").use { cursor ->
                check(cursor.moveToFirst() && cursor.getString(0) == "Telephone") { "Legacy utility category must remain readable" }
            }
        }
    }

    @Test
    fun migrateV14ToV15AddsContextsAndBillDefaults() {
        helper.createDatabase("migration-v14-pay", 14).apply { execSQL("INSERT INTO payment_records (id, occurrenceId, amountPaidMinor, paymentDateEpochDay, paymentMode, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('pr1', 'occ1', 100, 1, 'Cash', 1, 1)"); close() }
        helper.runMigrationsAndValidate("migration-v14-pay", 15, true, DatabaseProvider.MIGRATION_14_15).close()
        helper.createDatabase("migration-v14", 14).apply {
            execSQL("INSERT INTO user_profiles (id, displayName, baseCurrency, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('user', 'Demo', 'PKR', 1, 1)")
            execSQL("INSERT INTO financial_events (id, eventType, dateEpochDay, amountMinor, currency, description, taxRelevance, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('fe1', 'INCOME', 1, 1000, 'PKR', 'Desc', 'UNKNOWN', 1, 1)")
            execSQL("INSERT INTO utility_bill_profiles (id, name, category, referenceNumber, issueDayAnchor, dueDayAnchor, recurrenceStartMonth, status, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('ub1', 'K-Electric', 'Electricity', 'REF123', 1, 10, '2024-01', 'ACTIVE', 1, 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(
            "migration-v14",
            15,
            true,
            DatabaseProvider.MIGRATION_14_15,
        )

        val cursor = db.query("SELECT contextId FROM financial_events WHERE id = 'fe1'")
        cursor.moveToFirst()
        assert(cursor.isNull(0))
        cursor.close()

        val billCursor = db.query("SELECT defaultAccountId, defaultContextId, defaultExpenseCategory FROM utility_bill_profiles WHERE id = 'ub1'")
        billCursor.moveToFirst()
        assert(billCursor.isNull(0))
        assert(billCursor.isNull(1))
        assert(billCursor.isNull(2))
        billCursor.close()
    }

    @Test
    fun migrateV15ToV16AddsCategoriesRecurringAndSettlementTablesWithoutDroppingData() {
        helper.createDatabase("migration-v15", 15).apply {
            execSQL("INSERT INTO financial_events (id, eventType, dateEpochDay, amountMinor, currency, description, taxRelevance, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('fe1', 'EXPENSE', 1, 500, 'PKR', 'Groceries', 'UNKNOWN', 1, 1)")
            execSQL("INSERT INTO liabilities (id, type, title, originalAmountMinor, outstandingAmountMinor, currency, startDateEpochDay, status) VALUES ('lb1', 'LOAN', 'Car loan', 100000, 80000, 'PKR', 1, 'ACTIVE')")
            execSQL("INSERT INTO receivables (id, title, counterparty, originalAmountMinor, outstandingAmountMinor, status) VALUES ('rc1', 'Loaned to Ali', 'Ali', 5000, 5000, 'OUTSTANDING')")
            close()
        }

        helper.runMigrationsAndValidate("migration-v15", 16, true, MIGRATION_15_16).use { database ->
            database.query("SELECT description, categoryId, cashEffectMinor FROM financial_events WHERE id = 'fe1'").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getString(0) == "Groceries")
                check(cursor.isNull(1))
                check(cursor.isNull(2))
            }
            database.query("SELECT outstandingAmountMinor, contextId, linkedAssetId FROM liabilities WHERE id = 'lb1'").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getLong(0) == 80000L)
                check(cursor.isNull(1))
                check(cursor.isNull(2))
            }
            database.query("SELECT outstandingAmountMinor, receivableType FROM receivables WHERE id = 'rc1'").use { cursor ->
                check(cursor.moveToFirst())
                check(cursor.getLong(0) == 5000L)
                check(cursor.getString(1) == "OTHER")
            }

            database.execSQL("INSERT INTO categories (id, name, family, status, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('cat1', 'Groceries', 'EXPENSE', 'ACTIVE', 1, 1)")
            database.query("SELECT COUNT(*) FROM categories").use { cursor -> check(cursor.moveToFirst() && cursor.getInt(0) == 1) }

            database.execSQL("INSERT INTO recurring_templates (id, title, eventType, amountMode, currency, frequency, intervalCount, startDateEpochDay, status, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('rt1', 'Rent', 'EXPENSE', 'FIXED', 'PKR', 'MONTHLY', 1, 1, 'ACTIVE', 1, 1)")
            database.execSQL("INSERT INTO expected_occurrences (id, templateId, dueDateEpochDay, status, createdAtEpochMillis, updatedAtEpochMillis) VALUES ('eo1', 'rt1', 2, 'PENDING', 1, 1)")
            database.query("SELECT COUNT(*) FROM expected_occurrences WHERE templateId = 'rt1'").use { cursor -> check(cursor.moveToFirst() && cursor.getInt(0) == 1) }

            database.execSQL("INSERT INTO settlement_events (id, entityType, entityId, financialEventId, principalAmountMinor, financingCostMinor, dateEpochDay, status) VALUES ('se1', 'LIABILITY', 'lb1', 'fe1', 1000, 0, 1, 'POSTED')")
            database.execSQL("INSERT INTO simple_investments (id, title, type, acquisitionDateEpochDay, principalInvestedMinor, currentEstimatedValueMinor, currency, status) VALUES ('si1', 'Mutual fund', 'FUND', 1, 10000, 11000, 'PKR', 'ACTIVE')")
            database.query("SELECT COUNT(*) FROM settlement_events").use { cursor -> check(cursor.moveToFirst() && cursor.getInt(0) == 1) }
            database.query("SELECT COUNT(*) FROM simple_investments").use { cursor -> check(cursor.moveToFirst() && cursor.getInt(0) == 1) }
        }
    }

    @Test
    fun migrateV16ToV17AddsPositionSnapshotsWithoutChangingExistingRows() {
        helper.createDatabase("migration-v16", 16).apply {
            execSQL("INSERT INTO wealth_snapshots (id, taxYearId, kind, snapshotDateEpochDay, liquidFundsMinor, investmentsValueMinor, assetsValueMinor, receivablesValueMinor, liabilitiesValueMinor, netWealthMinor, createdAtEpochMillis) VALUES ('legacy', 'PK-2026', 'OPENING', 1, 10, 20, 30, 40, 5, 95, 1)")
            close()
        }
        helper.runMigrationsAndValidate("migration-v16", 17, true, MIGRATION_16_17).use { database ->
            database.query("SELECT netWealthMinor FROM wealth_snapshots WHERE id = 'legacy'").use { cursor ->
                check(cursor.moveToFirst() && cursor.getLong(0) == 95L)
            }
            database.execSQL("INSERT INTO position_snapshots (id, kind, snapshotDateEpochDay, liquidFundsMinor, investmentsValueMinor, assetsValueMinor, receivablesValueMinor, liabilitiesValueMinor, netWorthMinor, createdAtEpochMillis) VALUES ('position-1', 'MANUAL', 2, 1, 2, 3, 4, 5, 5, 2)")
            database.query("SELECT COUNT(*) FROM position_snapshots").use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 1)
            }
        }
    }
}
