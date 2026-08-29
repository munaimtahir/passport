package pk.vexel.financepassport.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import pk.vexel.financepassport.core.database.AccountEntity
import pk.vexel.financepassport.core.database.FinanceRepository
import pk.vexel.financepassport.core.database.FinancialEventEntity
import pk.vexel.financepassport.core.database.UtilityBillProfileEntity
import pk.vexel.financepassport.core.database.MonthlyBillOccurrenceEntity
import pk.vexel.financepassport.core.database.PaymentRecordEntity
import pk.vexel.financepassport.core.database.BillAttachmentEntity
import pk.vexel.financepassport.core.database.UtilityRecurrenceEngine
import pk.vexel.financepassport.core.model.FinancialEventType
import pk.vexel.financepassport.core.security.AppPreferences
import java.time.LocalDate

class MainViewModel(private val repository: FinanceRepository, private val preferences: AppPreferences) : ViewModel() {
    var paymentRevision by mutableIntStateOf(0)
        private set
    private val billStateMutex = Mutex()

    // Collect Room's invalidation flows directly. A cached stateIn snapshot can retain the
    // initial empty list across the add-bill dialog transition and hide a persisted profile.
    private val utilityProfilesState = MutableStateFlow<List<UtilityBillProfileEntity>>(emptyList())
    val utilityProfiles: StateFlow<List<UtilityBillProfileEntity>> = utilityProfilesState
    private val monthlyOccurrencesState = MutableStateFlow<List<MonthlyBillOccurrenceEntity>>(emptyList())
    val monthlyOccurrences: StateFlow<List<MonthlyBillOccurrenceEntity>> = monthlyOccurrencesState

    init {
        viewModelScope.launch {
            billStateMutex.withLock {
                runCatching {
                    UtilityRecurrenceEngine.reconcileAll(repository.database, LocalDate.now())
                }
                refreshBillState()
            }
        }
    }

    private suspend fun refreshBillState() {
        utilityProfilesState.value = repository.database.utilityBillDao().getAll()
        monthlyOccurrencesState.value = repository.database.monthlyBillOccurrenceDao().getAll()
    }

    fun addUtilityProfile(context: android.content.Context, profile: UtilityBillProfileEntity, onSaved: () -> Unit = {}) = write {
        billStateMutex.withLock {
            repository.addUtilityProfile(profile)
            UtilityRecurrenceEngine.reconcileProfile(repository.database, profile, LocalDate.now())
            refreshBillState()
            onSaved()
            runCatching { repository.scheduleUtilityReminders(context) }
        }
    }

    fun updateUtilityProfile(context: android.content.Context, profile: UtilityBillProfileEntity, onSaved: () -> Unit = {}) = write {
        billStateMutex.withLock {
            repository.updateUtilityProfile(profile)
            UtilityRecurrenceEngine.reconcileProfile(repository.database, profile, LocalDate.now())
            refreshBillState()
            onSaved()
            runCatching { repository.scheduleUtilityReminders(context) }
        }
    }

    fun archiveUtilityProfile(context: android.content.Context, id: String) = write {
        repository.archiveUtilityProfile(id, System.currentTimeMillis())
        repository.database.monthlyBillOccurrenceDao().getByProfile(id).forEach { occ ->
            repository.cancelUtilityReminders(context, occ.id)
        }
    }

    fun reactivateUtilityProfile(context: android.content.Context, id: String, reactivateMonth: String) = write {
        val now = System.currentTimeMillis()
        repository.reactivateUtilityProfile(id, reactivateMonth, now)
        repository.database.utilityBillDao().getById(id)?.let { profile ->
            UtilityRecurrenceEngine.reconcileProfile(repository.database, profile, LocalDate.now())
        }
        repository.scheduleUtilityReminders(context)
    }

    fun deleteUtilityProfile(context: android.content.Context, id: String) = write {
        billStateMutex.withLock {
            val vault = pk.vexel.financepassport.core.files.UtilityAttachmentVault(context, repository)
            repository.utilityAttachmentsForProfile(id).forEach(vault::delete)
            repository.database.monthlyBillOccurrenceDao().getByProfile(id).forEach { occ ->
                repository.cancelUtilityReminders(context, occ.id)
            }
            repository.deleteUtilityProfile(id)
            refreshBillState()
        }
    }

    fun addMonthlyOccurrence(context: android.content.Context, occurrence: MonthlyBillOccurrenceEntity) = write {
        billStateMutex.withLock {
            repository.addMonthlyOccurrence(occurrence)
            refreshBillState()
            repository.scheduleUtilityReminders(context)
        }
    }

    fun updateMonthlyOccurrence(context: android.content.Context, occurrence: MonthlyBillOccurrenceEntity) = write {
        billStateMutex.withLock {
            repository.updateMonthlyOccurrence(occurrence)
            refreshBillState()
            if (occurrence.status == "Paid" || occurrence.status == "Skipped") {
                repository.cancelUtilityReminders(context, occurrence.id)
            } else {
                repository.scheduleUtilityReminders(context)
            }
        }
    }

    fun deleteMonthlyOccurrence(context: android.content.Context, id: String) = write {
        val vault = pk.vexel.financepassport.core.files.UtilityAttachmentVault(context, repository)
        repository.utilityAttachmentsForOccurrence(id).forEach(vault::delete)
        repository.cancelUtilityReminders(context, id)
        repository.deleteMonthlyOccurrence(id)
    }

    fun addPayment(context: android.content.Context, payment: PaymentRecordEntity) = write {
        repository.addPayment(payment)
        paymentRevision++
        repository.cancelUtilityReminders(context, payment.occurrenceId)
        repository.database.monthlyBillOccurrenceDao().getById(payment.occurrenceId)?.let { occ ->
            repository.database.utilityBillDao().getById(occ.profileId)?.let { profile ->
                UtilityRecurrenceEngine.reconcileProfile(repository.database, profile, LocalDate.now())
            }
        }
    }

    fun updatePayment(context: android.content.Context, id: String, occurrenceId: String, amountPaid: Long, paymentDate: Long, mode: String, accountId: String, bank: String?, reference: String?, notes: String?) = write {
        repository.updatePayment(id, amountPaid, paymentDate, mode, accountId, bank, reference, notes, System.currentTimeMillis())
        paymentRevision++
        repository.cancelUtilityReminders(context, occurrenceId)
        repository.database.monthlyBillOccurrenceDao().getById(occurrenceId)?.let { occ ->
            repository.database.utilityBillDao().getById(occ.profileId)?.let { profile ->
                UtilityRecurrenceEngine.reconcileProfile(repository.database, profile, LocalDate.now())
            }
        }
    }

    fun deletePayment(context: android.content.Context, id: String, occurrenceId: String) = write {
        val vault = pk.vexel.financepassport.core.files.UtilityAttachmentVault(context, repository)
        repository.utilityAttachmentsForPayment(id).forEach(vault::delete)
        repository.deletePayment(id)
        paymentRevision++
        repository.database.monthlyBillOccurrenceDao().getById(occurrenceId)?.let { occ ->
            repository.database.utilityBillDao().getById(occ.profileId)?.let { profile ->
                UtilityRecurrenceEngine.reconcileProfile(repository.database, profile, LocalDate.now())
            }
        }
        repository.scheduleUtilityReminders(context)
    }

    fun addAttachment(attachment: BillAttachmentEntity) = write {
        repository.addAttachment(attachment)
    }

    fun deleteAttachment(id: String) = write {
        repository.deleteAttachment(id)
    }

    fun observeAttachments(linkedId: String) = repository.observeAttachments(linkedId)

    var privacyModeEnabled by mutableStateOf(preferences.isPrivacyModeEnabled())
        private set
    fun togglePrivacyMode() {
        privacyModeEnabled = !privacyModeEnabled
        preferences.setPrivacyMode(privacyModeEnabled)
    }
    val accounts = repository.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activeAccounts = repository.activeAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val incomeSources = repository.incomeSources.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recentEvents = repository.recentEvents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val activeEventCount = repository.activeEventCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val totals = repository.totals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val taxItems = repository.taxItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val assets = repository.assets.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val liabilities = repository.liabilities.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val investments = repository.investments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val receivables = repository.receivables.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goals = repository.goals.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val goalProgress = repository.goalProgress.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val officialRecords = repository.officialRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val calendarItems = repository.calendarItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val documents = repository.documents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val drafts = repository.drafts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val taxIssues = repository.taxIssues.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reconciliations = repository.reconciliations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recurringItems = repository.recurringItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val taxYears = repository.taxYears.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    /** The tax year the Tax workspace is currently viewing/acting on (Phase 5A: a selected-year
     * workspace, not just whatever the device clock says "now" is). */
    var selectedTaxYear by mutableStateOf(LocalDate.now().year)
        private set
    fun selectTaxYear(year: Int) { selectedTaxYear = year }
    val selectedTaxYearId: String get() = "PK-$selectedTaxYear"
    val financialPosition = repository.financialPosition.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null as pk.vexel.financepassport.core.model.FinancialPosition?)
    var draftMessage by mutableStateOf<String?>(null)
    var reconciliationMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    fun clearError() { errorMessage = null }

    private fun write(action: suspend () -> Unit) = viewModelScope.launch {
        runCatching { action() }.onFailure { errorMessage = "Could not save this change. Check the fields and try again." }
    }

    suspend fun getPaymentForOccurrence(occurrenceId: String): PaymentRecordEntity? {
        return repository.database.paymentRecordDao().getForOccurrence(occurrenceId)
    }

    fun addAccount(name: String, type: String, openingBalanceMinor: Long, institution: String? = null, notes: String? = null, context: String? = null) = write { repository.addAccount(name, type, openingBalanceMinor, institution = institution, notes = notes, context = context) }
    fun accountMovement(accountId: String) = repository.accountMovement(accountId)
    fun updateAccount(id: String, name: String, type: String, openingBalanceMinor: Long, institution: String? = null, notes: String? = null, context: String? = null) = write { repository.updateAccount(id, name, openingBalanceMinor, institution, notes, type, context) }
    fun archiveAccount(id: String) = write { repository.archiveAccount(id) }
    fun reactivateAccount(id: String) = write { repository.reactivateAccount(id) }
    fun addEvent(type: FinancialEventType, amountMinor: Long, accountId: String, description: String, category: String? = null, date: LocalDate = LocalDate.now(), incomeSourceId: String? = null) = write { repository.addEvent(type, amountMinor, accountId, description, category, date = date, incomeSourceId = incomeSourceId) }
    fun addIncomeSource(name: String, sourceType: String, payerOrEmployer: String? = null) = write { repository.addIncomeSource(name, sourceType, payerOrEmployer) }
    fun addRecurringItem(context: android.content.Context, title: String, type: FinancialEventType, amountMinor: Long, accountId: String, category: String?, frequency: String, delayDays: Long) = write { repository.addRecurringItem(context, title, type, amountMinor, accountId, category, frequency, delayDays) }
    fun pauseRecurringItem(context: android.content.Context, id: String) = write { repository.pauseRecurringItem(context, id) }
    fun confirmRecurringItemNow(context: android.content.Context, id: String) = write { repository.confirmRecurringItemNow(context, id) }
    fun transfer(source: String, destination: String, amountMinor: Long, description: String, date: LocalDate = LocalDate.now()) = write { repository.transfer(source, destination, amountMinor, description, date) }
    fun addAsset(title: String, valueMinor: Long) = write { repository.addAsset(title, "OTHER", valueMinor) }
    fun updateAssetValuation(id: String, valueMinor: Long) = write { repository.updateAssetValuation(id, valueMinor) }
    fun disposeAsset(id: String, valueMinor: Long) = write { repository.disposeAsset(id, valueMinor) }
    fun addLiability(
        context: android.content.Context,
        title: String,
        type: String,
        valueMinor: Long,
        lender: String? = null,
        dueDate: LocalDate? = null,
        interestRateBps: Int? = null,
        installmentAmountMinor: Long? = null,
    ) = write { repository.addLiability(context, title, type, valueMinor, lender, dueDate, interestRateBps, installmentAmountMinor) }
    fun recordLiabilityPayment(id: String, amountMinor: Long) = write { repository.recordLiabilityPayment(id, amountMinor) }
    fun addInvestmentEvent(security: String, type: String, amountMinor: Long, quantityMinor: Long = 0, accountLabel: String = "Manual") = write { repository.addInvestmentEvent(security, type, amountMinor, quantityMinor, accountLabel = accountLabel) }
    fun addReceivable(context: android.content.Context, title: String, counterparty: String, amountMinor: Long, dueDate: LocalDate? = null) = write { repository.addReceivable(context, title, counterparty, amountMinor, dueDate) }
    fun scheduleTaxFilingDeadlineReminder(context: android.content.Context, deadline: LocalDate?) = write { repository.scheduleTaxFilingDeadlineReminder(context, selectedTaxYearId, deadline) }
    fun recordReceivablePayment(id: String, amountMinor: Long) = write { repository.recordReceivablePayment(id, amountMinor) }
    fun addGoal(title: String, targetMinor: Long, goalType: String = "CUSTOM", targetDate: LocalDate? = null) = write { repository.addGoal(title, targetMinor, goalType, targetDate?.toEpochDay()) }
    fun contributeToGoal(id: String, amountMinor: Long) = write { repository.contributeToGoal(id, amountMinor) }
    fun addOfficialRecord(context: android.content.Context, type: String, title: String, identifier: String?, issueDate: LocalDate? = null, expiryDate: LocalDate? = null) = write { repository.addOfficialRecord(context, type, title, identifier, issueDate, expiryDate) }
    fun addCalendarItem(context: android.content.Context, title: String, kind: String, delayMinutes: Long) = write { repository.addCalendarItem(context, title, kind, delayMinutes) }
    fun updateCalendarStatus(context: android.content.Context, id: String, status: String) = write { repository.updateCalendarStatus(context, id, status) }
    fun rescheduleCalendarItem(context: android.content.Context, id: String, delayMinutes: Long) = write { repository.rescheduleCalendarItem(context, id, delayMinutes) }
    fun deleteAllData(context: android.content.Context, onComplete: () -> Unit = {}) = viewModelScope.launch {
        runCatching { repository.deleteAllData(context) }
            .onSuccess { onComplete() }
            .onFailure { errorMessage = "Could not save this change. Check the fields and try again." }
    }
    fun prepareAnnualDraft() = viewModelScope.launch { draftMessage = runCatching { "Draft v${repository.prepareAnnualDraft(selectedTaxYear).draftVersion} prepared for $selectedTaxYearId" }.getOrElse { "Draft failed: ${it.message}" } }
    suspend fun getDraftLines(draftId: String) = repository.getDraftLines(draftId)
    suspend fun getMappingHistory(taxItemId: String) = repository.getMappingHistory(taxItemId)
    fun recordWealthSnapshot(kind: String) = viewModelScope.launch { reconciliationMessage = runCatching { repository.recordWealthSnapshot(selectedTaxYear, kind); "${kind.lowercase().replaceFirstChar { it.uppercase() }} snapshot recorded for $selectedTaxYearId" }.getOrElse { "Snapshot failed: ${it.message}" } }
    fun calculateReconciliation() = viewModelScope.launch { reconciliationMessage = runCatching { "Unexplained difference: PKR ${repository.calculateReconciliation(selectedTaxYearId).unexplainedDifference.minorUnits.value / 100}" }.getOrElse { "Reconciliation failed: ${it.message}" } }
    fun updateTaxYearStatus(newStatus: String) = write { repository.updateTaxYearStatus(selectedTaxYearId, newStatus) }
    fun updateTaxReview(id: String, state: String, reason: String? = null) = write { repository.updateTaxReview(id, state, reason) }
    fun reviewTaxItem(id: String, taxEventType: String, state: String, reason: String?) = write { repository.reviewTaxItem(id, taxEventType, state, reason) }
    fun updateTaxEvidenceState(id: String, evidenceState: String) = write { repository.updateTaxEvidenceState(id, evidenceState) }
    fun addManualTaxItem(type: String, amountMinor: Long, description: String, date: LocalDate = LocalDate.now()) = write { repository.addManualTaxItem(type, amountMinor, description, date) }
    fun linkDocument(documentId: String, entityType: String, entityId: String) = write { repository.linkDocument(documentId, entityType, entityId) }
    fun deleteDocument(documentId: String) = write { repository.deleteDocument(documentId) }
    suspend fun documentDependencyCount(documentId: String) = repository.documentDependencyCount(documentId)
    fun scheduleDocumentExpiry(context: android.content.Context, documentId: String, title: String, expiryDateEpochDay: Long) = write { repository.scheduleDocumentExpiryReminder(context, documentId, title, expiryDateEpochDay) }
    fun createBackup(context: android.content.Context, password: CharArray, onComplete: (Result<java.io.File>) -> Unit) = viewModelScope.launch { onComplete(runCatching { repository.createEncryptedBackupFile(context, password) }) }

    suspend fun importAttachment(context: android.content.Context, uri: android.net.Uri, linkedId: String, type: String): BillAttachmentEntity {
        val vault = pk.vexel.financepassport.core.files.UtilityAttachmentVault(context, repository)
        return vault.import(uri, linkedId, type)
    }

    fun decryptAttachment(context: android.content.Context, attachment: BillAttachmentEntity): ByteArray {
        val vault = pk.vexel.financepassport.core.files.UtilityAttachmentVault(context, repository)
        return vault.decrypt(attachment)
    }

    fun deleteAttachmentFile(context: android.content.Context, attachment: BillAttachmentEntity) = write {
        val vault = pk.vexel.financepassport.core.files.UtilityAttachmentVault(context, repository)
        vault.delete(attachment)
        repository.deleteAttachment(attachment.id)
    }
}

class MainViewModelFactory(private val repository: FinanceRepository, private val preferences: AppPreferences) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repository, preferences) as T
}
