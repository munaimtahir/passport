package pk.vexel.financepassport.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

// Single source of truth for the schema version. Referenced here AND wherever the encrypted-backup
// manifest's schemaVersion is written (FinanceRepository) so the two can never drift apart again —
// this project has already hit that exact bug twice (the 8->9 and 9->10 boundaries), both times
// from a hardcoded literal at the manifest call site.
const val DATABASE_VERSION = 11

@Database(
    entities = [
        UserProfileEntity::class,
        AccountEntity::class,
        FinancialEventEntity::class,
        AssetEntity::class,
        LiabilityEntity::class,
        InvestmentEventEntity::class,
        ReceivableEntity::class,
        GoalEntity::class,
        RecurringItemEntity::class,
        TransferLinkEntity::class,
        TaxYearEntity::class,
        TaxItemEntity::class,
        TaxMappingEntity::class,
        TaxAnnualDraftEntity::class,
        TaxDraftLineEntity::class,
        TaxIssueEntity::class,
        WealthReconciliationEntity::class,
        WealthSnapshotEntity::class,
        CalendarItemEntity::class,
        DocumentEntity::class,
        DocumentLinkEntity::class,
        OfficialRecordEntity::class,
        ChangeLogEntity::class,
        BudgetEntity::class,
        IncomeSourceEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun incomeSourceDao(): IncomeSourceDao
    abstract fun financialEventDao(): FinancialEventDao
    abstract fun transferLinkDao(): TransferLinkDao
    abstract fun wealthDao(): WealthDao
    abstract fun documentDao(): DocumentDao
    abstract fun documentLinkDao(): DocumentLinkDao
    abstract fun taxItemDao(): TaxItemDao
    abstract fun taxMappingDao(): TaxMappingDao
    abstract fun taxYearDao(): TaxYearDao
    abstract fun taxDraftDao(): TaxDraftDao
    abstract fun taxIssueDao(): TaxIssueDao
    abstract fun calendarDao(): CalendarDao
    abstract fun reconciliationDao(): ReconciliationDao
    abstract fun wealthSnapshotDao(): WealthSnapshotDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun receivableDao(): ReceivableDao
    abstract fun goalDao(): GoalDao
    abstract fun recurringItemDao(): RecurringItemDao
    abstract fun officialRecordDao(): OfficialRecordDao
    abstract fun budgetDao(): BudgetDao
}
