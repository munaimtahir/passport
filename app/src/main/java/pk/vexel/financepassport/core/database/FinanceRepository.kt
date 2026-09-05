package pk.vexel.financepassport.core.database

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pk.vexel.financepassport.core.model.FinancialEvent
import pk.vexel.financepassport.core.model.FinancialEventType
import pk.vexel.financepassport.core.model.Money
import pk.vexel.financepassport.core.model.MinorUnits
import pk.vexel.financepassport.core.model.CategoryBudgetStatus
import pk.vexel.financepassport.core.model.FinancialPosition
import pk.vexel.financepassport.core.model.GoalProgress
import pk.vexel.financepassport.core.model.RecurringFrequency
import pk.vexel.financepassport.core.model.calculateCategoryBudgets
import pk.vexel.financepassport.core.model.calculateFinancialPosition
import pk.vexel.financepassport.core.model.calculateGoalProgress
import pk.vexel.financepassport.core.model.advanceRecurringDueDate
import pk.vexel.financepassport.core.model.UtilityCategory
import pk.vexel.financepassport.core.model.toYearMonth
import pk.vexel.financepassport.core.model.CalendarProjectionSource
import pk.vexel.financepassport.core.model.calendarProjection
import java.time.Instant
import java.util.UUID
import pk.vexel.financepassport.core.export.ExportSnapshot
import pk.vexel.financepassport.core.taxrules.AnnualDraftGenerator
import pk.vexel.financepassport.core.taxrules.DuplicateCandidateInput
import pk.vexel.financepassport.core.taxrules.TaxCandidate
import pk.vexel.financepassport.core.taxrules.TaxEventType
import pk.vexel.financepassport.core.taxrules.TaxIssue
import pk.vexel.financepassport.core.taxrules.TaxRelevance
import pk.vexel.financepassport.core.taxrules.TaxYear
import pk.vexel.financepassport.core.taxrules.defaultPakistanStructuralRules
import pk.vexel.financepassport.core.taxrules.detectDuplicateCandidates
import pk.vexel.financepassport.core.taxrules.StructuralTaxClassifier
import pk.vexel.financepassport.core.security.AppPreferences
import pk.vexel.financepassport.core.security.PinStore
import pk.vexel.financepassport.core.taxrules.WealthReconciliationInput
import pk.vexel.financepassport.core.taxrules.WealthReconciliationResult
import pk.vexel.financepassport.core.taxrules.reconcileWealth
import java.time.LocalDate
import java.time.ZoneId
import android.content.Context
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import android.database.sqlite.SQLiteException
import pk.vexel.financepassport.BuildConfig
import pk.vexel.financepassport.core.security.BackupFile
import pk.vexel.financepassport.core.security.BackupDiskFile
import pk.vexel.financepassport.core.security.BackupPackageService
import pk.vexel.financepassport.core.security.KeystoreCryptoService
import pk.vexel.financepassport.core.calendar.ReminderScheduler
import androidx.work.WorkManager

data class AccountBalance(val account: AccountEntity, val balance: Money)

class FinanceRepository(private val db: AppDatabase) {
    internal val database: AppDatabase get() = db
    val utilityProfiles: Flow<List<UtilityBillProfileEntity>> = db.utilityBillDao().observeAll()
    val monthlyOccurrences: Flow<List<MonthlyBillOccurrenceEntity>> = db.monthlyBillOccurrenceDao().observeAll()

    fun observeOccurrencesByStatus(status: String): Flow<List<MonthlyBillOccurrenceEntity>> =
        db.monthlyBillOccurrenceDao().observeByStatus(status)

    suspend fun addUtilityProfile(profile: UtilityBillProfileEntity) {
        db.utilityBillDao().upsert(profile)
    }

    suspend fun updateUtilityProfile(profile: UtilityBillProfileEntity) {
        db.utilityBillDao().update(profile)
    }

    suspend fun archiveUtilityProfile(id: String, archiveDate: Long) {
        db.utilityBillDao().updateStatus(id, "ARCHIVED", archiveDate)
    }

    suspend fun reactivateUtilityProfile(id: String, reactivateMonth: String, now: Long) {
        db.utilityBillDao().getById(id)?.let { profile ->
            db.utilityBillDao().update(profile.copy(status = "ACTIVE", recurrenceStartMonth = reactivateMonth, updatedAtEpochMillis = now))
        }
    }

    suspend fun deleteUtilityProfile(id: String) {
        db.withTransaction {
            val occurrences = db.monthlyBillOccurrenceDao().getByProfile(id)
            val payments = occurrences.mapNotNull { db.paymentRecordDao().getForOccurrence(it.id) }
            payments.forEach { payment ->
                db.paymentRecordDao().delete(payment.id)
                payment.financialEventId?.let { db.financialEventDao().deleteById(it) }
            }
            db.billAttachmentDao().deleteForLinkedEntities(
                listOf(id) + occurrences.map { it.id } + payments.map { it.id },
            )
            db.utilityBillDao().delete(id)
        }
    }

    suspend fun addMonthlyOccurrence(occurrence: MonthlyBillOccurrenceEntity) {
        db.monthlyBillOccurrenceDao().upsert(occurrence)
    }

    suspend fun updateMonthlyOccurrence(occurrence: MonthlyBillOccurrenceEntity) {
        db.monthlyBillOccurrenceDao().update(occurrence)
    }

    suspend fun deleteMonthlyOccurrence(id: String) {
        db.withTransaction {
            db.paymentRecordDao().getForOccurrence(id)?.let { payment ->
                db.billAttachmentDao().deleteForLinkedEntities(listOf(payment.id))
                db.paymentRecordDao().delete(payment.id)
                payment.financialEventId?.let { db.financialEventDao().deleteById(it) }
            }
            db.billAttachmentDao().deleteForLinkedEntities(listOf(id))
            db.monthlyBillOccurrenceDao().delete(id)
        }
    }

    suspend fun addPayment(payment: PaymentRecordEntity) {
        require(payment.amountPaidMinor > 0) { "Payment amount must be greater than zero" }
        val accountId = requireNotNull(payment.accountId) { "Paid-from account is required" }
        require(db.accountDao().getById(accountId)?.status == "ACTIVE") { "Choose an active account" }
        db.withTransaction {
            // The occurrence uniqueness constraint is the idempotency key for repeated UI/worker calls.
            if (db.paymentRecordDao().getForOccurrence(payment.occurrenceId) != null) return@withTransaction
            val occurrence = db.monthlyBillOccurrenceDao().getById(payment.occurrenceId) ?: error("Bill occurrence not found")
            val profile = db.utilityBillDao().getById(occurrence.profileId) ?: error("Utility profile not found")
            val eventId = payment.financialEventId ?: UUID.randomUUID().toString()
            val now = Instant.now().toEpochMilli()
            val month = java.time.YearMonth.of(occurrence.billingYear, occurrence.billingMonth)
                .format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
            val category = UtilityCategory.fromStored(profile.category).label
            db.financialEventDao().upsert(
                FinancialEventEntity(
                    id = eventId,
                    eventType = FinancialEventType.EXPENSE.name,
                    dateEpochDay = payment.paymentDateEpochDay,
                    amountMinor = payment.amountPaidMinor,
                    currency = "PKR",
                    accountId = accountId,
                    contextId = payment.contextId ?: profile.defaultContextId,
                    category = "Utilities",
                    description = "${profile.name} — $month",
                    notes = "Utility: ${profile.name}; Category: $category; Billing period: $month; Payment: ${payment.id}; Occurrence: ${occurrence.id}",
                    taxRelevance = "UNKNOWN",
                    deletedAtEpochMillis = null,
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now,
                ),
            )
            db.paymentRecordDao().insert(payment.copy(accountId = accountId, financialEventId = eventId))
            // Payment state is part of the persisted occurrence lifecycle. Keeping it updated in
            // the same transaction makes backup snapshots and every reader agree immediately,
            // including callers that do not run the UI reconciliation pass afterward.
            db.monthlyBillOccurrenceDao().update(
                occurrence.copy(status = "Paid", updatedAtEpochMillis = now),
            )
        }
    }

    suspend fun updatePayment(id: String, amountPaid: Long, paymentDate: Long, mode: String, accountId: String, bank: String?, reference: String?, notes: String?, updatedAt: Long) {
        require(amountPaid > 0) { "Payment amount must be greater than zero" }
        require(db.accountDao().getById(accountId)?.status == "ACTIVE") { "Choose an active account" }
        db.withTransaction {
            val payment = db.paymentRecordDao().getById(id) ?: error("Payment not found")
            val eventId = payment.financialEventId ?: UUID.randomUUID().toString()
            val event = db.financialEventDao().getById(eventId) ?: run {
                val occurrence = db.monthlyBillOccurrenceDao().getById(payment.occurrenceId) ?: error("Bill occurrence not found")
                val profile = db.utilityBillDao().getById(occurrence.profileId) ?: error("Utility profile not found")
                val month = java.time.YearMonth.of(occurrence.billingYear, occurrence.billingMonth)
                    .format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
                FinancialEventEntity(
                    id = eventId,
                    eventType = FinancialEventType.EXPENSE.name,
                    dateEpochDay = paymentDate,
                    amountMinor = amountPaid,
                    currency = "PKR",
                    accountId = accountId,
                    contextId = payment.contextId ?: profile.defaultContextId,
                    category = "Utilities",
                    description = "${profile.name} — $month",
                    notes = "Utility: ${profile.name}; Category: ${UtilityCategory.canonicalLabel(profile.category)}; Billing period: $month; Payment: ${payment.id}; Occurrence: ${occurrence.id}",
                    taxRelevance = "UNKNOWN",
                    deletedAtEpochMillis = null,
                    createdAtEpochMillis = updatedAt,
                    updatedAtEpochMillis = updatedAt
                )
            }
            db.paymentRecordDao().update(payment.copy(amountPaidMinor = amountPaid, paymentDateEpochDay = paymentDate, paymentMode = mode, accountId = accountId, financialEventId = eventId, bankName = bank, transactionReference = reference, notes = notes, updatedAtEpochMillis = updatedAt))
            db.financialEventDao().upsert(event.copy(amountMinor = amountPaid, dateEpochDay = paymentDate, accountId = accountId, updatedAtEpochMillis = updatedAt))
        }
    }

    suspend fun deletePayment(id: String) {
        db.withTransaction {
            val payment = db.paymentRecordDao().getById(id) ?: return@withTransaction
            db.billAttachmentDao().deleteForLinkedEntities(listOf(payment.id))
            db.paymentRecordDao().delete(id)
            payment.financialEventId?.let { db.financialEventDao().deleteById(it) }
        }
    }

    suspend fun addAttachment(attachment: BillAttachmentEntity) {
        db.billAttachmentDao().insert(attachment)
    }

    suspend fun deleteAttachment(id: String) {
        db.billAttachmentDao().delete(id)
    }

    fun observeAttachments(linkedId: String): Flow<List<BillAttachmentEntity>> =
        db.billAttachmentDao().observeForLinkedEntity(linkedId)

    val accounts: Flow<List<AccountEntity>> = db.accountDao().observeAll()
    val activeAccounts: Flow<List<AccountEntity>> = db.accountDao().observeActive()
    val incomeSources: Flow<List<IncomeSourceEntity>> = db.incomeSourceDao().observeActive()
    val recentEvents: Flow<List<FinancialEventEntity>> = db.financialEventDao().observeRecent(200)
    val activeEventCount: Flow<Int> = db.financialEventDao().observeActiveCount()
    val taxItems: Flow<List<TaxItemEntity>> = db.taxItemDao().observeAll()
    val assets: Flow<List<AssetEntity>> = db.wealthDao().observeAssets()
    val liabilities: Flow<List<LiabilityEntity>> = db.wealthDao().observeLiabilities()
    val investments: Flow<List<InvestmentEventEntity>> = db.investmentDao().observeAll()
    val receivables: Flow<List<ReceivableEntity>> = db.receivableDao().observeAll()
    val goals: Flow<List<GoalEntity>> = db.goalDao().observeAll()
    val officialRecords: Flow<List<OfficialRecordEntity>> = db.officialRecordDao().observeAll()
    val calendarItems: Flow<List<CalendarItemEntity>> = db.calendarDao().observeOpen()
    val positionSnapshots: Flow<List<PositionSnapshotEntity>> = db.positionSnapshotDao().observeAll()
    val documents: Flow<List<DocumentEntity>> = db.documentDao().observeAll()
    val drafts: Flow<List<TaxAnnualDraftEntity>> = db.taxDraftDao().observeDrafts()
    val taxIssues: Flow<List<TaxIssueEntity>> = db.taxIssueDao().observeAll()
    val reconciliations: Flow<List<WealthReconciliationEntity>> = db.reconciliationDao().observeAll()
    val taxYears: Flow<List<TaxYearEntity>> = db.taxYearDao().observeAll()
    val recurringItems: Flow<List<RecurringItemEntity>> = db.recurringItemDao().observeActive()
    val simpleInvestments: Flow<List<SimpleInvestmentEntity>> = db.simpleInvestmentDao().observeAll()
    val budgets: Flow<List<BudgetEntity>> = db.budgetDao().observeActive()
    val totals: Flow<Pair<Money, Money>> = combine(db.financialEventDao().observeIncomeMinor(), db.financialEventDao().observeExpenseMinor()) { income, expense ->
        Money(MinorUnits(income), "PKR") to Money(MinorUnits(expense), "PKR")
    }
    val currentMonthBudgetStatuses: Flow<List<CategoryBudgetStatus>> = combine(db.budgetDao().observeActive(), db.financialEventDao().observeActive()) { budgets, events ->
        calculateCategoryBudgets(budgets, events, LocalDate.now().toYearMonth())
    }
    val goalProgress: Flow<List<Pair<GoalEntity, GoalProgress>>> = db.goalDao().observeAll().map { goals ->
        goals.map { goal -> goal to calculateGoalProgress(goal.currentAmountMinor, goal.targetAmountMinor, goal.targetDateEpochDay) }
    }

    /**
     * The one canonical net-worth/financial-position source of truth. Home, Wealth, Reports, Tax
     * and Reconciliation must read this instead of recomputing their own totals.
     */
    val financialPosition: Flow<FinancialPosition> = run {
        val monthStart = LocalDate.now().toYearMonth().atDay(1).toEpochDay()
        val monthEnd = LocalDate.now().toYearMonth().atEndOfMonth().toEpochDay()
        combine(
            db.accountDao().observeActive(),
            db.financialEventDao().observeActiveAccountsMovement(),
            db.financialEventDao().observeIncomeMinorInRange(monthStart, monthEnd),
            db.financialEventDao().observeExpenseMinorInRange(monthStart, monthEnd),
        ) { accounts, movement, monthlyIncome, monthlyExpense ->
            AccountsSnapshot(accounts.sumOf { it.openingBalanceMinor }, movement, monthlyIncome, monthlyExpense)
        }.combine(
            combine(db.wealthDao().observeAssets(), db.wealthDao().observeLiabilities(), db.investmentDao().observeAll(), db.receivableDao().observeAll(), db.simpleInvestmentDao().observeAll(), ::WealthSnapshot),
        ) { accountsSnapshot, wealthSnapshot ->
            calculateFinancialPosition(
                accountsSnapshot.openingBalanceMinor,
                accountsSnapshot.movementMinor,
                wealthSnapshot.assets,
                wealthSnapshot.liabilities,
                wealthSnapshot.investments,
                wealthSnapshot.receivables,
                accountsSnapshot.monthlyIncomeMinor,
                accountsSnapshot.monthlyExpenseMinor,
                wealthSnapshot.simpleInvestments,
            )
        }
    }

    private data class AccountsSnapshot(val openingBalanceMinor: Long, val movementMinor: Long, val monthlyIncomeMinor: Long, val monthlyExpenseMinor: Long)
    private data class WealthSnapshot(val assets: List<AssetEntity>, val liabilities: List<LiabilityEntity>, val investments: List<InvestmentEventEntity>, val receivables: List<ReceivableEntity>, val simpleInvestments: List<SimpleInvestmentEntity>)

    suspend fun recordPositionSnapshot(kind: String = "MANUAL", date: LocalDate = LocalDate.now()): PositionSnapshotEntity {
        require(kind == "MANUAL" || kind == "MONTHLY") { "Snapshot kind must be MANUAL or MONTHLY" }
        val position = financialPosition.first()
        return PositionSnapshotEntity(
            UUID.randomUUID().toString(), kind, date.toEpochDay(), position.liquidFundsMinor,
            position.investmentsValueMinor, position.assetsValueMinor, position.receivablesValueMinor,
            position.liabilitiesValueMinor, position.netWorthMinor, Instant.now().toEpochMilli(),
        ).also { db.positionSnapshotDao().insert(it) }
    }

    /**
     * Rebuilds only source-backed calendar rows. The source tables remain authoritative and this
     * operation is safe to repeat after relaunch or a worker retry.
     */
    suspend fun reconcileCalendarProjection() {
        val today = LocalDate.now().toEpochDay()
        val sources = buildList {
            db.monthlyBillOccurrenceDao().getAll().filter { it.status !in setOf("Paid", "Skipped") }.forEach {
                add(CalendarProjectionSource("BILL", it.id, "Bill due", it.expectedDueDateEpochDay))
            }
            db.recurringItemDao().getAll().filter { it.status == "ACTIVE" }.forEach {
                add(CalendarProjectionSource("RECURRING", it.id, it.title, it.nextDueDateEpochDay))
            }
            db.expectedOccurrenceDao().getAll().filter { it.status !in setOf("CONFIRMED", "SKIPPED") }.forEach {
                add(CalendarProjectionSource("OCCURRENCE", it.id, "Recurring occurrence", it.dueDateEpochDay))
            }
            db.wealthDao().getAllLiabilities().filter { it.status == "ACTIVE" && it.outstandingAmountMinor > 0 && it.dueDateEpochDay != null }.forEach {
                add(CalendarProjectionSource("LIABILITY", it.id, it.title, it.dueDateEpochDay!!))
            }
            db.receivableDao().getAll().filter { it.status == "ACTIVE" && it.outstandingAmountMinor > 0 && it.dueDateEpochDay != null }.forEach {
                add(CalendarProjectionSource("RECEIVABLE", it.id, it.title, it.dueDateEpochDay!!))
            }
            db.simpleInvestmentDao().getAll().filter { it.status == "ACTIVE" && it.maturityDateEpochDay != null }.forEach {
                add(CalendarProjectionSource("MATURITY", it.id, it.title, it.maturityDateEpochDay!!))
            }
            db.documentDao().getAll().filter { it.expiryDateEpochDay != null }.forEach {
                add(CalendarProjectionSource("DOCUMENT_EXPIRY", it.id, it.title, it.expiryDateEpochDay!!))
            }
            db.officialRecordDao().getAll().filter { it.status == "ACTIVE" && it.expiryDateEpochDay != null }.forEach {
                add(CalendarProjectionSource("RECORD_EXPIRY", it.id, it.title, it.expiryDateEpochDay!!))
            }
        }
        val projected = calendarProjection(sources, today)
        val activeIds = projected.map { it.stableId }.toSet()
        db.withTransaction {
            db.calendarDao().getAll().filter { it.kind.startsWith("SOURCE_") && it.id !in activeIds }.forEach {
                db.calendarDao().updateStatus(it.id, "CANCELLED")
            }
            projected.forEach { source ->
                val dueAt = LocalDate.ofEpochDay(source.dueDateEpochDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                db.calendarDao().upsert(CalendarItemEntity(source.stableId, source.title, source.kind, dueAt, source.sourceId, "OPEN", source.reminderMinutesBefore))
            }
        }
    }

    fun accountMovement(accountId: String): Flow<Long> = db.financialEventDao().observeAccountMovement(accountId)

    suspend fun addAccount(name: String, type: String, openingBalanceMinor: Long, currency: String = "PKR", institution: String? = null, notes: String? = null, context: String? = null) {
        require(name.isNotBlank() && openingBalanceMinor >= 0) { "Account details are invalid" }
        require(type.isNotBlank()) { "Account type is required" }
        val now = Instant.now().toEpochMilli()
        db.accountDao().upsert(
            AccountEntity(
                UUID.randomUUID().toString(), name.trim(), institution?.trim()?.takeIf { it.isNotEmpty() }, type, null, null,
                currency, openingBalanceMinor, LocalDate.now().toEpochDay(), "ACTIVE", notes?.trim()?.takeIf { it.isNotEmpty() }, now, now, context,
            ),
        )
    }

    suspend fun addIncomeSource(name: String, sourceType: String, payerOrEmployer: String? = null) {
        require(name.isNotBlank()) { "Income source name is required" }
        val now = Instant.now().toEpochMilli()
        db.incomeSourceDao().upsert(
            IncomeSourceEntity(
                UUID.randomUUID().toString(), name.trim(), sourceType, payerOrEmployer?.trim()?.takeIf { it.isNotEmpty() }, "ACTIVE", now, now,
            ),
        )
    }

    suspend fun archiveIncomeSource(id: String) {
        db.incomeSourceDao().archive(id, Instant.now().toEpochMilli())
    }

    suspend fun updateAccount(id: String, name: String, openingBalanceMinor: Long, institution: String? = null, notes: String? = null, type: String? = null, context: String? = null) {
        require(name.isNotBlank() && openingBalanceMinor >= 0) { "Account details are invalid" }
        val existing = db.accountDao().getById(id) ?: error("Account not found")
        val finalType = type ?: existing.accountType
        require(finalType.isNotBlank()) { "Account type is required" }
        db.accountDao().updateDetails(id, name.trim(), finalType, context ?: existing.context, openingBalanceMinor, institution?.trim()?.takeIf { it.isNotEmpty() }, notes?.trim()?.takeIf { it.isNotEmpty() }, Instant.now().toEpochMilli())
    }

    suspend fun archiveAccount(id: String) {
        require(db.accountDao().getById(id) != null) { "Account not found" }
        db.accountDao().archive(id, Instant.now().toEpochMilli())
    }

    suspend fun reactivateAccount(id: String) {
        require(db.accountDao().getById(id) != null) { "Account not found" }
        db.accountDao().reactivate(id, Instant.now().toEpochMilli())
    }

    suspend fun utilityAttachmentsForProfile(profileId: String): List<BillAttachmentEntity> {
        val occurrences = db.monthlyBillOccurrenceDao().getByProfile(profileId)
        val payments = occurrences.mapNotNull { db.paymentRecordDao().getForOccurrence(it.id) }
        return db.billAttachmentDao().getForLinkedEntities(listOf(profileId) + occurrences.map { it.id } + payments.map { it.id })
    }

    suspend fun utilityAttachmentsForOccurrence(occurrenceId: String): List<BillAttachmentEntity> {
        val paymentId = db.paymentRecordDao().getForOccurrence(occurrenceId)?.id
        return db.billAttachmentDao().getForLinkedEntities(listOfNotNull(occurrenceId, paymentId))
    }

    suspend fun utilityAttachmentsForPayment(paymentId: String): List<BillAttachmentEntity> =
        db.billAttachmentDao().getForLinkedEntities(listOf(paymentId))

    suspend fun addAsset(title: String, type: String, valueMinor: Long) {
        db.wealthDao().upsertAsset(AssetEntity(UUID.randomUUID().toString(), type, title.trim(), LocalDate.now().toEpochDay(), valueMinor, valueMinor, "PKR", 100, null, null, "ACTIVE"))
    }

    suspend fun updateAssetValuation(id: String, valueMinor: Long) {
        require(valueMinor >= 0) { "Asset valuation cannot be negative" }
        require(db.wealthDao().getAssetById(id) != null) { "Asset not found" }
        db.wealthDao().updateAssetValue(id, valueMinor)
    }

    suspend fun updateAssetPosition(id: String, valueMinor: Long, ownershipPercent: Int, includeInNetWorth: Boolean) {
        require(valueMinor >= 0) { "Asset valuation cannot be negative" }
        require(ownershipPercent in 0..100) { "Ownership must be between 0 and 100 percent" }
        require(db.wealthDao().getAssetById(id) != null) { "Asset not found" }
        db.wealthDao().updatePosition(id, valueMinor, ownershipPercent, includeInNetWorth, LocalDate.now().toEpochDay())
    }

    suspend fun disposeAsset(id: String, valueMinor: Long) {
        require(valueMinor >= 0) { "Disposal value cannot be negative" }
        require(db.wealthDao().getAssetById(id) != null) { "Asset not found" }
        db.wealthDao().archiveAsset(id, LocalDate.now().toEpochDay(), valueMinor)
    }

    suspend fun addLiability(
        context: Context,
        title: String,
        type: String,
        outstandingMinor: Long,
        lender: String? = null,
        dueDate: LocalDate? = null,
        interestRateBps: Int? = null,
        installmentAmountMinor: Long? = null,
    ) {
        val id = UUID.randomUUID().toString()
        val trimmedTitle = title.trim()
        db.wealthDao().upsertLiability(
            LiabilityEntity(
                id, type, trimmedTitle, lender?.trim()?.takeIf { it.isNotBlank() }, outstandingMinor, outstandingMinor,
                "PKR", LocalDate.now().toEpochDay(), dueDate?.toEpochDay(), "ACTIVE", interestRateBps, installmentAmountMinor,
            ),
        )
        if (dueDate != null) {
            scheduleOrCancelExpiryReminder(
                context, "liability-due-$id", "LIABILITY_DUE", trimmedTitle, id, dueDate.toEpochDay(),
                headline = "$trimmedTitle installment is due", body = "Review the next installment for this loan/liability.",
            )
        }
    }

    suspend fun recordLiabilityPayment(id: String, amountMinor: Long) {
        val liability = db.wealthDao().getLiabilityById(id) ?: error("Liability not found")
        require(amountMinor > 0 && amountMinor <= liability.outstandingAmountMinor) { "Payment exceeds outstanding liability" }
        val remaining = liability.outstandingAmountMinor - amountMinor
        db.wealthDao().updateLiabilityOutstanding(id, remaining, if (remaining == 0L) "SETTLED" else "ACTIVE")
    }

    /** Records borrowed principal as a signed financing movement, never as income. */
    suspend fun recordBorrowedPrincipal(id: String, accountId: String, amountMinor: Long, date: LocalDate = LocalDate.now()) {
        require(amountMinor > 0) { "Borrowed principal must be positive" }
        require(db.accountDao().getById(accountId)?.status == "ACTIVE") { "Choose an active account" }
        val liability = db.wealthDao().getLiabilityById(id) ?: error("Liability not found")
        val now = Instant.now().toEpochMilli()
        val event = FinancialEventEntity(id = UUID.randomUUID().toString(), eventType = FinancialEventType.FINANCING.name, dateEpochDay = date.toEpochDay(), amountMinor = amountMinor, currency = "PKR", accountId = accountId, contextId = liability.contextId, category = null, description = "${liability.title} — principal received", notes = null, taxRelevance = "NOT_RELEVANT", deletedAtEpochMillis = null, createdAtEpochMillis = now, updatedAtEpochMillis = now, cashEffectMinor = amountMinor)
        db.withTransaction {
            db.financialEventDao().upsert(event)
            db.wealthDao().updateLiabilityOutstanding(id, liability.outstandingAmountMinor + amountMinor, "ACTIVE")
            db.settlementEventDao().upsert(SettlementEventEntity(UUID.randomUUID().toString(), "LIABILITY", id, event.id, amountMinor, 0, date.toEpochDay()))
        }
    }

    /** Records one loan installment as one cash movement plus a non-cash expense component. */
    suspend fun recordLiabilityPayment(
        id: String,
        amountMinor: Long,
        accountId: String,
        principalAmountMinor: Long = amountMinor,
        financingCostMinor: Long = 0,
        date: LocalDate = LocalDate.now(),
    ) {
        require(amountMinor > 0 && principalAmountMinor >= 0 && financingCostMinor >= 0 && principalAmountMinor + financingCostMinor == amountMinor) { "Installment split is invalid" }
        require(db.accountDao().getById(accountId)?.status == "ACTIVE") { "Choose an active account" }
        val liability = db.wealthDao().getLiabilityById(id) ?: error("Liability not found")
        require(principalAmountMinor <= liability.outstandingAmountMinor) { "Principal exceeds outstanding liability" }
        val now = Instant.now().toEpochMilli()
        val groupId = UUID.randomUUID().toString()
        val cashEvent = FinancialEventEntity(id = UUID.randomUUID().toString(), eventType = FinancialEventType.FINANCING.name, dateEpochDay = date.toEpochDay(), amountMinor = amountMinor, currency = "PKR", accountId = accountId, contextId = liability.contextId, category = null, description = "${liability.title} — installment", notes = null, taxRelevance = "NOT_RELEVANT", deletedAtEpochMillis = null, createdAtEpochMillis = now, updatedAtEpochMillis = now, groupId = groupId, cashEffectMinor = -amountMinor)
        val costEvent = financingCostMinor.takeIf { it > 0 }?.let {
            FinancialEventEntity(id = UUID.randomUUID().toString(), eventType = FinancialEventType.EXPENSE.name, dateEpochDay = date.toEpochDay(), amountMinor = it, currency = "PKR", accountId = accountId, contextId = liability.contextId, category = "Financing cost", description = "${liability.title} — financing cost", notes = null, taxRelevance = "UNKNOWN", deletedAtEpochMillis = null, createdAtEpochMillis = now, updatedAtEpochMillis = now, groupId = groupId, cashEffectMinor = 0)
        }
        db.withTransaction {
            db.financialEventDao().upsert(cashEvent)
            costEvent?.let { db.financialEventDao().upsert(it) }
            val remaining = liability.outstandingAmountMinor - principalAmountMinor
            db.wealthDao().updateLiabilityOutstanding(id, remaining, if (remaining == 0L) "SETTLED" else "ACTIVE")
            db.settlementEventDao().upsert(SettlementEventEntity(UUID.randomUUID().toString(), "LIABILITY", id, cashEvent.id, principalAmountMinor, financingCostMinor, date.toEpochDay()))
        }
    }

    suspend fun addInvestmentEvent(securityName: String, type: String, amountMinor: Long, quantityMinor: Long = 0, feesMinor: Long = 0, taxWithheldMinor: Long = 0, accountLabel: String = "Manual") {
        require(amountMinor >= 0 && quantityMinor >= 0 && feesMinor >= 0 && taxWithheldMinor >= 0)
        val normalizedAccountLabel = accountLabel.trim().takeIf { it.isNotEmpty() } ?: "Manual"
        db.investmentDao().insert(InvestmentEventEntity(UUID.randomUUID().toString(), normalizedAccountLabel, securityName.trim(), type, LocalDate.now().toEpochDay(), quantityMinor, amountMinor, feesMinor, taxWithheldMinor, "PKR"))
    }

    suspend fun addReceivable(context: Context, title: String, counterparty: String, amountMinor: Long, dueDate: LocalDate? = null) {
        val id = UUID.randomUUID().toString()
        val trimmedTitle = title.trim()
        db.receivableDao().upsert(ReceivableEntity(id, trimmedTitle, counterparty.trim(), amountMinor, amountMinor, dueDate?.toEpochDay(), "OPEN"))
        if (dueDate != null) {
            scheduleOrCancelExpiryReminder(
                context, "receivable-due-$id", "RECEIVABLE_DUE", trimmedTitle, id, dueDate.toEpochDay(),
                headline = "$trimmedTitle is due", body = "Follow up with ${counterparty.trim()} about this receivable.",
            )
        }
    }

    /** Settles a receivable and classifies only income-due receipts as income. */
    suspend fun recordReceivablePayment(id: String, amountMinor: Long, accountId: String, date: LocalDate = LocalDate.now()) {
        val current = db.receivableDao().getById(id) ?: error("Receivable not found")
        require(amountMinor > 0 && amountMinor <= current.outstandingAmountMinor) { "Payment exceeds outstanding amount" }
        require(db.accountDao().getById(accountId)?.status == "ACTIVE") { "Choose an active account" }
        val now = Instant.now().toEpochMilli()
        val isIncome = current.receivableType == "INCOME_DUE"
        val event = FinancialEventEntity(id = UUID.randomUUID().toString(), eventType = if (isIncome) FinancialEventType.INCOME.name else FinancialEventType.FINANCING.name, dateEpochDay = date.toEpochDay(), amountMinor = amountMinor, currency = "PKR", accountId = accountId, contextId = current.contextId, category = null, description = "${current.title} — received", notes = null, taxRelevance = if (isIncome) "UNKNOWN" else "NOT_RELEVANT", deletedAtEpochMillis = null, createdAtEpochMillis = now, updatedAtEpochMillis = now, cashEffectMinor = if (isIncome) null else amountMinor)
        db.withTransaction {
            db.financialEventDao().upsert(event)
            val outstanding = current.outstandingAmountMinor - amountMinor
            db.receivableDao().upsert(current.copy(outstandingAmountMinor = outstanding, status = if (outstanding == 0L) "SETTLED" else "OPEN", receivedDateEpochDay = date.toEpochDay()))
            db.settlementEventDao().upsert(SettlementEventEntity(UUID.randomUUID().toString(), "RECEIVABLE", id, event.id, amountMinor, 0, date.toEpochDay()))
            if (isIncome) insertTaxCandidateForIncome(event, now)
        }
    }

    /**
     * Schedules (or cancels, if [deadline] is null) a filing-deadline reminder for a tax year —
     * mega-prompt 5F's other calendar-integration gap alongside receivable due dates. Not tied to
     * any particular status transition ([updateTaxYearStatus] is independent) since a user may
     * want the reminder well before actually starting review.
     */
    suspend fun scheduleTaxFilingDeadlineReminder(context: Context, taxYearId: String, deadline: LocalDate?) {
        scheduleOrCancelExpiryReminder(
            context, "tax-filing-deadline-$taxYearId", "TAX_FILING_DEADLINE", taxYearId, taxYearId, deadline?.toEpochDay(),
            headline = "$taxYearId filing deadline", body = "Prepare and file your $taxYearId return before this date.",
        )
    }

    suspend fun recordReceivablePayment(id: String, amountMinor: Long) {
        val current = db.receivableDao().getById(id) ?: error("Receivable not found")
        require(amountMinor > 0 && amountMinor <= current.outstandingAmountMinor) { "Payment exceeds outstanding amount" }
        val outstanding = current.outstandingAmountMinor - amountMinor
        db.receivableDao().upsert(current.copy(outstandingAmountMinor = outstanding, status = if (outstanding == 0L) "SETTLED" else "OPEN"))
    }

    suspend fun addGoal(title: String, targetAmountMinor: Long, goalType: String = "CUSTOM", targetDateEpochDay: Long? = null) {
        db.goalDao().upsert(GoalEntity(UUID.randomUUID().toString(), title.trim(), goalType, targetAmountMinor, targetDateEpochDay, "OPEN"))
    }

    suspend fun contributeToGoal(id: String, amountMinor: Long) {
        require(amountMinor > 0) { "Goal contribution must be positive" }
        val goal = db.goalDao().getById(id) ?: error("Goal not found")
        val updated = (goal.currentAmountMinor + amountMinor).coerceAtMost(goal.targetAmountMinor)
        db.goalDao().updateProgress(id, updated, if (updated >= goal.targetAmountMinor) "ACHIEVED" else goal.status)
    }

    suspend fun addBudget(category: String, monthlyLimitMinor: Long, currency: String = "PKR") {
        require(category.isNotBlank() && monthlyLimitMinor > 0) { "Budget details are invalid" }
        val now = Instant.now().toEpochMilli()
        val existing = db.budgetDao().getByCategory(category.trim())
        val id = existing?.id ?: UUID.randomUUID().toString()
        db.budgetDao().upsert(BudgetEntity(id, category.trim(), monthlyLimitMinor, currency, "ACTIVE", existing?.createdAtEpochMillis ?: now, now))
    }

    suspend fun addOfficialRecord(context: Context, type: String, title: String, identifier: String?, issueDate: LocalDate? = null, expiryDate: LocalDate? = null) {
        val normalized = identifier?.trim()?.takeIf { it.isNotEmpty() }
        val encrypted = normalized?.let { KeystoreCryptoService().encrypt(it.toByteArray(), title.toByteArray()).let { value -> value.nonce + value.ciphertext } }
        val masked = normalized?.let { if (it.length <= 4) "••••" else "••••${it.takeLast(4)}" }
        val id = UUID.randomUUID().toString()
        db.officialRecordDao().upsert(OfficialRecordEntity(id, type, title.trim(), masked, encrypted, issueDate?.toEpochDay(), expiryDate?.toEpochDay(), null, "ACTIVE"))
        if (expiryDate != null) scheduleOrCancelExpiryReminder(context, "official-record-expiry-$id", "OFFICIAL_RECORD_EXPIRY", title.trim(), id, expiryDate.toEpochDay())
    }

    suspend fun addCalendarItem(context: Context, title: String, kind: String, delayMinutes: Long) {
        require(delayMinutes > 0) { "Reminder delay must be positive" }
        val id = UUID.randomUUID().toString()
        val dueAt = System.currentTimeMillis() + delayMinutes * 60_000L
        db.calendarDao().upsert(CalendarItemEntity(id, title.trim(), kind, dueAt, null, "OPEN", 0))
        ReminderScheduler(context).schedule(id, dueAt, 0, title.trim(), "Open Vexel Finance Passport to review this obligation.")
    }

    suspend fun updateCalendarStatus(context: Context, id: String, status: String) {
        require(status in setOf("COMPLETED", "CANCELLED", "DISMISSED", "OPEN"))
        db.calendarDao().updateStatus(id, status)
        if (status == "OPEN") {
            val item = db.calendarDao().getById(id) ?: return
            ReminderScheduler(context).schedule(id, item.dueAtEpochMillis, item.reminderMinutesBefore, item.title, "Open Vexel Finance Passport to review this obligation.")
        } else ReminderScheduler(context).cancel(id)
    }

    suspend fun rescheduleCalendarItem(context: Context, id: String, delayMinutes: Long) {
        require(delayMinutes > 0) { "Reminder delay must be positive" }
        val item = db.calendarDao().getById(id) ?: error("Reminder not found")
        val dueAt = System.currentTimeMillis() + delayMinutes * 60_000L
        db.calendarDao().updateSchedule(id, dueAt)
        ReminderScheduler(context).schedule(id, dueAt, item.reminderMinutesBefore, item.title, "Open Vexel Finance Passport to review this obligation.")
    }

    /** Pushes a reminder's due time forward by [delayDays] from its own current due date, distinct from
     * [rescheduleCalendarItem] which resets the delay relative to now. */
    suspend fun snoozeCalendarItem(context: Context, id: String, delayDays: Long) {
        require(delayDays > 0) { "Snooze delay must be positive" }
        val item = db.calendarDao().getById(id) ?: error("Reminder not found")
        val dueAt = item.dueAtEpochMillis + delayDays * 86_400_000L
        db.calendarDao().updateSchedule(id, dueAt)
        ReminderScheduler(context).schedule(id, dueAt, item.reminderMinutesBefore, item.title, "Open Vexel Finance Passport to review this obligation.")
    }

    suspend fun addEvent(type: FinancialEventType, amountMinor: Long, accountId: String, description: String, category: String? = null, taxRelevance: String = "UNKNOWN", date: LocalDate = LocalDate.now(), incomeSourceId: String? = null) {
        require(amountMinor > 0) { "Amount must be greater than zero" }
        require(db.accountDao().getById(accountId)?.status == "ACTIVE") { "Choose an active account" }
        val now = Instant.now().toEpochMilli()
        val eventId = UUID.randomUUID().toString()
        val event = FinancialEventEntity(eventId, type.name, date.toEpochDay(), amountMinor, "PKR", accountId, null, category?.trim()?.takeIf { it.isNotEmpty() }, description.trim(), null, taxRelevance, null, now, now, incomeSourceId.takeIf { type == FinancialEventType.INCOME })
        db.withTransaction {
            db.financialEventDao().upsert(event)
            if (type == FinancialEventType.INCOME) {
                val yearId = ensureTaxYearExists(date.year)
                val taxItemId = UUID.randomUUID().toString()
                val taxEventType = "EMPLOYMENT_INCOME"
                val inserted = db.taxItemDao().insertIfAbsent(TaxItemEntity(taxItemId, yearId, "financial_event", eventId, taxEventType, date.toEpochDay(), amountMinor, null, "PKR", description.trim(), "CAPTURED", "REQUESTED", null, now, now))
                if (inserted != -1L) recordInitialMapping(yearId, taxItemId, "financial_event", eventId, date.toEpochDay(), amountMinor, description.trim(), taxEventType, now)
            }
        }
    }

    private suspend fun insertTaxCandidateForIncome(event: FinancialEventEntity, now: Long) {
        val yearId = ensureTaxYearExists(LocalDate.ofEpochDay(event.dateEpochDay).year)
        val taxItemId = UUID.randomUUID().toString()
        val taxEventType = "EMPLOYMENT_INCOME"
        val inserted = db.taxItemDao().insertIfAbsent(TaxItemEntity(taxItemId, yearId, "financial_event", event.id, taxEventType, event.dateEpochDay, event.amountMinor, null, event.currency, event.description, "CAPTURED", "REQUESTED", null, now, now))
        if (inserted != -1L) recordInitialMapping(yearId, taxItemId, "financial_event", event.id, event.dateEpochDay, event.amountMinor, event.description, taxEventType, now)
    }

    /** Normalized recurring template entry point. Expectations are created without money movement. */
    suspend fun createRecurringTemplate(template: RecurringTemplateEntity): ExpectedOccurrenceEntity {
        require(template.title.isNotBlank() && template.intervalCount > 0) { "Recurring template details are invalid" }
        require(template.eventType == FinancialEventType.INCOME.name || template.eventType == FinancialEventType.EXPENSE.name) { "Recurring templates support income or expense only" }
        require(template.amountMode == "FIXED" || template.amountMode == "VARIABLE") { "Unsupported recurring amount mode" }
        require(template.amountMode == "VARIABLE" || (template.expectedAmountMinor != null && template.expectedAmountMinor > 0)) { "Fixed recurring amount is required" }
        db.recurringTemplateDao().upsert(template)
        return generateExpectedOccurrence(template.id, template.startDateEpochDay)
    }

    suspend fun generateExpectedOccurrence(templateId: String, dueDateEpochDay: Long): ExpectedOccurrenceEntity {
        val template = db.recurringTemplateDao().getById(templateId) ?: error("Recurring template not found")
        require(template.endDateEpochDay == null || dueDateEpochDay <= template.endDateEpochDay) { "Occurrence is outside template range" }
        val existing = db.expectedOccurrenceDao().getForTemplateDate(templateId, dueDateEpochDay)
        if (existing != null) return existing
        val now = Instant.now().toEpochMilli()
        return ExpectedOccurrenceEntity(UUID.randomUUID().toString(), templateId, dueDateEpochDay, template.expectedAmountMinor, "UPCOMING", null, now, now).also { db.expectedOccurrenceDao().upsert(it) }
    }

    /** Idempotent confirmation: the persisted occurrence link is the retry key. */
    suspend fun confirmExpectedOccurrence(occurrenceId: String, actualAmountMinor: Long? = null, accountId: String? = null, contextId: String? = null, date: LocalDate? = null): String {
        return db.withTransaction {
            val occurrence = db.expectedOccurrenceDao().getById(occurrenceId) ?: error("Expected occurrence not found")
            occurrence.confirmedEventId?.let { return@withTransaction it }
            val template = db.recurringTemplateDao().getById(occurrence.templateId) ?: error("Recurring template not found")
            val amount = actualAmountMinor ?: occurrence.expectedAmountMinor
            require(amount != null && amount > 0) { "Actual amount is required for a variable occurrence" }
            if (accountId != null) require(db.accountDao().getById(accountId)?.status == "ACTIVE") { "Choose an active account" }
            val eventId = UUID.randomUUID().toString()
            val now = Instant.now().toEpochMilli()
            val event = FinancialEventEntity(eventId, template.eventType, date?.toEpochDay() ?: occurrence.dueDateEpochDay, amount, template.currency, accountId ?: template.defaultAccountId, contextId ?: template.defaultContextId, null, template.title, template.notes, "UNKNOWN", null, now, now, null, template.defaultCategoryId, template.counterparty, template.id, occurrence.id, null, null)
            db.financialEventDao().upsert(event)
            if (template.eventType == FinancialEventType.INCOME.name) insertTaxCandidateForIncome(event, now)
            db.expectedOccurrenceDao().markResolved(occurrence.id, "CONFIRMED", eventId, now)
            eventId
        }
    }

    suspend fun skipExpectedOccurrence(occurrenceId: String) {
        db.withTransaction {
            val occurrence = db.expectedOccurrenceDao().getById(occurrenceId) ?: error("Expected occurrence not found")
            require(occurrence.status != "CONFIRMED") { "Confirmed occurrence cannot be skipped" }
            db.expectedOccurrenceDao().markResolved(occurrence.id, "SKIPPED", null, Instant.now().toEpochMilli())
        }
    }

    suspend fun addSimpleInvestment(investment: SimpleInvestmentEntity, fundingAccountId: String? = null) {
        require(investment.title.isNotBlank() && investment.principalInvestedMinor >= 0 && investment.currentEstimatedValueMinor >= 0) { "Investment details are invalid" }
        db.withTransaction {
            db.simpleInvestmentDao().upsert(investment)
            if (fundingAccountId != null && investment.principalInvestedMinor > 0) {
                require(db.accountDao().getById(fundingAccountId)?.status == "ACTIVE") { "Choose an active account" }
                val now = Instant.now().toEpochMilli()
                db.financialEventDao().upsert(FinancialEventEntity(id = UUID.randomUUID().toString(), eventType = FinancialEventType.FINANCING.name, dateEpochDay = investment.acquisitionDateEpochDay, amountMinor = investment.principalInvestedMinor, currency = investment.currency, accountId = fundingAccountId, contextId = investment.contextId, category = null, description = "${investment.title} — funded", notes = investment.notes, taxRelevance = "NOT_RELEVANT", deletedAtEpochMillis = null, createdAtEpochMillis = now, updatedAtEpochMillis = now, groupId = investment.id, cashEffectMinor = -investment.principalInvestedMinor))
            }
        }
    }

    suspend fun updateSimpleInvestmentValuation(id: String, valueMinor: Long) {
        require(valueMinor >= 0) { "Investment valuation cannot be negative" }
        require(db.simpleInvestmentDao().getById(id) != null) { "Investment not found" }
        db.simpleInvestmentDao().updateValue(id, valueMinor)
    }

    suspend fun addRecurringItem(context: Context, title: String, eventType: FinancialEventType, amountMinor: Long, accountId: String, category: String?, frequency: String, delayDays: Long) {
        require(title.isNotBlank() && amountMinor > 0 && delayDays > 0) { "Recurring item details are invalid" }
        require(db.accountDao().getById(accountId) != null) { "Account not found" }
        require(frequency in setOf("WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY")) { "Unsupported recurring frequency" }
        val now = Instant.now().toEpochMilli()
        val id = UUID.randomUUID().toString()
        val dueDate = LocalDate.now().plusDays(delayDays)
        db.recurringItemDao().upsert(RecurringItemEntity(id, title.trim(), eventType.name, amountMinor, "PKR", accountId, category?.trim()?.takeIf { it.isNotEmpty() }, frequency, dueDate.toEpochDay(), "ACTIVE", true, now, now, dueDate.dayOfMonth))
        val dueAt = dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        db.calendarDao().upsert(CalendarItemEntity("recurring-$id", "Recurring draft: ${title.trim()}", "RECURRING_DRAFT", dueAt, id, "OPEN", 0))
        ReminderScheduler(context).schedule("recurring-$id", dueAt, 0, "Recurring draft: ${title.trim()}", "Review and confirm this recurring ${eventType.name.lowercase()}.")
    }

    suspend fun pauseRecurringItem(context: Context, id: String) {
        db.recurringItemDao().pause(id, Instant.now().toEpochMilli())
        db.calendarDao().updateStatus("recurring-$id", "CANCELLED")
        ReminderScheduler(context).cancel("recurring-$id")
    }

    /**
     * The explicit user action ("mark this cycle paid") that [processDueRecurringItems] deliberately
     * never performs on its own: records a real financial event from the recurring item's stored
     * fields right now, then advances the schedule exactly like a normal due-date rollover would.
     * Works regardless of whether the item is actually due yet — the user may confirm early.
     */
    suspend fun confirmRecurringItemNow(context: Context, id: String) {
        val item = db.recurringItemDao().getById(id) ?: error("Recurring item not found")
        val type = runCatching { FinancialEventType.valueOf(item.eventType) }.getOrNull() ?: error("Unknown recurring event type")
        addEvent(type, item.amountMinor, item.accountId, item.title, item.category)
        val frequency = runCatching { RecurringFrequency.valueOf(item.frequency) }.getOrNull() ?: return
        val nextDueDate = advanceRecurringDueDate(LocalDate.ofEpochDay(item.nextDueDateEpochDay), frequency, item.anchorDayOfMonth)
        db.recurringItemDao().advanceDueDate(item.id, nextDueDate.toEpochDay(), Instant.now().toEpochMilli())
        val dueAt = nextDueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        db.calendarDao().upsert(CalendarItemEntity("recurring-${item.id}", "Recurring draft: ${item.title}", "RECURRING_DRAFT", dueAt, item.id, "OPEN", 0))
        ReminderScheduler(context).schedule("recurring-${item.id}", dueAt, 0, "Recurring draft: ${item.title}", "Review and confirm this recurring ${item.eventType.lowercase()}.")
    }

    /**
     * Fires every ACTIVE recurring item whose next due date has arrived: optionally records the
     * draft financial event, then advances the schedule to its next occurrence and reschedules
     * the reminder. Safe to call repeatedly (e.g. from a periodic worker) — items not yet due are untouched.
     */
    suspend fun processDueRecurringItems(context: Context) {
        val today = LocalDate.now()
        val due = db.recurringItemDao().getDueActive(today.toEpochDay())
        for (item in due) {
            // A recurring rule may remind the user about a draft, but it must never
            // create a confirmed financial fact without an explicit user action.
            val frequency = runCatching { RecurringFrequency.valueOf(item.frequency) }.getOrNull() ?: continue
            val nextDueDate = advanceRecurringDueDate(LocalDate.ofEpochDay(item.nextDueDateEpochDay), frequency, item.anchorDayOfMonth)
            db.recurringItemDao().advanceDueDate(item.id, nextDueDate.toEpochDay(), Instant.now().toEpochMilli())
            val dueAt = nextDueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            db.calendarDao().upsert(CalendarItemEntity("recurring-${item.id}", "Recurring draft: ${item.title}", "RECURRING_DRAFT", dueAt, item.id, "OPEN", 0))
            ReminderScheduler(context).schedule("recurring-${item.id}", dueAt, 0, "Recurring draft: ${item.title}", "Review and confirm this recurring ${item.eventType.lowercase()}.")
        }
    }

    suspend fun transfer(sourceAccountId: String, destinationAccountId: String, amountMinor: Long, description: String, date: LocalDate = LocalDate.now()) {
        require(sourceAccountId != destinationAccountId) { "Transfer accounts must be different" }
        require(amountMinor > 0) { "Transfer amount must be positive" }
        require(db.accountDao().getById(sourceAccountId)?.status == "ACTIVE") { "Choose an active source account" }
        require(db.accountDao().getById(destinationAccountId)?.status == "ACTIVE") { "Choose an active destination account" }
        val now = Instant.now().toEpochMilli()
        val group = UUID.randomUUID().toString()
        val out = FinancialEventEntity(UUID.randomUUID().toString(), "TRANSFER", date.toEpochDay(), -amountMinor, "PKR", sourceAccountId, null, null, description.trim(), null, "NOT_RELEVANT", null, now, now)
        val incoming = FinancialEventEntity(UUID.randomUUID().toString(), "TRANSFER", date.toEpochDay(), amountMinor, "PKR", destinationAccountId, null, null, description.trim(), null, "NOT_RELEVANT", null, now, now)
        db.withTransaction {
            db.financialEventDao().insertAll(listOf(out, incoming))
            db.transferLinkDao().insert(TransferLinkEntity(UUID.randomUUID().toString(), out.id, incoming.id, group))
        }
    }

    fun toDomain(entity: FinancialEventEntity) = FinancialEvent(entity.id, runCatching { FinancialEventType.valueOf(entity.eventType) }.getOrDefault(FinancialEventType.ADJUSTMENT), Money(MinorUnits(kotlin.math.abs(entity.amountMinor)), entity.currency), entity.accountId, entity.contextId, entity.dateEpochDay, entity.description)

    suspend fun exportSnapshot() = ExportSnapshot(
        accounts = db.accountDao().getAll(), events = db.financialEventDao().getAll(), assets = db.wealthDao().getAllAssets(), liabilities = db.wealthDao().getAllLiabilities(),
        taxItems = db.taxItemDao().getAll(), documents = db.documentDao().getAll(), investments = db.investmentDao().getAll(), receivables = db.receivableDao().getAll(),
        goals = db.goalDao().getAll(), officialRecords = db.officialRecordDao().getAll(), budgets = db.budgetDao().getAll(),
        taxMappings = db.taxMappingDao().getAll(), wealthSnapshots = db.wealthSnapshotDao().getAll(), taxDrafts = db.taxDraftDao().getAll(),
        incomeSources = db.incomeSourceDao().getAll(), categories = db.categoryDao().getAll(), recurringTemplates = db.recurringTemplateDao().getAll(),
        expectedOccurrences = db.expectedOccurrenceDao().getAll(), settlements = db.settlementEventDao().getAll(), simpleInvestments = db.simpleInvestmentDao().getAll(),
        positionSnapshots = db.positionSnapshotDao().getAll(),
        contexts = db.financialContextDao().getAll(), calendarItems = db.calendarDao().getAll(),
        documentLinks = db.documentLinkDao().getAll(), utilityBills = db.utilityBillDao().getAll(),
        billOccurrences = db.monthlyBillOccurrenceDao().getAll(), payments = db.paymentRecordDao().getAll(),
        billAttachments = db.billAttachmentDao().getAll(),
    )

    suspend fun deleteAllData(context: Context) {
        db.withTransaction { db.clearAllTables() }
        WorkManager.getInstance(context).cancelAllWork()
        File(context.filesDir, "vault").deleteRecursively()
        File(context.filesDir, "utility_vault").deleteRecursively()
        context.cacheDir.listFiles()?.filter { it.name.startsWith("passport-") || it.name.startsWith("restore-") || it.name.startsWith("passport-preview-") }?.forEach { it.deleteRecursively() }
        // Clear through the live SharedPreferences instances (not raw file deletion) so any
        // already-constructed AppPreferences/PinStore in this process — Context caches one
        // in-memory instance per file — immediately reflects the reset without a process
        // restart. This is what lets OnboardingGate show onboarding again right after delete-all.
        AppPreferences(context).clear()
        PinStore(context).clear()
    }

    suspend fun updateTaxReview(id: String, state: String, reason: String? = null) {
        require(state in setOf("DRAFT", "CAPTURED", "NEEDS_EVIDENCE", "NEEDS_CLASSIFICATION", "REVIEWED", "INCLUDED", "EXCLUDED")) { "Invalid tax review state" }
        db.taxItemDao().updateReview(id, state, reason, Instant.now().toEpochMilli())
    }

    /**
     * Reclassifying a tax item never rewrites its mapping history in place (Phase 4F): this
     * inserts a new [TaxMappingEntity] carrying the override + reason and marks the previously
     * active mapping (if any) as superseded by it, leaving the prior mapping row intact.
     */
    suspend fun reviewTaxItem(id: String, taxEventType: String, state: String, reason: String?) {
        require(runCatching { TaxEventType.valueOf(taxEventType) }.isSuccess) { "Unsupported tax event type" }
        require(state in setOf("REVIEWED", "INCLUDED", "EXCLUDED")) { "Invalid reviewed state" }
        require(state != "EXCLUDED" || !reason.isNullOrBlank()) { "An exclusion reason is required" }
        val now = Instant.now().toEpochMilli()
        db.withTransaction {
            db.taxItemDao().updateClassification(id, taxEventType, state, reason?.trim(), now)
            val ruleset = defaultPakistanStructuralRules()
            val rule = ruleset.rules.firstOrNull { it.eventType.name == taxEventType }
            val newMapping = TaxMappingEntity(
                UUID.randomUUID().toString(), id, ruleset.version, taxEventType,
                rule?.sectionCode ?: "UNMAPPED", rule?.categoryCode ?: "UNMAPPED",
                "USER_OVERRIDE", reason?.trim(), null, now,
            )
            val previousActive = db.taxMappingDao().getActiveForTaxItem(id)
            db.taxMappingDao().insert(newMapping)
            previousActive?.let { db.taxMappingDao().markSuperseded(it.id, newMapping.id) }
        }
    }

    suspend fun updateTaxEvidenceState(id: String, evidenceState: String) {
        require(evidenceState in setOf("NONE", "OPTIONAL", "REQUESTED", "ATTACHED", "VERIFIED_BY_USER", "NOT_AVAILABLE", "NOT_REQUIRED")) { "Invalid evidence state" }
        db.taxItemDao().updateEvidenceState(id, evidenceState, Instant.now().toEpochMilli())
    }

    suspend fun linkDocument(documentId: String, entityType: String, entityId: String, purpose: String = "EVIDENCE") {
        require(db.documentDao().getAll().any { it.id == documentId }) { "Document not found" }
        require(entityType.isNotBlank() && entityId.isNotBlank()) { "A link target is required" }
        db.documentLinkDao().insert(DocumentLinkEntity(UUID.randomUUID().toString(), documentId, entityType, entityId, purpose))
        if (entityType == "tax_item") db.taxItemDao().updateEvidenceState(entityId, "ATTACHED", Instant.now().toEpochMilli())
    }

    suspend fun unlinkDocument(documentId: String, entityType: String, entityId: String) {
        require(db.documentDao().getAll().any { it.id == documentId }) { "Document not found" }
        db.documentLinkDao().deleteLink(documentId, entityType, entityId)
        if (entityType == "tax_item" && db.documentLinkDao().getForEntity(entityType, entityId).isEmpty()) {
            db.taxItemDao().updateEvidenceState(entityId, "REQUESTED", Instant.now().toEpochMilli())
        }
    }

    /** Links the replacement first, so a failed replacement never destroys the old evidence link. */
    suspend fun replaceDocumentLink(oldDocumentId: String, newDocumentId: String, entityType: String, entityId: String, purpose: String = "EVIDENCE") {
        db.withTransaction {
            linkDocument(newDocumentId, entityType, entityId, purpose)
            db.documentLinkDao().deleteLink(oldDocumentId, entityType, entityId)
        }
    }

    /** Number of records (tax items, accounts, etc.) currently linked to this document, for a safe-delete warning. */
    suspend fun documentDependencyCount(documentId: String): Int = db.documentLinkDao().getForDocument(documentId).size

    suspend fun deleteDocument(documentId: String) {
        val document = db.documentDao().getAll().firstOrNull { it.id == documentId } ?: error("Document not found")
        db.withTransaction {
            val links = db.documentLinkDao().getForDocument(documentId)
            val now = Instant.now().toEpochMilli()
            links.filter { it.entityType == "tax_item" }.forEach { link ->
                val item = db.taxItemDao().getById(link.entityId)
                if (item != null && item.evidenceState in setOf("ATTACHED", "VERIFIED_BY_USER")) {
                    db.taxItemDao().updateEvidenceState(link.entityId, "REQUESTED", now)
                }
            }
            db.documentLinkDao().deleteForDocument(documentId)
            db.documentDao().delete(documentId)
        }
        File(document.localEncryptedPath).delete()
    }

    /** Schedules (or, if [dueDateEpochDay] is null, cancels) a persisted calendar reminder tied to a due-dated record. [headline]/[body] default to expiry wording (the original, still most common caller); pass both explicitly for a reminder that isn't about something expiring. */
    private suspend fun scheduleOrCancelExpiryReminder(
        context: Context,
        calendarId: String,
        kind: String,
        title: String,
        linkedEntityId: String?,
        dueDateEpochDay: Long?,
        headline: String = "$title is expiring",
        body: String = "Review and renew this record before it expires.",
    ) {
        if (dueDateEpochDay != null) {
            val dueAt = LocalDate.ofEpochDay(dueDateEpochDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            db.calendarDao().upsert(CalendarItemEntity(calendarId, headline, kind, dueAt, linkedEntityId, "OPEN", 0))
            ReminderScheduler(context).schedule(calendarId, dueAt, 0, headline, body)
        } else {
            db.calendarDao().updateStatus(calendarId, "CANCELLED")
            ReminderScheduler(context).cancel(calendarId)
        }
    }

    /** Wires a just-imported document's expiry date to a due-date reminder; call once, right after a successful import that carried an expiry date. */
    suspend fun scheduleDocumentExpiryReminder(context: Context, documentId: String, title: String, expiryDateEpochDay: Long) {
        scheduleOrCancelExpiryReminder(context, "document-expiry-$documentId", "DOCUMENT_EXPIRY", title, documentId, expiryDateEpochDay)
    }

    suspend fun addManualTaxItem(type: String, amountMinor: Long, description: String, date: LocalDate = LocalDate.now()) {
        require(amountMinor > 0 && description.isNotBlank()) { "Tax item details are invalid" }
        val now = Instant.now().toEpochMilli()
        val taxItemId = UUID.randomUUID().toString()
        val sourceId = UUID.randomUUID().toString()
        db.withTransaction {
            val yearId = ensureTaxYearExists(date.year)
            val inserted = db.taxItemDao().insertIfAbsent(TaxItemEntity(taxItemId, yearId, "manual", sourceId, type, date.toEpochDay(), amountMinor, null, "PKR", description.trim(), "CAPTURED", "REQUESTED", null, now, now))
            if (inserted != -1L) recordInitialMapping(yearId, taxItemId, "manual", sourceId, date.toEpochDay(), amountMinor, description.trim(), type, now)
        }
    }

    /**
     * Records the ruleset-generated mapping for a newly-created tax item (Phase 4F). Only called
     * when [TaxItemDao.insertIfAbsent] actually inserted a row, so recomputation never creates a
     * second mapping history for the same source.
     */
    private suspend fun recordInitialMapping(taxYearId: String, taxItemId: String, sourceType: String, sourceId: String, dateEpochDay: Long, amountMinor: Long, description: String, taxEventType: String, createdAt: Long) {
        // Classify against the tax year's own stored ruleset version, not always "current" —
        // otherwise a tax item captured under an older tax year would silently get classified
        // with today's rules, defeating the "historical ruleset versions remain immutable"
        // requirement even though the version column itself is stored correctly.
        val rulesetVersion = db.taxYearDao().getById(taxYearId)?.rulesetVersion ?: pk.vexel.financepassport.core.taxrules.BundledTaxRulesets.CURRENT_VERSION
        val ruleset = pk.vexel.financepassport.core.taxrules.BundledTaxRulesets.loadVersion(rulesetVersion)
        val candidate = TaxCandidate(sourceType, sourceId, dateEpochDay, Money(MinorUnits(amountMinor), "PKR"), description, runCatching { TaxEventType.valueOf(taxEventType) }.getOrNull(), TaxRelevance.RELEVANT)
        val classification = StructuralTaxClassifier().classify(candidate, ruleset)
        val mapping = classification.mapping
        db.taxMappingDao().insert(
            TaxMappingEntity(
                UUID.randomUUID().toString(), taxItemId, ruleset.version,
                mapping?.eventType?.name ?: taxEventType,
                mapping?.sectionCode ?: "UNMAPPED",
                mapping?.categoryCode ?: "UNMAPPED",
                "SYSTEM_GENERATED", null, null, createdAt,
            ),
        )
    }

    suspend fun createEncryptedBackup(context: Context, password: CharArray): ByteArray {
        val snapshotFile = File(context.cacheDir, "passport-backup-${UUID.randomUUID()}.db")
        val escapedPath = snapshotFile.absolutePath.replace("'", "''")
        try {
            db.openHelper.writableDatabase.execSQL("VACUUM INTO '$escapedPath'")
        } catch (_: SQLiteException) {
            // VACUUM INTO was added after the SQLite version shipped on API 26.
            // Checkpoint WAL first, then copy the stable main database file.
            val sqlite = db.openHelper.writableDatabase
            // Keep the current connection mode. Room may still hold active
            // connections on older SQLite builds, where toggling WAL here
            // throws instead of waiting for those connections to drain.
            sqlite.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor -> while (cursor.moveToNext()) { } }
            Files.copy(File(sqlite.path ?: error("Database path is unavailable")).toPath(), snapshotFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        require(snapshotFile.isFile) { "The local database snapshot is not available" }
        val allDocuments = db.documentDao().getAll()
        val documents = allDocuments.map { document ->
            BackupFile("documents/${File(document.localEncryptedPath).name}", File(document.localEncryptedPath).readBytes())
        }
        // Utility bill/payment attachments live in a separate on-disk vault (utility_vault/) from
        // the legacy document vault, so they need their own bundled entries — otherwise the
        // database snapshot restores bill_attachments rows whose storagePath points at files that
        // were never included in the backup at all.
        val utilityAttachments = db.billAttachmentDao().getAll()
        val utilityDocuments = utilityAttachments.map { attachment ->
            BackupFile("documents/utility/${File(attachment.storagePath).name}", File(attachment.storagePath).readBytes())
        }
        val recordCount = db.financialContextDao().getAll().size + db.accountDao().getAll().size + db.financialEventDao().getAll().size + db.wealthDao().getAllAssets().size + db.wealthDao().getAllLiabilities().size + db.taxItemDao().getAll().size + db.documentDao().getAll().size + db.investmentDao().getAll().size + db.receivableDao().getAll().size + db.goalDao().getAll().size + db.officialRecordDao().getAll().size + db.budgetDao().getAll().size + db.incomeSourceDao().getAll().size + db.utilityBillDao().getAll().size + db.monthlyBillOccurrenceDao().getAll().size + db.paymentRecordDao().getAll().size + db.categoryDao().getAll().size + db.recurringTemplateDao().getActive().size + db.expectedOccurrenceDao().getAll().size + db.settlementEventDao().getAll().size + db.simpleInvestmentDao().getActive().size + utilityAttachments.size
        return try {
            BackupPackageService().create(snapshotFile.readBytes(), documents + utilityDocuments, BuildConfig.VERSION_NAME, DATABASE_VERSION, password, recordCount, allDocuments.map { it.sha256 }, runCatching { pk.vexel.financepassport.core.taxrules.BundledTaxRulesets.loadDefault().version }.getOrNull()).payload
        } finally {
            snapshotFile.delete()
        }
    }

    /** Creates the portable backup on disk so the UI never retains the complete archive in memory. */
    suspend fun createEncryptedBackupFile(context: Context, password: CharArray): File {
        val snapshotFile = File(context.cacheDir, "passport-backup-${UUID.randomUUID()}.db")
        val output = File(context.cacheDir, "passport-backup-${UUID.randomUUID()}.backup")
        val escapedPath = snapshotFile.absolutePath.replace("'", "''")
        try {
            try {
                db.openHelper.writableDatabase.execSQL("VACUUM INTO '$escapedPath'")
            } catch (_: SQLiteException) {
                val sqlite = db.openHelper.writableDatabase
                sqlite.query("PRAGMA wal_checkpoint(TRUNCATE)").use { cursor -> while (cursor.moveToNext()) { } }
                Files.copy(File(sqlite.path ?: error("Database path is unavailable")).toPath(), snapshotFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            val documents = db.documentDao().getAll().map { document ->
                BackupDiskFile("documents/${File(document.localEncryptedPath).name}", File(document.localEncryptedPath))
            }
            val utilityAttachments = db.billAttachmentDao().getAll()
            val utilityDocuments = utilityAttachments.map { attachment ->
                BackupDiskFile("documents/utility/${File(attachment.storagePath).name}", File(attachment.storagePath))
            }
            val recordCount = db.financialContextDao().getAll().size + db.accountDao().getAll().size + db.financialEventDao().getAll().size + db.wealthDao().getAllAssets().size + db.wealthDao().getAllLiabilities().size + db.taxItemDao().getAll().size + db.documentDao().getAll().size + db.investmentDao().getAll().size + db.receivableDao().getAll().size + db.goalDao().getAll().size + db.officialRecordDao().getAll().size + db.budgetDao().getAll().size + db.incomeSourceDao().getAll().size + db.utilityBillDao().getAll().size + db.monthlyBillOccurrenceDao().getAll().size + db.paymentRecordDao().getAll().size + db.categoryDao().getAll().size + db.recurringTemplateDao().getActive().size + db.expectedOccurrenceDao().getAll().size + db.settlementEventDao().getAll().size + db.simpleInvestmentDao().getActive().size + utilityAttachments.size
            BackupPackageService().createStreaming(snapshotFile, documents + utilityDocuments, BuildConfig.VERSION_NAME, DATABASE_VERSION, password, recordCount, output)
            return output
        } catch (failure: Throwable) {
            output.delete()
            throw failure
        } finally {
            snapshotFile.delete()
        }
    }

    /**
     * Generates (or regenerates, as a new version) the annual draft for [year] — defaults to the
     * current year but any prior tax year with captured tax items can be selected (Phase 5A: a
     * selected-year workspace, not just "now"). Regeneration never overwrites a prior draft: see
     * [pk.vexel.financepassport.core.database.TaxDraftDao.maxVersion].
     */
    suspend fun prepareAnnualDraft(year: Int = LocalDate.now().year): TaxAnnualDraftEntity {
        val yearId = ensureTaxYearExists(year)
        val items = db.taxItemDao().getAll().filter { it.taxYearId == yearId && it.reviewState != "EXCLUDED" }
        // Load the ruleset version this specific tax year was actually created under, not
        // whatever is "current" now — this is what makes regenerating a draft for an older tax
        // year keep using that year's original rules even after a newer ruleset version ships.
        val storedVersion = db.taxYearDao().getById(yearId)?.rulesetVersion ?: pk.vexel.financepassport.core.taxrules.BundledTaxRulesets.CURRENT_VERSION
        val rules = pk.vexel.financepassport.core.taxrules.BundledTaxRulesets.loadVersion(storedVersion).copy(taxYear = year.toString())
        val domainYear = TaxYear(yearId, "PK", year.toString(), LocalDate.of(year, 1, 1).toEpochDay(), LocalDate.of(year, 12, 31).toEpochDay(), rules.version)
        val candidates = items.mapNotNull { item -> runCatching { TaxEventType.valueOf(item.taxEventType) }.getOrNull()?.let { type -> TaxCandidate(item.sourceType, item.sourceId, item.dateEpochDay, Money(MinorUnits(item.grossAmountMinor ?: 0), item.currency), item.description, type, TaxRelevance.RELEVANT) } }
        val generated = AnnualDraftGenerator().generate(domainYear, rules, candidates)
        val now = Instant.now().toEpochMilli(); val version = db.taxDraftDao().maxVersion(yearId) + 1; val draftId = UUID.randomUUID().toString()

        // Preflight issues beyond classifier output (Phase 4I/5C, previously undone): these are
        // real, persisted, browsable TaxIssueEntity rows — not just a thrown reconciliation error
        // or a display-only UI count — so a user can see them without first triggering the action
        // that would otherwise surface them.
        val missingOpeningSnapshotIssue = if (db.wealthSnapshotDao().get(yearId, "OPENING") == null) {
            listOf(TaxIssue("MISSING_OPENING_SNAPSHOT", "Record an opening wealth snapshot", "Reconciliation for $year needs an opening wealth snapshot before it can run — record one from the Tax workspace.", null))
        } else emptyList()
        val duplicateCandidateIssues = detectDuplicateCandidates(
            items.map { item -> DuplicateCandidateInput(item.sourceId, item.dateEpochDay, item.grossAmountMinor, item.currency, item.description) },
        )
        val allIssues = generated.issues + missingOpeningSnapshotIssue + duplicateCandidateIssues

        val draft = TaxAnnualDraftEntity(draftId, yearId, version, rules.version, now, "DRAFT", allIssues.size)
        db.withTransaction {
            db.taxDraftDao().insertDraft(draft)
            db.taxDraftDao().insertLines(generated.lines.map { line -> TaxDraftLineEntity(UUID.randomUUID().toString(), draftId, line.sectionCode, line.categoryCode, line.amount.minorUnits.value, line.amount.currency, line.sourceIds.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\""), line.calculation) })
            db.taxIssueDao().insertAll(allIssues.map { issue -> TaxIssueEntity(UUID.randomUUID().toString(), draftId, issue.code, issue.title, issue.explanation, issue.sourceId, "OPEN", now) })
        }
        return draft
    }

    /** Chronological classification history for one tax item — system-generated vs. user-override,
     * with the supersession chain intact (Phase 4F lineage, drill-down added in Phase 11). */
    suspend fun getMappingHistory(taxItemId: String): List<TaxMappingEntity> = db.taxMappingDao().getForTaxItem(taxItemId)

    suspend fun getDraftLines(draftId: String): List<TaxDraftLineEntity> = db.taxDraftDao().getLines(draftId)

    /** Idempotently ensures a `tax_years` row exists for [year] so a snapshot, draft or
     * reconciliation can be recorded against it regardless of which of the three the user does
     * first. Returns the tax year's id. */
    private suspend fun ensureTaxYearExists(year: Int): String {
        val yearId = "PK-$year"
        val rulesetVersion = defaultPakistanStructuralRules().version
        db.openHelper.writableDatabase.execSQL(
            "INSERT OR IGNORE INTO tax_years (id, jurisdictionCode, yearLabel, startDateEpochDay, endDateEpochDay, rulesetVersion, status) VALUES (?, 'PK', ?, ?, ?, ?, 'OPEN')",
            arrayOf<Any>(yearId, year.toString(), LocalDate.of(year, 1, 1).toEpochDay(), LocalDate.of(year, 12, 31).toEpochDay(), rulesetVersion),
        )
        return yearId
    }

    /**
     * Records the current canonical [FinancialPosition] as the opening or closing wealth snapshot
     * for [year] (Phase 5D). Re-recording the same kind replaces the prior snapshot — this is
     * a working estimate the user can correct, not an immutable source fact; a reconciliation
     * already generated from an earlier snapshot keeps its own recorded figures regardless.
     */
    suspend fun recordWealthSnapshot(year: Int, kind: String, date: LocalDate = LocalDate.now()) {
        require(kind == "OPENING" || kind == "CLOSING") { "Snapshot kind must be OPENING or CLOSING" }
        val taxYearId = ensureTaxYearExists(year)
        val position = financialPosition.first()
        db.wealthSnapshotDao().upsert(
            WealthSnapshotEntity(
                UUID.randomUUID().toString(), taxYearId, kind, date.toEpochDay(),
                position.liquidFundsMinor, position.investmentsValueMinor, position.assetsValueMinor,
                position.receivablesValueMinor, position.liabilitiesValueMinor, position.netWorthMinor,
                Instant.now().toEpochMilli(),
            ),
        )
    }

    /**
     * Tax-year annual-close lifecycle (mega-prompt 5F): `tax_years.status` previously only ever
     * got set to "OPEN" at creation. This enforces the OPEN -> UNDER_REVIEW -> FILED progression,
     * plus an explicit reopen step back to OPEN for correcting a year that was moved to review or
     * filed too early — a real-world need, not a state a well-formed lifecycle should trap a user
     * behind. Never silently no-ops on an invalid transition; callers get a clear error instead.
     */
    suspend fun updateTaxYearStatus(taxYearId: String, newStatus: String) {
        val validStatuses = setOf("OPEN", "UNDER_REVIEW", "FILED")
        require(newStatus in validStatuses) { "Unknown tax year status: $newStatus" }
        val year = db.taxYearDao().getById(taxYearId) ?: error("Unknown tax year: $taxYearId")
        val allowedTransitions = mapOf(
            "OPEN" to setOf("UNDER_REVIEW"),
            "UNDER_REVIEW" to setOf("FILED", "OPEN"),
            "FILED" to setOf("OPEN"),
        )
        require(newStatus in (allowedTransitions[year.status] ?: emptySet())) {
            "Cannot move tax year ${year.id} from ${year.status} to $newStatus"
        }
        db.taxYearDao().updateStatus(taxYearId, newStatus)
    }

    /**
     * Reconciles [taxYearId]'s recorded closing wealth against what an opening position plus the
     * year's recognized income/expenditure would predict (Phase 5E). Requires an opening snapshot
     * to exist for the year — reconciliation must never silently treat opening wealth as zero — and
     * scopes income/expenditure to events that actually fall within the tax year's date range,
     * rather than the entire event history.
     */
    suspend fun calculateReconciliation(taxYearId: String): WealthReconciliationResult {
        val year = db.taxYearDao().getById(taxYearId) ?: error("Unknown tax year: $taxYearId. Prepare a draft or record a snapshot for it first.")
        val opening = db.wealthSnapshotDao().get(taxYearId, "OPENING")
            ?: error("Record an opening wealth snapshot for $taxYearId before reconciling.")
        val closing = db.wealthSnapshotDao().get(taxYearId, "CLOSING")
        val eventsInYear = db.financialEventDao().getAll().filter { it.dateEpochDay in year.startDateEpochDay..year.endDateEpochDay }
        val income = eventsInYear.filter { it.eventType == "INCOME" }.sumOf { it.amountMinor }
        val expense = eventsInYear.filter { it.eventType == "EXPENSE" }.sumOf { it.amountMinor }
        // A closing snapshot is preferred; falling back to the live canonical position only makes
        // sense for a year that has not been formally closed out yet.
        val recordedClosing = closing?.netWealthMinor ?: financialPosition.first().netWorthMinor
        val result = reconcileWealth(WealthReconciliationInput(Money(MinorUnits(opening.netWealthMinor)), Money(MinorUnits(income)), Money(MinorUnits(expense)), Money(MinorUnits(0)), Money(MinorUnits(0)), Money(MinorUnits(recordedClosing))))
        val now = Instant.now().toEpochMilli()
        db.reconciliationDao().insert(WealthReconciliationEntity(UUID.randomUUID().toString(), taxYearId, opening.netWealthMinor, income, expense, 0, 0, result.expectedClosing.minorUnits.value, recordedClosing, result.unexplainedDifference.minorUnits.value, result.calculation))
        return result
    }

    suspend fun scheduleUtilityReminders(context: Context) {
        val activeProfiles = db.utilityBillDao().getAll().filter { it.status == "ACTIVE" && it.reminderPreference == "ENABLED" }
        val unpaidOccurrences = db.monthlyBillOccurrenceDao().getAll().filter { it.status == "Pending" || it.status == "Overdue" }
        
        for (occ in unpaidOccurrences) {
            val profile = activeProfiles.find { it.id == occ.profileId } ?: continue
            val dueDate = java.time.LocalDate.ofEpochDay(occ.expectedDueDateEpochDay)
            val dueAtMillis = dueDate.atTime(9, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            val monthLabel = java.time.YearMonth.of(occ.billingYear, occ.billingMonth).format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
            val amtStr = occ.amountMinor?.let { "PKR " + (it / 100) } ?: "TBD"

            val idSoon = "soon-${occ.id}"
            val delayBefore = 2 * 24 * 60L
            ReminderScheduler(context).schedule(
                idSoon, 
                dueAtMillis, 
                delayBefore, 
                "Bill Due Soon: ${profile.name}", 
                "Your bill for $monthLabel is due on $dueDate. Expected amount: $amtStr."
            )
            
            val idDue = "due-${occ.id}"
            ReminderScheduler(context).schedule(
                idDue, 
                dueAtMillis, 
                0, 
                "Bill Due Today: ${profile.name}", 
                "Your bill for $monthLabel is due today! Please record payment."
            )
        }
    }

    fun cancelUtilityReminders(context: Context, occurrenceId: String) {
        ReminderScheduler(context).cancel("soon-$occurrenceId")
        ReminderScheduler(context).cancel("due-$occurrenceId")
    }

    suspend fun reconcileAllUtilityBills(context: Context) {
        UtilityRecurrenceEngine.reconcileAll(db, java.time.LocalDate.now())
        scheduleUtilityReminders(context)
    }
    suspend fun addAdjustment(accountId: String, amountMinor: Long, description: String) {
        require(db.accountDao().getById(accountId)?.status == "ACTIVE") { "Choose an active account" }
        val now = Instant.now().toEpochMilli()
        val eventId = UUID.randomUUID().toString()
        val event = FinancialEventEntity(eventId, FinancialEventType.ADJUSTMENT.name, LocalDate.now().toEpochDay(), amountMinor, "PKR", accountId, null, null, description.trim(), null, "UNKNOWN", null, now, now, null)
        db.financialEventDao().upsert(event)
    }

    val financialContexts: kotlinx.coroutines.flow.Flow<List<pk.vexel.financepassport.core.database.FinancialContextEntity>> = db.financialContextDao().observeActive()
    
    val unassignedEvents: kotlinx.coroutines.flow.Flow<List<pk.vexel.financepassport.core.database.FinancialEventEntity>> = db.financialEventDao().observeActive().map { list ->
        list.filter { it.accountId == null || it.contextId == null }
    }
    
    fun observeTotalsInRange(start: Long, end: Long): kotlinx.coroutines.flow.Flow<Pair<pk.vexel.financepassport.core.model.Money, pk.vexel.financepassport.core.model.Money>> = kotlinx.coroutines.flow.combine(
        db.financialEventDao().observeIncomeMinorInRange(start, end),
        db.financialEventDao().observeExpenseMinorInRange(start, end)
    ) { income, expense ->
        pk.vexel.financepassport.core.model.Money(pk.vexel.financepassport.core.model.MinorUnits(income), "PKR") to pk.vexel.financepassport.core.model.Money(pk.vexel.financepassport.core.model.MinorUnits(expense), "PKR")
    }

    suspend fun upsertFinancialContext(id: String, domain: String, name: String) {
        val now = Instant.now().toEpochMilli()
        db.withTransaction {
            val existing = db.financialContextDao().getById(id)
            if (existing != null) {
                db.financialContextDao().upsert(existing.copy(domain = domain, name = name, updatedAtEpochMillis = now))
            } else {
                db.financialContextDao().upsert(pk.vexel.financepassport.core.database.FinancialContextEntity(id, domain, name, "ACTIVE", now, now))
            }
        }
    }
    
    suspend fun assignContext(eventId: String, contextId: String?) {
        val now = Instant.now().toEpochMilli()
        db.withTransaction {
            db.financialEventDao().getById(eventId)?.let {
                db.financialEventDao().upsert(it.copy(contextId = contextId, updatedAtEpochMillis = now))
            }
        }
    }

    suspend fun assignAccount(eventId: String, accountId: String?) {
        val now = Instant.now().toEpochMilli()
        db.withTransaction {
            db.financialEventDao().getById(eventId)?.let {
                db.financialEventDao().upsert(it.copy(accountId = accountId, updatedAtEpochMillis = now))
            }
        }
    }
}
