package pk.vexel.financepassport.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pk.vexel.financepassport.PassportApplication
import pk.vexel.financepassport.core.database.AccountEntity
import pk.vexel.financepassport.core.model.FinancialEventType
import pk.vexel.financepassport.core.files.DocumentVault
import pk.vexel.financepassport.core.security.LiveRestoreService
import pk.vexel.financepassport.ui.theme.PassportTheme

private data class Destination(val label: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("Home", Icons.Default.Home), Destination("Money", Icons.Default.AccountBalanceWallet),
    Destination("Wealth", Icons.AutoMirrored.Filled.TrendingUp), Destination("Tax & Records", Icons.Default.Description),
    Destination("Vault", Icons.Default.Folder),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportApp() {
    val application = LocalContext.current.applicationContext as PassportApplication
    val vm: MainViewModel = viewModel(factory = MainViewModelFactory(application.repository))
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Vexel Finance Passport") }, actions = { IconButton(onClick = { showMore = true }) { Icon(Icons.Default.MoreHoriz, "More") } }) },
        floatingActionButton = { if (selected == 1) FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Add") } },
        bottomBar = { NavigationBar { destinations.forEachIndexed { index, destination -> NavigationBarItem(selected == index, { selected = index }, icon = { Icon(destination.icon, destination.label) }, label = { Text(destination.label) }) } } },
    ) { padding ->
        when (selected) {
            0 -> HomeScreen(vm, application, padding)
            1 -> MoneyScreen(vm, application, padding)
            2 -> WealthScreen(vm, padding)
            3 -> TaxScreen(vm, padding)
            4 -> VaultScreen(vm, application, padding)
            else -> EmptyModuleScreen(destinations[selected].label, padding)
        }
    }
    if (showAdd) AddAccountDialog(vm) { showAdd = false }
    if (showMore) MoreDialog(vm, application) { showMore = false }
}

@Composable
private fun MoreDialog(vm: MainViewModel, application: PassportApplication, onDismiss: () -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf(false) }
    var restorePayload by remember { mutableStateOf<ByteArray?>(null) }
    var pendingBackup by remember { mutableStateOf<ByteArray?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var requestedReport by remember { mutableStateOf("NET_WORTH") }
    var currentYearOnly by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    suspend fun reportSnapshot() = application.repository.exportSnapshot().let { snapshot ->
        if (!currentYearOnly) snapshot else {
            val today = java.time.LocalDate.now()
            snapshot.forDateRange(today.withDayOfYear(1).toEpochDay(), today.withDayOfYear(today.lengthOfYear()).toEpochDay())
        }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if (uri != null) scope.launch { application.contentResolver.openOutputStream(uri)?.use { it.write(pk.vexel.financepassport.core.export.DataExportService().json(application.repository.exportSnapshot()).toByteArray()) } } }
    val pdfExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> if (uri != null) scope.launch { application.contentResolver.openOutputStream(uri)?.use { val generator = pk.vexel.financepassport.core.reports.ReportGenerator(); generator.writePdf(generator.netWorth(reportSnapshot(), java.time.Instant.now().toString()), it) } } }
    val annualPdfExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> if (uri != null) scope.launch { application.contentResolver.openOutputStream(uri)?.use { val generator = pk.vexel.financepassport.core.reports.ReportGenerator(); generator.writePdf(generator.annualFinancialSummary(reportSnapshot(), java.time.Instant.now().toString()), it) } } }
    val catalogPdfExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri -> if (uri != null) scope.launch { application.contentResolver.openOutputStream(uri)?.use { val generator = pk.vexel.financepassport.core.reports.ReportGenerator(); val snapshot = reportSnapshot(); val stamp = java.time.Instant.now().toString(); val report = when (requestedReport) { "ASSETS" -> generator.assetStatement(snapshot, stamp); "LIABILITIES" -> generator.liabilityStatement(snapshot, stamp); "CASH_FLOW" -> generator.cashFlowSummary(snapshot, stamp); "INVESTMENTS" -> generator.investmentSummary(snapshot, stamp); "RECEIVABLES" -> generator.receivablesReport(snapshot, stamp); "TAX" -> generator.taxPreparationSummary(snapshot, stamp); "EVIDENCE" -> generator.evidenceChecklist(snapshot, stamp); else -> generator.netWorth(snapshot, stamp) }; generator.writePdf(report, it) } } }
    val csvExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> if (uri != null) scope.launch { application.contentResolver.openOutputStream(uri)?.use { it.write(pk.vexel.financepassport.core.export.DataExportService().csvEvents(application.repository.exportSnapshot()).toByteArray()) } } }
    val backupSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> val bytes = pendingBackup; if (uri != null && bytes != null) scope.launch { application.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }; status = "Encrypted backup exported" }; pendingBackup = null }
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch { restorePayload = application.contentResolver.openInputStream(uri)?.use { it.readBytes() } } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("More") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Reports and local data controls"); status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }; OutlinedButton(onClick = { currentYearOnly = !currentYearOnly }, modifier = Modifier.fillMaxWidth()) { Text(if (currentYearOnly) "Report range: current tax year" else "Report range: all recorded dates") }; Button(onClick = { exporter.launch("vexel-finance-passport-export.json") }, modifier = Modifier.fillMaxWidth()) { Text("Export structured JSON") }; Button(onClick = { pdfExporter.launch("vexel-net-worth.pdf") }, modifier = Modifier.fillMaxWidth()) { Text("Export net-worth PDF") }; Button(onClick = { annualPdfExporter.launch("vexel-annual-financial-summary.pdf") }, modifier = Modifier.fillMaxWidth()) { Text("Export annual summary PDF") }; listOf("ASSETS" to "Asset statement PDF", "LIABILITIES" to "Liability statement PDF", "CASH_FLOW" to "Cash-flow summary PDF", "INVESTMENTS" to "Investment summary PDF", "RECEIVABLES" to "Receivables report PDF", "TAX" to "Tax preparation summary PDF", "EVIDENCE" to "Evidence checklist PDF").forEach { (kind, label) -> Button(onClick = { requestedReport = kind; catalogPdfExporter.launch("vexel-${kind.lowercase()}-report.pdf") }, modifier = Modifier.fillMaxWidth()) { Text(label) } }; Button(onClick = { csvExporter.launch("vexel-financial-events.csv") }, modifier = Modifier.fillMaxWidth()) { Text("Export financial events CSV") }; Button(onClick = { backupPassword = true }, modifier = Modifier.fillMaxWidth()) { Text("Create encrypted backup") }; Button(onClick = { backupPicker.launch(arrayOf("application/octet-stream", "application/zip", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) { Text("Restore encrypted backup") }; Button(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete all application data") } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete everything?") }, text = { Text("This removes local records and returns the app to an empty data state. It cannot be undone.") }, confirmButton = { Button(onClick = { vm.deleteAllData(); confirmDelete = false; onDismiss() }) { Text("Delete all") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
    if (backupPassword) BackupPasswordDialog("Create encrypted backup", onDismiss = { backupPassword = false }) { password ->
        backupPassword = false
        vm.createBackup(application, password.toCharArray()) { result -> result.onSuccess { pendingBackup = it; backupSaver.launch("vexel-finance-passport.backup") }.onFailure { status = "Backup failed: ${it.message}" } }
    }
    restorePayload?.let { payload -> BackupPasswordDialog("Restore encrypted backup", onDismiss = { restorePayload = null }) { password ->
        scope.launch { runCatching { LiveRestoreService(application).restore(payload, password.toCharArray()) }.onSuccess { status = "Restore complete. Close and relaunch the app to reopen restored records." }.onFailure { status = "Restore failed: ${it.message}" }; restorePayload = null }
    } }
}

@Composable
private fun BackupPasswordDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(password, { password = it }, label = { Text("Backup password (8+ characters)") }, singleLine = true) }, confirmButton = { Button(onClick = { onConfirm(password) }, enabled = password.length >= 8) { Text("Continue") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun HomeScreen(vm: MainViewModel, application: PassportApplication, padding: PaddingValues) {
    val totals by vm.totals.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val activeEventCount by vm.activeEventCount.collectAsState()
    val recentEvents by vm.recentEvents.collectAsState()
    val calendarItems by vm.calendarItems.collectAsState()
    var showCalendarAdd by rememberSaveable { mutableStateOf(false) }
    var rescheduleTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.CalendarItemEntity?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Your financial passport", style = MaterialTheme.typography.headlineSmall) }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), Arrangement.spacedBy(6.dp)) { Text("Net recorded movement", style = MaterialTheme.typography.labelLarge); Text(formatPkr((totals?.first?.minorUnits?.value ?: 0L) - (totals?.second?.minorUnits?.value ?: 0L)), style = MaterialTheme.typography.displaySmall); Text("Based on ${accounts.size} active account(s) and $activeEventCount recorded event(s).") } } }
        item { Text("Tax-year readiness", style = MaterialTheme.typography.titleLarge) }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), Arrangement.spacedBy(6.dp)) { Text("Capture once, keep evidence linked."); Text("Your local records remain available without internet.") } } }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Upcoming obligations", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showCalendarAdd = true }) { Text("Add") } } }
        if (calendarItems.isEmpty()) item { Text("No reminders scheduled.") }
        items(calendarItems.take(5), key = { it.id }) { reminder -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) { Text(reminder.title, style = MaterialTheme.typography.titleMedium); Text(reminder.kind); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { rescheduleTarget = reminder }) { Text("Reschedule") }; TextButton(onClick = { vm.updateCalendarStatus(application, reminder.id, "COMPLETED") }) { Text("Complete") }; TextButton(onClick = { vm.updateCalendarStatus(application, reminder.id, "CANCELLED") }) { Text("Cancel") } } } } }
        item { Text("Recent activity", style = MaterialTheme.typography.titleLarge) }
        items(recentEvents.take(5), key = { it.id }) { event -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(event.description); Text(formatPkr(event.amountMinor)) } }
    }
    if (showCalendarAdd) CalendarItemDialog(vm, application) { showCalendarAdd = false }
    rescheduleTarget?.let { reminder -> RescheduleDialog(reminder, vm, application) { rescheduleTarget = null } }
}

@Composable
private fun CalendarItemDialog(vm: MainViewModel, application: PassportApplication, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("REVIEW") }
    var delay by remember { mutableStateOf("60") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add reminder") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true); OutlinedTextField(kind, { kind = it }, label = { Text("Kind") }, singleLine = true); OutlinedTextField(delay, { delay = it.filter(Char::isDigit) }, label = { Text("Remind in minutes") }, singleLine = true) } }, confirmButton = { Button(onClick = { vm.addCalendarItem(application, title, kind, delay.toLong()); onDismiss() }, enabled = title.isNotBlank() && delay.toLongOrNull()?.let { it > 0 } == true) { Text("Schedule") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun RescheduleDialog(reminder: pk.vexel.financepassport.core.database.CalendarItemEntity, vm: MainViewModel, application: PassportApplication, onDismiss: () -> Unit) {
    var delay by remember { mutableStateOf("60") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Reschedule ${reminder.title}") }, text = { OutlinedTextField(delay, { delay = it.filter(Char::isDigit) }, label = { Text("Remind in minutes") }, singleLine = true) }, confirmButton = { Button(onClick = { vm.rescheduleCalendarItem(application, reminder.id, delay.toLong()); onDismiss() }, enabled = delay.toLongOrNull()?.let { it > 0 } == true) { Text("Reschedule") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun MoneyScreen(vm: MainViewModel, application: PassportApplication, padding: PaddingValues) {
    val accounts by vm.accounts.collectAsState()
    val recentEvents by vm.recentEvents.collectAsState()
    val recurringItems by vm.recurringItems.collectAsState()
    var showEvent by rememberSaveable { mutableStateOf(false) }
    var showTransfer by rememberSaveable { mutableStateOf(false) }
    var showRecurring by rememberSaveable { mutableStateOf(false) }
    var editAccount by remember { mutableStateOf<AccountEntity?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Money", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Accounts", style = MaterialTheme.typography.titleLarge) }
        if (accounts.isEmpty()) item { Text("No accounts yet. Use + to add cash or a bank account.") }
        items(accounts, key = { it.id }) { account -> AccountCard(account, vm, onEdit = { editAccount = account }, onArchive = { vm.archiveAccount(account.id) }) }
        item { Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { Button(onClick = { showEvent = true }, enabled = accounts.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Income / expense") }; Button(onClick = { showTransfer = true }, enabled = accounts.size >= 2, modifier = Modifier.weight(1f)) { Text("Transfer") } } }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Recurring drafts", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showRecurring = true }, enabled = accounts.isNotEmpty()) { Text("Add") } } }
        if (recurringItems.isEmpty()) item { Text("No recurring drafts. Add one to receive a reminder without silently creating a confirmed event.") }
        items(recurringItems, key = { it.id }) { recurring -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text(recurring.title, style = MaterialTheme.typography.titleMedium); Text("${recurring.eventType} · ${formatPkr(recurring.amountMinor)} · ${recurring.frequency}"); Text("Next draft reminder: ${java.time.LocalDate.ofEpochDay(recurring.nextDueDateEpochDay)}"); TextButton(onClick = { vm.pauseRecurringItem(application, recurring.id) }) { Text("Pause") } } } }
        item { Text("Activity", style = MaterialTheme.typography.titleLarge) }
        items(recentEvents, key = { it.id }) { event -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), Arrangement.SpaceBetween) { Column { Text(event.description); Text(listOfNotNull(event.eventType, event.category).joinToString(" · "), style = MaterialTheme.typography.labelSmall) }; Text(formatPkr(event.amountMinor)) } } }
    }
    if (showEvent) AddEventDialog(vm, accounts) { showEvent = false }
    if (showTransfer) TransferDialog(vm, accounts) { showTransfer = false }
    if (showRecurring) RecurringItemDialog(vm, application, accounts) { showRecurring = false }
    editAccount?.let { account -> EditAccountDialog(account, vm, onDismiss = { editAccount = null }) }
}

@Composable
private fun TaxScreen(vm: MainViewModel, padding: PaddingValues) {
    val items by vm.taxItems.collectAsState()
    val officialRecords by vm.officialRecords.collectAsState()
    val drafts by vm.drafts.collectAsState()
    val issues by vm.taxIssues.collectAsState()
    val reconciliations by vm.reconciliations.collectAsState()
    var showOfficialRecord by rememberSaveable { mutableStateOf(false) }
    var showManualTaxItem by rememberSaveable { mutableStateOf(false) }
    var reviewTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.TaxItemEntity?>(null) }
    var draftLines by remember { mutableStateOf<List<pk.vexel.financepassport.core.database.TaxDraftLineEntity>?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val evidencePending = items.count { it.evidenceState == "NONE" || it.evidenceState == "REQUESTED" }
    val unmapped = items.count { it.reviewState == "NEEDS_CLASSIFICATION" || it.taxEventType == "OTHER_TAX_EVENT" }
    val duplicateCandidates = items.groupBy { Triple(it.dateEpochDay, it.grossAmountMinor, it.currency) }.values.filter { group -> group.size > 1 }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Tax & Records", style = MaterialTheme.typography.headlineMedium) }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Official records", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showOfficialRecord = true }) { Text("Add") } } }
        if (officialRecords.isEmpty()) item { Text("No official records yet. Sensitive identifiers are encrypted and masked.") }
        items(officialRecords, key = { it.id }) { record -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text(record.title, style = MaterialTheme.typography.titleMedium); Text("${record.recordType} · ${record.maskedIdentifier ?: "No identifier"}") } } }
        item { Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { Button(onClick = { showManualTaxItem = true }, modifier = Modifier.weight(1f)) { Text("Add tax item") }; Button(onClick = { vm.prepareAnnualDraft() }, modifier = Modifier.weight(1f)) { Text("Prepare draft") } } }
        if (vm.draftMessage != null) item { Text(vm.draftMessage!!, color = MaterialTheme.colorScheme.primary) }
        item { Button(onClick = { vm.calculateReconciliation() }, modifier = Modifier.fillMaxWidth()) { Text("Reconcile recorded wealth") } }
        if (vm.reconciliationMessage != null) item { Text(vm.reconciliationMessage!!, color = MaterialTheme.colorScheme.primary) }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text("Annual review readiness", style = MaterialTheme.typography.titleMedium); Text("${items.size - evidencePending}/${items.size} tax item(s) have evidence status resolved"); Text("$unmapped item(s) need classification review · ${duplicateCandidates.size} duplicate candidate group(s)"); Text("These are workflow signals, not a statement of tax correctness.", style = MaterialTheme.typography.bodySmall) } } }
        if (duplicateCandidates.isNotEmpty()) item { Text("Duplicate candidates", style = MaterialTheme.typography.titleLarge) }
        items(duplicateCandidates, key = { group -> group.joinToString("|") { it.id } }) { group -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text("${group.size} items share date, amount and currency", style = MaterialTheme.typography.titleSmall); group.forEach { candidate -> Text("${candidate.description} · ${candidate.sourceType}/${candidate.sourceId}", style = MaterialTheme.typography.bodySmall) }; Text("Review only; no records are merged automatically.", style = MaterialTheme.typography.bodySmall) } } }
        item { Text("Annual draft history", style = MaterialTheme.typography.titleLarge) }
        if (drafts.isEmpty()) item { Text("No annual drafts generated yet.") }
        items(drafts.take(5), key = { it.id }) { draft -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text("${draft.taxYearId} · draft v${draft.draftVersion}", style = MaterialTheme.typography.titleMedium); Text("Ruleset ${draft.rulesetVersion} · ${draft.status} · ${draft.issueCount} issue(s)"); TextButton(onClick = { scope.launch { draftLines = vm.getDraftLines(draft.id) } }) { Text("View calculation lines") } } } }
        item { Text("Review issues", style = MaterialTheme.typography.titleLarge) }
        if (issues.isEmpty()) item { Text("No generated tax issues.") }
        items(issues.take(10), key = { it.id }) { issue -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text(issue.title, style = MaterialTheme.typography.titleMedium); Text("${issue.code} · ${issue.status}"); Text(issue.explanation); issue.sourceId?.let { Text("Source: $it", style = MaterialTheme.typography.labelSmall) } } } }
        item { Text("Reconciliation history", style = MaterialTheme.typography.titleLarge) }
        if (reconciliations.isEmpty()) item { Text("No reconciliation snapshots generated yet.") }
        items(reconciliations.take(5), key = { it.id }) { reconciliation -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text(reconciliation.taxYearId, style = MaterialTheme.typography.titleMedium); Text("Expected ${formatPkr(reconciliation.expectedClosingMinor)} · recorded ${formatPkr(reconciliation.recordedClosingMinor)}"); Text("Unexplained ${formatPkr(reconciliation.unexplainedDifferenceMinor)}"); Text(reconciliation.calculation, style = MaterialTheme.typography.bodySmall) } } }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) { Text("Tax Inbox", style = MaterialTheme.typography.titleLarge); Text("${items.size} captured item(s). Source facts remain separate from their interpretation.") } } }
        if (items.isEmpty()) item { Text("Income captured in Money will appear here once, with its source link and evidence status.") }
        items(items, key = { it.id }) { item ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text(item.description, style = MaterialTheme.typography.titleMedium); Text("${item.taxEventType} · ${item.reviewState}"); Text("Evidence: ${item.evidenceState}"); Text("Source: ${item.sourceType}/${item.sourceId}", style = MaterialTheme.typography.labelSmall); Text(formatPkr(item.grossAmountMinor ?: 0)); if (item.reviewState != "EXCLUDED") TextButton(onClick = { reviewTarget = item }) { Text("Review classification") } } }
        }
    }
    if (showOfficialRecord) OfficialRecordDialog(vm) { showOfficialRecord = false }
    if (showManualTaxItem) ManualTaxItemDialog(vm) { showManualTaxItem = false }
    reviewTarget?.let { item -> TaxReviewDialog(item, vm) { reviewTarget = null } }
    draftLines?.let { lines ->
        AlertDialog(onDismissRequest = { draftLines = null }, title = { Text("Draft calculation lines") }, text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { if (lines.isEmpty()) item { Text("No generated lines.") }; items(lines, key = { it.id }) { line -> Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { Text("${line.sectionCode} / ${line.categoryCode}", style = MaterialTheme.typography.titleSmall); Text("Amount ${formatPkr(line.amountMinor)} · sources ${line.sourceIdsJson}"); Text(line.calculation, style = MaterialTheme.typography.bodySmall) } } } }, confirmButton = { TextButton(onClick = { draftLines = null }) { Text("Close") } })
    }
}

@Composable
private fun TaxReviewDialog(item: pk.vexel.financepassport.core.database.TaxItemEntity, vm: MainViewModel, onDismiss: () -> Unit) {
    var type by remember(item.id) { mutableStateOf(item.taxEventType) }
    var reason by remember(item.id) { mutableStateOf(item.exclusionReason.orEmpty()) }
    var excluded by remember(item.id) { mutableStateOf(false) }
    var evidenceState by remember(item.id) { mutableStateOf(item.evidenceState) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review tax item") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Source: ${item.sourceType}/${item.sourceId}", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(type, { type = it.uppercase() }, label = { Text("Tax event type") }, singleLine = true)
            OutlinedButton(onClick = { excluded = !excluded }) { Text(if (excluded) "Mark for inclusion" else "Exclude this item") }
            if (excluded) OutlinedTextField(reason, { reason = it }, label = { Text("Reason for exclusion") }, singleLine = false)
            OutlinedButton(onClick = { evidenceState = listOf("NONE", "REQUESTED", "NOT_AVAILABLE", "NOT_REQUIRED", "VERIFIED_BY_USER").let { states -> states[(states.indexOf(evidenceState).coerceAtLeast(0) + 1) % states.size] } }) { Text("Evidence status: $evidenceState") }
            Text("The original financial fact remains unchanged.", style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { Button(onClick = { vm.reviewTaxItem(item.id, type, if (excluded) "EXCLUDED" else "REVIEWED", reason.takeIf { excluded }); vm.updateTaxEvidenceState(item.id, evidenceState); onDismiss() }, enabled = type.isNotBlank() && (!excluded || reason.isNotBlank())) { Text("Save review") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun OfficialRecordDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf("CNIC/NICOP") }
    var title by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add official record") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(type, { type = it }, label = { Text("Record type") }, singleLine = true); OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true); OutlinedTextField(identifier, { identifier = it }, label = { Text("Sensitive identifier (optional)") }, singleLine = true) } }, confirmButton = { Button(onClick = { vm.addOfficialRecord(type, title, identifier); onDismiss() }, enabled = title.isNotBlank()) { Text("Save encrypted") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun ManualTaxItemDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    var type by remember { mutableStateOf("OTHER_INCOME") }; var description by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add tax item") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(type, { type = it.uppercase() }, label = { Text("Tax event type") }, singleLine = true); OutlinedTextField(description, { description = it }, label = { Text("What happened?") }, singleLine = true); OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Amount (PKR)") }, singleLine = true); Text("This stays a source fact until reviewed; evidence can be attached later.", style = MaterialTheme.typography.bodySmall) } }, confirmButton = { Button(onClick = { vm.addManualTaxItem(type, amount.toLong() * 100, description); onDismiss() }, enabled = description.isNotBlank() && amount.toLongOrNull()?.let { it > 0 } == true && type.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun WealthScreen(vm: MainViewModel, padding: PaddingValues) {
    val assets by vm.assets.collectAsState()
    val liabilities by vm.liabilities.collectAsState()
    val investments by vm.investments.collectAsState()
    val receivables by vm.receivables.collectAsState()
    val goals by vm.goals.collectAsState()
    var showAdd by rememberSaveable { mutableStateOf(false) }
    val assetTotal = assets.sumOf { it.currentEstimatedValueMinor }
    val liabilityTotal = liabilities.sumOf { it.outstandingAmountMinor }
    var valuationTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.AssetEntity?>(null) }
    var disposalTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.AssetEntity?>(null) }
    var liabilityPaymentTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.LiabilityEntity?>(null) }
    var receivablePaymentTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.ReceivableEntity?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Wealth", style = MaterialTheme.typography.headlineMedium) }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text("Recorded net wealth", style = MaterialTheme.typography.labelLarge); Text(formatPkr(assetTotal - liabilityTotal), style = MaterialTheme.typography.headlineLarge); Text("Assets ${formatPkr(assetTotal)} · Liabilities ${formatPkr(liabilityTotal)}") } } }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Assets", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showAdd = true }) { Text("Add") } } }
        if (assets.isEmpty()) item { Text("No assets recorded yet.") }
        items(assets, key = { it.id }) { asset -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) { Text(asset.title, style = MaterialTheme.typography.titleMedium); Text("${asset.type} · current ${formatPkr(asset.currentEstimatedValueMinor)} · acquired ${formatPkr(asset.acquisitionCostMinor)}"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { valuationTarget = asset }) { Text("Update value") }; TextButton(onClick = { disposalTarget = asset }) { Text("Dispose") } } } } }
        item { Text("Liabilities", style = MaterialTheme.typography.titleLarge) }
        if (liabilities.isEmpty()) item { Text("No liabilities recorded yet.") }
        items(liabilities, key = { it.id }) { liability -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) { Text(liability.title, style = MaterialTheme.typography.titleMedium); Text("${liability.type} · outstanding ${formatPkr(liability.outstandingAmountMinor)} of ${formatPkr(liability.originalAmountMinor)}"); TextButton(onClick = { liabilityPaymentTarget = liability }) { Text("Record repayment") } } } }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Investments", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showAdd = true }) { Text("Add") } } }
        if (investments.isEmpty()) item { Text("No investment events yet.") }
        items(investments, key = { it.id }) { event -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(event.securityName, style = MaterialTheme.typography.titleMedium); Text("${event.type} · ${formatPkr(event.grossAmountMinor)}") } } }
        item { Text("Receivables", style = MaterialTheme.typography.titleLarge) }
        if (receivables.isEmpty()) item { Text("No receivables yet.") }
        items(receivables, key = { it.id }) { value -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) { Text(value.title, style = MaterialTheme.typography.titleMedium); Text("${value.counterparty} · outstanding ${formatPkr(value.outstandingAmountMinor)} of ${formatPkr(value.originalAmountMinor)}"); if (value.outstandingAmountMinor > 0) TextButton(onClick = { receivablePaymentTarget = value }) { Text("Record receipt") } } } }
        item { Text("Goals", style = MaterialTheme.typography.titleLarge) }
        if (goals.isEmpty()) item { Text("No goals yet.") }
        items(goals, key = { it.id }) { goal -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text(goal.title, style = MaterialTheme.typography.titleMedium); Text("Target ${formatPkr(goal.targetAmountMinor)}") } } }
    }
    if (showAdd) AddWealthDialog(vm) { showAdd = false }
    valuationTarget?.let { asset -> AmountDialog("Update ${asset.title} valuation", "Current value (PKR)", onDismiss = { valuationTarget = null }) { value -> vm.updateAssetValuation(asset.id, value * 100); valuationTarget = null } }
    disposalTarget?.let { asset -> AmountDialog("Dispose ${asset.title}", "Disposal value (PKR)", onDismiss = { disposalTarget = null }) { value -> vm.disposeAsset(asset.id, value * 100); disposalTarget = null } }
    liabilityPaymentTarget?.let { liability -> AmountDialog("Repay ${liability.title}", "Payment (PKR)", onDismiss = { liabilityPaymentTarget = null }) { value -> vm.recordLiabilityPayment(liability.id, value * 100); liabilityPaymentTarget = null } }
    receivablePaymentTarget?.let { receivable -> AmountDialog("Receipt from ${receivable.counterparty}", "Received (PKR)", onDismiss = { receivablePaymentTarget = null }) { value -> vm.recordReceivablePayment(receivable.id, value * 100); receivablePaymentTarget = null } }
}

@Composable
private fun AddWealthDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    var mode by remember { mutableStateOf("ASSET") }
    var title by remember { mutableStateOf("") }
    var secondary by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var investmentType by remember { mutableStateOf("BUY") }
    val needsSecondary = mode == "RECEIVABLE"
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add ${mode.lowercase()}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("ASSET", "LIABILITY", "INVESTMENT", "RECEIVABLE", "GOAL").forEach { option -> OutlinedButton(onClick = { mode = option }) { Text(option.take(4)) } } }; OutlinedTextField(title, { title = it }, label = { Text(if (mode == "INVESTMENT") "Security" else "Name") }, singleLine = true); if (mode == "INVESTMENT") { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("BUY", "SELL", "DIVIDEND", "PROFIT", "FEE").forEach { option -> OutlinedButton(onClick = { investmentType = option }) { Text(option.take(4)) } } }; OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit) }, label = { Text("Quantity (optional)") }, singleLine = true) }; if (needsSecondary) OutlinedTextField(secondary, { secondary = it }, label = { Text("Counterparty") }, singleLine = true); OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Amount / target (PKR)") }, singleLine = true) } }, confirmButton = { Button(onClick = { val value = amount.toLong() * 100; when (mode) { "ASSET" -> vm.addAsset(title, value); "LIABILITY" -> vm.addLiability(title, value); "INVESTMENT" -> vm.addInvestmentEvent(title, investmentType, value, quantity.toLongOrNull() ?: 0); "RECEIVABLE" -> vm.addReceivable(title, secondary, value); "GOAL" -> vm.addGoal(title, value) }; onDismiss() }, enabled = title.isNotBlank() && (!needsSecondary || secondary.isNotBlank()) && amount.toLongOrNull()?.let { it > 0 } == true) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun AmountDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text(label) }, singleLine = true) }, confirmButton = { Button(onClick = { onConfirm(amount.toLong()) }, enabled = amount.toLongOrNull()?.let { it > 0 } == true) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun VaultScreen(vm: MainViewModel, application: PassportApplication, padding: PaddingValues) {
    val documents by vm.documents.collectAsState()
    val taxItems by vm.taxItems.collectAsState()
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var linkTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.DocumentEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.DocumentEntity?>(null) }
    var previewTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.DocumentEntity?>(null) }
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var previewError by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { pendingUri = it }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Vault", style = MaterialTheme.typography.headlineMedium); IconButton(onClick = { launcher.launch(arrayOf("application/pdf", "image/jpeg", "image/png", "image/webp")) }) { Icon(Icons.Default.Add, "Import document") } } }
        item { Text("Evidence stays attached to structured records.") }
        if (documents.isEmpty()) item { Text("No documents yet. Import a PDF or image to keep local evidence.") }
        items(documents, key = { it.id }) { document -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text(document.title, style = MaterialTheme.typography.titleMedium); Text("${document.category} · ${document.mimeType}"); Text("SHA-256 ${document.sha256.take(12)}…"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { previewTarget = document; previewBitmap = null; previewError = null; scope.launch { runCatching { withContext(Dispatchers.IO) { renderDocumentPreview(application, document) }.asImageBitmap() }.onSuccess { previewBitmap = it }.onFailure { previewError = it.message ?: "Preview failed" } } }) { Text("Preview") }; if (taxItems.isNotEmpty()) TextButton(onClick = { linkTarget = document }) { Text("Link to tax item") }; TextButton(onClick = { deleteTarget = document }) { Text("Delete") } } } } }
    }
    pendingUri?.let { uri ->
        AlertDialog(onDismissRequest = { pendingUri = null }, title = { Text("Save document") }, text = { Text("The selected file will be encrypted into app-private storage.") }, confirmButton = { Button(onClick = { scope.launch { runCatching { DocumentVault(application, application.repository).import(uri, "Imported evidence", "Other") }; pendingUri = null } }) { Text("Import") } }, dismissButton = { TextButton(onClick = { pendingUri = null }) { Text("Cancel") } })
    }
    linkTarget?.let { document -> LinkDocumentDialog(document, taxItems, vm) { linkTarget = null } }
    deleteTarget?.let { document ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete encrypted document?") }, text = { Text("This permanently removes the encrypted file, metadata, and all evidence links for ${document.title}.") }, confirmButton = { Button(onClick = { vm.deleteDocument(document.id); deleteTarget = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } })
    }
    previewTarget?.let { document ->
        AlertDialog(onDismissRequest = { previewTarget = null; previewBitmap = null }, title = { Text(document.title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { previewBitmap?.let { Image(it, contentDescription = "Preview of ${document.title}", modifier = Modifier.fillMaxWidth()) } ?: Text(previewError ?: "Decrypting preview…") } }, confirmButton = { TextButton(onClick = { previewTarget = null; previewBitmap = null }) { Text("Close") } })
    }
}

internal fun renderDocumentPreview(application: PassportApplication, document: pk.vexel.financepassport.core.database.DocumentEntity): Bitmap {
    val bytes = DocumentVault(application, application.repository).decrypt(document)
    if (document.mimeType != "application/pdf") return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("Image preview could not be decoded")
    val temporary = java.io.File.createTempFile("passport-preview-", ".pdf", application.cacheDir)
    return try {
        temporary.writeBytes(bytes)
        ParcelFileDescriptor.open(temporary, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                require(renderer.pageCount > 0) { "PDF has no pages" }
                renderer.openPage(0).use { page ->
                    Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888).also { bitmap ->
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }
    } finally {
        temporary.delete()
    }
}

@Composable private fun LinkDocumentDialog(document: pk.vexel.financepassport.core.database.DocumentEntity, taxItems: List<pk.vexel.financepassport.core.database.TaxItemEntity>, vm: MainViewModel, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Link ${document.title}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Choose a tax item. The same document can be linked again to another record.", style = MaterialTheme.typography.bodySmall); taxItems.take(8).forEach { item -> TextButton(onClick = { vm.linkDocument(document.id, "tax_item", item.id); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text(item.description, maxLines = 1) } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable private fun AccountCard(account: AccountEntity, vm: MainViewModel, onEdit: () -> Unit, onArchive: () -> Unit) {
    val movement by vm.accountMovement(account.id).collectAsState(0L)
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text(account.name, style = MaterialTheme.typography.titleMedium); Text(account.accountType); Text("Current balance ${formatPkr(account.openingBalanceMinor + movement)}", style = MaterialTheme.typography.titleLarge); Text("Opening balance ${formatPkr(account.openingBalanceMinor)}"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = onEdit) { Text("Edit") }; TextButton(onClick = onArchive) { Text("Archive") } } } }
}

@Composable private fun AddAccountDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add account") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Account name") }, singleLine = true); OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Opening balance (PKR)") }, singleLine = true) } }, confirmButton = { Button(onClick = { vm.addAccount(name, "OTHER", amount.toLongOrNull()?.times(100) ?: 0); onDismiss() }, enabled = name.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun EditAccountDialog(account: AccountEntity, vm: MainViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(account.name) }; var amount by remember { mutableStateOf((account.openingBalanceMinor / 100).toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit account") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Account name") }, singleLine = true); OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Opening balance (PKR)") }, singleLine = true) } }, confirmButton = { Button(onClick = { vm.updateAccount(account.id, name, amount.toLong() * 100); onDismiss() }, enabled = name.isNotBlank() && amount.toLongOrNull()?.let { it >= 0 } == true) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun AddEventDialog(vm: MainViewModel, accounts: List<AccountEntity>, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var category by remember { mutableStateOf("") }; var income by remember { mutableStateOf(true) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (income) "Record income" else "Record expense") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ income = true }) { Text("Income") }; OutlinedButton({ income = false }) { Text("Expense") } }; OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Amount (PKR)") }, singleLine = true, modifier = Modifier.testTag("money-event-amount")); OutlinedTextField(description, { description = it }, label = { Text("What happened?") }, singleLine = true, modifier = Modifier.testTag("money-event-description")); OutlinedTextField(category, { category = it }, label = { Text("Category (optional)") }, singleLine = true, modifier = Modifier.testTag("money-event-category")) } }, confirmButton = { Button(onClick = { vm.addEvent(if (income) FinancialEventType.INCOME else FinancialEventType.EXPENSE, amount.toLongOrNull()?.times(100) ?: 0, accounts.first().id, description, category); onDismiss() }, enabled = amount.toLongOrNull()?.let { it > 0 } == true && description.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun RecurringItemDialog(vm: MainViewModel, application: PassportApplication, accounts: List<AccountEntity>, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("MONTHLY") }
    var delayDays by remember { mutableStateOf("1") }
    var income by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add recurring draft") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ income = true }) { Text("Income") }; OutlinedButton({ income = false }) { Text("Expense") } }
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.testTag("recurring-title"))
            OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Amount (PKR)") }, singleLine = true, modifier = Modifier.testTag("recurring-amount"))
            OutlinedTextField(category, { category = it }, label = { Text("Category (optional)") }, singleLine = true, modifier = Modifier.testTag("recurring-category"))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY").forEach { option -> OutlinedButton({ frequency = option }) { Text(option.take(3)) } } }
            OutlinedTextField(delayDays, { delayDays = it.filter(Char::isDigit) }, label = { Text("First reminder in days") }, singleLine = true, modifier = Modifier.testTag("recurring-delay"))
            Text("This creates a reminder/draft only; it never silently records a financial event.", style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { Button(onClick = { vm.addRecurringItem(application, title, if (income) FinancialEventType.INCOME else FinancialEventType.EXPENSE, amount.toLong() * 100, accounts.first().id, category, frequency, delayDays.toLong()); onDismiss() }, enabled = title.isNotBlank() && amount.toLongOrNull()?.let { it > 0 } == true && delayDays.toLongOrNull()?.let { it > 0 } == true) { Text("Save draft") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable private fun TransferDialog(vm: MainViewModel, accounts: List<AccountEntity>, onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Transfer between accounts") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("${accounts[0].name} → ${accounts[1].name}"); OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Amount (PKR)") }, singleLine = true); OutlinedTextField(description, { description = it }, label = { Text("Reason") }, singleLine = true) } }, confirmButton = { Button(onClick = { vm.transfer(accounts[0].id, accounts[1].id, amount.toLong() * 100, description); onDismiss() }, enabled = amount.toLongOrNull()?.let { it > 0 } == true && description.isNotBlank()) { Text("Transfer") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun EmptyModuleScreen(label: String, padding: PaddingValues) { Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), Arrangement.spacedBy(12.dp)) { Text(label, style = MaterialTheme.typography.headlineMedium); Text("This workspace is next in the build sequence. Your existing local records are preserved.") } }
private fun formatPkr(minor: Long): String = "PKR ${minor / 100}"
