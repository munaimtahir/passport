package pk.vexel.financepassport.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

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
        CalendarItemEntity::class,
        DocumentEntity::class,
        DocumentLinkEntity::class,
        OfficialRecordEntity::class,
        ChangeLogEntity::class,
        BudgetEntity::class,
    ],
    version = 9,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun financialEventDao(): FinancialEventDao
    abstract fun transferLinkDao(): TransferLinkDao
    abstract fun wealthDao(): WealthDao
    abstract fun documentDao(): DocumentDao
    abstract fun documentLinkDao(): DocumentLinkDao
    abstract fun taxItemDao(): TaxItemDao
    abstract fun taxMappingDao(): TaxMappingDao
    abstract fun taxDraftDao(): TaxDraftDao
    abstract fun taxIssueDao(): TaxIssueDao
    abstract fun calendarDao(): CalendarDao
    abstract fun reconciliationDao(): ReconciliationDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun receivableDao(): ReceivableDao
    abstract fun goalDao(): GoalDao
    abstract fun recurringItemDao(): RecurringItemDao
    abstract fun officialRecordDao(): OfficialRecordDao
    abstract fun budgetDao(): BudgetDao
}
