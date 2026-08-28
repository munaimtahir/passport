package pk.vexel.financepassport.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name")
    suspend fun getAll(): List<AccountEntity>
    @Query("SELECT * FROM accounts WHERE status = 'ACTIVE' ORDER BY name")
    fun observeActive(): Flow<List<AccountEntity>>
    @Query("SELECT * FROM accounts ORDER BY status, name")
    fun observeAll(): Flow<List<AccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AccountEntity?

    @Query("UPDATE accounts SET status = 'ARCHIVED', updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archive(id: String, updatedAt: Long)

    @Query("UPDATE accounts SET status = 'ACTIVE', updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun reactivate(id: String, updatedAt: Long)

    @Query("UPDATE accounts SET name = :name, accountType = :accountType, context = :context, openingBalanceMinor = :openingBalanceMinor, institution = :institution, notes = :notes, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateDetails(id: String, name: String, accountType: String, context: String?, openingBalanceMinor: Long, institution: String?, notes: String?, updatedAt: Long)
}

@Dao
interface IncomeSourceDao {
    @Query("SELECT * FROM income_sources ORDER BY name")
    suspend fun getAll(): List<IncomeSourceEntity>
    @Query("SELECT * FROM income_sources WHERE status = 'ACTIVE' ORDER BY name")
    fun observeActive(): Flow<List<IncomeSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(incomeSource: IncomeSourceEntity)

    @Query("SELECT * FROM income_sources WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): IncomeSourceEntity?

    @Query("UPDATE income_sources SET status = 'ARCHIVED', updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archive(id: String, updatedAt: Long)
}

@Dao
interface WealthDao {
    @Query("SELECT * FROM assets ORDER BY title")
    suspend fun getAllAssets(): List<AssetEntity>

    @Query("SELECT * FROM liabilities ORDER BY title")
    suspend fun getAllLiabilities(): List<LiabilityEntity>
    @Query("SELECT * FROM assets WHERE status = 'ACTIVE' ORDER BY title")
    fun observeAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM liabilities WHERE status = 'ACTIVE' ORDER BY title")
    fun observeLiabilities(): Flow<List<LiabilityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAsset(asset: AssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLiability(liability: LiabilityEntity)

    @Query("UPDATE assets SET currentEstimatedValueMinor = :valueMinor, status = 'ACTIVE' WHERE id = :id")
    suspend fun updateAssetValue(id: String, valueMinor: Long)

    @Query("UPDATE assets SET status = 'ARCHIVED', disposalDateEpochDay = :dateEpochDay, disposalValueMinor = :valueMinor WHERE id = :id")
    suspend fun archiveAsset(id: String, dateEpochDay: Long, valueMinor: Long)

    @Query("UPDATE liabilities SET outstandingAmountMinor = :outstandingMinor, status = :status WHERE id = :id")
    suspend fun updateLiabilityOutstanding(id: String, outstandingMinor: Long, status: String)

    @Query("SELECT * FROM assets WHERE id = :id LIMIT 1")
    suspend fun getAssetById(id: String): AssetEntity?

    @Query("SELECT * FROM liabilities WHERE id = :id LIMIT 1")
    suspend fun getLiabilityById(id: String): LiabilityEntity?
}

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investment_events ORDER BY dateEpochDay DESC")
    fun observeAll(): Flow<List<InvestmentEventEntity>>
    @Query("SELECT * FROM investment_events ORDER BY dateEpochDay DESC")
    suspend fun getAll(): List<InvestmentEventEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(event: InvestmentEventEntity)
}

@Dao
interface ReceivableDao {
    @Query("SELECT * FROM receivables ORDER BY status, title")
    fun observeAll(): Flow<List<ReceivableEntity>>
    @Query("SELECT * FROM receivables WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReceivableEntity?
    @Query("SELECT * FROM receivables ORDER BY title")
    suspend fun getAll(): List<ReceivableEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: ReceivableEntity)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY status, title")
    fun observeAll(): Flow<List<GoalEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: GoalEntity)
    @Query("SELECT * FROM goals ORDER BY title")
    suspend fun getAll(): List<GoalEntity>
    @Query("SELECT * FROM goals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GoalEntity?
    @Query("UPDATE goals SET currentAmountMinor = :currentAmountMinor, status = :status WHERE id = :id")
    suspend fun updateProgress(id: String, currentAmountMinor: Long, status: String)
}

@Dao
interface RecurringItemDao {
    @Query("SELECT * FROM recurring_items WHERE status = 'ACTIVE' ORDER BY nextDueDateEpochDay, title")
    fun observeActive(): Flow<List<RecurringItemEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: RecurringItemEntity)
    @Query("UPDATE recurring_items SET status = 'PAUSED', updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun pause(id: String, updatedAt: Long)
    @Query("SELECT * FROM recurring_items ORDER BY nextDueDateEpochDay, title")
    suspend fun getAll(): List<RecurringItemEntity>
    @Query("SELECT * FROM recurring_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurringItemEntity?
    @Query("SELECT * FROM recurring_items WHERE status = 'ACTIVE' AND nextDueDateEpochDay <= :today ORDER BY nextDueDateEpochDay")
    suspend fun getDueActive(today: Long): List<RecurringItemEntity>
    @Query("UPDATE recurring_items SET nextDueDateEpochDay = :nextDueDateEpochDay, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun advanceDueDate(id: String, nextDueDateEpochDay: Long, updatedAt: Long)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE status = 'ACTIVE' ORDER BY category")
    fun observeActive(): Flow<List<BudgetEntity>>
    @Query("SELECT * FROM budgets ORDER BY category")
    suspend fun getAll(): List<BudgetEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: BudgetEntity)
    @Query("SELECT * FROM budgets WHERE category = :category LIMIT 1")
    suspend fun getByCategory(category: String): BudgetEntity?
}

@Dao
interface ReconciliationDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(value: WealthReconciliationEntity)

    @Query("SELECT * FROM wealth_reconciliations ORDER BY taxYearId DESC, id DESC")
    fun observeAll(): Flow<List<WealthReconciliationEntity>>
}

@Dao
interface TaxYearDao {
    @Query("SELECT * FROM tax_years WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TaxYearEntity?

    @Query("SELECT * FROM tax_years ORDER BY yearLabel DESC")
    fun observeAll(): Flow<List<TaxYearEntity>>

    @Query("UPDATE tax_years SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)
}

/** Snapshots are a user-recorded convenience, not an immutable source fact: re-recording the same
 * kind for a tax year replaces it, unlike financial events or tax mappings. */
@Dao
interface WealthSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: WealthSnapshotEntity)

    @Query("SELECT * FROM wealth_snapshots WHERE taxYearId = :taxYearId AND kind = :kind LIMIT 1")
    suspend fun get(taxYearId: String, kind: String): WealthSnapshotEntity?

    @Query("SELECT * FROM wealth_snapshots WHERE taxYearId = :taxYearId ORDER BY kind")
    fun observeForYear(taxYearId: String): Flow<List<WealthSnapshotEntity>>

    @Query("SELECT * FROM wealth_snapshots ORDER BY taxYearId, kind")
    suspend fun getAll(): List<WealthSnapshotEntity>
}

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents ORDER BY createdAtEpochMillis")
    suspend fun getAll(): List<DocumentEntity>
    @Query("SELECT * FROM documents ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface DocumentLinkDao {
    @Query("SELECT * FROM document_links WHERE documentId = :documentId ORDER BY entityType, entityId")
    suspend fun getForDocument(documentId: String): List<DocumentLinkEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: DocumentLinkEntity): Long

    @Query("SELECT * FROM document_links ORDER BY documentId")
    suspend fun getAll(): List<DocumentLinkEntity>

    @Query("DELETE FROM document_links WHERE documentId = :documentId")
    suspend fun deleteForDocument(documentId: String)
}

@Dao
interface OfficialRecordDao {
    @Query("SELECT * FROM official_records ORDER BY recordType, title")
    fun observeAll(): Flow<List<OfficialRecordEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: OfficialRecordEntity)
    @Query("SELECT * FROM official_records ORDER BY title")
    suspend fun getAll(): List<OfficialRecordEntity>
}

@Dao
interface FinancialEventDao {
    @Query("SELECT * FROM financial_events ORDER BY dateEpochDay")
    suspend fun getAll(): List<FinancialEventEntity>
    @Query("SELECT * FROM financial_events WHERE deletedAtEpochMillis IS NULL ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC")
    fun observeActive(): Flow<List<FinancialEventEntity>>

    @Query("SELECT * FROM financial_events WHERE deletedAtEpochMillis IS NULL ORDER BY dateEpochDay DESC, createdAtEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<FinancialEventEntity>>

    @Query("SELECT COALESCE(SUM(CASE WHEN eventType = 'EXPENSE' THEN -amountMinor ELSE amountMinor END), 0) FROM financial_events WHERE accountId = :accountId AND deletedAtEpochMillis IS NULL")
    fun observeAccountMovement(accountId: String): Flow<Long>

    @Query("SELECT COUNT(*) FROM financial_events WHERE deletedAtEpochMillis IS NULL")
    fun observeActiveCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: FinancialEventEntity)

    @Query("SELECT * FROM financial_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FinancialEventEntity?

    @Query("DELETE FROM financial_events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(events: List<FinancialEventEntity>)

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM financial_events WHERE eventType = 'INCOME' AND deletedAtEpochMillis IS NULL")
    fun observeIncomeMinor(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amountMinor), 0) FROM financial_events WHERE eventType = 'EXPENSE' AND deletedAtEpochMillis IS NULL")
    fun observeExpenseMinor(): Flow<Long>

    @Query(
        "SELECT COALESCE(SUM(CASE WHEN eventType = 'EXPENSE' THEN -amountMinor ELSE amountMinor END), 0) " +
            "FROM financial_events WHERE deletedAtEpochMillis IS NULL AND accountId IN (SELECT id FROM accounts WHERE status = 'ACTIVE')",
    )
    fun observeActiveAccountsMovement(): Flow<Long>

    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM financial_events " +
            "WHERE eventType = 'INCOME' AND deletedAtEpochMillis IS NULL AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay",
    )
    fun observeIncomeMinorInRange(startEpochDay: Long, endEpochDay: Long): Flow<Long>

    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM financial_events " +
            "WHERE eventType = 'EXPENSE' AND deletedAtEpochMillis IS NULL AND dateEpochDay BETWEEN :startEpochDay AND :endEpochDay",
    )
    fun observeExpenseMinorInRange(startEpochDay: Long, endEpochDay: Long): Flow<Long>
}

@Dao
interface TransferLinkDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(link: TransferLinkEntity)
}

@Dao
interface TaxItemDao {
    @Query("SELECT * FROM tax_items ORDER BY dateEpochDay")
    suspend fun getAll(): List<TaxItemEntity>
    @Query("SELECT * FROM tax_items ORDER BY dateEpochDay DESC")
    fun observeAll(): Flow<List<TaxItemEntity>>

    @Query("SELECT * FROM tax_items WHERE id = :id")
    suspend fun getById(id: String): TaxItemEntity?

    @Query("SELECT * FROM tax_items WHERE taxYearId = :taxYearId ORDER BY dateEpochDay DESC")
    fun observeForYear(taxYearId: String): Flow<List<TaxItemEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(item: TaxItemEntity): Long

    @Query("UPDATE tax_items SET reviewState = :state, exclusionReason = :reason, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateReview(id: String, state: String, reason: String?, updatedAt: Long)

    @Query("UPDATE tax_items SET taxEventType = :taxEventType, reviewState = :state, exclusionReason = :reason, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateClassification(id: String, taxEventType: String, state: String, reason: String?, updatedAt: Long)

    @Query("UPDATE tax_items SET evidenceState = :evidenceState, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateEvidenceState(id: String, evidenceState: String, updatedAt: Long)
}

@Dao
interface TaxMappingDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(mapping: TaxMappingEntity)

    @Query("SELECT * FROM tax_mappings WHERE taxItemId = :taxItemId ORDER BY createdAtEpochMillis")
    suspend fun getForTaxItem(taxItemId: String): List<TaxMappingEntity>

    @Query("SELECT * FROM tax_mappings WHERE taxItemId = :taxItemId ORDER BY createdAtEpochMillis DESC")
    fun observeForTaxItem(taxItemId: String): Flow<List<TaxMappingEntity>>

    @Query("SELECT * FROM tax_mappings WHERE taxItemId = :taxItemId AND supersededByMappingId IS NULL LIMIT 1")
    suspend fun getActiveForTaxItem(taxItemId: String): TaxMappingEntity?

    @Query("UPDATE tax_mappings SET supersededByMappingId = :supersededByMappingId WHERE id = :id")
    suspend fun markSuperseded(id: String, supersededByMappingId: String)

    @Query("SELECT * FROM tax_mappings ORDER BY createdAtEpochMillis")
    suspend fun getAll(): List<TaxMappingEntity>
}

@Dao
interface TaxDraftDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDraft(draft: TaxAnnualDraftEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLines(lines: List<TaxDraftLineEntity>)

    @Query("SELECT * FROM tax_annual_drafts ORDER BY generatedAtEpochMillis DESC")
    fun observeDrafts(): Flow<List<TaxAnnualDraftEntity>>

    @Query("SELECT COALESCE(MAX(draftVersion), 0) FROM tax_annual_drafts WHERE taxYearId = :taxYearId")
    suspend fun maxVersion(taxYearId: String): Int

    @Query("SELECT * FROM tax_draft_lines WHERE draftId = :draftId ORDER BY sectionCode, categoryCode")
    suspend fun getLines(draftId: String): List<TaxDraftLineEntity>

    @Query("SELECT * FROM tax_annual_drafts ORDER BY generatedAtEpochMillis DESC")
    suspend fun getAll(): List<TaxAnnualDraftEntity>
}

@Dao
interface TaxIssueDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(issues: List<TaxIssueEntity>)

    @Query("SELECT * FROM tax_issues ORDER BY status, createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<TaxIssueEntity>>

    @Query("SELECT * FROM tax_issues WHERE draftId = :draftId ORDER BY code, id")
    suspend fun getForDraft(draftId: String): List<TaxIssueEntity>

    @Query("SELECT * FROM tax_issues ORDER BY createdAtEpochMillis DESC")
    suspend fun getAll(): List<TaxIssueEntity>
}

@Dao
interface CalendarDao {
    @Query("SELECT * FROM calendar_items WHERE status = 'OPEN' ORDER BY dueAtEpochMillis")
    fun observeOpen(): Flow<List<CalendarItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CalendarItemEntity)

    @Query("UPDATE calendar_items SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE calendar_items SET dueAtEpochMillis = :dueAtEpochMillis, status = 'OPEN' WHERE id = :id")
    suspend fun updateSchedule(id: String, dueAtEpochMillis: Long)

    @Query("SELECT * FROM calendar_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CalendarItemEntity?
}

@Dao
interface UtilityBillDao {
    @Query("SELECT * FROM utility_bill_profiles ORDER BY name")
    fun observeAll(): Flow<List<UtilityBillProfileEntity>>

    @Query("SELECT * FROM utility_bill_profiles ORDER BY name")
    suspend fun getAll(): List<UtilityBillProfileEntity>

    @Query("SELECT * FROM utility_bill_profiles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): UtilityBillProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UtilityBillProfileEntity)

    // See MonthlyBillOccurrenceDao.update()'s comment: upsert() (INSERT OR REPLACE) deletes and
    // reinserts the row, which cascade-deletes every monthly_bill_occurrences row (and, through
    // that, their payment_records) for this profile via ON DELETE CASCADE. Editing or
    // reactivating a profile that already has billing history must never touch that history, so
    // both go through a real UPDATE instead; upsert() stays for creating a brand-new profile.
    @Update
    suspend fun update(profile: UtilityBillProfileEntity)

    @Query("UPDATE utility_bill_profiles SET status = :status, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)
    
    @Query("DELETE FROM utility_bill_profiles WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface MonthlyBillOccurrenceDao {
    @Query("SELECT * FROM monthly_bill_occurrences ORDER BY billingYear DESC, billingMonth DESC")
    fun observeAll(): Flow<List<MonthlyBillOccurrenceEntity>>

    @Query("SELECT * FROM monthly_bill_occurrences WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MonthlyBillOccurrenceEntity?

    @Query("SELECT * FROM monthly_bill_occurrences WHERE profileId = :profileId ORDER BY billingYear DESC, billingMonth DESC")
    fun observeByProfile(profileId: String): Flow<List<MonthlyBillOccurrenceEntity>>

    @Query("SELECT * FROM monthly_bill_occurrences WHERE profileId = :profileId ORDER BY billingYear DESC, billingMonth DESC")
    suspend fun getByProfile(profileId: String): List<MonthlyBillOccurrenceEntity>

    @Query("SELECT * FROM monthly_bill_occurrences WHERE profileId = :profileId AND billingYear = :year AND billingMonth = :month LIMIT 1")
    suspend fun getForMonth(profileId: String, year: Int, month: Int): MonthlyBillOccurrenceEntity?

    @Query("SELECT * FROM monthly_bill_occurrences WHERE status = :status ORDER BY expectedDueDateEpochDay ASC")
    fun observeByStatus(status: String): Flow<List<MonthlyBillOccurrenceEntity>>

    @Query("SELECT * FROM monthly_bill_occurrences WHERE status = :status ORDER BY expectedDueDateEpochDay ASC")
    suspend fun getByStatus(status: String): List<MonthlyBillOccurrenceEntity>

    // ABORT (not REPLACE): this is only ever called to insert a brand-new occurrence — every
    // call site checks non-existence first (UtilityRecurrenceEngine's generation loop checks
    // getForMonth() == null; the "add missing historical occurrence" dialog checks the in-memory
    // list). If a genuine bug or race ever produced a duplicate on the (profileId, billingYear,
    // billingMonth) unique index anyway, REPLACE would silently delete the existing occurrence —
    // cascade-deleting its payment_records via ON DELETE CASCADE — instead of surfacing the bug.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun upsert(occurrence: MonthlyBillOccurrenceEntity)

    // @Update issues a plain UPDATE statement, never a delete, so it cannot cascade-delete the
    // occurrence's payment_records/bill_attachments children via their ON DELETE CASCADE foreign
    // keys. Always use this for editing an occurrence that may already have a payment recorded
    // against it; reserve upsert() for creating occurrences that don't exist yet.
    @Update
    suspend fun update(occurrence: MonthlyBillOccurrenceEntity)

    @Query("DELETE FROM monthly_bill_occurrences WHERE id = :id")
    suspend fun delete(id: String)
    
    @Query("SELECT * FROM monthly_bill_occurrences ORDER BY billingYear DESC, billingMonth DESC")
    suspend fun getAll(): List<MonthlyBillOccurrenceEntity>
}

@Dao
interface PaymentRecordDao {
    @Query("SELECT * FROM payment_records WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PaymentRecordEntity?

    @Query("SELECT * FROM payment_records WHERE occurrenceId = :occurrenceId LIMIT 1")
    suspend fun getForOccurrence(occurrenceId: String): PaymentRecordEntity?

    @Query("SELECT * FROM payment_records WHERE financialEventId = :financialEventId LIMIT 1")
    suspend fun getForFinancialEvent(financialEventId: String): PaymentRecordEntity?

    @Query("SELECT * FROM payment_records WHERE occurrenceId = :occurrenceId LIMIT 1")
    fun observeForOccurrence(occurrenceId: String): Flow<PaymentRecordEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(payment: PaymentRecordEntity)

    @Update
    suspend fun update(payment: PaymentRecordEntity)

    @Query("UPDATE payment_records SET amountPaidMinor = :amountPaid, paymentDateEpochDay = :paymentDate, paymentMode = :mode, accountId = :accountId, bankName = :bank, transactionReference = :reference, notes = :notes, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateDetails(id: String, amountPaid: Long, paymentDate: Long, mode: String, accountId: String, bank: String?, reference: String?, notes: String?, updatedAt: Long)

    @Query("DELETE FROM payment_records WHERE id = :id")
    suspend fun delete(id: String)
    
    @Query("SELECT * FROM payment_records ORDER BY paymentDateEpochDay DESC")
    suspend fun getAll(): List<PaymentRecordEntity>
}

@Dao
interface BillAttachmentDao {
    @Query("SELECT * FROM bill_attachments WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BillAttachmentEntity?

    @Query("SELECT * FROM bill_attachments WHERE linkedId = :linkedId")
    suspend fun getForLinkedEntity(linkedId: String): List<BillAttachmentEntity>

    @Query("SELECT * FROM bill_attachments WHERE linkedId IN (:linkedIds)")
    suspend fun getForLinkedEntities(linkedIds: List<String>): List<BillAttachmentEntity>

    @Query("SELECT * FROM bill_attachments WHERE linkedId = :linkedId")
    fun observeForLinkedEntity(linkedId: String): Flow<List<BillAttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: BillAttachmentEntity)

    @Query("DELETE FROM bill_attachments WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM bill_attachments WHERE linkedId IN (:linkedIds)")
    suspend fun deleteForLinkedEntities(linkedIds: List<String>)
    
    @Query("SELECT * FROM bill_attachments")
    suspend fun getAll(): List<BillAttachmentEntity>
}
