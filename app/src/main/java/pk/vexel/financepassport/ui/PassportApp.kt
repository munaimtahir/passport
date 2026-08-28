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
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.LaunchedEffect
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
import pk.vexel.financepassport.core.database.UtilityBillProfileEntity
import pk.vexel.financepassport.core.database.MonthlyBillOccurrenceEntity
import pk.vexel.financepassport.core.database.PaymentRecordEntity
import pk.vexel.financepassport.core.database.BillAttachmentEntity
import pk.vexel.financepassport.core.database.UtilityRecurrenceEngine
import pk.vexel.financepassport.core.model.FinancialEventType
import pk.vexel.financepassport.core.model.PkrMoneyInput
import pk.vexel.financepassport.core.files.DocumentVault
import pk.vexel.financepassport.core.security.LiveRestoreService
import pk.vexel.financepassport.ui.theme.PassportTheme
import java.util.UUID
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private data class Destination(val label: String, val icon: ImageVector)
private val destinations = listOf(
    Destination("Home", Icons.Default.Home),
    Destination("Bills", Icons.Default.Description),
    Destination("History", Icons.Default.Folder),
)

/** Whether monetary values should render masked; toggled from the top app bar and persisted in [pk.vexel.financepassport.core.security.AppPreferences]. */
val LocalPrivacyMode = compositionLocalOf { false }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassportApp() {
    val application = LocalContext.current.applicationContext as PassportApplication
    val vm: MainViewModel = viewModel(factory = MainViewModelFactory(application.repository, application.preferences))
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var showAddBill by rememberSaveable { mutableStateOf(false) }
    var showMore by rememberSaveable { mutableStateOf(false) }
    CompositionLocalProvider(LocalPrivacyMode provides vm.privacyModeEnabled) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Utility Bill Tracker") }, actions = {
                    IconButton(onClick = vm::togglePrivacyMode) {
                        Icon(
                            if (vm.privacyModeEnabled) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (vm.privacyModeEnabled) "Show amounts" else "Hide amounts",
                        )
                    }
                    IconButton(onClick = { showMore = true }) { Icon(Icons.Default.MoreHoriz, "Settings") }
                })
            },
            floatingActionButton = {
                if (selected == 1) {
                    FloatingActionButton(onClick = { showAddBill = true }, modifier = Modifier.testTag("add-bill-fab")) {
                        Icon(Icons.Default.Add, "Add Bill")
                    }
                }
            },
            bottomBar = { NavigationBar { destinations.forEachIndexed { index, destination -> NavigationBarItem(selected == index, { selected = index }, icon = { Icon(destination.icon, destination.label) }, label = { Text(destination.label) }) } } },
        ) { padding ->
            when (selected) {
                0 -> HomeScreen(vm, application, padding) { selected = it }
                1 -> BillsScreen(vm, application, padding)
                2 -> HistoryScreen(vm, application, padding)
                else -> EmptyModuleScreen(destinations[selected].label, padding)
            }
        }
        if (showAddBill) AddBillDialog(vm, application) { showAddBill = false }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings & Local Data") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("more-dialog-scroll"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Offline local backup and data controls")
                status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

                Button(
                    onClick = { backupPassword = true },
                    modifier = Modifier.fillMaxWidth().testTag("backup-button")
                ) {
                    Text("Create Encrypted Backup")
                }

                Button(
                    onClick = { backupPicker.launch(arrayOf("application/octet-stream", "application/zip", "application/octet-stream")) },
                    modifier = Modifier.fillMaxWidth().testTag("restore-button")
                ) {
                    Text("Restore Encrypted Backup")
                }

                Button(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth().testTag("delete-all-button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All Application Data")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
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
    val profiles by vm.utilityProfiles.collectAsState()
    val occurrences by vm.monthlyOccurrences.collectAsState()
    val today = remember { java.time.LocalDate.now() }

    val unpaidObligations = remember(occurrences, today) {
        occurrences.filter {
            it.status in listOf("Pending", "Due soon", "Overdue") &&
                    !today.isBefore(java.time.LocalDate.ofEpochDay(it.expectedIssueDateEpochDay))
        }.sortedWith(
            compareBy<MonthlyBillOccurrenceEntity> {
                // Priority sorting: Overdue (0), Due soon (1), Pending (2)
                when (it.status) {
                    "Overdue" -> 0
                    "Due soon" -> 1
                    else -> 2
                }
            }.thenBy { it.expectedDueDateEpochDay }
        )
    }

    val overdueBills = remember(occurrences) {
        occurrences.filter { it.status == "Overdue" }
    }

    val paidBillsThisMonth = remember(occurrences, today) {
        occurrences.filter {
            it.status == "Paid" &&
                    it.billingYear == today.year &&
                    it.billingMonth == today.monthValue
        }
    }

    var totalPaidThisMonth by remember { mutableStateOf(0L) }
    LaunchedEffect(occurrences, today) {
        var total = 0L
        val currentMonthStartEpoch = java.time.LocalDate.of(today.year, today.monthValue, 1).toEpochDay()
        val currentMonthEndEpoch = java.time.LocalDate.of(today.year, today.monthValue, today.lengthOfMonth()).toEpochDay()
        for (occ in occurrences) {
            val payment = vm.getPaymentForOccurrence(occ.id)
            if (payment != null && payment.paymentDateEpochDay in currentMonthStartEpoch..currentMonthEndEpoch) {
                total += payment.amountPaidMinor
            }
        }
        totalPaidThisMonth = total
    }

    var selectedOccurrenceForDetails by remember { mutableStateOf<MonthlyBillOccurrenceEntity?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium)

        if (profiles.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(Modifier.padding(24.dp), Arrangement.spacedBy(12.dp), Alignment.CenterHorizontally) {
                    Text("Welcome to Vexel Passport!", style = MaterialTheme.typography.titleMedium)
                    Text("Register your monthly utility bills to automatically track occurrences, outstanding payments, and due dates.", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { onNavigate(1) }, modifier = Modifier.testTag("add-first-bill-cta")) {
                        Text("Add Your First Bill")
                    }
                }
            }
        } else {
            // Metrics grid
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Unpaid Bills", style = MaterialTheme.typography.labelMedium)
                        Text("${unpaidObligations.size}", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Overdue", style = MaterialTheme.typography.labelMedium, color = if (overdueBills.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        Text("${overdueBills.size}", style = MaterialTheme.typography.headlineSmall, color = if (overdueBills.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Paid This Month", style = MaterialTheme.typography.labelMedium)
                        Text("${paidBillsThisMonth.size}", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Card(Modifier.weight(1f)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Total Paid This Month", style = MaterialTheme.typography.labelMedium)
                        Text(formatPkr(totalPaidThisMonth), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }

            Text("Bills Requiring Attention", style = MaterialTheme.typography.titleMedium)
            if (unpaidObligations.isEmpty()) {
                Card(Modifier.fillMaxWidth()) {
                    Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No pending obligations. All bills are up to date!", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    unpaidObligations.forEach { occ ->
                        val profile = profiles.find { it.id == occ.profileId }
                        if (profile != null) {
                            val monthLabel = java.time.YearMonth.of(occ.billingYear, occ.billingMonth).format(java.time.format.DateTimeFormatter.ofPattern("MMM yyyy"))
                            val dueDateStr = java.time.LocalDate.ofEpochDay(occ.expectedDueDateEpochDay).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
                            Card(
                                onClick = { selectedOccurrenceForDetails = occ },
                                modifier = Modifier.fillMaxWidth().testTag("pending-card-${occ.id}")
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CategoryIcon(profile.category, modifier = Modifier.size(32.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("${profile.name} ($monthLabel)", style = MaterialTheme.typography.titleMedium)
                                        Text("Due: $dueDateStr", style = MaterialTheme.typography.bodyMedium)
                                        Text("Ref: ${maskReferenceNumber(profile.referenceNumber)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        StatusChip(occ.status)
                                        Text(
                                            if (occ.amountMinor != null) formatPkr(occ.amountMinor) else "Amount TBD",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedOccurrenceForDetails?.let { occ ->
        MonthlyOccurrenceDetailsDialog(occ, vm, application, onDismiss = { selectedOccurrenceForDetails = null })
    }
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
    val incomeSources by vm.incomeSources.collectAsState()
    val incomeBySource = remember(recentEvents, incomeSources) {
        recentEvents.filter { it.eventType == "INCOME" }
            .groupBy { event -> incomeSources.firstOrNull { it.id == event.incomeSourceId }?.name ?: "Unassigned" }
            .mapValues { (_, events) -> events.sumOf { it.amountMinor } }
            .entries.sortedByDescending { it.value }
    }
    var showEvent by rememberSaveable { mutableStateOf(false) }
    var showTransfer by rememberSaveable { mutableStateOf(false) }
    var showRecurring by rememberSaveable { mutableStateOf(false) }
    var showAddAccount by rememberSaveable { mutableStateOf(false) }
    var editAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var activityFilterType by rememberSaveable { mutableStateOf("ALL") }
    var activityDateRangeEnabled by rememberSaveable { mutableStateOf(false) }
    var activityDateFrom by rememberSaveable { mutableStateOf(java.time.LocalDate.now().minusMonths(1)) }
    var activityDateTo by rememberSaveable { mutableStateOf(java.time.LocalDate.now()) }
    val filteredEvents = remember(recentEvents, activityFilterType, activityDateRangeEnabled, activityDateFrom, activityDateTo) {
        recentEvents.filter { event ->
            (activityFilterType == "ALL" || event.eventType == activityFilterType) &&
                (!activityDateRangeEnabled || event.dateEpochDay in activityDateFrom.toEpochDay()..activityDateTo.toEpochDay())
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("money-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Money", style = MaterialTheme.typography.headlineMedium) }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Accounts", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showAddAccount = true }, modifier = Modifier.testTag("add-account")) { Text("Add account") } } }
        if (accounts.isEmpty()) item { Text("No accounts yet. Use + to add cash or a bank account.") }
        items(accounts, key = { it.id }) { account -> AccountCard(account, vm, onEdit = { editAccount = account }, onArchive = { vm.archiveAccount(account.id) }) }
        item { Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) { Button(onClick = { showEvent = true }, enabled = accounts.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Income / expense") }; Button(onClick = { showTransfer = true }, enabled = accounts.size >= 2, modifier = Modifier.weight(1f)) { Text("Transfer") } } }
        item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Bills & Recurring", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showRecurring = true }, enabled = accounts.isNotEmpty(), modifier = Modifier.testTag("add-recurring")) { Text("Add") } } }
        if (recurringItems.isEmpty()) item { Text("No bills or recurring items yet. Add one to receive a reminder without silently creating a confirmed event.") }
        items(recurringItems, key = { it.id }) { recurring -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text(recurring.title, style = MaterialTheme.typography.titleMedium); Text(listOfNotNull(recurring.eventType, recurring.category).joinToString(" · ") + " · ${MaskedPkr(recurring.amountMinor)} · ${recurring.frequency}"); Text("Next due: ${java.time.LocalDate.ofEpochDay(recurring.nextDueDateEpochDay)}"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { vm.confirmRecurringItemNow(application, recurring.id) }, modifier = Modifier.testTag("mark-paid-${recurring.id}")) { Text("Mark paid") }; TextButton(onClick = { vm.pauseRecurringItem(application, recurring.id) }) { Text("Pause") } } } } }
        if (incomeBySource.isNotEmpty()) item { Text("Income by source", style = MaterialTheme.typography.titleLarge) }
        items(incomeBySource, key = { (source, _) -> "income-source-$source" }) { (source, totalMinor) -> Card(Modifier.fillMaxWidth().testTag("income-by-source-row")) { Row(Modifier.padding(16.dp), Arrangement.SpaceBetween) { Text(source); Text(MaskedPkr(totalMinor)) } } }
        item { Text("Activity", style = MaterialTheme.typography.titleLarge) }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("activity-filter-bar"), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL", "INCOME", "EXPENSE", "TRANSFER").forEach { type ->
                    FilterChip(
                        selected = activityFilterType == type,
                        onClick = { activityFilterType = type },
                        label = { Text(type) },
                        modifier = Modifier.testTag("activity-filter-$type"),
                    )
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth()) {
                TextButton(onClick = { activityDateRangeEnabled = !activityDateRangeEnabled }, modifier = Modifier.testTag("activity-date-range-toggle")) {
                    Text(if (activityDateRangeEnabled) "Remove date range" else "Filter by date range")
                }
                if (activityDateRangeEnabled) {
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                        DateField("From", activityDateFrom, { activityDateFrom = it }, modifier = Modifier.weight(1f), testTag = "activity-date-from")
                        DateField("To", activityDateTo, { activityDateTo = it }, modifier = Modifier.weight(1f), testTag = "activity-date-to")
                    }
                }
            }
        }
        if (filteredEvents.isEmpty()) item { Text("No activity matches these filters.") }
        items(filteredEvents, key = { it.id }) { event -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), Arrangement.SpaceBetween) { Column { Text(event.description); Text(listOfNotNull(event.eventType, event.category).joinToString(" · "), style = MaterialTheme.typography.labelSmall) }; Text(MaskedPkr(event.amountMinor)) } } }
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
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabModes = listOf("ASSET", "INVESTMENT", "LIABILITY", "RECEIVABLE", "GOAL")
    val tabLabels = listOf("Assets", "Investments", "Liabilities", "Receivables", "Goals")
    Column(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.padding(20.dp), Arrangement.spacedBy(12.dp)) {
            Text("Wealth", style = MaterialTheme.typography.headlineMedium)
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) { Text("Recorded net wealth", style = MaterialTheme.typography.labelLarge); Text(MaskedPkr(assetTotal - liabilityTotal), style = MaterialTheme.typography.headlineLarge); Text("Assets ${MaskedPkr(assetTotal)} · Liabilities ${MaskedPkr(liabilityTotal)}") } }
        }
        SecondaryScrollableTabRow(selectedTabIndex = selectedTab, modifier = Modifier.testTag("wealth-tabs")) {
            tabLabels.forEachIndexed { index, label -> Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label) }, modifier = Modifier.testTag("wealth-tab-${tabModes[index]}")) }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (selectedTab) {
                0 -> {
                    item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Assets", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showAdd = true }) { Text("Add") } } }
                    if (assets.isEmpty()) item { Text("No assets recorded yet.") }
                    items(assets, key = { it.id }) { asset -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) { Text(asset.title, style = MaterialTheme.typography.titleMedium); Text("${asset.type} · current ${MaskedPkr(asset.currentEstimatedValueMinor)} · acquired ${MaskedPkr(asset.acquisitionCostMinor)}"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { valuationTarget = asset }) { Text("Update value") }; TextButton(onClick = { disposalTarget = asset }) { Text("Dispose") } } } } }
                }
                1 -> {
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
                }
                2 -> {
                    item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Liabilities", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showAdd = true }) { Text("Add") } } }
                    if (liabilities.isEmpty()) item { Text("No liabilities recorded yet.") }
                    items(liabilities, key = { it.id }) { liability -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) { Text(liability.title, style = MaterialTheme.typography.titleMedium); Text("${liability.type} · outstanding ${MaskedPkr(liability.outstandingAmountMinor)} of ${MaskedPkr(liability.originalAmountMinor)}"); liability.lender?.let { Text("Lender: $it", style = MaterialTheme.typography.bodySmall) }; liability.dueDateEpochDay?.let { Text("Due ${java.time.LocalDate.ofEpochDay(it)}", style = MaterialTheme.typography.bodySmall) }; liability.installmentAmountMinor?.let { Text("Installment ${MaskedPkr(it)}", style = MaterialTheme.typography.bodySmall) }; liability.interestRateBps?.let { Text("Interest ${it / 100.0}%", style = MaterialTheme.typography.bodySmall) }; TextButton(onClick = { liabilityPaymentTarget = liability }) { Text("Record repayment") } } } }
                }
                3 -> {
                    item { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text("Receivables", style = MaterialTheme.typography.titleLarge); OutlinedButton(onClick = { showAdd = true }) { Text("Add") } } }
                    if (receivables.isEmpty()) item { Text("No receivables yet.") }
                    items(receivables, key = { it.id }) { value -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) { Text(value.title, style = MaterialTheme.typography.titleMedium); Text("${value.counterparty} · outstanding ${MaskedPkr(value.outstandingAmountMinor)} of ${MaskedPkr(value.originalAmountMinor)}"); value.dueDateEpochDay?.let { Text("Due ${java.time.LocalDate.ofEpochDay(it)}", style = MaterialTheme.typography.bodySmall) }; if (value.outstandingAmountMinor > 0) TextButton(onClick = { receivablePaymentTarget = value }) { Text("Record receipt") } } } }
                }
                4 -> {
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
            }
        }
    }
    if (showAdd) AddWealthDialog(vm, initialMode = tabModes[selectedTab]) { showAdd = false }
    valuationTarget?.let { asset -> AmountDialog("Update ${asset.title} valuation", "Current value (PKR)", onDismiss = { valuationTarget = null }) { value -> vm.updateAssetValuation(asset.id, value * 100); valuationTarget = null } }
    disposalTarget?.let { asset -> AmountDialog("Dispose ${asset.title}", "Disposal value (PKR)", onDismiss = { disposalTarget = null }) { value -> vm.disposeAsset(asset.id, value * 100); disposalTarget = null } }
    liabilityPaymentTarget?.let { liability -> AmountDialog("Repay ${liability.title}", "Payment (PKR)", onDismiss = { liabilityPaymentTarget = null }) { value -> vm.recordLiabilityPayment(liability.id, value * 100); liabilityPaymentTarget = null } }
    receivablePaymentTarget?.let { receivable -> AmountDialog("Receipt from ${receivable.counterparty}", "Received (PKR)", onDismiss = { receivablePaymentTarget = null }) { value -> vm.recordReceivablePayment(receivable.id, value * 100); receivablePaymentTarget = null } }
    goalContributeTarget?.let { goal -> AmountDialog("Contribute to ${goal.title}", "Contribution (PKR)", onDismiss = { goalContributeTarget = null }) { value -> vm.contributeToGoal(goal.id, value * 100); goalContributeTarget = null } }
}

@Composable
private fun AddWealthDialog(vm: MainViewModel, initialMode: String = "ASSET", onDismiss: () -> Unit) {
    val context = LocalContext.current
    // Keyed on initialMode: without this, rememberSaveable restores whatever mode this dialog was
    // left on the last time it was open within this Activity session (e.g. closed while on
    // LIABILITY from a previous tab), silently ignoring a freshly-passed initialMode from a
    // different tab's Add button.
    var mode by rememberSaveable(initialMode) { mutableStateOf(initialMode) }
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

internal fun renderAttachmentPreview(context: android.content.Context, vm: MainViewModel, attachment: BillAttachmentEntity): Bitmap {
    val bytes = vm.decryptAttachment(context, attachment)
    if (attachment.mimeType != "application/pdf") return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("Image preview could not be decoded")
    val temporary = java.io.File.createTempFile("passport-attachment-preview-", ".pdf", context.cacheDir)
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
    var incomeSourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNewIncomeSource by rememberSaveable { mutableStateOf(false) }
    var newIncomeSourceName by rememberSaveable { mutableStateOf("") }
    val recentEvents by vm.recentEvents.collectAsState()
    val incomeSources by vm.incomeSources.collectAsState()
    val categorySuggestions = remember(recentEvents) { recentEvents.mapNotNull { it.category }.filter { it.isNotBlank() }.distinct().take(8) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (income) "Record income" else "Record expense") }, text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ income = true }) { Text("Income") }; OutlinedButton({ income = false }) { Text("Expense") } }; AccountPicker("Account", accounts, accountId) { accountId = it }; DateField("Date", date, { date = it }, testTag = "money-event-date"); AmountField(amount, { amount = it }, "Amount (PKR)", modifier = Modifier.testTag("money-event-amount")); OutlinedTextField(description, { description = it }, label = { Text("What happened? (optional)") }, singleLine = true, modifier = Modifier.testTag("money-event-description")); OutlinedTextField(category, { category = it }, label = { Text("Category (optional)") }, singleLine = true, modifier = Modifier.testTag("money-event-category")); if (categorySuggestions.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) { categorySuggestions.forEach { suggestion -> OutlinedButton(onClick = { category = suggestion }, modifier = Modifier.testTag("category-chip-$suggestion")) { Text(suggestion) } } }; if (income) { IncomeSourcePicker(incomeSources, incomeSourceId) { incomeSourceId = it }; if (showNewIncomeSource) { OutlinedTextField(newIncomeSourceName, { newIncomeSourceName = it }, label = { Text("New source name") }, singleLine = true, modifier = Modifier.testTag("new-income-source-name")); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { vm.addIncomeSource(newIncomeSourceName, "OTHER"); showNewIncomeSource = false; newIncomeSourceName = "" }, enabled = newIncomeSourceName.isNotBlank(), modifier = Modifier.testTag("save-new-income-source")) { Text("Add source") }; TextButton(onClick = { showNewIncomeSource = false; newIncomeSourceName = "" }) { Text("Cancel") } } } else TextButton(onClick = { showNewIncomeSource = true }, modifier = Modifier.testTag("add-new-income-source")) { Text("+ New income source") } } } }, confirmButton = { Button(onClick = { vm.addEvent(if (income) FinancialEventType.INCOME else FinancialEventType.EXPENSE, PkrMoneyInput.toMinorUnits(amount, false), accountId, description, category, date, if (income) incomeSourceId else null); onDismiss() }, enabled = accountId.isNotBlank() && runCatching { PkrMoneyInput.parseRupees(amount, false) }.isSuccess) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
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
            if (!income) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("Electricity", "Gas", "Water", "Internet", "Rent", "Subscription", "Other").forEach { option -> OutlinedButton(onClick = { category = option }, modifier = Modifier.testTag("bill-category-$option")) { Text(option.take(4)) } } }
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
private fun IncomeSourcePicker(sources: List<pk.vexel.financepassport.core.database.IncomeSourceEntity>, selectedId: String?, onSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = sources.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(selected?.name.orEmpty(), {}, readOnly = true, label = { Text("Income source (optional)") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth().testTag("income-source-picker"))
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onSelected(null); expanded = false }, modifier = Modifier.testTag("income-source-none"))
            sources.forEach { source -> DropdownMenuItem(text = { Text(source.name) }, onClick = { onSelected(source.id); expanded = false }, modifier = Modifier.testTag("income-source-${source.id}")) }
        }
    }
}

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

private fun getProfileStatusAndDueDate(profile: UtilityBillProfileEntity, occurrences: List<MonthlyBillOccurrenceEntity>): Pair<String, String> {
    val profileOccurrences = occurrences.filter { it.profileId == profile.id }
    if (profileOccurrences.isEmpty()) return "No bills" to "-"
    val today = java.time.LocalDate.now()
    val currentMonthOcc = profileOccurrences.find { it.billingYear == today.year && it.billingMonth == today.monthValue }
    if (currentMonthOcc != null) {
        val dueDateStr = java.time.LocalDate.ofEpochDay(currentMonthOcc.expectedDueDateEpochDay).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
        return currentMonthOcc.status to dueDateStr
    }
    val nextOcc = profileOccurrences.sortedBy { it.expectedDueDateEpochDay }.firstOrNull { it.status != "Paid" && it.status != "Skipped" }
        ?: profileOccurrences.maxByOrNull { it.billingYear * 12 + it.billingMonth }
    if (nextOcc != null) {
        val dueDateStr = java.time.LocalDate.ofEpochDay(nextOcc.expectedDueDateEpochDay).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
        return nextOcc.status to dueDateStr
    }
    return "-" to "-"
}

private fun maskReferenceNumber(ref: String): String {
    if (ref.length <= 4) return ref
    return "••••" + ref.takeLast(4)
}

@Composable
private fun CategoryIcon(category: String, modifier: Modifier = Modifier) {
    val icon = when (category) {
        "Electricity" -> Icons.Filled.FlashOn
        "Telephone" -> Icons.Filled.Phone
        "Gas" -> Icons.Filled.Star
        else -> Icons.Filled.Description
    }
    Icon(icon, contentDescription = category, modifier = modifier)
}

@Composable
private fun StatusChip(status: String) {
    val containerColor = when (status) {
        "Overdue" -> MaterialTheme.colorScheme.errorContainer
        "Due soon" -> MaterialTheme.colorScheme.tertiaryContainer
        "Paid" -> MaterialTheme.colorScheme.primaryContainer
        "Pending" -> MaterialTheme.colorScheme.secondaryContainer
        "Expected" -> MaterialTheme.colorScheme.surfaceVariant
        "Skipped" -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (status) {
        "Overdue" -> MaterialTheme.colorScheme.onErrorContainer
        "Due soon" -> MaterialTheme.colorScheme.onTertiaryContainer
        "Paid" -> MaterialTheme.colorScheme.onPrimaryContainer
        "Pending" -> MaterialTheme.colorScheme.onSecondaryContainer
        "Expected" -> MaterialTheme.colorScheme.onSurfaceVariant
        "Skipped" -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillsScreen(vm: MainViewModel, application: PassportApplication, padding: PaddingValues) {
    val profiles by vm.utilityProfiles.collectAsState()
    val occurrences by vm.monthlyOccurrences.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("All") }
    var selectedStatusFilter by rememberSaveable { mutableStateOf("Active") }
    var selectedProfileForDetails by remember { mutableStateOf<UtilityBillProfileEntity?>(null) }

    val filteredProfiles = remember(profiles, searchQuery, selectedCategoryFilter, selectedStatusFilter) {
        profiles.filter { profile ->
            val matchesSearch = profile.name.contains(searchQuery, ignoreCase = true) ||
                    profile.referenceNumber.contains(searchQuery, ignoreCase = true) ||
                    (profile.provider ?: "").contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == "All" || profile.category == selectedCategoryFilter
            val matchesStatus = when (selectedStatusFilter) {
                "Active" -> profile.status == "ACTIVE"
                "Archived" -> profile.status == "ARCHIVED"
                else -> true
            }
            matchesSearch && matchesCategory && matchesStatus
        }
    }

    val groupedProfiles = remember(filteredProfiles) {
        filteredProfiles.groupBy { it.category }
    }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Utility Connections", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by name, provider, or reference") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("bills-search")
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Active", "Archived", "All").forEach { statusOpt ->
                    FilterChip(
                        selected = selectedStatusFilter == statusOpt,
                        onClick = { selectedStatusFilter = statusOpt },
                        label = { Text(statusOpt) },
                        modifier = Modifier.testTag("filter-status-$statusOpt")
                    )
                }
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Electricity", "Gas", "Telephone", "Other").forEach { catOpt ->
                    FilterChip(
                        selected = selectedCategoryFilter == catOpt,
                        onClick = { selectedCategoryFilter = catOpt },
                        label = { Text(catOpt) },
                        modifier = Modifier.testTag("filter-category-$catOpt")
                    )
                }
            }
            if (filteredProfiles.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No utility bills found. Tap + to add one.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    groupedProfiles.forEach { (category, categoryProfiles) ->
                        item {
                            Text(category, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        items(categoryProfiles, key = { it.id }) { profile ->
                            val (currentStatus, nextDueDate) = remember(profile, occurrences) {
                                getProfileStatusAndDueDate(profile, occurrences)
                            }
                            Card(
                                onClick = { selectedProfileForDetails = profile },
                                modifier = Modifier.fillMaxWidth().testTag("profile-card-${profile.id}")
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CategoryIcon(profile.category, modifier = Modifier.size(32.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(profile.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${profile.provider ?: "Unknown"} · ${profile.locationLabel ?: "Home"}", style = MaterialTheme.typography.bodyMedium)
                                        Text("Ref: ${maskReferenceNumber(profile.referenceNumber)}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        StatusChip(currentStatus)
                                        Text("Due: $nextDueDate", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    selectedProfileForDetails?.let { profile ->
        UtilityProfileDetailsDialog(profile, vm, application, onDismiss = { selectedProfileForDetails = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(vm: MainViewModel, application: PassportApplication, padding: PaddingValues) {
    val profiles by vm.utilityProfiles.collectAsState()
    val occurrences by vm.monthlyOccurrences.collectAsState()
    val context = LocalContext.current

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatus by rememberSaveable { mutableStateOf("All") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var selectedYear by rememberSaveable { mutableStateOf("All") }
    var selectedPaymentMode by rememberSaveable { mutableStateOf("All") }

    var selectedOccurrenceForDetails by remember { mutableStateOf<MonthlyBillOccurrenceEntity?>(null) }

    var paymentsMap by remember { mutableStateOf<Map<String, PaymentRecordEntity>>(emptyMap()) }
    LaunchedEffect(occurrences) {
        val map = mutableMapOf<String, PaymentRecordEntity>()
        for (occ in occurrences) {
            val pay = vm.getPaymentForOccurrence(occ.id)
            if (pay != null) {
                map[occ.id] = pay
            }
        }
        paymentsMap = map
    }

    val filteredOccurrences = remember(occurrences, profiles, paymentsMap, searchQuery, selectedStatus, selectedCategory, selectedYear, selectedPaymentMode) {
        occurrences.filter { occ ->
            val profile = profiles.find { it.id == occ.profileId } ?: return@filter false
            val payment = paymentsMap[occ.id]

            val matchesSearch = profile.name.contains(searchQuery, ignoreCase = true) ||
                    profile.referenceNumber.contains(searchQuery, ignoreCase = true) ||
                    (profile.provider ?: "").contains(searchQuery, ignoreCase = true) ||
                    (payment?.transactionReference ?: "").contains(searchQuery, ignoreCase = true) ||
                    (payment?.bankName ?: "").contains(searchQuery, ignoreCase = true)

            val matchesStatus = selectedStatus == "All" || occ.status.equals(selectedStatus, ignoreCase = true)

            val matchesCategory = selectedCategory == "All" || profile.category.equals(selectedCategory, ignoreCase = true)

            val matchesYear = selectedYear == "All" || occ.billingYear.toString() == selectedYear

            val matchesPaymentMode = selectedPaymentMode == "All" || (payment != null && payment.paymentMode.equals(selectedPaymentMode, ignoreCase = true))

            matchesSearch && matchesStatus && matchesCategory && matchesYear && matchesPaymentMode
        }.sortedWith(compareByDescending<MonthlyBillOccurrenceEntity> { it.billingYear }.thenByDescending { it.billingMonth })
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Global Bill & Payment History", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by reference, provider, bank, or transaction ref") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("history-search")
        )

        Text("Filters", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag("filter-status-button")) {
                    Text("Status: $selectedStatus")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("All", "Paid", "Pending", "Overdue", "Skipped").forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = { selectedStatus = opt; expanded = false },
                            modifier = Modifier.testTag("filter-status-opt-$opt")
                        )
                    }
                }
            }

            Box {
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag("filter-category-button")) {
                    Text("Category: $selectedCategory")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("All", "Electricity", "Gas", "Water", "Internet", "Telephone", "Other").forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = { selectedCategory = opt; expanded = false },
                            modifier = Modifier.testTag("filter-category-opt-$opt")
                        )
                    }
                }
            }

            Box {
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag("filter-year-button")) {
                    Text("Year: $selectedYear")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    val years = remember(occurrences) {
                        listOf("All") + occurrences.map { it.billingYear.toString() }.distinct().sortedDescending()
                    }
                    years.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = { selectedYear = opt; expanded = false },
                            modifier = Modifier.testTag("filter-year-opt-$opt")
                        )
                    }
                }
            }

            Box {
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag("filter-paymode-button")) {
                    Text("Mode: $selectedPaymentMode")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("All", "Cash", "Bank Transfer", "Card", "Mobile Wallet", "Other").forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = { selectedPaymentMode = opt; expanded = false },
                            modifier = Modifier.testTag("filter-paymode-opt-$opt")
                        )
                    }
                }
            }

            if (searchQuery.isNotEmpty() || selectedStatus != "All" || selectedCategory != "All" || selectedYear != "All" || selectedPaymentMode != "All") {
                TextButton(
                    onClick = {
                        searchQuery = ""
                        selectedStatus = "All"
                        selectedCategory = "All"
                        selectedYear = "All"
                        selectedPaymentMode = "All"
                    },
                    modifier = Modifier.testTag("filter-reset")
                ) {
                    Text("Reset")
                }
            }
        }

        if (filteredOccurrences.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No bills or payments match the filters.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().testTag("history-list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredOccurrences, key = { it.id }) { occ ->
                    val profile = profiles.find { it.id == occ.profileId }
                    if (profile != null) {
                        val payment = paymentsMap[occ.id]
                        val monthLabel = java.time.YearMonth.of(occ.billingYear, occ.billingMonth).format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
                        Card(
                            onClick = { selectedOccurrenceForDetails = occ },
                            modifier = Modifier.fillMaxWidth().testTag("history-item-${occ.id}")
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CategoryIcon(profile.category, modifier = Modifier.size(24.dp))
                                        Text(profile.name, style = MaterialTheme.typography.titleMedium)
                                    }
                                    StatusChip(occ.status)
                                }
                                Text(monthLabel, style = MaterialTheme.typography.bodyMedium)
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Ref: ${maskReferenceNumber(profile.referenceNumber)}", style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        if (occ.amountMinor != null) formatPkr(occ.amountMinor) else "Amount TBD",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                                if (payment != null) {
                                    val payDateStr = java.time.LocalDate.ofEpochDay(payment.paymentDateEpochDay).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
                                    Text(
                                        "Paid: ${formatPkr(payment.amountPaidMinor)} on $payDateStr via ${payment.paymentMode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedOccurrenceForDetails?.let { occ ->
        MonthlyOccurrenceDetailsDialog(occ, vm, application, onDismiss = { selectedOccurrenceForDetails = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBillDialog(
    vm: MainViewModel,
    application: PassportApplication,
    profileToEdit: UtilityBillProfileEntity? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(profileToEdit?.name ?: "") }
    var category by rememberSaveable { mutableStateOf(profileToEdit?.category ?: "Electricity") }
    var customCategoryName by rememberSaveable { mutableStateOf(profileToEdit?.customCategoryName ?: "") }
    var referenceNumber by rememberSaveable { mutableStateOf(profileToEdit?.referenceNumber ?: "") }
    var provider by rememberSaveable { mutableStateOf(profileToEdit?.provider ?: "") }
    var locationLabel by rememberSaveable { mutableStateOf(profileToEdit?.locationLabel ?: "Home") }
    var customLocationLabel by rememberSaveable { mutableStateOf(if (profileToEdit?.locationLabel != "Home" && profileToEdit?.locationLabel != "Clinic" && profileToEdit?.locationLabel != "Office") profileToEdit?.locationLabel ?: "" else "") }
    var connectionIdentifier by rememberSaveable { mutableStateOf(profileToEdit?.connectionIdentifier ?: "") }
    var issueDayAnchor by rememberSaveable { mutableStateOf(profileToEdit?.issueDayAnchor?.toString() ?: "15") }
    var dueDayAnchor by rememberSaveable { mutableStateOf(profileToEdit?.dueDayAnchor?.toString() ?: "27") }
    var recurrenceStartMonth by rememberSaveable { mutableStateOf(profileToEdit?.recurrenceStartMonth ?: java.time.YearMonth.now().toString()) }
    var reminderPreference by rememberSaveable { mutableStateOf(profileToEdit?.reminderPreference ?: "ENABLED") }
    var notes by rememberSaveable { mutableStateOf(profileToEdit?.notes ?: "") }

    val profiles by vm.utilityProfiles.collectAsState()
    var duplicateWarningShown by remember { mutableStateOf(false) }
    var forceSave by remember { mutableStateOf(false) }

    val issueVal = issueDayAnchor.toIntOrNull()
    val dueVal = dueDayAnchor.toIntOrNull()
    val isValid = name.isNotBlank() &&
            referenceNumber.isNotBlank() &&
            (category != "Other" || customCategoryName.isNotBlank()) &&
            issueVal in 1..31 &&
            dueVal in 1..31 &&
            runCatching { java.time.YearMonth.parse(recurrenceStartMonth) }.isSuccess

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (profileToEdit == null) "Add Utility Bill" else "Edit Utility Bill") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (duplicateWarningShown) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Warning: Duplicate Detected", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("A utility bill with this category, provider, and reference number already exists. Save anyway?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                Text("Bill Identity", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(name, { name = it }, label = { Text("Bill Name") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("bill-name"))
                Text("Category")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Electricity", "Gas", "Telephone", "Other").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("chip-category-$cat")
                        )
                    }
                }
                if (category == "Other") {
                    OutlinedTextField(customCategoryName, { customCategoryName = it }, label = { Text("Service Name (Required)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("custom-category"))
                }
                OutlinedTextField(provider, { provider = it }, label = { Text("Provider/Company") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("provider"))
                OutlinedTextField(referenceNumber, { referenceNumber = it }, label = { Text("Reference/Consumer Number") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("reference-number"))
                Text("Location")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Home", "Clinic", "Office", "Other").forEach { loc ->
                        FilterChip(
                            selected = locationLabel == loc,
                            onClick = { locationLabel = loc },
                            label = { Text(loc) },
                            modifier = Modifier.testTag("chip-location-$loc")
                        )
                    }
                }
                if (locationLabel == "Other") {
                    OutlinedTextField(customLocationLabel, { customLocationLabel = it }, label = { Text("Custom Location Label") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("custom-location"))
                }
                Text("Monthly Schedule", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(issueDayAnchor, { issueDayAnchor = it.filter(Char::isDigit) }, label = { Text("Approx. Issue Day (1-31)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth().testTag("issue-day"))
                OutlinedTextField(dueDayAnchor, { dueDayAnchor = it.filter(Char::isDigit) }, label = { Text("Approx. Due Day (1-31)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth().testTag("due-day"))
                OutlinedTextField(recurrenceStartMonth, { recurrenceStartMonth = it }, label = { Text("Start Month (YYYY-MM)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("start-month"))
                Text("Reminder Preference")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ENABLED", "DISABLED").forEach { pref ->
                        FilterChip(
                            selected = reminderPreference == pref,
                            onClick = { reminderPreference = pref },
                            label = { Text(pref.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier.testTag("chip-reminder-$pref")
                        )
                    }
                }
                Text("Optional Information", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(connectionIdentifier, { connectionIdentifier = it }, label = { Text("Connection/Identifier") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("connection-id"))
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth().testTag("notes"))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (profileToEdit == null && !forceSave) {
                        val isDuplicate = profiles.any {
                            it.category.equals(category, ignoreCase = true) &&
                                    it.referenceNumber.equals(referenceNumber, ignoreCase = true) &&
                                    (it.provider ?: "").equals(provider, ignoreCase = true)
                        }
                        if (isDuplicate && !duplicateWarningShown) {
                            duplicateWarningShown = true
                            return@Button
                        }
                    }
                    val finalLocation = if (locationLabel == "Other") customLocationLabel else locationLabel
                    val now = System.currentTimeMillis()
                    val profile = UtilityBillProfileEntity(
                        id = profileToEdit?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        category = category,
                        referenceNumber = referenceNumber.trim(),
                        issueDayAnchor = issueDayAnchor.toInt(),
                        dueDayAnchor = dueDayAnchor.toInt(),
                        recurrenceStartMonth = recurrenceStartMonth.trim(),
                        status = profileToEdit?.status ?: "ACTIVE",
                        provider = provider.trim().takeIf { it.isNotEmpty() },
                        customCategoryName = customCategoryName.trim().takeIf { category == "Other" },
                        locationLabel = finalLocation.trim().takeIf { it.isNotEmpty() },
                        connectionIdentifier = connectionIdentifier.trim().takeIf { it.isNotEmpty() },
                        notes = notes.trim().takeIf { it.isNotEmpty() },
                        reminderPreference = reminderPreference,
                        createdAtEpochMillis = profileToEdit?.createdAtEpochMillis ?: now,
                        updatedAtEpochMillis = now
                    )
                    if (profileToEdit != null) {
                        vm.updateUtilityProfile(context, profile)
                    } else {
                        vm.addUtilityProfile(context, profile)
                    }
                    onDismiss()
                },
                enabled = isValid,
                modifier = Modifier.testTag("save-bill-button")
            ) {
                Text(if (duplicateWarningShown) "Save Anyway" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UtilityProfileDetailsDialog(
    profile: UtilityBillProfileEntity,
    vm: MainViewModel,
    application: PassportApplication,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val occurrences by vm.monthlyOccurrences.collectAsState()
    val profileOccurrences = remember(occurrences, profile) {
        occurrences.filter { it.profileId == profile.id }
            .sortedWith(compareByDescending<MonthlyBillOccurrenceEntity> { it.billingYear }.thenByDescending { it.billingMonth })
    }

    val totalOccurrences = profileOccurrences.size
    val paidCount = profileOccurrences.count { it.status == "Paid" }
    val pendingCount = profileOccurrences.count { it.status == "Pending" }
    val overdueCount = profileOccurrences.count { it.status == "Overdue" }

    var totalPaidAmount by remember { mutableStateOf(0L) }
    var latestPaymentDateStr by remember { mutableStateOf("-") }

    LaunchedEffect(profileOccurrences) {
        var total = 0L
        var latestDate = 0L
        for (occ in profileOccurrences) {
            val payment = vm.getPaymentForOccurrence(occ.id)
            if (payment != null) {
                total += payment.amountPaidMinor
                if (payment.paymentDateEpochDay > latestDate) {
                    latestDate = payment.paymentDateEpochDay
                }
            }
        }
        totalPaidAmount = total
        latestPaymentDateStr = if (latestDate > 0) {
            java.time.LocalDate.ofEpochDay(latestDate).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
        } else "-"
    }

    var showEditProfile by remember { mutableStateOf(false) }
    var showAddHistorical by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedOccurrenceForDetails by remember { mutableStateOf<MonthlyBillOccurrenceEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryIcon(profile.category, modifier = Modifier.size(28.dp))
                Text(profile.name)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("profile-details-scroll"),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Reference: ${profile.referenceNumber}", style = MaterialTheme.typography.bodyMedium)
                    if (profile.provider != null) Text("Provider: ${profile.provider}", style = MaterialTheme.typography.bodyMedium)
                    if (profile.locationLabel != null) Text("Location: ${profile.locationLabel}", style = MaterialTheme.typography.bodyMedium)
                    Text("Schedule: Issue approx. ${profile.issueDayAnchor}th · Due approx. ${profile.dueDayAnchor}th", style = MaterialTheme.typography.bodySmall)
                    Text("Status: ${profile.status}", style = MaterialTheme.typography.bodySmall)
                    if (profile.notes != null) Text("Notes: ${profile.notes}", style = MaterialTheme.typography.bodySmall)
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Connection Statistics", style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Bills: $totalOccurrences")
                            Text("Paid: $paidCount")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Pending: $pendingCount")
                            Text("Overdue: $overdueCount")
                        }
                        Text("Total Paid: ${formatPkr(totalPaidAmount)}", style = MaterialTheme.typography.titleSmall)
                        Text("Latest Payment: $latestPaymentDateStr", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Text("Billing History", style = MaterialTheme.typography.titleMedium)
                if (profileOccurrences.isEmpty()) {
                    Text("No billing history found.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        profileOccurrences.forEach { occ ->
                            val monthLabel = remember(occ) {
                                java.time.YearMonth.of(occ.billingYear, occ.billingMonth).format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
                            }
                            Card(
                                onClick = { selectedOccurrenceForDetails = occ },
                                modifier = Modifier.fillMaxWidth().testTag("history-occ-${occ.id}")
                            ) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(monthLabel, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            if (occ.amountMinor != null) formatPkr(occ.amountMinor) else "Amount not entered",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    StatusChip(occ.status)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showEditProfile = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Edit, "Edit")
                        Spacer(Modifier.width(4.dp))
                        Text("Edit")
                    }
                    if (profile.status == "ACTIVE") {
                        Button(
                            onClick = { vm.archiveUtilityProfile(context, profile.id); onDismiss() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Archive, "Archive")
                            Spacer(Modifier.width(4.dp))
                            Text("Archive")
                        }
                    } else {
                        Button(
                            onClick = { showAddHistorical = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Unarchive, "Reactivate")
                            Spacer(Modifier.width(4.dp))
                            Text("Reactivate")
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showAddHistorical = true }, modifier = Modifier.weight(1f)) {
                        Text("Add Month")
                    }
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.Delete, "Delete")
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    )

    if (showEditProfile) {
        AddBillDialog(vm, application, profileToEdit = profile) {
            showEditProfile = false
            onDismiss()
        }
    }

    if (showAddHistorical) {
        if (profile.status == "ARCHIVED") {
            ReactivateProfileDialog(profile, vm, onDismiss = { showAddHistorical = false; onDismiss() })
        } else {
            AddHistoricalOccurrenceDialog(profile, vm, onDismiss = { showAddHistorical = false })
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Utility Connection?") },
            text = { Text("This permanently deletes the connection profile '${profile.name}' and all its occurrences, payments, and attachments. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteUtilityProfile(context, profile.id)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    selectedOccurrenceForDetails?.let { occ ->
        MonthlyOccurrenceDetailsDialog(occ, vm, application, onDismiss = { selectedOccurrenceForDetails = null })
    }
}

@Composable
private fun ReactivateProfileDialog(
    profile: UtilityBillProfileEntity,
    vm: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var reactivateMonth by rememberSaveable { mutableStateOf(java.time.YearMonth.now().toString()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reactivate Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
                Text("Select the month from which to resume automatic bill generation.")
                OutlinedTextField(reactivateMonth, { reactivateMonth = it }, label = { Text("Reactivation Month (YYYY-MM)") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (!runCatching { java.time.YearMonth.parse(reactivateMonth) }.isSuccess) {
                    errorMsg = "Please enter month in YYYY-MM format."
                    return@Button
                }
                vm.reactivateUtilityProfile(context, profile.id, reactivateMonth)
                onDismiss()
            }) {
                Text("Reactivate")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddHistoricalOccurrenceDialog(
    profile: UtilityBillProfileEntity,
    vm: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var year by rememberSaveable { mutableStateOf(java.time.LocalDate.now().year.toString()) }
    var month by rememberSaveable { mutableStateOf(java.time.LocalDate.now().monthValue.toString()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val occurrences by vm.monthlyOccurrences.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Historical Month") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(year, { year = it.filter(Char::isDigit) }, label = { Text("Year (YYYY)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(month, { month = it.filter(Char::isDigit) }, label = { Text("Month (1-12)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                AmountField(amount, { amount = it }, "Expected Amount (PKR, optional)")
            }
        },
        confirmButton = {
            Button(onClick = {
                val yVal = year.toIntOrNull()
                val mVal = month.toIntOrNull()
                if (yVal == null || mVal == null || mVal !in 1..12) {
                    errorMsg = "Please enter valid Year and Month (1-12)."
                    return@Button
                }
                val duplicate = occurrences.any { it.profileId == profile.id && it.billingYear == yVal && it.billingMonth == mVal }
                if (duplicate) {
                    errorMsg = "An occurrence for $yVal-$mVal already exists."
                    return@Button
                }
                val (issueDate, dueDate) = UtilityRecurrenceEngine.calculateDates(yVal, mVal, profile.issueDayAnchor, profile.dueDayAnchor)
                val amtMinor = amount.takeIf { it.isNotBlank() }?.let { PkrMoneyInput.toMinorUnits(it, false) }
                val now = System.currentTimeMillis()
                val occ = MonthlyBillOccurrenceEntity(
                    id = UUID.randomUUID().toString(),
                    profileId = profile.id,
                    billingYear = yVal,
                    billingMonth = mVal,
                    expectedIssueDateEpochDay = issueDate.toEpochDay(),
                    expectedDueDateEpochDay = dueDate.toEpochDay(),
                    actualIssueDateEpochDay = null,
                    actualDueDateEpochDay = null,
                    amountMinor = amtMinor,
                    status = "Pending",
                                    notes = "Manually added historical month",
                    creationSource = "Manual",
                    createdAtEpochMillis = now,
                    updatedAtEpochMillis = now
                )
                vm.addMonthlyOccurrence(context, occ)
                onDismiss()
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyOccurrenceDetailsDialog(
    occurrence: MonthlyBillOccurrenceEntity,
    vm: MainViewModel,
    application: PassportApplication,
    onDismiss: () -> Unit
) {
    val profiles by vm.utilityProfiles.collectAsState()
    val profile = remember(profiles, occurrence) { profiles.find { it.id == occurrence.profileId } }
    if (profile == null) {
        onDismiss()
        return
    }

    val context = LocalContext.current

    var actualIssueDate by remember { mutableStateOf(occurrence.actualIssueDateEpochDay?.let { java.time.LocalDate.ofEpochDay(it) } ?: java.time.LocalDate.ofEpochDay(occurrence.expectedIssueDateEpochDay)) }
    var setActualIssueDate by remember { mutableStateOf(occurrence.actualIssueDateEpochDay != null) }

    var actualDueDate by remember { mutableStateOf(occurrence.actualDueDateEpochDay?.let { java.time.LocalDate.ofEpochDay(it) } ?: java.time.LocalDate.ofEpochDay(occurrence.expectedDueDateEpochDay)) }
    var setActualDueDate by remember { mutableStateOf(occurrence.actualDueDateEpochDay != null) }

    var billAmount by remember { mutableStateOf(occurrence.amountMinor?.let { (it / 100).toString() } ?: "") }

    var showPayForm by remember { mutableStateOf(false) }
    var showSkipForm by remember { mutableStateOf(false) }

    // Payment Form state variables
    var payAmount by remember { mutableStateOf(billAmount) }
    var paymentDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    var paymentMode by remember { mutableStateOf("Bank Transfer") }
    var bankName by remember { mutableStateOf("") }
    var transactionReference by remember { mutableStateOf("") }
    var payNotes by remember { mutableStateOf("") }

    // Skip notes
    var skipNotes by remember { mutableStateOf("") }

    var paymentRecord by remember { mutableStateOf<PaymentRecordEntity?>(null) }
    LaunchedEffect(occurrence) {
        paymentRecord = vm.getPaymentForOccurrence(occurrence.id)
    }

    var previewAttachmentTarget by remember { mutableStateOf<BillAttachmentEntity?>(null) }
    var previewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var previewError by remember { mutableStateOf<String?>(null) }

    val monthLabel = remember(occurrence) {
        java.time.YearMonth.of(occurrence.billingYear, occurrence.billingMonth).format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${profile.name} - $monthLabel")
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (showPayForm) {
                    Text("Record Payment", style = MaterialTheme.typography.titleMedium)
                    AmountField(payAmount, { payAmount = it }, "Amount Paid (PKR)", modifier = Modifier.testTag("pay-amount"))
                    DateField("Payment Date", paymentDate, { paymentDate = it }, testTag = "pay-date")

                    Text("Payment Mode")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cash", "Bank Transfer", "Card", "Mobile Wallet", "Other").forEach { mode ->
                            FilterChip(
                                selected = paymentMode == mode,
                                onClick = { paymentMode = mode },
                                label = { Text(mode) },
                                modifier = Modifier.testTag("chip-paymode-$mode")
                            )
                        }
                    }

                    OutlinedTextField(bankName, { bankName = it }, label = { Text("Bank Name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("pay-bank"))
                    OutlinedTextField(transactionReference, { transactionReference = it }, label = { Text("Transaction Reference (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("pay-ref"))
                    OutlinedTextField(payNotes, { payNotes = it }, label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth().testTag("pay-notes"))
                } else if (showSkipForm) {
                    Text("Mark as Skipped", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(skipNotes, { skipNotes = it }, label = { Text("Reason / Notes for skipping") }, modifier = Modifier.fillMaxWidth().testTag("skip-notes"))
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Expected Issue: ${java.time.LocalDate.ofEpochDay(occurrence.expectedIssueDateEpochDay).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))}")
                        Text("Expected Due: ${java.time.LocalDate.ofEpochDay(occurrence.expectedDueDateEpochDay).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))}")
                        Text("Creation Source: ${occurrence.creationSource}")
                        Text("Current Status: ${occurrence.status}")
                    }

                    if (paymentRecord != null) {
                        val scope = androidx.compose.runtime.rememberCoroutineScope()
                        val attachments by vm.observeAttachments(paymentRecord!!.id).collectAsState(initial = emptyList())
                        val pickerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.GetContent()
                        ) { uri: android.net.Uri? ->
                            uri?.let {
                                scope.launch {
                                    runCatching {
                                        vm.importAttachment(context, it, paymentRecord!!.id, "PAYMENT_PROOF")
                                    }
                                }
                            }
                        }

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Payment Details", style = MaterialTheme.typography.titleMedium)
                                Text("Amount Paid: ${formatPkr(paymentRecord!!.amountPaidMinor)}")
                                Text("Date Paid: ${java.time.LocalDate.ofEpochDay(paymentRecord!!.paymentDateEpochDay).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))}")
                                Text("Mode: ${paymentRecord!!.paymentMode}")
                                if (paymentRecord!!.bankName != null) Text("Bank: ${paymentRecord!!.bankName}")
                                if (paymentRecord!!.transactionReference != null) Text("Reference: ${paymentRecord!!.transactionReference}")
                                if (paymentRecord!!.notes != null) Text("Notes: ${paymentRecord!!.notes}")

                                Spacer(Modifier.height(8.dp))
                                Text("Proof of Payment (Attachments)", style = MaterialTheme.typography.titleSmall)
                                if (attachments.isEmpty()) {
                                    Text("No proof documents attached.", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    attachments.forEach { att ->
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(att.displayName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                            Row {
                                                IconButton(onClick = { previewAttachmentTarget = att }) {
                                                    Icon(Icons.Filled.Visibility, "View")
                                                }
                                                IconButton(onClick = { vm.deleteAttachmentFile(context, att) }) {
                                                    Icon(Icons.Filled.Delete, "Delete")
                                                }
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = { pickerLauncher.launch("*/*") },
                                    modifier = Modifier.fillMaxWidth().testTag("add-attachment-button")
                                ) {
                                    Icon(Icons.Default.Add, "Add Attachment")
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add Attachment Proof")
                                }
                            }
                        }
                    } else if (occurrence.status == "Skipped") {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Skipped Occurrence", style = MaterialTheme.typography.titleMedium)
                                if (occurrence.notes != null) Text("Notes: ${occurrence.notes}")
                            }
                        }
                    } else {
                        Text("Billing Parameters", style = MaterialTheme.typography.titleMedium)
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Checkbox(checked = setActualIssueDate, onCheckedChange = { setActualIssueDate = it })
                            Text("Override Actual Issue Date")
                        }
                        if (setActualIssueDate) {
                            DateField("Actual Issue Date", actualIssueDate, { actualIssueDate = it }, testTag = "actual-issue-date")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Checkbox(checked = setActualDueDate, onCheckedChange = { setActualDueDate = it })
                            Text("Override Actual Due Date")
                        }
                        if (setActualDueDate) {
                            DateField("Actual Due Date", actualDueDate, { actualDueDate = it }, testTag = "actual-due-date")
                        }

                        AmountField(billAmount, { billAmount = it }, "Expected/Actual Bill Amount (PKR)", modifier = Modifier.testTag("bill-amount"))
                    }
                }
            }
        },
        confirmButton = {
            if (showPayForm) {
                Button(
                    onClick = {
                        val amtPaidMinor = PkrMoneyInput.toMinorUnits(payAmount, false)
                        val payment = PaymentRecordEntity(
                            id = UUID.randomUUID().toString(),
                            occurrenceId = occurrence.id,
                            amountPaidMinor = amtPaidMinor,
                            paymentDateEpochDay = paymentDate.toEpochDay(),
                            paymentMode = paymentMode,
                            bankName = bankName.trim().takeIf { it.isNotEmpty() },
                            transactionReference = transactionReference.trim().takeIf { it.isNotEmpty() },
                            notes = payNotes.trim().takeIf { it.isNotEmpty() },
                            createdAtEpochMillis = System.currentTimeMillis(),
                            updatedAtEpochMillis = System.currentTimeMillis()
                        )
                        vm.addPayment(context, payment)
                        val finalAmtMinor = billAmount.takeIf { it.isNotBlank() }?.let { PkrMoneyInput.toMinorUnits(it, false) } ?: amtPaidMinor
                        vm.updateMonthlyOccurrence(context, occurrence.copy(
                            actualIssueDateEpochDay = actualIssueDate.toEpochDay().takeIf { setActualIssueDate },
                            actualDueDateEpochDay = actualDueDate.toEpochDay().takeIf { setActualDueDate },
                            amountMinor = finalAmtMinor,
                            status = "Paid",
                            updatedAtEpochMillis = System.currentTimeMillis()
                        ))
                        onDismiss()
                    },
                    enabled = runCatching { PkrMoneyInput.parseRupees(payAmount, false) }.isSuccess && PkrMoneyInput.toMinorUnits(payAmount, false) > 0,
                    modifier = Modifier.testTag("save-payment-button")
                ) {
                    Text("Save Payment")
                }
            } else if (showSkipForm) {
                Button(
                    onClick = {
                        vm.updateMonthlyOccurrence(context, occurrence.copy(
                            status = "Skipped",
                            notes = skipNotes.trim().takeIf { it.isNotEmpty() } ?: "Skipped",
                            updatedAtEpochMillis = System.currentTimeMillis()
                        ))
                        onDismiss()
                    },
                    modifier = Modifier.testTag("save-skip-button")
                ) {
                    Text("Skip Occurrence")
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (paymentRecord != null) {
                        Button(
                            onClick = {
                                vm.deletePayment(context, paymentRecord!!.id, occurrence.id)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).testTag("delete-payment-button")
                        ) {
                            Text("Delete Payment")
                        }
                    } else if (occurrence.status == "Skipped") {
                        Button(
                            onClick = {
                                vm.updateMonthlyOccurrence(context, occurrence.copy(
                                    status = "Pending",
                                    notes = null,
                                    updatedAtEpochMillis = System.currentTimeMillis()
                                ))
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).testTag("unskip-button")
                        ) {
                            Text("Revert Skip")
                        }
                    } else {
                        Button(onClick = { showPayForm = true; payAmount = billAmount }, modifier = Modifier.weight(1f).testTag("pay-button")) {
                            Text("Mark Paid")
                        }
                        Button(onClick = { showSkipForm = true }, modifier = Modifier.weight(1f).testTag("skip-button")) {
                            Text("Skip Month")
                        }
                        Button(
                            onClick = {
                                val amtMinor = billAmount.takeIf { it.isNotBlank() }?.let { PkrMoneyInput.toMinorUnits(it, false) }
                                vm.updateMonthlyOccurrence(context, occurrence.copy(
                                    actualIssueDateEpochDay = actualIssueDate.toEpochDay().takeIf { setActualIssueDate },
                                    actualDueDateEpochDay = actualDueDate.toEpochDay().takeIf { setActualDueDate },
                                    amountMinor = amtMinor,
                                    updatedAtEpochMillis = System.currentTimeMillis()
                                ))
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).testTag("save-occurrence-button")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (showPayForm) {
                        showPayForm = false
                    } else if (showSkipForm) {
                        showSkipForm = false
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(if (showPayForm || showSkipForm) "Back" else "Cancel")
            }
        }
    )

    previewAttachmentTarget?.let { att ->
        AlertDialog(
            onDismissRequest = { previewAttachmentTarget = null; previewBitmap = null },
            title = { Text(att.displayName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    previewBitmap?.let {
                        androidx.compose.foundation.Image(
                            it,
                            contentDescription = "Preview of ${att.displayName}",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } ?: Text(previewError ?: "Decrypting preview…")
                }
            },
            confirmButton = {
                TextButton(onClick = { previewAttachmentTarget = null; previewBitmap = null }) {
                    Text("Close")
                }
            }
        )

        val context = LocalContext.current
        LaunchedEffect(att) {
            runCatching {
                withContext(Dispatchers.IO) {
                    renderAttachmentPreview(context, vm, att)
                }.asImageBitmap()
            }.onSuccess {
                previewBitmap = it
            }.onFailure {
                previewError = it.message ?: "Preview failed"
            }
        }
    }
}
