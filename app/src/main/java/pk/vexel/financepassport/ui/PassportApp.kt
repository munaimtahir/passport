package pk.vexel.financepassport.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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
import pk.vexel.financepassport.core.model.PkrMoneyInput
import pk.vexel.financepassport.core.files.DocumentVault
import pk.vexel.financepassport.core.security.LiveRestoreService
import pk.vexel.financepassport.ui.theme.PassportTheme

private data class Destination(val label: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("Home", Icons.Default.Home), Destination("Money", Icons.Default.AccountBalanceWallet),
    Destination("Wealth", Icons.AutoMirrored.Filled.TrendingUp), Destination("Tax & Records", Icons.Default.Description),
    Destination("Vault", Icons.Default.Folder),
)

/** Whether monetary values should render masked; toggled from the top app bar and persisted in [pk.vexel.financepassport.core.security.AppPreferences]. */
val LocalPrivacyMode = compositionLocalOf { false }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportApp() {
    val application = LocalContext.current.applicationContext as PassportApplication
    val vm: MainViewModel = viewModel(factory = MainViewModelFactory(application.repository, application.preferences))
    val fabAccounts by vm.accounts.collectAsState()
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    CompositionLocalProvider(LocalPrivacyMode provides vm.privacyModeEnabled) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Vexel Finance Passport") }, actions = {
                    IconButton(onClick = vm::togglePrivacyMode) {
                        Icon(
                            if (vm.privacyModeEnabled) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (vm.privacyModeEnabled) "Show amounts" else "Hide amounts",
                        )
                    }
                    IconButton(onClick = { showMore = true }) { Icon(Icons.Default.MoreHoriz, "More") }
                })
            },
            floatingActionButton = { if (selected == 1) FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.testTag("money-fab")) { Icon(Icons.Default.Add, "Add") } },
            bottomBar = { NavigationBar { destinations.forEachIndexed { index, destination -> NavigationBarItem(selected == index, { selected = index }, icon = { Icon(destination.icon, destination.label) }, label = { Text(destination.label) }) } } },
        ) { padding ->
            when (selected) {
                0 -> HomeScreen(vm, application, padding) { selected = it }
                1 -> MoneyScreen(vm, application, padding)
                2 -> WealthScreen(vm, padding)
                3 -> TaxScreen(vm, application, padding)
                4 -> VaultScreen(vm, application, padding)
                else -> EmptyModuleScreen(destinations[selected].label, padding)
            }
        }
        if (showAdd) AddEventDialog(vm, fabAccounts) { showAdd = false }
        if (showMore) MoreDialog(vm, application) { showMore = false }
        vm.errorMessage?.let { message ->
            AlertDialog(onDismissRequest = vm::clearError, title = { Text("Could not save") }, text = { Text(message) }, confirmButton = { TextButton(onClick = vm::clearError) { Text("OK") } })
        }
    }
}

@Composable
private fun MoreDialog(vm: MainViewModel, application: PassportApplication, onDismiss: () -> Unit) {
    val activity = androidx.activity.compose.LocalActivity.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var deleteConfirmation by rememberSaveable { mutableStateOf("") }
    var backupPassword by rememberSaveable { mutableStateOf(false) }
    var restorePayload by remember { mutableStateOf<ByteArray?>(null) }
    var pendingBackup by remember { mutableStateOf<java.io.File?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var requestedReport by rememberSaveable { mutableStateOf("NET_WORTH") }
    var currentYearOnly by rememberSaveable { mutableStateOf(false) }
    var previewReport by remember { mutableStateOf<pk.vexel.financepassport.core.reports.FinancialReport?>(null) }
    var pendingReportExport by remember { mutableStateOf<(() -> Unit)?>(null) }
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
    val backupSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri -> val file = pendingBackup; if (uri != null && file != null) scope.launch { application.contentResolver.openOutputStream(uri)?.use { destination -> file.inputStream().use { it.copyTo(destination) } }; file.delete(); status = "Encrypted backup exported" }; pendingBackup = null }
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) scope.launch { restorePayload = application.contentResolver.openInputStream(uri)?.use { it.readBytes() } } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("More") }, text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()).testTag("more-dialog-scroll"), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Reports and local data controls"); status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }; OutlinedButton(onClick = { currentYearOnly = !currentYearOnly }, modifier = Modifier.fillMaxWidth()) { Text(if (currentYearOnly) "Report range: current tax year" else "Report range: all recorded dates") }; Button(onClick = { exporter.launch("vexel-finance-passport-export.json") }, modifier = Modifier.fillMaxWidth()) { Text("Export structured JSON") }; Button(onClick = { scope.launch { previewReport = pk.vexel.financepassport.core.reports.ReportGenerator().netWorth(reportSnapshot(), java.time.Instant.now().toString()); pendingReportExport = { pdfExporter.launch("vexel-net-worth.pdf") } } }, modifier = Modifier.fillMaxWidth()) { Text("Preview net-worth report") }; Button(onClick = { scope.launch { previewReport = pk.vexel.financepassport.core.reports.ReportGenerator().annualFinancialSummary(reportSnapshot(), java.time.Instant.now().toString()); pendingReportExport = { annualPdfExporter.launch("vexel-annual-financial-summary.pdf") } } }, modifier = Modifier.fillMaxWidth()) { Text("Preview annual summary report") }; listOf("ASSETS" to "Asset statement", "LIABILITIES" to "Liability statement", "CASH_FLOW" to "Cash-flow summary", "INVESTMENTS" to "Investment summary", "RECEIVABLES" to "Receivables report", "TAX" to "Tax preparation summary", "EVIDENCE" to "Evidence checklist").forEach { (kind, label) -> Button(onClick = { requestedReport = kind; scope.launch { val snapshot = reportSnapshot(); val stamp = java.time.Instant.now().toString(); val generator = pk.vexel.financepassport.core.reports.ReportGenerator(); val report = when (kind) { "ASSETS" -> generator.assetStatement(snapshot, stamp); "LIABILITIES" -> generator.liabilityStatement(snapshot, stamp); "CASH_FLOW" -> generator.cashFlowSummary(snapshot, stamp); "INVESTMENTS" -> generator.investmentSummary(snapshot, stamp); "RECEIVABLES" -> generator.receivablesReport(snapshot, stamp); "TAX" -> generator.taxPreparationSummary(snapshot, stamp); "EVIDENCE" -> generator.evidenceChecklist(snapshot, stamp); else -> generator.netWorth(snapshot, stamp) }; previewReport = report; pendingReportExport = { requestedReport = kind; catalogPdfExporter.launch("vexel-${kind.lowercase()}-report.pdf") } } }, modifier = Modifier.fillMaxWidth()) { Text("Preview $label") } }; Button(onClick = { csvExporter.launch("vexel-financial-events.csv") }, modifier = Modifier.fillMaxWidth()) { Text("Export financial events CSV") }; Button(onClick = { backupPassword = true }, modifier = Modifier.fillMaxWidth()) { Text("Create encrypted backup") }; Button(onClick = { backupPicker.launch(arrayOf("application/octet-stream", "application/zip", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) { Text("Restore encrypted backup") }; Button(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete all application data") } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false; deleteConfirmation = "" }, title = { Text("Delete everything?") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("This permanently removes local records, encrypted vault files, preferences, and scheduled work. It cannot be undone."); OutlinedTextField(deleteConfirmation, { deleteConfirmation = it }, label = { Text("Type DELETE to confirm") }, singleLine = true, modifier = Modifier.testTag("delete-confirmation")) } }, confirmButton = { Button(onClick = { vm.deleteAllData(application) { activity?.recreate() }; confirmDelete = false; deleteConfirmation = ""; onDismiss() }, enabled = deleteConfirmation == "DELETE") { Text("Delete all") } }, dismissButton = { TextButton(onClick = { confirmDelete = false; deleteConfirmation = "" }) { Text("Cancel") } })
    if (backupPassword) BackupPasswordDialog("Create encrypted backup", onDismiss = { backupPassword = false }) { password ->
        backupPassword = false
        vm.createBackup(application, password.toCharArray()) { result -> result.onSuccess { pendingBackup = it; backupSaver.launch("vexel-finance-passport.backup") }.onFailure { status = "Backup failed: ${it.message}" } }
    }
    restorePayload?.let { payload -> BackupPasswordDialog("Restore encrypted backup", onDismiss = { restorePayload = null }) { password ->
        scope.launch { runCatching { LiveRestoreService(application).restore(payload, password.toCharArray()) }.onSuccess { status = "Restore complete. Close and relaunch the app to reopen restored records." }.onFailure { status = "Restore failed: ${it.message}" }; restorePayload = null }
    } }
    previewReport?.let { report ->
        AlertDialog(
            onDismissRequest = { previewReport = null; pendingReportExport = null },
            title = { Text(report.title) },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Generated ${report.generatedAt}", style = MaterialTheme.typography.labelSmall)
                    report.lines.forEach { line -> Text(line, style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = { Button(onClick = { pendingReportExport?.invoke(); previewReport = null; pendingReportExport = null }) { Text("Export as PDF") } },
            dismissButton = { TextButton(onClick = { previewReport = null; pendingReportExport = null }) { Text("Close") } },
        )
    }
}

@Composable
private fun BackupPasswordDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    // Deliberately plain `remember`, not `rememberSaveable`: a backup/restore password must not
    // be written into the saved-instance-state Bundle, which Android can persist to disk across
    // process death. Losing this field on rotation/process death is the correct, safer behavior.
    var password by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(password, { password = it }, label = { Text("Backup password (8+ characters)") }, singleLine = true) }, confirmButton = { Button(onClick = { onConfirm(password) }, enabled = password.length >= 8) { Text("Continue") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun HomeScreen(vm: MainViewModel, application: PassportApplication, padding: PaddingValues, onNavigate: (Int) -> Unit) {
    val totals by vm.totals.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val activeEventCount by vm.activeEventCount.collectAsState()
    val recentEvents by vm.recentEvents.collectAsState()
    val calendarItems by vm.calendarItems.collectAsState()
    val position by vm.financialPosition.collectAsState()
    val taxItems by vm.taxItems.collectAsState()
    val readiness = pk.vexel.financepassport.core.model.calculateTaxReadiness(taxItems)
    var showCalendarAdd by rememberSaveable { mutableStateOf(false) }
    var rescheduleTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.CalendarItemEntity?>(null) }
    var showQuickEvent by rememberSaveable { mutableStateOf(false) }
    var showQuickTransfer by rememberSaveable { mutableStateOf(false) }
    var showQuickAsset by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Your financial passport", style = MaterialTheme.typography.headlineSmall) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), Arrangement.spacedBy(6.dp)) {
                    Text("Net worth", style = MaterialTheme.typography.labelLarge)
                    Text(MaskedPkr(position?.netWorthMinor ?: 0L), style = MaterialTheme.typography.displaySmall)
                    Text("Everything you own minus everything you owe, from your recorded accounts, assets, investments, receivables and liabilities.", style = MaterialTheme.typography.bodySmall)
                    if (position != null) {
                        Column(Modifier.padding(top = 8.dp), Arrangement.spacedBy(2.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Liquid funds"); Text(MaskedPkr(position!!.liquidFundsMinor)) }
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Investments"); Text(MaskedPkr(position!!.investmentsValueMinor)) }
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Assets"); Text(MaskedPkr(position!!.assetsValueMinor)) }
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Receivables"); Text(MaskedPkr(position!!.receivablesValueMinor)) }
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Liabilities"); Text(MaskedPkr(-position!!.liabilitiesValueMinor)) }
                        }
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), Arrangement.spacedBy(4.dp)) {
                    Text("Income vs. expense this period", style = MaterialTheme.typography.labelLarge)
                    Text(MaskedPkr((totals?.first?.minorUnits?.value ?: 0L) - (totals?.second?.minorUnits?.value ?: 0L)), style = MaterialTheme.typography.titleLarge)
                    Text("Income ${MaskedPkr(position?.monthlyIncomeMinor ?: 0L)} · Expense ${MaskedPkr(position?.monthlyExpenseMinor ?: 0L)} this month, from ${accounts.size} active account(s) and $activeEventCount recorded event(s).", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("Quick add", style = MaterialTheme.typography.titleLarge) }
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                // Falls back to switching to Money when there's no account yet — an income/expense
                // or transfer dialog with nothing to pick from is worse than the existing nav shortcut.
                OutlinedButton(onClick = { if (accounts.isNotEmpty()) showQuickEvent = true else onNavigate(1) }, modifier = Modifier.weight(1f).testTag("quick-add-event")) { Text("Income / expense") }
                OutlinedButton(onClick = { if (accounts.size >= 2) showQuickTransfer = true else onNavigate(1) }, modifier = Modifier.weight(1f).testTag("quick-add-transfer")) { Text("Transfer") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showQuickAsset = true }, modifier = Modifier.weight(1f).testTag("quick-add-asset")) { Text("Asset") }
                OutlinedButton(onClick = { onNavigate(3) }, modifier = Modifier.weight(1f)) { Text("Tax item") }
                OutlinedButton(onClick = { onNavigate(4) }, modifier = Modifier.weight(1f)) { Text("Document") }
            }
        }
        item { Text("Tax-year readiness", style = MaterialTheme.typography.titleLarge) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), Arrangement.spacedBy(6.dp)) {
                    Text("${readiness.evidenceResolvedCount}/${readiness.totalItemCount} tax item(s) have evidence status resolved")
                    Text("${readiness.unmappedCount} item(s) need classification review · ${readiness.duplicateGroupCount} duplicate candidate group(s)")
                    Text("These are workflow-completeness signals, not a statement of tax correctness.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Upcoming obligations", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showCalendarAdd = true }) { Text("Add") } } }
        if (calendarItems.isEmpty()) item { Text("No reminders scheduled.") }
        items(calendarItems.take(5), key = { it.id }) { reminder -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) { Text(reminder.title, style = MaterialTheme.typography.titleMedium); Text(reminder.kind); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { rescheduleTarget = reminder }) { Text("Reschedule") }; TextButton(onClick = { vm.updateCalendarStatus(application, reminder.id, "COMPLETED") }) { Text("Complete") }; TextButton(onClick = { vm.updateCalendarStatus(application, reminder.id, "CANCELLED") }) { Text("Cancel") } } } } }
        item { Text("Recent activity", style = MaterialTheme.typography.titleLarge) }
        items(recentEvents.take(5), key = { it.id }) { event -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(event.description); Text(MaskedPkr(event.amountMinor)) } }
    }
    if (showCalendarAdd) CalendarItemDialog(vm, application) { showCalendarAdd = false }
    rescheduleTarget?.let { reminder -> RescheduleDialog(reminder, vm, application) { rescheduleTarget = null } }
    if (showQuickEvent) AddEventDialog(vm, accounts) { showQuickEvent = false }
    if (showQuickTransfer) TransferDialog(vm, accounts) { showQuickTransfer = false }
    if (showQuickAsset) AddWealthDialog(vm) { showQuickAsset = false }
}

@Composable
private fun CalendarItemDialog(vm: MainViewModel, application: PassportApplication, onDismiss: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf("REVIEW") }
    var delay by rememberSaveable { mutableStateOf("60") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add reminder") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true); OutlinedTextField(kind, { kind = it }, label = { Text("Kind") }, singleLine = true); OutlinedTextField(delay, { delay = it.filter(Char::isDigit) }, label = { Text("Remind in minutes") }, singleLine = true) } }, confirmButton = { Button(onClick = { vm.addCalendarItem(application, title, kind, delay.toLong()); onDismiss() }, enabled = title.isNotBlank() && delay.toLongOrNull()?.let { it > 0 } == true) { Text("Schedule") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun RescheduleDialog(reminder: pk.vexel.financepassport.core.database.CalendarItemEntity, vm: MainViewModel, application: PassportApplication, onDismiss: () -> Unit) {
    var delay by rememberSaveable { mutableStateOf("60") }
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
    var showAddAccount by rememberSaveable { mutableStateOf(false) }
    var editAccount by remember { mutableStateOf<AccountEntity?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("money-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Money", style = MaterialTheme.typography.headlineMedium) }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Accounts", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showAddAccount = true }, modifier = Modifier.testTag("add-account")) { Text("Add account") } } }
        if (accounts.isEmpty()) item { Text("No accounts yet. Use + to add cash or a bank account.") }
        items(accounts, key = { it.id }) { account -> AccountCard(account, vm, onEdit = { editAccount = account }, onArchive = { vm.archiveAccount(account.id) }) }
        item { Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { Button(onClick = { showEvent = true }, enabled = accounts.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Income / expense") }; Button(onClick = { showTransfer = true }, enabled = accounts.size >= 2, modifier = Modifier.weight(1f)) { Text("Transfer") } } }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Recurring drafts", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showRecurring = true }, enabled = accounts.isNotEmpty(), modifier = Modifier.testTag("add-recurring")) { Text("Add") } } }
        if (recurringItems.isEmpty()) item { Text("No recurring drafts. Add one to receive a reminder without silently creating a confirmed event.") }
        items(recurringItems, key = { it.id }) { recurring -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text(recurring.title, style = MaterialTheme.typography.titleMedium); Text("${recurring.eventType} · ${MaskedPkr(recurring.amountMinor)} · ${recurring.frequency}"); Text("Next draft reminder: ${java.time.LocalDate.ofEpochDay(recurring.nextDueDateEpochDay)}"); TextButton(onClick = { vm.pauseRecurringItem(application, recurring.id) }) { Text("Pause") } } } }
        item { Text("Activity", style = MaterialTheme.typography.titleLarge) }
        items(recentEvents, key = { it.id }) { event -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), Arrangement.SpaceBetween) { Column { Text(event.description); Text(listOfNotNull(event.eventType, event.category).joinToString(" · "), style = MaterialTheme.typography.labelSmall) }; Text(MaskedPkr(event.amountMinor)) } } }
    }
    if (showEvent) AddEventDialog(vm, accounts) { showEvent = false }
    if (showTransfer) TransferDialog(vm, accounts) { showTransfer = false }
    if (showRecurring) RecurringItemDialog(vm, application, accounts) { showRecurring = false }
    if (showAddAccount) AddAccountDialog(vm) { showAddAccount = false }
    editAccount?.let { account -> EditAccountDialog(account, vm, onDismiss = { editAccount = null }) }
}

@Composable
private fun TaxScreen(vm: MainViewModel, application: PassportApplication, padding: PaddingValues) {
    val items by vm.taxItems.collectAsState()
    val officialRecords by vm.officialRecords.collectAsState()
    val drafts by vm.drafts.collectAsState()
    val issues by vm.taxIssues.collectAsState()
    val reconciliations by vm.reconciliations.collectAsState()
    val taxYears by vm.taxYears.collectAsState()
    var showOfficialRecord by rememberSaveable { mutableStateOf(false) }
    var showManualTaxItem by rememberSaveable { mutableStateOf(false) }
    var showFilingDeadline by rememberSaveable { mutableStateOf(false) }
    var reviewTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.TaxItemEntity?>(null) }
    var draftLines by remember { mutableStateOf<List<pk.vexel.financepassport.core.database.TaxDraftLineEntity>?>(null) }
    var mappingHistory by remember { mutableStateOf<List<pk.vexel.financepassport.core.database.TaxMappingEntity>?>(null) }
    var mappingHistoryTaxItemId by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val readiness = pk.vexel.financepassport.core.model.calculateTaxReadiness(items)
    val duplicateCandidates = items.groupBy { Triple(it.dateEpochDay, it.grossAmountMinor, it.currency) }.values.filter { group -> group.size > 1 }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Tax & Records", style = MaterialTheme.typography.headlineMedium) }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Official records", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showOfficialRecord = true }) { Text("Add") } } }
        if (officialRecords.isEmpty()) item { Text("No official records yet. Sensitive identifiers are encrypted and masked.") }
        items(officialRecords, key = { it.id }) { record -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text(record.title, style = MaterialTheme.typography.titleMedium); Text("${record.recordType} · ${record.maskedIdentifier ?: "No identifier"}") } } }
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, androidx.compose.ui.Alignment.CenterVertically) {
                Text("Tax year", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    IconButton(onClick = { vm.selectTaxYear(vm.selectedTaxYear - 1) }) { Icon(Icons.Default.ChevronLeft, "Previous tax year") }
                    Text(vm.selectedTaxYearId, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { vm.selectTaxYear(vm.selectedTaxYear + 1) }, enabled = vm.selectedTaxYear < java.time.LocalDate.now().year) { Icon(Icons.Default.ChevronRight, "Next tax year") }
                }
            }
        }
        item { Text("Selecting a year works on that year's draft, snapshots and reconciliation below — it never changes past years' saved history.", style = MaterialTheme.typography.bodySmall) }
        item {
            val currentYearStatus = taxYears.find { it.id == vm.selectedTaxYearId }?.status ?: "OPEN"
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, androidx.compose.ui.Alignment.CenterVertically) {
                Text("Status: $currentYearStatus", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("tax-year-status"))
                when (currentYearStatus) {
                    "OPEN" -> Button(onClick = { vm.updateTaxYearStatus("UNDER_REVIEW") }, modifier = Modifier.testTag("tax-year-start-review")) { Text("Start review") }
                    "UNDER_REVIEW" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.updateTaxYearStatus("OPEN") }) { Text("Reopen") }
                        Button(onClick = { vm.updateTaxYearStatus("FILED") }, modifier = Modifier.testTag("tax-year-mark-filed")) { Text("Mark filed") }
                    }
                    "FILED" -> OutlinedButton(onClick = { vm.updateTaxYearStatus("OPEN") }) { Text("Reopen") }
                }
            }
        }
        item { OutlinedButton(onClick = { showFilingDeadline = true }, modifier = Modifier.fillMaxWidth()) { Text("Set filing deadline reminder") } }
        item { Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { Button(onClick = { showManualTaxItem = true }, modifier = Modifier.weight(1f)) { Text("Add tax item") }; Button(onClick = { vm.prepareAnnualDraft() }, modifier = Modifier.weight(1f)) { Text("Prepare draft") } } }
        if (vm.draftMessage != null) item { Text(vm.draftMessage!!, color = MaterialTheme.colorScheme.primary) }
        item { Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { vm.recordWealthSnapshot("OPENING") }, modifier = Modifier.weight(1f)) { Text("Record opening snapshot") }; OutlinedButton(onClick = { vm.recordWealthSnapshot("CLOSING") }, modifier = Modifier.weight(1f)) { Text("Record closing snapshot") } } }
        item { Button(onClick = { vm.calculateReconciliation() }, modifier = Modifier.fillMaxWidth()) { Text("Reconcile recorded wealth") } }
        if (vm.reconciliationMessage != null) item { Text(vm.reconciliationMessage!!, color = MaterialTheme.colorScheme.primary) }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text("Annual review readiness", style = MaterialTheme.typography.titleMedium); Text("${readiness.evidenceResolvedCount}/${readiness.totalItemCount} tax item(s) have evidence status resolved"); Text("${readiness.unmappedCount} item(s) need classification review · ${duplicateCandidates.size} duplicate candidate group(s)"); Text("These are workflow signals, not a statement of tax correctness.", style = MaterialTheme.typography.bodySmall) } } }
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
    if (showOfficialRecord) OfficialRecordDialog(vm, application) { showOfficialRecord = false }
    if (showManualTaxItem) ManualTaxItemDialog(vm) { showManualTaxItem = false }
    if (showFilingDeadline) FilingDeadlineDialog(vm, application, vm.selectedTaxYearId) { showFilingDeadline = false }
    reviewTarget?.let { item -> TaxReviewDialog(item, vm) { reviewTarget = null } }
    draftLines?.let { lines ->
        AlertDialog(onDismissRequest = { draftLines = null }, title = { Text("Draft calculation lines") }, text = { LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { if (lines.isEmpty()) item { Text("No generated lines.") }; items(lines, key = { it.id }) { line ->
            val sourceIds = Regex("\"([^\"]*)\"").findAll(line.sourceIdsJson).map { it.groupValues[1] }.toList()
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${line.sectionCode} / ${line.categoryCode}", style = MaterialTheme.typography.titleSmall)
                Text("Amount ${formatPkr(line.amountMinor)} · sources ${line.sourceIdsJson}")
                Text(line.calculation, style = MaterialTheme.typography.bodySmall)
                sourceIds.forEach { sourceId -> TextButton(onClick = { scope.launch { mappingHistoryTaxItemId = sourceId; mappingHistory = vm.getMappingHistory(sourceId) } }) { Text("View mapping history: $sourceId") } }
            }
        } } }, confirmButton = { TextButton(onClick = { draftLines = null }) { Text("Close") } })
    }
    mappingHistory?.let { history ->
        AlertDialog(
            onDismissRequest = { mappingHistory = null; mappingHistoryTaxItemId = null },
            title = { Text("Mapping history") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text("Source: ${mappingHistoryTaxItemId}", style = MaterialTheme.typography.labelSmall) }
                    if (history.isEmpty()) item { Text("No mapping history recorded for this source.") }
                    items(history, key = { it.id }) { mapping ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("${mapping.taxEventType} · ${mapping.sectionCode}/${mapping.categoryCode}", style = MaterialTheme.typography.titleSmall)
                            Text("${mapping.source} · ruleset ${mapping.rulesetVersion}${if (mapping.supersededByMappingId == null) " · active" else " · superseded"}", style = MaterialTheme.typography.bodySmall)
                            mapping.overrideReason?.let { Text("Reason: $it", style = MaterialTheme.typography.bodySmall) }
                            Text(java.time.Instant.ofEpochMilli(mapping.createdAtEpochMillis).toString(), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { mappingHistory = null; mappingHistoryTaxItemId = null }) { Text("Close") } },
        )
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
private fun OfficialRecordDialog(vm: MainViewModel, application: PassportApplication, onDismiss: () -> Unit) {
    var type by rememberSaveable { mutableStateOf("CNIC/NICOP") }
    var title by rememberSaveable { mutableStateOf("") }
    var identifier by rememberSaveable { mutableStateOf("") }
    var hasExpiry by rememberSaveable { mutableStateOf(false) }
    var expiry by rememberSaveable { mutableStateOf(java.time.LocalDate.now().plusYears(1)) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add official record") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(type, { type = it }, label = { Text("Record type") }, singleLine = true)
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
        OutlinedTextField(identifier, { identifier = it }, label = { Text("Sensitive identifier (optional)") }, singleLine = true)
        OutlinedButton(onClick = { hasExpiry = !hasExpiry }) { Text(if (hasExpiry) "Remove expiry date" else "Set expiry date") }
        if (hasExpiry) DateField("Expiry date", expiry, { expiry = it })
    } }, confirmButton = { Button(onClick = { vm.addOfficialRecord(application, type, title, identifier, expiryDate = expiry.takeIf { hasExpiry }); onDismiss() }, enabled = title.isNotBlank()) { Text("Save encrypted") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun ManualTaxItemDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    var type by rememberSaveable { mutableStateOf("OTHER_INCOME") }; var description by rememberSaveable { mutableStateOf("") }; var amount by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add tax item") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(type, { type = it.uppercase() }, label = { Text("Tax event type") }, singleLine = true); OutlinedTextField(description, { description = it }, label = { Text("What happened?") }, singleLine = true); AmountField(amount, { amount = it }, "Amount (PKR)"); Text("This stays a source fact until reviewed; evidence can be attached later.", style = MaterialTheme.typography.bodySmall) } }, confirmButton = { Button(onClick = { vm.addManualTaxItem(type, PkrMoneyInput.toMinorUnits(amount, false), description); onDismiss() }, enabled = description.isNotBlank() && runCatching { PkrMoneyInput.parseRupees(amount, false) }.isSuccess && type.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun FilingDeadlineDialog(vm: MainViewModel, application: PassportApplication, taxYearId: String, onDismiss: () -> Unit) {
    var hasDeadline by rememberSaveable { mutableStateOf(true) }
    var deadline by rememberSaveable { mutableStateOf(java.time.LocalDate.now().plusMonths(3)) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Filing deadline for $taxYearId") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("A one-time reminder, not an FBR deadline lookup — set the date that applies to you.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = { hasDeadline = !hasDeadline }) { Text(if (hasDeadline) "Clear reminder" else "Set a deadline") }
        if (hasDeadline) DateField("Deadline", deadline, { deadline = it })
    } }, confirmButton = { Button(onClick = { vm.scheduleTaxFilingDeadlineReminder(application, deadline.takeIf { hasDeadline }); onDismiss() }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun WealthScreen(vm: MainViewModel, padding: PaddingValues) {
    val assets by vm.assets.collectAsState()
    val liabilities by vm.liabilities.collectAsState()
    val investments by vm.investments.collectAsState()
    val receivables by vm.receivables.collectAsState()
    val goalProgress by vm.goalProgress.collectAsState()
    var showAdd by rememberSaveable { mutableStateOf(false) }
    val assetTotal = assets.sumOf { it.currentEstimatedValueMinor }
    val liabilityTotal = liabilities.sumOf { it.outstandingAmountMinor }
    var valuationTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.AssetEntity?>(null) }
    var disposalTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.AssetEntity?>(null) }
    var liabilityPaymentTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.LiabilityEntity?>(null) }
    var receivablePaymentTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.ReceivableEntity?>(null) }
    var goalContributeTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.GoalEntity?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Wealth", style = MaterialTheme.typography.headlineMedium) }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text("Recorded net wealth", style = MaterialTheme.typography.labelLarge); Text(MaskedPkr(assetTotal - liabilityTotal), style = MaterialTheme.typography.headlineLarge); Text("Assets ${MaskedPkr(assetTotal)} · Liabilities ${MaskedPkr(liabilityTotal)}") } } }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Assets", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showAdd = true }) { Text("Add") } } }
        if (assets.isEmpty()) item { Text("No assets recorded yet.") }
        items(assets, key = { it.id }) { asset -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) { Text(asset.title, style = MaterialTheme.typography.titleMedium); Text("${asset.type} · current ${MaskedPkr(asset.currentEstimatedValueMinor)} · acquired ${MaskedPkr(asset.acquisitionCostMinor)}"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { valuationTarget = asset }) { Text("Update value") }; TextButton(onClick = { disposalTarget = asset }) { Text("Dispose") } } } } }
        item { Text("Liabilities", style = MaterialTheme.typography.titleLarge) }
        if (liabilities.isEmpty()) item { Text("No liabilities recorded yet.") }
        items(liabilities, key = { it.id }) { liability -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) { Text(liability.title, style = MaterialTheme.typography.titleMedium); Text("${liability.type} · outstanding ${MaskedPkr(liability.outstandingAmountMinor)} of ${MaskedPkr(liability.originalAmountMinor)}"); liability.lender?.let { Text("Lender: $it", style = MaterialTheme.typography.bodySmall) }; liability.dueDateEpochDay?.let { Text("Due ${java.time.LocalDate.ofEpochDay(it)}", style = MaterialTheme.typography.bodySmall) }; liability.installmentAmountMinor?.let { Text("Installment ${MaskedPkr(it)}", style = MaterialTheme.typography.bodySmall) }; liability.interestRateBps?.let { Text("Interest ${it / 100.0}%", style = MaterialTheme.typography.bodySmall) }; TextButton(onClick = { liabilityPaymentTarget = liability }) { Text("Record repayment") } } } }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Investments", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showAdd = true }) { Text("Add") } } }
        if (investments.isEmpty()) item { Text("No investment events yet.") }
        val holdings = investments.groupBy { it.securityName }.entries.sortedBy { it.key }
        items(holdings, key = { (security, _) -> "holding-$security" }) { (security, events) ->
            val position = pk.vexel.financepassport.core.model.calculateInvestmentPosition(security, events)
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) {
                Text(security, style = MaterialTheme.typography.titleMedium)
                Text("Held ${position.quantityMinor} · cost basis ${MaskedPkr(position.costBasisMinor)}")
                Text("Realized gain/loss ${MaskedPkr(position.realizedGainLossMinor)} · income (net of withholding) ${MaskedPkr(position.incomeMinor)}", style = MaterialTheme.typography.bodySmall)
                Text("No live market price is used; cost basis is the recorded, traceable investment value.", style = MaterialTheme.typography.bodySmall)
            } }
        }
        item { Text("Investment activity", style = MaterialTheme.typography.titleSmall) }
        items(investments, key = { it.id }) { event -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("${event.securityName} · ${event.investmentAccountId}", style = MaterialTheme.typography.titleMedium); Text("${event.type} · ${MaskedPkr(event.grossAmountMinor)}") } } }
        item { Text("Receivables", style = MaterialTheme.typography.titleLarge) }
        if (receivables.isEmpty()) item { Text("No receivables yet.") }
        items(receivables, key = { it.id }) { value -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) { Text(value.title, style = MaterialTheme.typography.titleMedium); Text("${value.counterparty} · outstanding ${MaskedPkr(value.outstandingAmountMinor)} of ${MaskedPkr(value.originalAmountMinor)}"); value.dueDateEpochDay?.let { Text("Due ${java.time.LocalDate.ofEpochDay(it)}", style = MaterialTheme.typography.bodySmall) }; if (value.outstandingAmountMinor > 0) TextButton(onClick = { receivablePaymentTarget = value }) { Text("Record receipt") } } } }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Goals", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showAdd = true }) { Text("Add") } } }
        if (goalProgress.isEmpty()) item { Text("No goals yet.") }
        items(goalProgress, key = { (goal, _) -> goal.id }) { (goal, progress) ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) {
                Text(goal.title, style = MaterialTheme.typography.titleMedium)
                Text("${goal.goalType} · ${MaskedPkr(goal.currentAmountMinor)} of ${MaskedPkr(goal.targetAmountMinor)}")
                LinearProgressIndicator(progress = { progress.progressPercent / 100f }, modifier = Modifier.fillMaxWidth().testTag("goal-progress-${goal.id}"))
                Text(
                    when {
                        progress.isAchieved -> "Achieved"
                        goal.targetDateEpochDay != null -> "${progress.progressPercent}% · target ${java.time.LocalDate.ofEpochDay(goal.targetDateEpochDay)} · needs ${MaskedPkr(progress.requiredMonthlySavingsMinor ?: 0)}/mo"
                        else -> "${progress.progressPercent}% of target"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!progress.isAchieved) TextButton(onClick = { goalContributeTarget = goal }) { Text("Contribute") }
            } }
        }
    }
    if (showAdd) AddWealthDialog(vm) { showAdd = false }
    valuationTarget?.let { asset -> AmountDialog("Update ${asset.title} valuation", "Current value (PKR)", onDismiss = { valuationTarget = null }) { value -> vm.updateAssetValuation(asset.id, value * 100); valuationTarget = null } }
    disposalTarget?.let { asset -> AmountDialog("Dispose ${asset.title}", "Disposal value (PKR)", onDismiss = { disposalTarget = null }) { value -> vm.disposeAsset(asset.id, value * 100); disposalTarget = null } }
    liabilityPaymentTarget?.let { liability -> AmountDialog("Repay ${liability.title}", "Payment (PKR)", onDismiss = { liabilityPaymentTarget = null }) { value -> vm.recordLiabilityPayment(liability.id, value * 100); liabilityPaymentTarget = null } }
    receivablePaymentTarget?.let { receivable -> AmountDialog("Receipt from ${receivable.counterparty}", "Received (PKR)", onDismiss = { receivablePaymentTarget = null }) { value -> vm.recordReceivablePayment(receivable.id, value * 100); receivablePaymentTarget = null } }
    goalContributeTarget?.let { goal -> AmountDialog("Contribute to ${goal.title}", "Contribution (PKR)", onDismiss = { goalContributeTarget = null }) { value -> vm.contributeToGoal(goal.id, value * 100); goalContributeTarget = null } }
}

@Composable
private fun AddWealthDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var mode by rememberSaveable { mutableStateOf("ASSET") }
    var title by rememberSaveable { mutableStateOf("") }
    var secondary by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var investmentType by rememberSaveable { mutableStateOf("BUY") }
    var investmentAccount by rememberSaveable { mutableStateOf("") }
    var hasDueDate by rememberSaveable { mutableStateOf(false) }
    var dueDate by rememberSaveable { mutableStateOf(java.time.LocalDate.now().plusMonths(1)) }
    var goalType by rememberSaveable { mutableStateOf("CUSTOM") }
    var hasTargetDate by rememberSaveable { mutableStateOf(false) }
    var targetDate by rememberSaveable { mutableStateOf(java.time.LocalDate.now()) }
    var liabilityType by rememberSaveable { mutableStateOf("OTHER") }
    var lender by rememberSaveable { mutableStateOf("") }
    var interestRate by rememberSaveable { mutableStateOf("") }
    var installmentAmount by rememberSaveable { mutableStateOf("") }
    val needsSecondary = mode == "RECEIVABLE"
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add ${mode.lowercase()}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("ASSET", "LIABILITY", "INVESTMENT", "RECEIVABLE", "GOAL").forEach { option -> OutlinedButton(onClick = { mode = option }, modifier = Modifier.testTag("wealth-mode-$option")) { Text(option.take(4)) } } }; OutlinedTextField(title, { title = it }, label = { Text(if (mode == "INVESTMENT") "Security" else "Name") }, singleLine = true, modifier = Modifier.testTag("wealth-name")); if (mode == "INVESTMENT") { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("BUY", "SELL", "DIVIDEND", "PROFIT", "FEE").forEach { option -> OutlinedButton(onClick = { investmentType = option }, modifier = Modifier.testTag("investment-type-$option")) { Text(option.take(4)) } } }; OutlinedTextField(investmentAccount, { investmentAccount = it }, label = { Text("Broker / account (optional)") }, singleLine = true); OutlinedTextField(quantity, { quantity = it.filter(Char::isDigit) }, label = { Text("Quantity (optional)") }, singleLine = true) }; if (mode == "LIABILITY") { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("CREDIT_CARD", "PERSONAL_LOAN", "CAR_FINANCING", "HOME_FINANCING", "INFORMAL", "BUSINESS", "OTHER").forEach { option -> OutlinedButton(onClick = { liabilityType = option }, modifier = Modifier.testTag("liability-type-$option")) { Text(option.take(4)) } } }; OutlinedTextField(lender, { lender = it }, label = { Text("Lender (optional)") }, singleLine = true, modifier = Modifier.testTag("liability-lender")); OutlinedButton(onClick = { hasDueDate = !hasDueDate }) { Text(if (hasDueDate) "Remove due date" else "Set due date + reminder") }; if (hasDueDate) DateField("Due date", dueDate, { dueDate = it }); OutlinedTextField(interestRate, { interestRate = it }, label = { Text("Interest rate % (optional)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.testTag("liability-interest-rate")); AmountField(installmentAmount, { installmentAmount = it }, "Installment amount (optional)", modifier = Modifier.testTag("liability-installment")) }; if (needsSecondary) { OutlinedTextField(secondary, { secondary = it }, label = { Text("Counterparty") }, singleLine = true); OutlinedButton(onClick = { hasDueDate = !hasDueDate }) { Text(if (hasDueDate) "Remove due date" else "Set due date + reminder") }; if (hasDueDate) DateField("Due date", dueDate, { dueDate = it }) }; if (mode == "GOAL") { Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("EMERGENCY_FUND", "PURCHASE", "DEBT_PAYOFF", "CUSTOM").forEach { option -> OutlinedButton(onClick = { goalType = option }, modifier = Modifier.testTag("goal-type-$option")) { Text(option.take(4)) } } } }; AmountField(amount, { amount = it }, "Amount / target (PKR)", modifier = Modifier.testTag("wealth-amount")); if (mode == "GOAL") { OutlinedButton(onClick = { hasTargetDate = !hasTargetDate }) { Text(if (hasTargetDate) "Remove target date" else "Set target date") }; if (hasTargetDate) DateField("Target date", targetDate, { targetDate = it }) } } }, confirmButton = { Button(onClick = { val value = PkrMoneyInput.toMinorUnits(amount, false); when (mode) { "ASSET" -> vm.addAsset(title, value); "LIABILITY" -> vm.addLiability(context, title, liabilityType, value, lender.takeIf { it.isNotBlank() }, dueDate.takeIf { hasDueDate }, interestRate.toDoubleOrNull()?.let { (it * 100).toInt() }, installmentAmount.takeIf { it.isNotBlank() }?.let { PkrMoneyInput.toMinorUnits(it, false) }); "INVESTMENT" -> vm.addInvestmentEvent(title, investmentType, value, quantity.toLongOrNull() ?: 0, investmentAccount.takeIf { it.isNotBlank() } ?: "Manual"); "RECEIVABLE" -> vm.addReceivable(context, title, secondary, value, dueDate.takeIf { hasDueDate }); "GOAL" -> vm.addGoal(title, value, goalType, if (hasTargetDate) targetDate else null) }; onDismiss() }, enabled = title.isNotBlank() && (!needsSecondary || secondary.isNotBlank()) && runCatching { PkrMoneyInput.parseRupees(amount, false) }.isSuccess) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun AmountField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value,
        { PkrMoneyInput.groupedInput(it)?.let(onValueChange) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

@Composable
private fun AmountDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { AmountField(amount, { amount = it }, label) }, confirmButton = { Button(onClick = { onConfirm(PkrMoneyInput.parseRupees(amount, false)) }, enabled = runCatching { PkrMoneyInput.parseRupees(amount, false) }.isSuccess) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun VaultScreen(vm: MainViewModel, application: PassportApplication, padding: PaddingValues) {
    val documents by vm.documents.collectAsState()
    val taxItems by vm.taxItems.collectAsState()
    val accounts by vm.accounts.collectAsState()
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var linkTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.DocumentEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.DocumentEntity?>(null) }
    var deleteDependencyCount by rememberSaveable { mutableStateOf(0) }
    var previewTarget by remember { mutableStateOf<pk.vexel.financepassport.core.database.DocumentEntity?>(null) }
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var previewError by remember { mutableStateOf<String?>(null) }
    var importError by remember { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { pendingUri = it; importError = null }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val visibleDocuments = documents.filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Vault", style = MaterialTheme.typography.headlineMedium); IconButton(onClick = { launcher.launch(arrayOf("application/pdf", "image/jpeg", "image/png", "image/webp")) }) { Icon(Icons.Default.Add, "Import document") } } }
        item { Text("Evidence stays attached to structured records.") }
        item { OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text("Search by title or category") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        if (importError != null) item { Text(importError!!, color = MaterialTheme.colorScheme.error) }
        if (documents.isEmpty()) item { Text("No documents yet. Import a PDF or image to keep local evidence.") }
        else if (visibleDocuments.isEmpty()) item { Text("No documents match \"$searchQuery\".") }
        items(visibleDocuments, key = { it.id }) { document -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text(document.title, style = MaterialTheme.typography.titleMedium); Text("${document.category} · ${document.mimeType}"); Text("SHA-256 ${document.sha256.take(12)}…"); document.expiryDateEpochDay?.let { Text("Expires ${java.time.LocalDate.ofEpochDay(it)}", style = MaterialTheme.typography.bodySmall) }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { previewTarget = document; previewBitmap = null; previewError = null; scope.launch { runCatching { withContext(Dispatchers.IO) { renderDocumentPreview(application, document) }.asImageBitmap() }.onSuccess { previewBitmap = it }.onFailure { previewError = it.message ?: "Preview failed" } } }) { Text("Preview") }; if (taxItems.isNotEmpty() || accounts.isNotEmpty()) TextButton(onClick = { linkTarget = document }) { Text("Link") }; TextButton(onClick = { scope.launch { deleteDependencyCount = vm.documentDependencyCount(document.id); deleteTarget = document } }) { Text("Delete") } } } } }
    }
    pendingUri?.let { uri ->
        ImportDocumentDialog(
            onDismiss = { pendingUri = null },
            onImport = { title, category, expiry ->
                scope.launch {
                    runCatching { DocumentVault(application, application.repository).import(uri, title, category, expiry?.toEpochDay()) }
                        .onSuccess { imported -> expiry?.let { vm.scheduleDocumentExpiry(application, imported.metadata.id, imported.metadata.title, it.toEpochDay()) }; pendingUri = null }
                        .onFailure { importError = it.message ?: "Import failed" }
                }
            },
        )
    }
    linkTarget?.let { document -> LinkDocumentDialog(document, taxItems, accounts, vm) { linkTarget = null } }
    deleteTarget?.let { document ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete encrypted document?") },
            text = { Text(if (deleteDependencyCount > 0) "This document is evidence for $deleteDependencyCount record(s). Deleting it unlinks those records and reverts any evidence they relied on to \"requested\". This cannot be undone." else "This permanently removes the encrypted file and metadata for ${document.title}. It has no linked records.") },
            confirmButton = { Button(onClick = { vm.deleteDocument(document.id); deleteTarget = null }) { Text(if (deleteDependencyCount > 0) "Unlink and delete" else "Delete") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
        )
    }
    previewTarget?.let { document ->
        AlertDialog(onDismissRequest = { previewTarget = null; previewBitmap = null }, title = { Text(document.title) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { previewBitmap?.let { Image(it, contentDescription = "Preview of ${document.title}", modifier = Modifier.fillMaxWidth()) } ?: Text(previewError ?: "Decrypting preview…") } }, confirmButton = { TextButton(onClick = { previewTarget = null; previewBitmap = null }) { Text("Close") } })
    }
}

@Composable
private fun ImportDocumentDialog(onDismiss: () -> Unit, onImport: (String, String, java.time.LocalDate?) -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("Other") }
    var hasExpiry by rememberSaveable { mutableStateOf(false) }
    var expiry by rememberSaveable { mutableStateOf(java.time.LocalDate.now().plusYears(1)) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Save document") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("The selected file will be encrypted into app-private storage.", style = MaterialTheme.typography.bodySmall)
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
        OutlinedTextField(category, { category = it }, label = { Text("Category") }, singleLine = true)
        OutlinedButton(onClick = { hasExpiry = !hasExpiry }) { Text(if (hasExpiry) "Remove expiry date" else "Set expiry date") }
        if (hasExpiry) DateField("Expiry date", expiry, { expiry = it })
    } }, confirmButton = { Button(onClick = { onImport(title.ifBlank { "Imported document" }, category.ifBlank { "Other" }, expiry.takeIf { hasExpiry }) }) { Text("Import") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
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

@Composable private fun LinkDocumentDialog(document: pk.vexel.financepassport.core.database.DocumentEntity, taxItems: List<pk.vexel.financepassport.core.database.TaxItemEntity>, accounts: List<AccountEntity>, vm: MainViewModel, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Link ${document.title}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("The same document can be linked again to another record.", style = MaterialTheme.typography.bodySmall)
        if (taxItems.isNotEmpty()) { Text("Tax items", style = MaterialTheme.typography.labelLarge); taxItems.take(8).forEach { item -> TextButton(onClick = { vm.linkDocument(document.id, "tax_item", item.id); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text(item.description, maxLines = 1) } } }
        if (accounts.isNotEmpty()) { Text("Accounts", style = MaterialTheme.typography.labelLarge); accounts.take(8).forEach { account -> TextButton(onClick = { vm.linkDocument(document.id, "account", account.id); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text(account.name, maxLines = 1) } } }
    } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable private fun AccountCard(account: AccountEntity, vm: MainViewModel, onEdit: () -> Unit, onArchive: () -> Unit) {
    val movement by vm.accountMovement(account.id).collectAsState(0L)
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text(account.name, style = MaterialTheme.typography.titleMedium); Text(listOfNotNull(account.accountType, account.institution).joinToString(" · ")); Text("Current balance ${MaskedPkr(account.openingBalanceMinor + movement)}", style = MaterialTheme.typography.titleLarge); Text("Opening balance ${MaskedPkr(account.openingBalanceMinor)}"); account.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }; Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = onEdit) { Text("Edit") }; TextButton(onClick = onArchive) { Text("Archive") } } } }
}

@Composable private fun AddAccountDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }; var amount by rememberSaveable { mutableStateOf("") }
    var institution by rememberSaveable { mutableStateOf("") }; var notes by rememberSaveable { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add account") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Account name") }, singleLine = true, modifier = Modifier.testTag("account-name")); OutlinedTextField(institution, { institution = it }, label = { Text("Institution (optional)") }, singleLine = true, modifier = Modifier.testTag("account-institution")); AmountField(amount, { amount = it }, "Opening balance (PKR)", modifier = Modifier.testTag("account-amount")); OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, singleLine = true, modifier = Modifier.testTag("account-notes")) } }, confirmButton = { Button(onClick = { vm.addAccount(name, "OTHER", PkrMoneyInput.toMinorUnits(amount), institution.takeIf { it.isNotBlank() }, notes.takeIf { it.isNotBlank() }); onDismiss() }, enabled = name.isNotBlank() && runCatching { PkrMoneyInput.parseRupees(amount) }.isSuccess) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun EditAccountDialog(account: AccountEntity, vm: MainViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(account.name) }; var amount by rememberSaveable { mutableStateOf((account.openingBalanceMinor / 100).toString()) }
    var institution by rememberSaveable { mutableStateOf(account.institution.orEmpty()) }; var notes by rememberSaveable { mutableStateOf(account.notes.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit account") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Account name") }, singleLine = true); OutlinedTextField(institution, { institution = it }, label = { Text("Institution (optional)") }, singleLine = true); AmountField(amount, { amount = it }, "Opening balance (PKR)"); OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, singleLine = true) } }, confirmButton = { Button(onClick = { vm.updateAccount(account.id, name, PkrMoneyInput.toMinorUnits(amount), institution.takeIf { it.isNotBlank() }, notes.takeIf { it.isNotBlank() }); onDismiss() }, enabled = name.isNotBlank() && runCatching { PkrMoneyInput.parseRupees(amount) }.isSuccess) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun AddEventDialog(vm: MainViewModel, accounts: List<AccountEntity>, onDismiss: () -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }; var description by rememberSaveable { mutableStateOf("") }; var category by rememberSaveable { mutableStateOf("") }; var income by rememberSaveable { mutableStateOf(true) }; var accountId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(java.time.LocalDate.now()) }
    val recentEvents by vm.recentEvents.collectAsState()
    val categorySuggestions = remember(recentEvents) { recentEvents.mapNotNull { it.category }.filter { it.isNotBlank() }.distinct().take(8) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (income) "Record income" else "Record expense") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ income = true }) { Text("Income") }; OutlinedButton({ income = false }) { Text("Expense") } }; AccountPicker("Account", accounts, accountId) { accountId = it }; DateField("Date", date, { date = it }, testTag = "money-event-date"); AmountField(amount, { amount = it }, "Amount (PKR)", modifier = Modifier.testTag("money-event-amount")); OutlinedTextField(description, { description = it }, label = { Text("What happened? (optional)") }, singleLine = true, modifier = Modifier.testTag("money-event-description")); OutlinedTextField(category, { category = it }, label = { Text("Category (optional)") }, singleLine = true, modifier = Modifier.testTag("money-event-category")); if (categorySuggestions.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) { categorySuggestions.forEach { suggestion -> OutlinedButton(onClick = { category = suggestion }, modifier = Modifier.testTag("category-chip-$suggestion")) { Text(suggestion) } } } } }, confirmButton = { Button(onClick = { vm.addEvent(if (income) FinancialEventType.INCOME else FinancialEventType.EXPENSE, PkrMoneyInput.toMinorUnits(amount, false), accountId, description, category, date); onDismiss() }, enabled = accountId.isNotBlank() && runCatching { PkrMoneyInput.parseRupees(amount, false) }.isSuccess) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun RecurringItemDialog(vm: MainViewModel, application: PassportApplication, accounts: List<AccountEntity>, onDismiss: () -> Unit) {
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var frequency by rememberSaveable { mutableStateOf("MONTHLY") }
    var delayDays by rememberSaveable { mutableStateOf("1") }
    var income by rememberSaveable { mutableStateOf(true) }
    var accountId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add recurring draft") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ income = true }) { Text("Income") }; OutlinedButton({ income = false }) { Text("Expense") } }
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.testTag("recurring-title"))
            AccountPicker("Account", accounts, accountId) { accountId = it }
            AmountField(amount, { amount = it }, "Amount (PKR)", modifier = Modifier.testTag("recurring-amount"))
            OutlinedTextField(category, { category = it }, label = { Text("Category (optional)") }, singleLine = true, modifier = Modifier.testTag("recurring-category"))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY").forEach { option -> OutlinedButton({ frequency = option }) { Text(option.take(3)) } } }
            OutlinedTextField(delayDays, { delayDays = it.filter(Char::isDigit) }, label = { Text("First reminder in days") }, singleLine = true, modifier = Modifier.testTag("recurring-delay"))
            Text("This creates a reminder/draft only; it never silently records a financial event.", style = MaterialTheme.typography.bodySmall)
        } },
        confirmButton = { Button(onClick = { vm.addRecurringItem(application, title, if (income) FinancialEventType.INCOME else FinancialEventType.EXPENSE, PkrMoneyInput.toMinorUnits(amount, false), accountId, category, frequency, delayDays.toLong()); onDismiss() }, enabled = title.isNotBlank() && accountId.isNotBlank() && runCatching { PkrMoneyInput.parseRupees(amount, false) }.isSuccess && delayDays.toLongOrNull()?.let { it > 0 } == true) { Text("Save draft") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable private fun TransferDialog(vm: MainViewModel, accounts: List<AccountEntity>, onDismiss: () -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }; var description by rememberSaveable { mutableStateOf("") }; var sourceId by rememberSaveable { mutableStateOf(accounts.getOrNull(0)?.id.orEmpty()) }; var destinationId by rememberSaveable { mutableStateOf(accounts.getOrNull(1)?.id.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(java.time.LocalDate.now()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Transfer between accounts") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { AccountPicker("From account", accounts, sourceId) { sourceId = it }; AccountPicker("To account", accounts, destinationId) { destinationId = it }; DateField("Date", date, { date = it }); AmountField(amount, { amount = it }, "Amount (PKR)"); OutlinedTextField(description, { description = it }, label = { Text("Reason") }, singleLine = true) } }, confirmButton = { Button(onClick = { vm.transfer(sourceId, destinationId, PkrMoneyInput.toMinorUnits(amount, false), description, date); onDismiss() }, enabled = sourceId.isNotBlank() && destinationId.isNotBlank() && sourceId != destinationId && runCatching { PkrMoneyInput.parseRupees(amount, false) }.isSuccess && description.isNotBlank()) { Text("Transfer") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable private fun EmptyModuleScreen(label: String, padding: PaddingValues) { Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), Arrangement.spacedBy(12.dp)) { Text(label, style = MaterialTheme.typography.headlineMedium); Text("This workspace is next in the build sequence. Your existing local records are preserved.") } }
private fun formatPkr(minor: Long): String = PkrMoneyInput.formatMinorUnits(minor)

/** Privacy-aware amount rendering: shows masked placeholders instead of the value while [LocalPrivacyMode] is enabled. */
@Composable
private fun MaskedPkr(minor: Long): String = if (LocalPrivacyMode.current) "PKR ••••••" else formatPkr(minor)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPicker(label: String, accounts: List<AccountEntity>, selectedId: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = accounts.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(selected?.name.orEmpty(), {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account -> DropdownMenuItem(text = { Text(account.name) }, onClick = { onSelected(account.id); expanded = false }) }
        }
    }
}
