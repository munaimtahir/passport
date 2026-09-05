package pk.vexel.financepassport.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Wave C: Categories
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `categories` (
                `id` TEXT NOT NULL, 
                `name` TEXT NOT NULL, 
                `family` TEXT NOT NULL, 
                `parentId` TEXT, 
                `status` TEXT NOT NULL, 
                `createdAtEpochMillis` INTEGER NOT NULL, 
                `updatedAtEpochMillis` INTEGER NOT NULL, 
                PRIMARY KEY(`id`)
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_family` ON `categories` (`family`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_status` ON `categories` (`status`)")
        
        // Add Category mapping to FinancialEventEntity and BudgetEntity
        // contextId already exists in schema 15; these are the new Wave C source/link fields.
        db.execSQL("ALTER TABLE `financial_events` ADD COLUMN `categoryId` TEXT")
        db.execSQL("ALTER TABLE `financial_events` ADD COLUMN `counterparty` TEXT")
        db.execSQL("ALTER TABLE `financial_events` ADD COLUMN `sourceTemplateId` TEXT")
        db.execSQL("ALTER TABLE `financial_events` ADD COLUMN `sourceOccurrenceId` TEXT")
        db.execSQL("ALTER TABLE `financial_events` ADD COLUMN `groupId` TEXT")
        db.execSQL("ALTER TABLE `financial_events` ADD COLUMN `cashEffectMinor` INTEGER")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_financial_events_sourceOccurrenceId` ON `financial_events` (`sourceOccurrenceId`)")
        
        // Wave C: Recurring Templates and Occurrences
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `recurring_templates` (
                `id` TEXT NOT NULL, 
                `title` TEXT NOT NULL, 
                `eventType` TEXT NOT NULL, 
                `amountMode` TEXT NOT NULL, 
                `expectedAmountMinor` INTEGER, 
                `currency` TEXT NOT NULL, 
                `frequency` TEXT NOT NULL, 
                `intervalCount` INTEGER NOT NULL, 
                `startDateEpochDay` INTEGER NOT NULL, 
                `endDateEpochDay` INTEGER, 
                `defaultAccountId` TEXT, 
                `defaultContextId` TEXT, 
                `defaultCategoryId` TEXT, 
                `counterparty` TEXT, 
                `notes` TEXT, 
                `status` TEXT NOT NULL, 
                `createdAtEpochMillis` INTEGER NOT NULL, 
                `updatedAtEpochMillis` INTEGER NOT NULL, 
                PRIMARY KEY(`id`)
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_templates_status` ON `recurring_templates` (`status`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `expected_occurrences` (
                `id` TEXT NOT NULL, 
                `templateId` TEXT NOT NULL, 
                `dueDateEpochDay` INTEGER NOT NULL, 
                `expectedAmountMinor` INTEGER, 
                `status` TEXT NOT NULL, 
                `confirmedEventId` TEXT, 
                `createdAtEpochMillis` INTEGER NOT NULL, 
                `updatedAtEpochMillis` INTEGER NOT NULL, 
                PRIMARY KEY(`id`), 
                FOREIGN KEY(`templateId`) REFERENCES `recurring_templates`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expected_occurrences_templateId` ON `expected_occurrences` (`templateId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expected_occurrences_status` ON `expected_occurrences` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_expected_occurrences_dueDateEpochDay` ON `expected_occurrences` (`dueDateEpochDay`)")

        // Keep recurring_items intact for backward compatibility and restore support. New code
        // uses the normalized template/occurrence tables; no historical rows are destroyed.

        // Wave D & E: Liability, Receivable, Investment updates
        // Since we are modifying Liabilities and Receivables to match exact semantics, we add the missing columns
        db.execSQL("ALTER TABLE `liabilities` ADD COLUMN `contextId` TEXT")
        db.execSQL("ALTER TABLE `liabilities` ADD COLUMN `notes` TEXT")
        db.execSQL("ALTER TABLE `liabilities` ADD COLUMN `linkedAssetId` TEXT")

        db.execSQL("ALTER TABLE `receivables` ADD COLUMN `receivableType` TEXT NOT NULL DEFAULT 'OTHER'")
        db.execSQL("ALTER TABLE `receivables` ADD COLUMN `contextId` TEXT")
        db.execSQL("ALTER TABLE `receivables` ADD COLUMN `notes` TEXT")
        db.execSQL("ALTER TABLE `receivables` ADD COLUMN `activityDateEpochDay` INTEGER")
        db.execSQL("ALTER TABLE `receivables` ADD COLUMN `receivedDateEpochDay` INTEGER")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `settlement_events` (
                `id` TEXT NOT NULL,
                `entityType` TEXT NOT NULL, -- LIABILITY or RECEIVABLE
                `entityId` TEXT NOT NULL,
                `financialEventId` TEXT NOT NULL,
                `principalAmountMinor` INTEGER NOT NULL,
                `financingCostMinor` INTEGER NOT NULL,
                `dateEpochDay` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_settlement_events_entityId` ON `settlement_events` (`entityId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_settlement_events_financialEventId` ON `settlement_events` (`financialEventId`)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `simple_investments` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `institution` TEXT,
                `contextId` TEXT,
                `acquisitionDateEpochDay` INTEGER NOT NULL,
                `principalInvestedMinor` INTEGER NOT NULL,
                `currentEstimatedValueMinor` INTEGER NOT NULL,
                `currency` TEXT NOT NULL,
                `maturityDateEpochDay` INTEGER,
                `notes` TEXT,
                `status` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_simple_investments_contextId` ON `simple_investments` (`contextId`)")

        // Keep investment_events intact: existing investment history remains auditable while the
        // simple position model is introduced additively.
    }
}
