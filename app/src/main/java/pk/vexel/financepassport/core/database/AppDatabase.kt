package pk.vexel.financepassport.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

// Single source of truth for the schema version. Referenced here AND wherever the encrypted-backup
// manifest's schemaVersion is written (FinanceRepository) so the two can never drift apart again —
// this project has already hit that exact bug twice (the 8->9 and 9->10 boundaries), both times
// from a hardcoded literal at the manifest call site.
const val DATABASE_VERSION = 17

@Database(
    entities = [
        UserProfileEntity::class,
        AccountEntity::class,
        FinancialContextEntity::class,
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
        PositionSnapshotEntity::class,
        CalendarItemEntity::class,
        DocumentEntity::class,
        DocumentLinkEntity::class,
        OfficialRecordEntity::class,
        ChangeLogEntity::class,
        BudgetEntity::class,
        IncomeSourceEntity::class,
        UtilityBillProfileEntity::class,
        MonthlyBillOccurrenceEntity::class,
        PaymentRecordEntity::class,
        BillAttachmentEntity::class,
        CategoryEntity::class,
        RecurringTemplateEntity::class,
        ExpectedOccurrenceEntity::class,
        SettlementEventEntity::class,
        SimpleInvestmentEntity::class,
    ],
    version = DATABASE_VERSION,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financialContextDao(): FinancialContextDao
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
    abstract fun positionSnapshotDao(): PositionSnapshotDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun receivableDao(): ReceivableDao
    abstract fun goalDao(): GoalDao
    abstract fun recurringItemDao(): RecurringItemDao
    abstract fun officialRecordDao(): OfficialRecordDao
    abstract fun budgetDao(): BudgetDao
    abstract fun utilityBillDao(): UtilityBillDao
    abstract fun monthlyBillOccurrenceDao(): MonthlyBillOccurrenceDao
    abstract fun paymentRecordDao(): PaymentRecordDao
    abstract fun billAttachmentDao(): BillAttachmentDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringTemplateDao(): RecurringTemplateDao
    abstract fun expectedOccurrenceDao(): ExpectedOccurrenceDao
    abstract fun settlementEventDao(): SettlementEventDao
    abstract fun simpleInvestmentDao(): SimpleInvestmentDao
}
