package pk.vexel.financepassport.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import pk.vexel.financepassport.core.database.AccountEntity
import pk.vexel.financepassport.core.database.FinanceRepository
import pk.vexel.financepassport.core.database.FinancialEventEntity
import pk.vexel.financepassport.core.model.FinancialEventType
import pk.vexel.financepassport.core.security.AppPreferences
import java.time.LocalDate

class MainViewModel(private val repository: FinanceRepository, private val preferences: AppPreferences) : ViewModel() {
    var privacyModeEnabled by mutableStateOf(preferences.isPrivacyModeEnabled())
        private set
    fun togglePrivacyMode() {
        privacyModeEnabled = !privacyModeEnabled
        preferences.setPrivacyMode(privacyModeEnabled)
    }
    val accounts = repository.accounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
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

    fun addAccount(name: String, type: String, openingBalanceMinor: Long, institution: String? = null, notes: String? = null) = write { repository.addAccount(name, type, openingBalanceMinor, institution = institution, notes = notes) }
    fun accountMovement(accountId: String) = repository.accountMovement(accountId)
    fun updateAccount(id: String, name: String, openingBalanceMinor: Long, institution: String? = null, notes: String? = null) = write { repository.updateAccount(id, name, openingBalanceMinor, institution, notes) }
    fun archiveAccount(id: String) = write { repository.archiveAccount(id) }
    fun addEvent(type: FinancialEventType, amountMinor: Long, accountId: String, description: String, category: String? = null, date: LocalDate = LocalDate.now()) = write { repository.addEvent(type, amountMinor, accountId, description, category, date = date) }
    fun addRecurringItem(context: android.content.Context, title: String, type: FinancialEventType, amountMinor: Long, accountId: String, category: String?, frequency: String, delayDays: Long) = write { repository.addRecurringItem(context, title, type, amountMinor, accountId, category, frequency, delayDays) }
    fun pauseRecurringItem(context: android.content.Context, id: String) = write { repository.pauseRecurringItem(context, id) }
    fun transfer(source: String, destination: String, amountMinor: Long, description: String, date: LocalDate = LocalDate.now()) = write { repository.transfer(source, destination, amountMinor, description, date) }
    fun addAsset(title: String, valueMinor: Long) = write { repository.addAsset(title, "OTHER", valueMinor) }
    fun updateAssetValuation(id: String, valueMinor: Long) = write { repository.updateAssetValuation(id, valueMinor) }
    fun disposeAsset(id: String, valueMinor: Long) = write { repository.disposeAsset(id, valueMinor) }
    fun addLiability(title: String, valueMinor: Long) = write { repository.addLiability(title, "OTHER", valueMinor) }
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
}

class MainViewModelFactory(private val repository: FinanceRepository, private val preferences: AppPreferences) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repository, preferences) as T
}
