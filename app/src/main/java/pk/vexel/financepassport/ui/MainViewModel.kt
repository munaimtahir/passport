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

class MainViewModel(private val repository: FinanceRepository) : ViewModel() {
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
    val officialRecords = repository.officialRecords.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val calendarItems = repository.calendarItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val documents = repository.documents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val drafts = repository.drafts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val taxIssues = repository.taxIssues.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reconciliations = repository.reconciliations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recurringItems = repository.recurringItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    var draftMessage by mutableStateOf<String?>(null)
    var reconciliationMessage by mutableStateOf<String?>(null)

    fun addAccount(name: String, type: String, openingBalanceMinor: Long) = viewModelScope.launch { repository.addAccount(name, type, openingBalanceMinor) }
    fun accountMovement(accountId: String) = repository.accountMovement(accountId)
    fun updateAccount(id: String, name: String, openingBalanceMinor: Long) = viewModelScope.launch { repository.updateAccount(id, name, openingBalanceMinor) }
    fun archiveAccount(id: String) = viewModelScope.launch { repository.archiveAccount(id) }
    fun addEvent(type: FinancialEventType, amountMinor: Long, accountId: String, description: String, category: String? = null) = viewModelScope.launch { repository.addEvent(type, amountMinor, accountId, description, category) }
    fun addRecurringItem(context: android.content.Context, title: String, type: FinancialEventType, amountMinor: Long, accountId: String, category: String?, frequency: String, delayDays: Long) = viewModelScope.launch { repository.addRecurringItem(context, title, type, amountMinor, accountId, category, frequency, delayDays) }
    fun pauseRecurringItem(context: android.content.Context, id: String) = viewModelScope.launch { repository.pauseRecurringItem(context, id) }
    fun transfer(source: String, destination: String, amountMinor: Long, description: String) = viewModelScope.launch { repository.transfer(source, destination, amountMinor, description) }
    fun addAsset(title: String, valueMinor: Long) = viewModelScope.launch { repository.addAsset(title, "OTHER", valueMinor) }
    fun updateAssetValuation(id: String, valueMinor: Long) = viewModelScope.launch { repository.updateAssetValuation(id, valueMinor) }
    fun disposeAsset(id: String, valueMinor: Long) = viewModelScope.launch { repository.disposeAsset(id, valueMinor) }
    fun addLiability(title: String, valueMinor: Long) = viewModelScope.launch { repository.addLiability(title, "OTHER", valueMinor) }
    fun recordLiabilityPayment(id: String, amountMinor: Long) = viewModelScope.launch { repository.recordLiabilityPayment(id, amountMinor) }
    fun addInvestmentEvent(security: String, type: String, amountMinor: Long, quantityMinor: Long = 0) = viewModelScope.launch { repository.addInvestmentEvent(security, type, amountMinor, quantityMinor) }
    fun addReceivable(title: String, counterparty: String, amountMinor: Long) = viewModelScope.launch { repository.addReceivable(title, counterparty, amountMinor) }
    fun recordReceivablePayment(id: String, amountMinor: Long) = viewModelScope.launch { repository.recordReceivablePayment(id, amountMinor) }
    fun addGoal(title: String, targetMinor: Long) = viewModelScope.launch { repository.addGoal(title, targetMinor) }
    fun addOfficialRecord(type: String, title: String, identifier: String?) = viewModelScope.launch { repository.addOfficialRecord(type, title, identifier) }
    fun addCalendarItem(context: android.content.Context, title: String, kind: String, delayMinutes: Long) = viewModelScope.launch { repository.addCalendarItem(context, title, kind, delayMinutes) }
    fun updateCalendarStatus(context: android.content.Context, id: String, status: String) = viewModelScope.launch { repository.updateCalendarStatus(context, id, status) }
    fun rescheduleCalendarItem(context: android.content.Context, id: String, delayMinutes: Long) = viewModelScope.launch { repository.rescheduleCalendarItem(context, id, delayMinutes) }
    fun deleteAllData() = viewModelScope.launch { repository.deleteAllData() }
    fun prepareAnnualDraft() = viewModelScope.launch { draftMessage = runCatching { "Draft v${repository.prepareAnnualDraft().draftVersion} prepared" }.getOrElse { "Draft failed: ${it.message}" } }
    suspend fun getDraftLines(draftId: String) = repository.getDraftLines(draftId)
    fun calculateReconciliation() = viewModelScope.launch { reconciliationMessage = runCatching { "Unexplained difference: PKR ${repository.calculateCurrentReconciliation().unexplainedDifference.minorUnits.value / 100}" }.getOrElse { "Reconciliation failed: ${it.message}" } }
    fun updateTaxReview(id: String, state: String, reason: String? = null) = viewModelScope.launch { repository.updateTaxReview(id, state, reason) }
    fun reviewTaxItem(id: String, taxEventType: String, state: String, reason: String?) = viewModelScope.launch { repository.reviewTaxItem(id, taxEventType, state, reason) }
    fun updateTaxEvidenceState(id: String, evidenceState: String) = viewModelScope.launch { repository.updateTaxEvidenceState(id, evidenceState) }
    fun addManualTaxItem(type: String, amountMinor: Long, description: String) = viewModelScope.launch { repository.addManualTaxItem(type, amountMinor, description) }
    fun linkDocument(documentId: String, entityType: String, entityId: String) = viewModelScope.launch { repository.linkDocument(documentId, entityType, entityId) }
    fun deleteDocument(documentId: String) = viewModelScope.launch { repository.deleteDocument(documentId) }
    fun createBackup(context: android.content.Context, password: CharArray, onComplete: (Result<ByteArray>) -> Unit) = viewModelScope.launch { onComplete(runCatching { repository.createEncryptedBackup(context, password) }) }
}

class MainViewModelFactory(private val repository: FinanceRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repository) as T
}
