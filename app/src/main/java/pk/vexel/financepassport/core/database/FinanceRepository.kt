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
import pk.vexel.financepassport.core.model.toYearMonth
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
    val accounts: Flow<List<AccountEntity>> = db.accountDao().observeActive()
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
    val documents: Flow<List<DocumentEntity>> = db.documentDao().observeAll()
    val drafts: Flow<List<TaxAnnualDraftEntity>> = db.taxDraftDao().observeDrafts()
    val taxIssues: Flow<List<TaxIssueEntity>> = db.taxIssueDao().observeAll()
    val reconciliations: Flow<List<WealthReconciliationEntity>> = db.reconciliationDao().observeAll()
    val taxYears: Flow<List<TaxYearEntity>> = db.taxYearDao().observeAll()
    val recurringItems: Flow<List<RecurringItemEntity>> = db.recurringItemDao().observeActive()
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
            combine(db.wealthDao().observeAssets(), db.wealthDao().observeLiabilities(), db.investmentDao().observeAll(), db.receivableDao().observeAll(), ::WealthSnapshot),
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
            )
        }
    }

    private data class AccountsSnapshot(val openingBalanceMinor: Long, val movementMinor: Long, val monthlyIncomeMinor: Long, val monthlyExpenseMinor: Long)
    private data class WealthSnapshot(val assets: List<AssetEntity>, val liabilities: List<LiabilityEntity>, val investments: List<InvestmentEventEntity>, val receivables: List<ReceivableEntity>)

    fun accountMovement(accountId: String): Flow<Long> = db.financialEventDao().observeAccountMovement(accountId)

    suspend fun addAccount(name: String, type: String, openingBalanceMinor: Long, currency: String = "PKR", institution: String? = null, notes: String? = null) {
        val now = Instant.now().toEpochMilli()
        db.accountDao().upsert(
            AccountEntity(
                UUID.randomUUID().toString(), name.trim(), institution?.trim()?.takeIf { it.isNotEmpty() }, type, null, null,
                currency, openingBalanceMinor, LocalDate.now().toEpochDay(), "ACTIVE", notes?.trim()?.takeIf { it.isNotEmpty() }, now, now,
            ),
        )
    }

    suspend fun updateAccount(id: String, name: String, openingBalanceMinor: Long, institution: String? = null, notes: String? = null) {
        require(name.isNotBlank() && openingBalanceMinor >= 0) { "Account details are invalid" }
        require(db.accountDao().getById(id) != null) { "Account not found" }
        db.accountDao().updateDetails(id, name.trim(), openingBalanceMinor, institution?.trim()?.takeIf { it.isNotEmpty() }, notes?.trim()?.takeIf { it.isNotEmpty() }, Instant.now().toEpochMilli())
    }

    suspend fun archiveAccount(id: String) {
        require(db.accountDao().getById(id) != null) { "Account not found" }
        db.accountDao().archive(id, Instant.now().toEpochMilli())
    }

    suspend fun addAsset(title: String, type: String, valueMinor: Long) {
        db.wealthDao().upsertAsset(AssetEntity(UUID.randomUUID().toString(), type, title.trim(), LocalDate.now().toEpochDay(), valueMinor, valueMinor, "PKR", 100, null, null, "ACTIVE"))
    }

    suspend fun updateAssetValuation(id: String, valueMinor: Long) {
        require(valueMinor >= 0) { "Asset valuation cannot be negative" }
        require(db.wealthDao().getAssetById(id) != null) { "Asset not found" }
        db.wealthDao().updateAssetValue(id, valueMinor)
    }

    suspend fun disposeAsset(id: String, valueMinor: Long) {
        require(valueMinor >= 0) { "Disposal value cannot be negative" }
        require(db.wealthDao().getAssetById(id) != null) { "Asset not found" }
        db.wealthDao().archiveAsset(id, LocalDate.now().toEpochDay(), valueMinor)
    }

    suspend fun addLiability(title: String, type: String, outstandingMinor: Long) {
        db.wealthDao().upsertLiability(LiabilityEntity(UUID.randomUUID().toString(), type, title.trim(), null, outstandingMinor, outstandingMinor, "PKR", LocalDate.now().toEpochDay(), null, "ACTIVE"))
    }

    suspend fun recordLiabilityPayment(id: String, amountMinor: Long) {
        val liability = db.wealthDao().getLiabilityById(id) ?: error("Liability not found")
        require(amountMinor > 0 && amountMinor <= liability.outstandingAmountMinor) { "Payment exceeds outstanding liability" }
        val remaining = liability.outstandingAmountMinor - amountMinor
        db.wealthDao().updateLiabilityOutstanding(id, remaining, if (remaining == 0L) "SETTLED" else "ACTIVE")
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
        require(status in setOf("COMPLETED", "CANCELLED", "OPEN"))
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

    suspend fun addEvent(type: FinancialEventType, amountMinor: Long, accountId: String, description: String, category: String? = null, taxRelevance: String = "UNKNOWN", date: LocalDate = LocalDate.now()) {
        require(amountMinor > 0) { "Amount must be greater than zero" }
        require(db.accountDao().getById(accountId)?.status == "ACTIVE") { "Choose an active account" }
        val now = Instant.now().toEpochMilli()
        val eventId = UUID.randomUUID().toString()
        val event = FinancialEventEntity(eventId, type.name, date.toEpochDay(), amountMinor, "PKR", accountId, category?.trim()?.takeIf { it.isNotEmpty() }, description.trim(), null, taxRelevance, null, now, now)
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
        val out = FinancialEventEntity(UUID.randomUUID().toString(), "TRANSFER", date.toEpochDay(), -amountMinor, "PKR", sourceAccountId, null, description.trim(), null, "NOT_RELEVANT", null, now, now)
        val incoming = FinancialEventEntity(UUID.randomUUID().toString(), "TRANSFER", date.toEpochDay(), amountMinor, "PKR", destinationAccountId, null, description.trim(), null, "NOT_RELEVANT", null, now, now)
        db.withTransaction {
            db.financialEventDao().insertAll(listOf(out, incoming))
            db.transferLinkDao().insert(TransferLinkEntity(UUID.randomUUID().toString(), out.id, incoming.id, group))
        }
    }

    fun toDomain(entity: FinancialEventEntity) = FinancialEvent(entity.id, runCatching { FinancialEventType.valueOf(entity.eventType) }.getOrDefault(FinancialEventType.ADJUSTMENT), Money(MinorUnits(kotlin.math.abs(entity.amountMinor)), entity.currency), entity.accountId, entity.dateEpochDay, entity.description)

    suspend fun exportSnapshot() = ExportSnapshot(
        db.accountDao().getAll(), db.financialEventDao().getAll(), db.wealthDao().getAllAssets(), db.wealthDao().getAllLiabilities(),
        db.taxItemDao().getAll(), db.documentDao().getAll(), db.investmentDao().getAll(), db.receivableDao().getAll(),
        db.goalDao().getAll(), db.officialRecordDao().getAll(), db.budgetDao().getAll(),
        db.taxMappingDao().getAll(), db.wealthSnapshotDao().getAll(), db.taxDraftDao().getAll(),
    )

    suspend fun deleteAllData(context: Context) {
        db.withTransaction { db.clearAllTables() }
        WorkManager.getInstance(context).cancelAllWork()
        File(context.filesDir, "vault").deleteRecursively()
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
        db.documentLinkDao().insert(DocumentLinkEntity(UUID.randomUUID().toString(), documentId, entityType, entityId, purpose))
        if (entityType == "tax_item") db.taxItemDao().updateEvidenceState(entityId, "ATTACHED", Instant.now().toEpochMilli())
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
        val recordCount = db.accountDao().getAll().size + db.financialEventDao().getAll().size + db.wealthDao().getAllAssets().size + db.wealthDao().getAllLiabilities().size + db.taxItemDao().getAll().size + db.documentDao().getAll().size + db.investmentDao().getAll().size + db.receivableDao().getAll().size + db.goalDao().getAll().size + db.officialRecordDao().getAll().size + db.budgetDao().getAll().size
        return try {
            BackupPackageService().create(snapshotFile.readBytes(), documents, BuildConfig.VERSION_NAME, 10, password, recordCount, allDocuments.map { it.sha256 }, runCatching { pk.vexel.financepassport.core.taxrules.BundledTaxRulesets.loadDefault().version }.getOrNull()).payload
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
            val recordCount = db.accountDao().getAll().size + db.financialEventDao().getAll().size + db.wealthDao().getAllAssets().size + db.wealthDao().getAllLiabilities().size + db.taxItemDao().getAll().size + db.documentDao().getAll().size + db.investmentDao().getAll().size + db.receivableDao().getAll().size + db.goalDao().getAll().size + db.officialRecordDao().getAll().size + db.budgetDao().getAll().size
            BackupPackageService().createStreaming(snapshotFile, documents, BuildConfig.VERSION_NAME, 10, password, recordCount, output)
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
}
