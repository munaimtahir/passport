package pk.vexel.financepassport.core.database

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import pk.vexel.financepassport.core.model.FinancialEvent
import pk.vexel.financepassport.core.model.FinancialEventType
import pk.vexel.financepassport.core.model.Money
import pk.vexel.financepassport.core.model.MinorUnits
import pk.vexel.financepassport.core.model.CategoryBudgetStatus
import pk.vexel.financepassport.core.model.GoalProgress
import pk.vexel.financepassport.core.model.RecurringFrequency
import pk.vexel.financepassport.core.model.calculateCategoryBudgets
import pk.vexel.financepassport.core.model.calculateGoalProgress
import pk.vexel.financepassport.core.model.advanceRecurringDueDate
import pk.vexel.financepassport.core.model.toYearMonth
import java.time.Instant
import java.util.UUID
import pk.vexel.financepassport.core.export.ExportSnapshot
import pk.vexel.financepassport.core.taxrules.AnnualDraftGenerator
import pk.vexel.financepassport.core.taxrules.TaxCandidate
import pk.vexel.financepassport.core.taxrules.TaxEventType
import pk.vexel.financepassport.core.taxrules.TaxRelevance
import pk.vexel.financepassport.core.taxrules.TaxYear
import pk.vexel.financepassport.core.taxrules.defaultPakistanStructuralRules
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
import pk.vexel.financepassport.core.security.BackupPackageService
import pk.vexel.financepassport.core.security.KeystoreCryptoService
import pk.vexel.financepassport.core.calendar.ReminderScheduler

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

    fun accountMovement(accountId: String): Flow<Long> = db.financialEventDao().observeAccountMovement(accountId)

    suspend fun addAccount(name: String, type: String, openingBalanceMinor: Long, currency: String = "PKR") {
        val now = Instant.now().toEpochMilli()
        db.accountDao().upsert(AccountEntity(UUID.randomUUID().toString(), name.trim(), null, type, null, null, currency, openingBalanceMinor, LocalDate.now().toEpochDay(), "ACTIVE", null, now, now))
    }

    suspend fun updateAccount(id: String, name: String, openingBalanceMinor: Long) {
        require(name.isNotBlank() && openingBalanceMinor >= 0) { "Account details are invalid" }
        require(db.accountDao().getById(id) != null) { "Account not found" }
        db.accountDao().updateDetails(id, name.trim(), openingBalanceMinor, Instant.now().toEpochMilli())
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

    suspend fun addInvestmentEvent(securityName: String, type: String, amountMinor: Long, quantityMinor: Long = 0, feesMinor: Long = 0, taxWithheldMinor: Long = 0) {
        require(amountMinor >= 0 && quantityMinor >= 0 && feesMinor >= 0 && taxWithheldMinor >= 0)
        db.investmentDao().insert(InvestmentEventEntity(UUID.randomUUID().toString(), "manual", securityName.trim(), type, LocalDate.now().toEpochDay(), quantityMinor, amountMinor, feesMinor, taxWithheldMinor, "PKR"))
    }

    suspend fun addReceivable(title: String, counterparty: String, amountMinor: Long) {
        db.receivableDao().upsert(ReceivableEntity(UUID.randomUUID().toString(), title.trim(), counterparty.trim(), amountMinor, amountMinor, null, "OPEN"))
    }

    suspend fun recordReceivablePayment(id: String, amountMinor: Long) {
        val current = db.receivableDao().getById(id) ?: error("Receivable not found")
        require(amountMinor > 0 && amountMinor <= current.outstandingAmountMinor) { "Payment exceeds outstanding amount" }
        val outstanding = current.outstandingAmountMinor - amountMinor
        db.receivableDao().upsert(current.copy(outstandingAmountMinor = outstanding, status = if (outstanding == 0L) "SETTLED" else "OPEN"))
    }

    suspend fun addGoal(title: String, targetAmountMinor: Long) {
        db.goalDao().upsert(GoalEntity(UUID.randomUUID().toString(), title.trim(), "CUSTOM", targetAmountMinor, null, "OPEN"))
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

    suspend fun addOfficialRecord(type: String, title: String, identifier: String?) {
        val normalized = identifier?.trim()?.takeIf { it.isNotEmpty() }
        val encrypted = normalized?.let { KeystoreCryptoService().encrypt(it.toByteArray(), title.toByteArray()).let { value -> value.nonce + value.ciphertext } }
        val masked = normalized?.let { if (it.length <= 4) "••••" else "••••${it.takeLast(4)}" }
        db.officialRecordDao().upsert(OfficialRecordEntity(UUID.randomUUID().toString(), type, title.trim(), masked, encrypted, null, null, null, "ACTIVE"))
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

    suspend fun addEvent(type: FinancialEventType, amountMinor: Long, accountId: String, description: String, category: String? = null, taxRelevance: String = "UNKNOWN") {
        val now = Instant.now().toEpochMilli()
        val eventId = UUID.randomUUID().toString()
        val date = LocalDate.now()
        val event = FinancialEventEntity(eventId, type.name, date.toEpochDay(), amountMinor, "PKR", accountId, category?.trim()?.takeIf { it.isNotEmpty() }, description.trim(), null, taxRelevance, null, now, now)
        db.withTransaction {
            db.financialEventDao().upsert(event)
            if (type == FinancialEventType.INCOME) {
                val yearId = "PK-${date.year}"
                db.openHelper.writableDatabase.execSQL("INSERT OR IGNORE INTO tax_years (id, jurisdictionCode, yearLabel, startDateEpochDay, endDateEpochDay, rulesetVersion, status) VALUES (?, 'PK', ?, ?, ?, 'pk-structural-1', 'OPEN')", arrayOf<Any>(yearId, date.year.toString(), LocalDate.of(date.year, 1, 1).toEpochDay(), LocalDate.of(date.year, 12, 31).toEpochDay()))
                db.taxItemDao().insertIfAbsent(TaxItemEntity(UUID.randomUUID().toString(), yearId, "financial_event", eventId, "EMPLOYMENT_INCOME", date.toEpochDay(), amountMinor, null, "PKR", description.trim(), "CAPTURED", "REQUESTED", null, now, now))
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
            // Recurring processing is deliberately non-posting. It advances the reminder and
            // leaves an unconfirmed draft/review surface for explicit user confirmation.
            val frequency = runCatching { RecurringFrequency.valueOf(item.frequency) }.getOrNull() ?: continue
            val nextDueDate = advanceRecurringDueDate(LocalDate.ofEpochDay(item.nextDueDateEpochDay), frequency, item.anchorDayOfMonth)
            db.recurringItemDao().advanceDueDate(item.id, nextDueDate.toEpochDay(), Instant.now().toEpochMilli())
            val dueAt = nextDueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            db.calendarDao().upsert(CalendarItemEntity("recurring-${item.id}", "Recurring draft: ${item.title}", "RECURRING_DRAFT", dueAt, item.id, "OPEN", 0))
            ReminderScheduler(context).schedule("recurring-${item.id}", dueAt, 0, "Recurring draft: ${item.title}", "Review and confirm this recurring ${item.eventType.lowercase()}.")
        }
    }

    suspend fun transfer(sourceAccountId: String, destinationAccountId: String, amountMinor: Long, description: String) {
        require(sourceAccountId != destinationAccountId) { "Transfer accounts must be different" }
        require(amountMinor > 0) { "Transfer amount must be positive" }
        val now = Instant.now().toEpochMilli()
        val group = UUID.randomUUID().toString()
        val out = FinancialEventEntity(UUID.randomUUID().toString(), "TRANSFER", LocalDate.now().toEpochDay(), -amountMinor, "PKR", sourceAccountId, null, description.trim(), null, "NOT_RELEVANT", null, now, now)
        val incoming = FinancialEventEntity(UUID.randomUUID().toString(), "TRANSFER", LocalDate.now().toEpochDay(), amountMinor, "PKR", destinationAccountId, null, description.trim(), null, "NOT_RELEVANT", null, now, now)
        db.withTransaction {
            db.financialEventDao().insertAll(listOf(out, incoming))
            db.transferLinkDao().insert(TransferLinkEntity(UUID.randomUUID().toString(), out.id, incoming.id, group))
        }
    }

    fun toDomain(entity: FinancialEventEntity) = FinancialEvent(entity.id, runCatching { FinancialEventType.valueOf(entity.eventType) }.getOrDefault(FinancialEventType.ADJUSTMENT), Money(MinorUnits(kotlin.math.abs(entity.amountMinor)), entity.currency), entity.accountId, entity.dateEpochDay, entity.description)

    suspend fun exportSnapshot() = ExportSnapshot(db.accountDao().getAll(), db.financialEventDao().getAll(), db.wealthDao().getAllAssets(), db.wealthDao().getAllLiabilities(), db.taxItemDao().getAll(), db.documentDao().getAll(), db.investmentDao().getAll(), db.receivableDao().getAll(), db.goalDao().getAll(), db.officialRecordDao().getAll(), db.budgetDao().getAll())

    suspend fun deleteAllData() { db.withTransaction { db.clearAllTables() } }

    suspend fun updateTaxReview(id: String, state: String, reason: String? = null) {
        require(state in setOf("DRAFT", "CAPTURED", "NEEDS_EVIDENCE", "NEEDS_CLASSIFICATION", "REVIEWED", "INCLUDED", "EXCLUDED")) { "Invalid tax review state" }
        db.taxItemDao().updateReview(id, state, reason, Instant.now().toEpochMilli())
    }

    suspend fun reviewTaxItem(id: String, taxEventType: String, state: String, reason: String?) {
        require(runCatching { TaxEventType.valueOf(taxEventType) }.isSuccess) { "Unsupported tax event type" }
        require(state in setOf("REVIEWED", "INCLUDED", "EXCLUDED")) { "Invalid reviewed state" }
        require(state != "EXCLUDED" || !reason.isNullOrBlank()) { "An exclusion reason is required" }
        db.taxItemDao().updateClassification(id, taxEventType, state, reason?.trim(), Instant.now().toEpochMilli())
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

    suspend fun deleteDocument(documentId: String) {
        val document = db.documentDao().getAll().firstOrNull { it.id == documentId } ?: error("Document not found")
        db.withTransaction {
            db.documentLinkDao().deleteForDocument(documentId)
            db.documentDao().delete(documentId)
        }
        File(document.localEncryptedPath).delete()
    }

    suspend fun addManualTaxItem(type: String, amountMinor: Long, description: String) {
        require(amountMinor > 0 && description.isNotBlank()) { "Tax item details are invalid" }
        val date = LocalDate.now()
        val yearId = "PK-${date.year}"
        val now = Instant.now().toEpochMilli()
        db.withTransaction {
            db.openHelper.writableDatabase.execSQL("INSERT OR IGNORE INTO tax_years (id, jurisdictionCode, yearLabel, startDateEpochDay, endDateEpochDay, rulesetVersion, status) VALUES (?, 'PK', ?, ?, ?, 'pk-structural-1', 'OPEN')", arrayOf<Any>(yearId, date.year.toString(), LocalDate.of(date.year, 1, 1).toEpochDay(), LocalDate.of(date.year, 12, 31).toEpochDay()))
            db.taxItemDao().insertIfAbsent(TaxItemEntity(UUID.randomUUID().toString(), yearId, "manual", UUID.randomUUID().toString(), type, date.toEpochDay(), amountMinor, null, "PKR", description.trim(), "CAPTURED", "REQUESTED", null, now, now))
        }
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
        val documents = db.documentDao().getAll().map { document ->
            BackupFile("documents/${File(document.localEncryptedPath).name}", File(document.localEncryptedPath).readBytes())
        }
        val recordCount = db.accountDao().getAll().size + db.financialEventDao().getAll().size + db.wealthDao().getAllAssets().size + db.wealthDao().getAllLiabilities().size + db.taxItemDao().getAll().size + db.documentDao().getAll().size + db.investmentDao().getAll().size + db.receivableDao().getAll().size + db.goalDao().getAll().size + db.officialRecordDao().getAll().size + db.budgetDao().getAll().size
        return try {
            BackupPackageService().create(snapshotFile.readBytes(), documents, BuildConfig.VERSION_NAME, 5, password, recordCount).payload
        } finally {
            snapshotFile.delete()
        }
    }

    suspend fun prepareAnnualDraft(): TaxAnnualDraftEntity {
        val year = LocalDate.now().year
        val yearId = "PK-$year"
        db.openHelper.writableDatabase.execSQL("INSERT OR IGNORE INTO tax_years (id, jurisdictionCode, yearLabel, startDateEpochDay, endDateEpochDay, rulesetVersion, status) VALUES (?, 'PK', ?, ?, ?, 'pk-structural-1', 'OPEN')", arrayOf<Any>(yearId, year.toString(), LocalDate.of(year, 1, 1).toEpochDay(), LocalDate.of(year, 12, 31).toEpochDay()))
        val items = db.taxItemDao().getAll().filter { it.taxYearId == yearId && it.reviewState != "EXCLUDED" }
        val rules = defaultPakistanStructuralRules().copy(taxYear = year.toString())
        val domainYear = TaxYear(yearId, "PK", year.toString(), LocalDate.of(year, 1, 1).toEpochDay(), LocalDate.of(year, 12, 31).toEpochDay(), rules.version)
        val candidates = items.mapNotNull { item -> runCatching { TaxEventType.valueOf(item.taxEventType) }.getOrNull()?.let { type -> TaxCandidate(item.sourceType, item.sourceId, item.dateEpochDay, Money(MinorUnits(item.grossAmountMinor ?: 0), item.currency), item.description, type, TaxRelevance.RELEVANT) } }
        val generated = AnnualDraftGenerator().generate(domainYear, rules, candidates)
        val now = Instant.now().toEpochMilli(); val version = db.taxDraftDao().maxVersion(yearId) + 1; val draftId = UUID.randomUUID().toString()
        val draft = TaxAnnualDraftEntity(draftId, yearId, version, rules.version, now, "DRAFT", generated.issues.size)
        db.withTransaction {
            db.taxDraftDao().insertDraft(draft)
            db.taxDraftDao().insertLines(generated.lines.map { line -> TaxDraftLineEntity(UUID.randomUUID().toString(), draftId, line.sectionCode, line.categoryCode, line.amount.minorUnits.value, line.amount.currency, line.sourceIds.joinToString(prefix = "[\"", postfix = "\"]", separator = "\",\""), line.calculation) })
            db.taxIssueDao().insertAll(generated.issues.map { issue -> TaxIssueEntity(UUID.randomUUID().toString(), draftId, issue.code, issue.title, issue.explanation, issue.sourceId, "OPEN", now) })
        }
        return draft
    }

    suspend fun getDraftLines(draftId: String): List<TaxDraftLineEntity> = db.taxDraftDao().getLines(draftId)

    suspend fun calculateCurrentReconciliation(): WealthReconciliationResult {
        val allEvents = db.financialEventDao().getAll()
        val income = allEvents.filter { it.eventType == "INCOME" }.sumOf { it.amountMinor }
        val expense = allEvents.filter { it.eventType == "EXPENSE" }.sumOf { it.amountMinor }
        val assets = db.wealthDao().getAllAssets().sumOf { it.currentEstimatedValueMinor }
        val liabilities = db.wealthDao().getAllLiabilities().sumOf { it.outstandingAmountMinor }
        val result = reconcileWealth(WealthReconciliationInput(Money(MinorUnits(0)), Money(MinorUnits(income)), Money(MinorUnits(expense)), Money(MinorUnits(0)), Money(MinorUnits(0)), Money(MinorUnits(assets - liabilities))))
        val now = Instant.now().toEpochMilli()
        db.reconciliationDao().insert(WealthReconciliationEntity(UUID.randomUUID().toString(), "PK-${LocalDate.now().year}", 0, income, expense, 0, 0, result.expectedClosing.minorUnits.value, assets - liabilities, result.unexplainedDifference.minorUnits.value, result.calculation))
        return result
    }
}
