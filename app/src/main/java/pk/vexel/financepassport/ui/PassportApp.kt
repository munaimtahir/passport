package pk.vexel.financepassport.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pk.vexel.financepassport.PassportApplication
import pk.vexel.financepassport.core.database.AccountEntity
import pk.vexel.financepassport.core.database.BillAttachmentEntity
import pk.vexel.financepassport.core.database.CalendarItemEntity
import pk.vexel.financepassport.core.database.DocumentEntity
import pk.vexel.financepassport.core.database.MonthlyBillOccurrenceEntity
import pk.vexel.financepassport.core.database.PaymentRecordEntity
import pk.vexel.financepassport.core.database.UtilityBillProfileEntity
import pk.vexel.financepassport.core.database.UtilityRecurrenceEngine
import pk.vexel.financepassport.core.files.DocumentVault
import pk.vexel.financepassport.core.model.FinancialEventType
import pk.vexel.financepassport.core.model.PkrMoneyInput
import pk.vexel.financepassport.core.model.UtilityCategory
import pk.vexel.financepassport.core.security.LiveRestoreService
import pk.vexel.financepassport.core.security.PinStore
import pk.vexel.financepassport.core.security.PinVerifier
import pk.vexel.financepassport.core.export.DataExportService
import pk.vexel.financepassport.core.reports.ReportGenerator
import java.io.ByteArrayOutputStream
import pk.vexel.financepassport.ui.components.FinancialAttentionCard
import pk.vexel.financepassport.ui.components.FinancialAttentionItem
import pk.vexel.financepassport.ui.components.FinancialDayGroupHeader
import pk.vexel.financepassport.ui.components.FinancialTimelineEventRow
import pk.vexel.financepassport.ui.components.LivingBillCard
import pk.vexel.financepassport.ui.components.VexelCaptureAction
import pk.vexel.financepassport.ui.components.VexelCaptureControl
import pk.vexel.financepassport.ui.components.VexelCaptureTraySheet
import pk.vexel.financepassport.ui.components.VexelEmptyState
import pk.vexel.financepassport.ui.components.VexelStatusChip
import pk.vexel.financepassport.ui.theme.PassportTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.io.File
import java.util.UUID

private data class Destination(val label: String, val icon: ImageVector)

private val destinations = listOf(
    Destination("Home", Icons.Default.Home),
    Destination("Money", Icons.Default.AccountBalanceWallet),
    Destination("Bills", Icons.Default.Description),
    Destination("History", Icons.Default.Folder),
    Destination("Position", Icons.Default.AccountBalance),
    Destination("Calendar", Icons.Default.Event),
    Destination("Vault", Icons.Default.Security),
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
    var showCaptureTray by rememberSaveable { mutableStateOf(false) }

    var captureEventIncome by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var captureTransfer by rememberSaveable { mutableStateOf(false) }

    val activeAccounts by vm.activeAccounts.collectAsState()

    PassportTheme {
        CompositionLocalProvider(LocalPrivacyMode provides vm.privacyModeEnabled) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Vexel Finance Passport", style = MaterialTheme.typography.titleLarge) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                        actions = {
                            IconButton(onClick = vm::togglePrivacyMode) {
                                Icon(
                                    if (vm.privacyModeEnabled) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (vm.privacyModeEnabled) "Show amounts" else "Hide amounts",
                                )
                            }
                            IconButton(onClick = { showMore = true }) { Icon(Icons.Default.MoreHoriz, "Settings") }
                        },
                    )
                },
                floatingActionButton = {
                    if (selected == 2) {
                        FloatingActionButton(
                            onClick = { showAddBill = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("add-bill-fab"),
                        ) {
                            Icon(Icons.Default.Add, "Add Bill")
                        }
                    } else if (selected < 4) {
                        VexelCaptureControl(onClick = { showCaptureTray = true })
                    }
                },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                    ) {
                        destinations.forEachIndexed { index, destination ->
                            NavigationBarItem(
                                selected = selected == index,
                                onClick = { selected = index },
                                icon = { Icon(destination.icon, destination.label) },
                                label = { Text(destination.label, style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }
                },
            ) { padding ->
                when (selected) {
                    0 -> HomeScreen(vm, application, padding) { selected = it }
                    1 -> MoneyScreen(vm, application, padding)
                    2 -> BillsScreen(vm, application, padding)
                    3 -> HistoryScreen(vm, application, padding)
                    4 -> PositionScreen(vm, padding)
                    5 -> CalendarScreen(vm, application, padding)
                    6 -> VaultScreen(vm, application, padding)
                    else -> EmptyModuleScreen(destinations[selected].label, padding)
                }
            }

            if (showCaptureTray) {
                VexelCaptureTraySheet(
                    onDismiss = { showCaptureTray = false },
                    hasAccounts = activeAccounts.isNotEmpty(),
                    onSelectAction = { action ->
                        showCaptureTray = false
                        when (action) {
                            VexelCaptureAction.EXPENSE -> captureEventIncome = false
                            VexelCaptureAction.INCOME -> captureEventIncome = true
                            VexelCaptureAction.TRANSFER -> captureTransfer = true
                            VexelCaptureAction.BILL -> showAddBill = true
                        }
                    },
                )
            }

            captureEventIncome?.let { income ->
                AddEventDialog(vm, activeAccounts, income) { captureEventIncome = null }
            }

            if (captureTransfer) {
                TransferDialog(vm, activeAccounts) { captureTransfer = false }
            }

            if (showAddBill) AddBillDialog(vm, application) { showAddBill = false }
            if (showMore) MoreDialog(vm, application) { showMore = false }
            vm.errorMessage?.let { message ->
                AlertDialog(
                    onDismissRequest = vm::clearError,
                    title = { Text("Could not save") },
                    text = { Text(message) },
                    confirmButton = { TextButton(onClick = vm::clearError) { Text("OK") } },
                )
            }
        }
    }
}

@Composable
private fun MoreDialog(vm: MainViewModel, application: PassportApplication, onDismiss: () -> Unit) {
    val activity = LocalActivity.current
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var deleteConfirmation by rememberSaveable { mutableStateOf("") }
    var backupPassword by rememberSaveable { mutableStateOf(false) }
    var restorePayload by remember { mutableStateOf<ByteArray?>(null) }
    var pendingBackup by remember { mutableStateOf<java.io.File?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var showPinManagement by rememberSaveable { mutableStateOf(false) }
    val pinStore = remember(application) { PinStore(application) }
    var requestedReport by rememberSaveable { mutableStateOf("NET_WORTH") }
    var currentYearOnly by rememberSaveable { mutableStateOf(false) }
    var previewReport by remember { mutableStateOf<pk.vexel.financepassport.core.reports.FinancialReport?>(null) }
    var pendingDataExport by remember { mutableStateOf<ByteArray?>(null) }
    val scope = rememberCoroutineScope()
    suspend fun reportSnapshot() = application.repository.exportSnapshot().let { snapshot ->
        if (!currentYearOnly) snapshot else {
            val today = LocalDate.now()
            snapshot.forDateRange(today.withDayOfYear(1).toEpochDay(), today.withDayOfYear(today.lengthOfYear()).toEpochDay())
        }
    }
    val backupSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val file = pendingBackup
        if (uri != null && file != null) scope.launch {
            application.contentResolver.openOutputStream(uri)?.use { destination -> file.inputStream().use { it.copyTo(destination) } }
            file.delete()
            status = "Encrypted backup exported"
        }
        pendingBackup = null
    }
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { restorePayload = application.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
    }
    val dataSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val bytes = pendingDataExport
        if (uri != null && bytes != null) scope.launch {
            runCatching { application.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Unable to open export destination") }
                .onSuccess { status = "Export saved" }.onFailure { status = "Export failed: ${it.message}" }
        }
        pendingDataExport = null
    }
    val reportSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val bytes = pendingDataExport
        if (uri != null && bytes != null) scope.launch { runCatching { application.contentResolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Unable to open export destination") }.onSuccess { status = "PDF report saved" }.onFailure { status = "PDF export failed: ${it.message}" } }
        pendingDataExport = null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings & Local Data", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("more-dialog-scroll"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Offline local backup and data controls", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                status?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium) }

                Text("Reports & data ownership", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { scope.launch { previewReport = ReportGenerator().netWorth(reportSnapshot(), java.time.Instant.now().toString()) } }, modifier = Modifier.weight(1f)) { Text("Net worth") }
                    Button(onClick = { scope.launch { previewReport = ReportGenerator().incomeExpense(reportSnapshot(), java.time.Instant.now().toString()) } }, modifier = Modifier.weight(1f)) { Text("Income / expense") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { scope.launch { previewReport = ReportGenerator().cashFlowSummary(reportSnapshot(), java.time.Instant.now().toString()) } }, modifier = Modifier.weight(1f)) { Text("Cash flow") }
                    OutlinedButton(onClick = { currentYearOnly = !currentYearOnly }, modifier = Modifier.weight(1f)) { Text(if (currentYearOnly) "Current year" else "All dates") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { scope.launch { pendingDataExport = DataExportService().json(reportSnapshot()).toByteArray(); dataSaver.launch("vexel-finance-passport.json") } }, modifier = Modifier.weight(1f)) { Text("Export JSON") }
                    OutlinedButton(onClick = { scope.launch { pendingDataExport = DataExportService().csvEvents(reportSnapshot()).toByteArray(); dataSaver.launch("financial-events.csv") } }, modifier = Modifier.weight(1f)) { Text("Events CSV") }
                }

                Button(
                    onClick = { showPinManagement = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin-management-button"),
                ) {
                    Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (pinStore.hasPin()) "Change or Remove App PIN" else "Set App PIN")
                }

                Button(
                    onClick = { backupPassword = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup-button"),
                ) {
                    Text("Create Encrypted Backup")
                }

                Button(
                    onClick = { backupPicker.launch(arrayOf("application/octet-stream", "application/zip", "application/octet-stream")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("restore-button"),
                ) {
                    Text("Restore Encrypted Backup")
                }

                Button(
                    onClick = { confirmDelete = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete-all-button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete All Application Data")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false; deleteConfirmation = "" },
        title = { Text("Delete everything?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("This permanently removes local records, encrypted vault files, preferences, and scheduled work. It cannot be undone.")
                OutlinedTextField(
                    deleteConfirmation,
                    { deleteConfirmation = it },
                    label = { Text("Type DELETE to confirm") },
                    singleLine = true,
                    modifier = Modifier.testTag("delete-confirmation"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    vm.deleteAllData(application) { activity?.recreate() }
                    confirmDelete = false
                    deleteConfirmation = ""
                    onDismiss()
                },
                enabled = deleteConfirmation == "DELETE",
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Delete all") }
        },
        dismissButton = { TextButton(onClick = { confirmDelete = false; deleteConfirmation = "" }) { Text("Cancel") } },
    )
    if (showPinManagement) PinManagementDialog(pinStore, onDismiss = { showPinManagement = false }) { status = it; showPinManagement = false }
    if (backupPassword) BackupPasswordDialog("Create encrypted backup", onDismiss = { backupPassword = false }) { password ->
        backupPassword = false
        vm.createBackup(application, password.toCharArray()) { result ->
            result.onSuccess {
                pendingBackup = it
                backupSaver.launch("vexel-finance-passport.backup")
            }.onFailure { status = "Backup failed: ${it.message}" }
        }
    }
    previewReport?.let { report ->
        AlertDialog(
            onDismissRequest = { previewReport = null },
            title = { Text(report.title) },
            text = { Column(Modifier.verticalScroll(rememberScrollState())) { Text("Generated ${report.generatedAt} · ${report.currency} · ${report.scope}", style = MaterialTheme.typography.labelSmall); report.lines.forEach { Text(it, modifier = Modifier.padding(vertical = 3.dp)) } } },
            confirmButton = {
                TextButton(onClick = {
                    val copy = report
                    val output = ByteArrayOutputStream()
                    ReportGenerator().writePdf(copy, output)
                    pendingDataExport = output.toByteArray()
                    previewReport = null
                    reportSaver.launch("${copy.title.lowercase().replace(" ", "-")}.pdf")
                }) { Text("Export PDF") }
            },
            dismissButton = { TextButton(onClick = { previewReport = null }) { Text("Close") } },
        )
    }
    restorePayload?.let { payload ->
        BackupPasswordDialog("Restore encrypted backup", onDismiss = { restorePayload = null }) { password ->
            scope.launch {
                runCatching { LiveRestoreService(application).restore(payload, password.toCharArray()) }
                    .onSuccess { status = "Restore complete. Close and relaunch the app to reopen restored records." }
                    .onFailure { status = "Restore failed: ${it.message}" }
                restorePayload = null
            }
        }
    }
}

@Composable
private fun PinManagementDialog(store: PinStore, onDismiss: () -> Unit, onComplete: (String) -> Unit) {
    val hadPin = remember { store.hasPin() }
    var currentPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var remove by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hadPin) "Manage App PIN" else "Set App PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hadPin) OutlinedTextField(currentPin, { currentPin = it.filter(Char::isDigit).take(12) }, label = { Text("Current PIN") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                if (hadPin) FilterChip(remove, { remove = !remove }, label = { Text("Remove PIN") })
                if (!remove) {
                    OutlinedTextField(newPin, { newPin = it.filter(Char::isDigit).take(12) }, label = { Text("New PIN") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                    OutlinedTextField(confirmation, { confirmation = it.filter(Char::isDigit).take(12) }, label = { Text("Confirm new PIN") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    runCatching {
                        if (hadPin) require(store.verify(currentPin.toCharArray())) { "Current PIN is incorrect." }
                        if (remove) store.clear() else {
                            require(newPin.length >= 4) { "Use at least 4 digits." }
                            require(newPin == confirmation) { "PINs do not match." }
                            store.save(PinVerifier.create(newPin.toCharArray()))
                        }
                    }.onSuccess { onComplete(if (remove) "App PIN removed" else if (hadPin) "App PIN changed" else "App PIN set") }
                        .onFailure { error = it.message }
                },
                enabled = (!hadPin || currentPin.length >= 4) && (remove || (newPin.length >= 4 && confirmation.length >= 4)),
            ) { Text(if (remove) "Remove PIN" else "Save PIN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun BackupPasswordDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(password, { password = it }, label = { Text("Backup password (8+ characters)") }, singleLine = true) },
        confirmButton = { Button(onClick = { onConfirm(password) }, enabled = password.length >= 8) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun HomeScreen(
    vm: MainViewModel,
    application: PassportApplication,
    padding: PaddingValues,
    onNavigate: (Int) -> Unit,
) {
    val profiles by vm.utilityProfiles.collectAsState(initial = emptyList())
    val occurrences by vm.monthlyOccurrences.collectAsState(initial = emptyList())
    val financialPosition by vm.financialPosition.collectAsState()
    val activeAccounts by vm.activeAccounts.collectAsState()
    val recentEvents by vm.recentEvents.collectAsState()
    val isMasked = LocalPrivacyMode.current
    val thisMonthTotals by vm.thisMonthTotals.collectAsState()
    val unassignedEvents by vm.unassignedEvents.collectAsState()
    val financialContexts by vm.financialContexts.collectAsState()


    var quickEvent by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var quickBill by rememberSaveable { mutableStateOf(false) }
    val today = remember { LocalDate.now() }

    Text("Dashboard", style = MaterialTheme.typography.titleMedium)

    val unpaidObligations = remember(occurrences, today) {
        occurrences.filter {
            it.status in listOf("Pending", "Due soon", "Overdue") &&
                !today.isBefore(LocalDate.ofEpochDay(it.expectedIssueDateEpochDay))
        }.sortedWith(
            compareBy<MonthlyBillOccurrenceEntity> {
                when (it.status) {
                    "Overdue" -> 0
                    "Due soon" -> 1
                    else -> 2
                }
            }.thenBy { it.expectedDueDateEpochDay },
        )
    }

    val overdueBills = remember(occurrences) {
        occurrences.filter { it.status == "Overdue" }
    }

    var selectedOccurrenceForDetails by remember { mutableStateOf<MonthlyBillOccurrenceEntity?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Quiet Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = today.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Financial Pulse",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }

        // FINANCIAL PULSE ATTENTION QUEUE
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "WHAT MATTERS NOW",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (unpaidObligations.isEmpty()) {
                VexelEmptyState(
                    title = "Nothing needs your attention today",
                    description = "All recurring bills and obligations are fully paid and up to date.",
                    icon = Icons.Filled.Check,
                )
            } else {
                unpaidObligations.take(4).forEach { occ ->
                    val profile = profiles.find { it.id == occ.profileId }
                    if (profile != null) {
                        val dueDateStr = LocalDate.ofEpochDay(occ.expectedDueDateEpochDay).format(DateTimeFormatter.ofPattern("d MMM"))
                        val isOverdue = occ.status == "Overdue"
                        FinancialAttentionCard(
                            item = FinancialAttentionItem(
                                id = occ.id,
                                title = profile.name,
                                subtitle = "Due $dueDateStr · Ref: ${maskReferenceNumber(profile.referenceNumber)}",
                                status = occ.status,
                                amountMinor = occ.amountMinor,
                                isCritical = isOverdue,
                                onClick = { selectedOccurrenceForDetails = occ },
                            ),
                            isMasked = isMasked,
                        )
                    }
                }
            }
        }

        // FINANCIAL POSITION SUMMARY
        financialPosition?.let { position ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CURRENT FINANCIAL POSITION",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Column(Modifier.padding(18.dp), Arrangement.spacedBy(10.dp)) {
                        Text("Liquid Available Funds", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (isMasked) "PKR ••••••" else PkrMoneyInput.formatMinorUnits(position.liquidFundsMinor),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text(
                                "Monthly Inflow: ${if (isMasked) "••••" else PkrMoneyInput.formatMinorUnits(position.monthlyIncomeMinor)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                "Monthly Outflow: ${if (isMasked) "••••" else PkrMoneyInput.formatMinorUnits(position.monthlyExpenseMinor)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }

        // QUICK ACTIONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { quickEvent = true },
                enabled = activeAccounts.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("home-add-income"),
            ) { Text("+ Income") }
            OutlinedButton(
                onClick = { quickEvent = false },
                enabled = activeAccounts.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("home-add-expense"),
            ) { Text("+ Expense") }
            OutlinedButton(
                onClick = { quickBill = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("home-add-bill"),
            ) { Text("+ Bill") }
        }

        // LIVING BILLS SUMMARY
        if (profiles.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "RECURRING BILLS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { onNavigate(2) }) {
                        Text("View all (${profiles.size})", style = MaterialTheme.typography.labelMedium)
                    }
                }

                profiles.take(3).forEach { profile ->
                    LivingBillCard(
                        profile = profile,
                        occurrences = occurrences,
                        onClick = { onNavigate(2) },
                        isMasked = isMasked,
                    )
                }
            }
        } else {
            VexelEmptyState(
                title = "Welcome to Vexel Finance Passport",
                description = "Register your monthly utility bills to automatically track occurrences, outstanding payments, and due dates.",
                icon = Icons.Filled.Description,
                actionLabel = "Add Your First Bill",
                onAction = { onNavigate(2) },
            )
        }

        // RECENT ACTIVITY LOG
        if (recentEvents.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "RECENT ACTIVITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                recentEvents.take(5).forEach { event ->
                    val acc = activeAccounts.find { it.id == event.accountId }?.name
                    FinancialTimelineEventRow(
                        event = event,
                        accountName = acc,
                        onClick = { onNavigate(3) },
                        isMasked = isMasked,
                    )
                }
            }
        }

        Spacer(Modifier.height(48.dp))
    }

    selectedOccurrenceForDetails?.let { occ ->
        MonthlyOccurrenceDetailsDialog(occ, vm, application, onDismiss = { selectedOccurrenceForDetails = null })
    }
    quickEvent?.let { income -> AddEventDialog(vm, activeAccounts, income) { quickEvent = null } }
    if (quickBill) AddBillDialog(vm, application) { quickBill = false }
}

@Composable
private fun PositionScreen(vm: MainViewModel, padding: PaddingValues) {
    val position by vm.financialPosition.collectAsState()
    val assets by vm.assets.collectAsState()
    val liabilities by vm.liabilities.collectAsState()
    val receivables by vm.receivables.collectAsState()
    val investments by vm.simpleInvestments.collectAsState()
    val masked = LocalPrivacyMode.current
    val amount: (Long) -> String = { if (masked) "PKR ••••••" else PkrMoneyInput.formatMinorUnits(it) }
    LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Financial Position", style = MaterialTheme.typography.headlineMedium) }
        item { Card { Column(Modifier.padding(16.dp)) {
            Text("Net Worth", style = MaterialTheme.typography.titleMedium)
            Text(amount(position?.netWorthMinor ?: 0), style = MaterialTheme.typography.headlineSmall)
            Text("Derived from accounts, receivables, investments, assets and liabilities", style = MaterialTheme.typography.bodySmall)
        } } }
        item { PositionLine("Liquid Funds", position?.liquidFundsMinor ?: 0, amount) }
        item { PositionLine("Receivables", position?.receivablesValueMinor ?: 0, amount) }
        item { PositionLine("Investments", position?.investmentsValueMinor ?: 0, amount) }
        item { PositionLine("Included Assets", position?.assetsValueMinor ?: 0, amount) }
        item { PositionLine("Liabilities", position?.liabilitiesValueMinor ?: 0, amount) }
        item { Text("Sources", style = MaterialTheme.typography.titleMedium) }
        items(assets) { asset ->
            Column {
                Text("Asset · ${asset.title} · ${amount(asset.currentEstimatedValueMinor * asset.ownershipPercent / 100L)}")
                TextButton(onClick = { vm.updateAssetPosition(asset.id, asset.currentEstimatedValueMinor, asset.ownershipPercent, !asset.includeInNetWorth) }) {
                    Text(if (asset.includeInNetWorth) "Exclude from Net Worth" else "Include in Net Worth")
                }
            }
        }
        items(investments) { Text("Investment · ${it.title} · ${amount(it.currentEstimatedValueMinor)}") }
        items(receivables) { Text("Receivable · ${it.title} · ${amount(it.outstandingAmountMinor)}") }
        items(liabilities) { Text("Liability · ${it.title} · ${amount(it.outstandingAmountMinor)}") }
    }
}

@Composable
private fun PositionLine(label: String, value: Long, format: (Long) -> String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(format(value), style = MaterialTheme.typography.titleMedium) } }
}

@Composable
private fun CalendarScreen(vm: MainViewModel, application: PassportApplication, padding: PaddingValues) {
    val items by vm.calendarItems.collectAsState()
    LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Financial Calendar", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Upcoming and attention items are linked to their source records.", style = MaterialTheme.typography.bodyMedium) }
        items(items.filter { it.status == "OPEN" }) { item -> CalendarRow(item, vm, application) }
        if (items.none { it.status == "OPEN" }) item { VexelEmptyState("Nothing needs attention", "Your upcoming financial life is clear.", Icons.Default.Event) }
    }
}

@Composable
private fun CalendarRow(item: CalendarItemEntity, vm: MainViewModel, application: PassportApplication) {
    val date = java.time.Instant.ofEpochMilli(item.dueAtEpochMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    Card { Column(Modifier.padding(14.dp)) {
        Text(item.title, style = MaterialTheme.typography.titleMedium)
        Text("${item.kind} · $date", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.updateCalendarStatus(application, item.id, "DISMISSED") }) { Text("Dismiss") }
            TextButton(onClick = { vm.rescheduleCalendarItem(application, item.id, 24 * 60) }) { Text("Snooze 1 day") }
        }
    } }
}

@Composable
private fun VaultScreen(vm: MainViewModel, application: PassportApplication, padding: PaddingValues) {
    val documents by vm.documents.collectAsState()
    val scope = rememberCoroutineScope()
    var importError by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        val uri = cameraUri
        val file = cameraFile
        cameraUri = null
        cameraFile = null
        if (uri != null) {
            if (captured) scope.launch {
                runCatching { DocumentVault(application, application.repository).import(uri, "Camera evidence", "Receipt") }
                    .onFailure { importError = it.message ?: "Camera import failed" }
                file?.delete()
            } else file?.delete()
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val file = File(application.cacheDir, "camera/${UUID.randomUUID()}.jpg").apply { parentFile?.mkdirs() }
            cameraFile = file
            val uri = FileProvider.getUriForFile(application, "${application.packageName}.files", file)
            cameraUri = uri
            camera.launch(uri)
        } else importError = "Camera permission was not granted"
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { DocumentVault(application, application.repository).import(uri, "Imported evidence", "Other") }
                .onFailure { importError = it.message ?: "Import failed" }
        }
    }
    LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Text("Evidence Vault", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f)); OutlinedButton(onClick = { cameraPermission.launch(android.Manifest.permission.CAMERA) }) { Text("Camera") }; Button(onClick = { picker.launch(arrayOf("application/pdf", "image/jpeg", "image/png", "image/webp")) }) { Text("Import") } } }
        item { Text("Encrypted app-private evidence shared across financial records.", style = MaterialTheme.typography.bodyMedium) }
        importError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        items(documents) { document -> DocumentRow(document, vm) }
        if (documents.isEmpty()) item { VexelEmptyState("No evidence yet", "Import a PDF or image to keep a financial record with its proof.", Icons.Default.Security) }
    }
}

@Composable
private fun DocumentRow(document: DocumentEntity, vm: MainViewModel) {
    var showDelete by rememberSaveable(document.id) { mutableStateOf(false) }
    var dependencyCount by rememberSaveable(document.id) { mutableIntStateOf(0) }
    LaunchedEffect(document.id) { dependencyCount = vm.documentDependencyCount(document.id) }
    Card { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(document.title, style = MaterialTheme.typography.titleMedium); Text("${document.mimeType} · ${document.sizeBytes} bytes", style = MaterialTheme.typography.bodySmall); Text(document.sha256.take(12), style = MaterialTheme.typography.labelSmall) }
        IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.Delete, "Delete document") }
    } }
    if (showDelete) AlertDialog(onDismissRequest = { showDelete = false }, title = { Text("Delete evidence?") }, text = { Text(if (dependencyCount == 0) "This removes the encrypted document." else "This document is linked to $dependencyCount record${if (dependencyCount == 1) "" else "s"}. Unlinking and deleting will remove the shared evidence from those records.") }, confirmButton = { Button(onClick = { vm.deleteDocument(document.id); showDelete = false }) { Text("Unlink and Delete") } }, dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } })
}

@Composable
private fun MoneyScreen(vm: MainViewModel, application: PassportApplication, padding: PaddingValues) {
    val accounts by vm.accounts.collectAsState()
    val activeAccounts = accounts.filter { it.status == "ACTIVE" }
    val recentEvents by vm.recentEvents.collectAsState()
    val recurringItems by vm.recurringItems.collectAsState()
    val isMasked = LocalPrivacyMode.current
    val thisMonthTotals by vm.thisMonthTotals.collectAsState()
    val unassignedEvents by vm.unassignedEvents.collectAsState()
    val financialContexts by vm.financialContexts.collectAsState()


    var showEvent by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var showTransfer by rememberSaveable { mutableStateOf(false) }
    var showRecurring by rememberSaveable { mutableStateOf(false) }
    var showAddAccount by rememberSaveable { mutableStateOf(false) }
    var editAccount by remember { mutableStateOf<AccountEntity?>(null) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .testTag("money-list"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text("Money & Accounts", style = MaterialTheme.typography.headlineMedium) }
        if (unassignedEvents.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Unassigned Reconciliation", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("${unassignedEvents.size} events require account or context assignment.", color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Column {
                    Text("Income (This Month)", style = MaterialTheme.typography.labelMedium)
                    Text(pk.vexel.financepassport.core.model.PkrMoneyInput.formatMinorUnits(thisMonthTotals?.first?.minorValue ?: 0), style = MaterialTheme.typography.titleLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Expenses (This Month)", style = MaterialTheme.typography.labelMedium)
                    Text(pk.vexel.financepassport.core.model.PkrMoneyInput.formatMinorUnits(thisMonthTotals?.second?.minorValue ?: 0), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = true, onClick = {}, label = { Text("All") })
                financialContexts.forEach { ctx ->
                    FilterChip(selected = false, onClick = {}, label = { Text(ctx.name) })
                }
            }
        }


        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Accounts", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = { showAddAccount = true }, modifier = Modifier.testTag("add-account")) {
                    Text("Add account")
                }
            }
        }

        if (accounts.isEmpty()) {
            item {
                VexelEmptyState(
                    title = "No Accounts Configured",
                    description = "Add your cash wallet, bank account, or clinic ledger to begin tracking money contextually.",
                    icon = Icons.Filled.AccountBalanceWallet,
                    actionLabel = "Add Account",
                    onAction = { showAddAccount = true },
                )
            }
        } else {
            items(accounts, key = { it.id }) { account ->
                AccountCard(account, vm, onEdit = { editAccount = account }, onArchive = { vm.archiveAccount(account.id) }, onReactivate = { vm.reactivateAccount(account.id) }, isMasked = isMasked)
            }
        }

        item { Text("Quick Actions", style = MaterialTheme.typography.titleLarge) }
        item {
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showEvent = true }, enabled = activeAccounts.isNotEmpty(), modifier = Modifier.weight(1f).testTag("add-income")) { Text("+ Income") }
                Button(onClick = { showEvent = false }, enabled = activeAccounts.isNotEmpty(), modifier = Modifier.weight(1f).testTag("add-expense")) { Text("+ Expense") }
            }
        }
        item {
            Button(onClick = { showTransfer = true }, enabled = activeAccounts.size >= 2, modifier = Modifier.fillMaxWidth().testTag("add-transfer")) {
                Text("Transfer Between Accounts")
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Recurring Schedules", style = MaterialTheme.typography.titleLarge)
                OutlinedButton(onClick = { showRecurring = true }, enabled = accounts.isNotEmpty(), modifier = Modifier.testTag("add-recurring")) { Text("Add") }
            }
        }
        if (recurringItems.isEmpty()) {
            item { Text("No recurring items yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(recurringItems, key = { it.id }) { recurring ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), Arrangement.spacedBy(6.dp)) {
                        Text(recurring.title, style = MaterialTheme.typography.titleMedium)
                        Text("${recurring.eventType} · ${recurring.category ?: "General"} · ${if (isMasked) "PKR ••••••" else PkrMoneyInput.formatMinorUnits(recurring.amountMinor)} · ${recurring.frequency}", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { vm.confirmRecurringItemNow(application, recurring.id) }, modifier = Modifier.testTag("mark-paid-${recurring.id}")) { Text("Confirm now") }
                            TextButton(onClick = { vm.pauseRecurringItem(application, recurring.id) }) { Text("Pause") }
                        }
                    }
                }
            }
        }
    }

    showEvent?.let { income -> AddEventDialog(vm, activeAccounts, income) { showEvent = null } }
    if (showTransfer) TransferDialog(vm, activeAccounts) { showTransfer = false }
    if (showRecurring) RecurringItemDialog(vm, application, activeAccounts) { showRecurring = false }
    if (showAddAccount) AddAccountDialog(vm) { showAddAccount = false }
    editAccount?.let { account -> EditAccountDialog(account, vm, onDismiss = { editAccount = null }) }
}

@Composable
private fun BillsScreen(vm: MainViewModel, application: PassportApplication, padding: PaddingValues) {
    val profiles by vm.utilityProfiles.collectAsState(initial = emptyList())
    val occurrences by vm.monthlyOccurrences.collectAsState(initial = emptyList())
    val isMasked = LocalPrivacyMode.current
    val thisMonthTotals by vm.thisMonthTotals.collectAsState()
    val unassignedEvents by vm.unassignedEvents.collectAsState()
    val financialContexts by vm.financialContexts.collectAsState()


    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategoryFilter by rememberSaveable { mutableStateOf("All") }
    var selectedStatusFilter by rememberSaveable { mutableStateOf("Active") }
    var selectedProfileForDetails by remember { mutableStateOf<UtilityBillProfileEntity?>(null) }

    val filteredProfiles = remember(profiles, searchQuery, selectedCategoryFilter, selectedStatusFilter) {
        profiles.filter { profile ->
            val matchesSearch = profile.name.contains(searchQuery, ignoreCase = true) ||
                profile.referenceNumber.contains(searchQuery, ignoreCase = true) ||
                (profile.provider ?: "").contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == "All" || UtilityCategory.canonicalLabel(profile.category) == selectedCategoryFilter
            val matchesStatus = when (selectedStatusFilter) {
                "Active" -> profile.status == "ACTIVE"
                "Archived" -> profile.status == "ARCHIVED"
                else -> true
            }
            matchesSearch && matchesCategory && matchesStatus
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Living Bills", style = MaterialTheme.typography.headlineMedium)
        Text("Utility Connections", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by name, provider, or reference") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bills-search"),
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Active", "Archived", "All").forEach { statusOpt ->
                FilterChip(
                    selected = selectedStatusFilter == statusOpt,
                    onClick = { selectedStatusFilter = statusOpt },
                    label = { Text(statusOpt) },
                    modifier = Modifier.testTag("filter-status-$statusOpt"),
                )
            }
        }

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf("All") + UtilityCategory.selectable.map { it.label }).forEach { catOpt ->
                FilterChip(
                    selected = selectedCategoryFilter == catOpt,
                    onClick = { selectedCategoryFilter = catOpt },
                    label = { Text(catOpt) },
                    modifier = Modifier.testTag("filter-category-$catOpt"),
                )
            }
        }

        if (filteredProfiles.isEmpty()) {
            VexelEmptyState(
                title = "No utility bills found",
                description = "Tap + below to add your electricity, mobile, gas, or internet bill profile.",
                icon = Icons.Filled.Description,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                items(filteredProfiles, key = { it.id }) { profile ->
                    LivingBillCard(
                        profile = profile,
                        occurrences = occurrences,
                        onClick = { selectedProfileForDetails = profile },
                        isMasked = isMasked,
                    )
                }
            }
        }
    }

    selectedProfileForDetails?.let { profile ->
        UtilityProfileDetailsDialog(profile, vm, application, onDismiss = { selectedProfileForDetails = null })
    }
}

@Composable
private fun HistoryScreen(vm: MainViewModel, application: PassportApplication, padding: PaddingValues) {
    val occurrences by vm.monthlyOccurrences.collectAsState(initial = emptyList())
    val profiles by vm.utilityProfiles.collectAsState(initial = emptyList())
    val recentEvents by vm.recentEvents.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val isMasked = LocalPrivacyMode.current
    val thisMonthTotals by vm.thisMonthTotals.collectAsState()
    val unassignedEvents by vm.unassignedEvents.collectAsState()
    val financialContexts by vm.financialContexts.collectAsState()


    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedStatus by rememberSaveable { mutableStateOf("All") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    var selectedOccurrenceForDetails by remember { mutableStateOf<MonthlyBillOccurrenceEntity?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Financial Memory", style = MaterialTheme.typography.headlineMedium)
        Text("Global Bill & Payment History", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search memory log by description or category") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history-search"),
        )

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                var expanded by remember { mutableStateOf(false) }
                OutlinedButton(onClick = { expanded = true }, modifier = Modifier.testTag("filter-status-button")) {
                    Text("Type: $selectedStatus")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("All", "INCOME", "EXPENSE", "TRANSFER").forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(if (opt == "All") "All Types" else opt.lowercase().replaceFirstChar(Char::uppercase)) },
                            onClick = { selectedStatus = opt; expanded = false },
                            modifier = Modifier.testTag("filter-status-opt-$opt"),
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
                    (listOf("All") + UtilityCategory.selectable.map { it.label }).forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = { selectedCategory = opt; expanded = false },
                            modifier = Modifier.testTag("filter-category-opt-$opt"),
                        )
                    }
                }
            }

            if (searchQuery.isNotEmpty() || selectedStatus != "All" || selectedCategory != "All") {
                TextButton(
                    onClick = {
                        searchQuery = ""
                        selectedStatus = "All"
                        selectedCategory = "All"
                    },
                    modifier = Modifier.testTag("filter-reset"),
                ) { Text("Reset") }
            }
        }

        val filteredEvents = remember(recentEvents, searchQuery, selectedCategory, selectedStatus) {
            recentEvents.filter { event ->
                val matchesSearch = event.description.contains(searchQuery, ignoreCase = true) ||
                    (event.category ?: "").contains(searchQuery, ignoreCase = true)
                val matchesCategory = selectedCategory == "All" || (event.category ?: "").equals(selectedCategory, ignoreCase = true)
                val matchesStatus = selectedStatus == "All" || event.eventType.equals(selectedStatus, ignoreCase = true)
                matchesSearch && matchesCategory && matchesStatus
            }
        }
        val filteredOccurrences = remember(occurrences, profiles, searchQuery, selectedStatus, selectedCategory) {
            if (selectedStatus != "All") emptyList() else occurrences.filter { occurrence ->
                val profile = profiles.firstOrNull { it.id == occurrence.profileId }
                val matchesSearch = profile?.name?.contains(searchQuery, ignoreCase = true) == true ||
                    profile?.category?.contains(searchQuery, ignoreCase = true) == true
                val matchesCategory = selectedCategory == "All" || profile?.category.equals(selectedCategory, ignoreCase = true)
                matchesSearch && matchesCategory
            }
        }

        if (filteredEvents.isEmpty() && filteredOccurrences.isEmpty()) {
            VexelEmptyState(
                title = "Financial Memory is Empty",
                description = "As you add bills, income, and expenses, your chronological financial record will be securely stored here.",
                icon = Icons.Filled.Folder,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("history-list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val grouped = filteredEvents.groupBy { LocalDate.ofEpochDay(it.dateEpochDay) }
                grouped.forEach { (date, events) ->
                    item { FinancialDayGroupHeader(date = date) }
                    items(events, key = { it.id }) { event ->
                        val accName = accounts.find { it.id == event.accountId }?.name
                        FinancialTimelineEventRow(
                            event = event,
                            accountName = accName,
                            onClick = {},
                            isMasked = isMasked,
                        )
                    }
                }
                items(filteredOccurrences, key = { "bill-${it.id}" }) { occurrence ->
                    val profile = profiles.firstOrNull { it.id == occurrence.profileId }
                    Card(
                        onClick = { selectedOccurrenceForDetails = occurrence },
                        modifier = Modifier.fillMaxWidth().testTag("history-bill-${occurrence.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(profile?.name ?: "Utility bill", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Bill · ${occurrence.status}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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

@Composable private fun AccountCard(account: AccountEntity, vm: MainViewModel, onEdit: () -> Unit, onArchive: () -> Unit, onReactivate: () -> Unit, isMasked: Boolean) {
    val movement by vm.accountMovement(account.id).collectAsState(0L)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) {
            Text(account.name, style = MaterialTheme.typography.titleMedium)
            Text(listOfNotNull(account.accountType.lowercase().replaceFirstChar(Char::uppercase), account.context, account.institution, account.status.lowercase().replaceFirstChar(Char::uppercase)).joinToString(" · "))
            Text("Current balance ${if (isMasked) "PKR ••••••" else PkrMoneyInput.formatMinorUnits(account.openingBalanceMinor + movement)}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("Opening balance ${if (isMasked) "PKR ••••••" else PkrMoneyInput.formatMinorUnits(account.openingBalanceMinor)}", style = MaterialTheme.typography.bodySmall)
            account.notes?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("Edit") }
                
                var showAdjust by remember { mutableStateOf(false) }
                if (showAdjust) {
                    var newBalance by remember { mutableStateOf("") }
                    AlertDialog(onDismissRequest = { showAdjust = false }, title = { Text("Set Current Balance") }, text = { AmountField(newBalance, { newBalance = it }, "Actual balance in account") }, confirmButton = { Button(onClick = { val target = PkrMoneyInput.toMinorUnits(newBalance); val diff = target - (account.openingBalanceMinor + movement); vm.addAdjustment(account.id, diff, "Balance Correction"); showAdjust = false }) { Text("Set") } })
                }
                TextButton(onClick = { showAdjust = true }) { Text("Set Balance") }
                if (account.status == "ACTIVE") TextButton(onClick = onArchive) { Text("Archive") } else TextButton(onClick = onReactivate) { Text("Reactivate") }
            }
        }
    }
}

@Composable private fun AddAccountDialog(vm: MainViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }; var amount by rememberSaveable { mutableStateOf("") }
    var institution by rememberSaveable { mutableStateOf("") }; var notes by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("BANK") }; var accountContext by rememberSaveable { mutableStateOf("Personal / Home") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add account") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Account name") }, singleLine = true, modifier = Modifier.testTag("account-name"))
                Text("Type")
                Row(Modifier.horizontalScroll(rememberScrollState()), Arrangement.spacedBy(4.dp)) {
                    listOf("CASH", "BANK", "WALLET", "OTHER").forEach { option ->
                        FilterChip(type == option, { type = option }, { Text(option.lowercase().replaceFirstChar(Char::uppercase)) })
                    }
                }
                Text("Context")
                Row(Modifier.horizontalScroll(rememberScrollState()), Arrangement.spacedBy(4.dp)) {
                    listOf("Personal / Home", "Clinic / Professional", "Other Business").forEach { option ->
                        FilterChip(accountContext == option, { accountContext = option }, { Text(option) })
                    }
                }
                OutlinedTextField(institution, { institution = it }, label = { Text("Institution (optional)") }, singleLine = true, modifier = Modifier.testTag("account-institution"))
                AmountField(amount, { amount = it }, "Opening balance (PKR)", modifier = Modifier.testTag("account-amount"))
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, singleLine = true, modifier = Modifier.testTag("account-notes"))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    vm.addAccount(name, type, PkrMoneyInput.toMinorUnits(amount), institution.takeIf { it.isNotBlank() }, notes.takeIf { it.isNotBlank() }, accountContext)
                    onDismiss()
                },
                enabled = name.isNotBlank() && runCatching { PkrMoneyInput.parseRupees(amount) }.isSuccess,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable private fun EditAccountDialog(account: AccountEntity, vm: MainViewModel, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(account.name) }
    var amount by rememberSaveable { mutableStateOf((account.openingBalanceMinor / 100).toString()) }
    var institution by rememberSaveable { mutableStateOf(account.institution.orEmpty()) }
    var notes by rememberSaveable { mutableStateOf(account.notes.orEmpty()) }
    var type by rememberSaveable { mutableStateOf(account.accountType) }
    var accountContext by rememberSaveable { mutableStateOf(account.context ?: "Personal / Home") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit account") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Account name") }, singleLine = true)
                Text("Type")
                Row(Modifier.horizontalScroll(rememberScrollState()), Arrangement.spacedBy(4.dp)) {
                    listOf("CASH", "BANK", "WALLET", "OTHER").forEach { option -> FilterChip(type == option, { type = option }, { Text(option.lowercase().replaceFirstChar(Char::uppercase)) }) }
                }
                OutlinedTextField(accountContext, { accountContext = it }, label = { Text("Context (optional)") }, singleLine = true)
                OutlinedTextField(institution, { institution = it }, label = { Text("Institution (optional)") }, singleLine = true)
                AmountField(amount, { amount = it }, "Opening balance (PKR)")
                OutlinedTextField(notes, { notes = it }, label = { Text("Notes (optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    vm.updateAccount(account.id, name, type, PkrMoneyInput.toMinorUnits(amount), institution.takeIf { it.isNotBlank() }, notes.takeIf { it.isNotBlank() }, accountContext.takeIf { it.isNotBlank() })
                    onDismiss()
                },
                enabled = name.isNotBlank() && runCatching { PkrMoneyInput.parseRupees(amount) }.isSuccess,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable private fun AddEventDialog(vm: MainViewModel, accounts: List<AccountEntity>, initialIncome: Boolean, onDismiss: () -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }; var description by rememberSaveable { mutableStateOf("") }; var category by rememberSaveable { mutableStateOf("") }; var income by rememberSaveable { mutableStateOf(initialIncome) }; var accountId by rememberSaveable { mutableStateOf(accounts.firstOrNull()?.id.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var incomeSourceId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNewIncomeSource by rememberSaveable { mutableStateOf(false) }
    var newIncomeSourceName by rememberSaveable { mutableStateOf("") }
    val recentEvents by vm.recentEvents.collectAsState()
    val incomeSources by vm.incomeSources.collectAsState()
    val categorySuggestions = remember(recentEvents) { recentEvents.mapNotNull { it.category }.filter { it.isNotBlank() }.distinct().take(8) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (income) "Record income" else "Record expense") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ income = true }) { Text("Income") }; OutlinedButton({ income = false }) { Text("Expense") } }
                AccountPicker("Account", accounts, accountId) { accountId = it }
                DateField("Date", date, { date = it }, testTag = "money-event-date")
                AmountField(amount, { amount = it }, "Amount (PKR)", modifier = Modifier.testTag("money-event-amount"))
                OutlinedTextField(description, { description = it }, label = { Text("What happened? (optional)") }, singleLine = true, modifier = Modifier.testTag("money-event-description"))
                OutlinedTextField(category, { category = it }, label = { Text("Category (optional)") }, singleLine = true, modifier = Modifier.testTag("money-event-category"))
                if (categorySuggestions.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) { categorySuggestions.forEach { suggestion -> OutlinedButton(onClick = { category = suggestion }, modifier = Modifier.testTag("category-chip-$suggestion")) { Text(suggestion) } } }
                if (income) {
                    IncomeSourcePicker(incomeSources, incomeSourceId) { incomeSourceId = it }
                    if (showNewIncomeSource) {
                        OutlinedTextField(newIncomeSourceName, { newIncomeSourceName = it }, label = { Text("New source name") }, singleLine = true, modifier = Modifier.testTag("new-income-source-name"))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { vm.addIncomeSource(newIncomeSourceName, "OTHER"); showNewIncomeSource = false; newIncomeSourceName = "" }, enabled = newIncomeSourceName.isNotBlank(), modifier = Modifier.testTag("save-new-income-source")) { Text("Add source") }
                            TextButton(onClick = { showNewIncomeSource = false; newIncomeSourceName = "" }) { Text("Cancel") }
                        }
                    } else TextButton(onClick = { showNewIncomeSource = true }, modifier = Modifier.testTag("add-new-income-source")) { Text("+ New income source") }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    vm.addEvent(if (income) FinancialEventType.INCOME else FinancialEventType.EXPENSE, PkrMoneyInput.toMinorUnits(amount, false), accountId, description, category, date, if (income) incomeSourceId else null)
                    onDismiss()
                },
                enabled = accountId.isNotBlank() && runCatching { PkrMoneyInput.parseRupees(amount, false) }.isSuccess,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable private fun RecurringItemDialog(vm: MainViewModel, application: PassportApplication, accounts: List<AccountEntity>, onDismiss: () -> Unit) {
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
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ income = true }) { Text("Income") }; OutlinedButton({ income = false }) { Text("Expense") } }
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.testTag("recurring-title"))
                AccountPicker("Account", accounts, accountId) { accountId = it }
                AmountField(amount, { amount = it }, "Amount (PKR)", modifier = Modifier.testTag("recurring-amount"))
                OutlinedTextField(category, { category = it }, label = { Text("Category (optional)") }, singleLine = true, modifier = Modifier.testTag("recurring-category"))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("WEEKLY", "MONTHLY", "QUARTERLY", "YEARLY").forEach { option -> OutlinedButton({ frequency = option }) { Text(option.take(3)) } } }
                OutlinedTextField(delayDays, { delayDays = it.filter(Char::isDigit) }, label = { Text("First reminder in days") }, singleLine = true, modifier = Modifier.testTag("recurring-delay"))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    vm.addRecurringItem(application, title, if (income) FinancialEventType.INCOME else FinancialEventType.EXPENSE, PkrMoneyInput.toMinorUnits(amount, false), accountId, category, frequency, delayDays.toLong())
                    onDismiss()
                },
                enabled = title.isNotBlank() && accountId.isNotBlank() && runCatching { PkrMoneyInput.parseRupees(amount, false) }.isSuccess && delayDays.toLongOrNull()?.let { it > 0 } == true,
            ) { Text("Save draft") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable private fun TransferDialog(vm: MainViewModel, accounts: List<AccountEntity>, onDismiss: () -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }; var description by rememberSaveable { mutableStateOf("") }; var sourceId by rememberSaveable { mutableStateOf(accounts.getOrNull(0)?.id.orEmpty()) }; var destinationId by rememberSaveable { mutableStateOf(accounts.getOrNull(1)?.id.orEmpty()) }
    var date by rememberSaveable { mutableStateOf(LocalDate.now()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer between accounts") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccountPicker("From account", accounts, sourceId) { sourceId = it }
                AccountPicker("To account", accounts, destinationId) { destinationId = it }
                DateField("Date", date, { date = it })
                AmountField(amount, { amount = it }, "Amount (PKR)")
                OutlinedTextField(description, { description = it }, label = { Text("Reason") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    vm.transfer(sourceId, destinationId, PkrMoneyInput.toMinorUnits(amount, false), description, date)
                    onDismiss()
                },
                enabled = sourceId.isNotBlank() && destinationId.isNotBlank() && sourceId != destinationId && runCatching { PkrMoneyInput.parseRupees(amount, false) }.isSuccess && description.isNotBlank(),
            ) { Text("Transfer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable private fun EmptyModuleScreen(label: String, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.headlineMedium)
        Text("This workspace is preserved and available.")
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeSourcePicker(sources: List<pk.vexel.financepassport.core.database.IncomeSourceEntity>, selectedId: String?, onSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = sources.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(selected?.name.orEmpty(), {}, readOnly = true, label = { Text("Income source (optional)") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth().testTag("income-source-picker"))
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
        OutlinedTextField(selected?.name.orEmpty(), {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account -> DropdownMenuItem(text = { Text(account.name) }, onClick = { onSelected(account.id); expanded = false }) }
        }
    }
}

private fun maskReferenceNumber(ref: String): String {
    if (ref.length <= 4) return ref
    return "••••" + ref.takeLast(4)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBillDialog(
    vm: MainViewModel,
    application: PassportApplication,
    profileToEdit: UtilityBillProfileEntity? = null,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(profileToEdit?.name ?: "") }
    var category by rememberSaveable { mutableStateOf(UtilityCategory.canonicalLabel(profileToEdit?.category ?: "Electricity")) }
    var customCategoryName by rememberSaveable { mutableStateOf(profileToEdit?.customCategoryName ?: "") }
    var referenceNumber by rememberSaveable { mutableStateOf(profileToEdit?.referenceNumber ?: "") }
    var provider by rememberSaveable { mutableStateOf(profileToEdit?.provider ?: "") }
    var locationLabel by rememberSaveable { mutableStateOf(profileToEdit?.locationLabel ?: "Home") }
    var customLocationLabel by rememberSaveable { mutableStateOf(if (profileToEdit?.locationLabel != "Home" && profileToEdit?.locationLabel != "Clinic" && profileToEdit?.locationLabel != "Office") profileToEdit?.locationLabel ?: "" else "") }
    var connectionIdentifier by rememberSaveable { mutableStateOf(profileToEdit?.connectionIdentifier ?: "") }
    var issueDayAnchor by rememberSaveable { mutableStateOf(profileToEdit?.issueDayAnchor?.toString() ?: "15") }
    var dueDayAnchor by rememberSaveable { mutableStateOf(profileToEdit?.dueDayAnchor?.toString() ?: "27") }
    var recurrenceStartMonth by rememberSaveable { mutableStateOf(profileToEdit?.recurrenceStartMonth ?: YearMonth.now().toString()) }
    var reminderPreference by rememberSaveable { mutableStateOf(profileToEdit?.reminderPreference ?: "ENABLED") }
    var notes by rememberSaveable { mutableStateOf(profileToEdit?.notes ?: "") }

    val profiles by vm.utilityProfiles.collectAsState(initial = emptyList())
    var duplicateWarningShown by remember { mutableStateOf(false) }
    var forceSave by remember { mutableStateOf(false) }

    val issueVal = issueDayAnchor.toIntOrNull()
    val dueVal = dueDayAnchor.toIntOrNull()
    val isValid = name.isNotBlank() &&
        referenceNumber.isNotBlank() &&
        (category != "Other" || customCategoryName.isNotBlank()) &&
        issueVal in 1..31 &&
        dueVal in 1..31 &&
        runCatching { YearMonth.parse(recurrenceStartMonth) }.isSuccess

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (profileToEdit == null) "Add Utility Bill" else "Edit Utility Bill") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    UtilityCategory.selectable.map { it.label }.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            modifier = Modifier.testTag("chip-category-$cat"),
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
                            modifier = Modifier.testTag("chip-location-$loc"),
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
                            modifier = Modifier.testTag("chip-reminder-$pref"),
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
                        updatedAtEpochMillis = now,
                    )
                    if (profileToEdit != null) {
                        vm.updateUtilityProfile(context, profile, onSaved = onDismiss)
                    } else {
                        vm.addUtilityProfile(context, profile, onSaved = onDismiss)
                    }
                },
                enabled = isValid,
                modifier = Modifier.testTag("save-bill-button"),
            ) {
                Text(if (duplicateWarningShown) "Save Anyway" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UtilityProfileDetailsDialog(
    profile: UtilityBillProfileEntity,
    vm: MainViewModel,
    application: PassportApplication,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val occurrences by vm.monthlyOccurrences.collectAsState(initial = emptyList())
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

    LaunchedEffect(profileOccurrences, vm.paymentRevision) {
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
            LocalDate.ofEpochDay(latestDate).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        } else "-"
    }

    var showEditProfile by remember { mutableStateOf(false) }
    var showAddHistorical by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var selectedOccurrenceForDetails by remember { mutableStateOf<MonthlyBillOccurrenceEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(profile.name, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .testTag("profile-details-scroll"),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Reference: ${profile.referenceNumber}", style = MaterialTheme.typography.bodyMedium)
                    if (profile.provider != null) Text("Provider: ${profile.provider}", style = MaterialTheme.typography.bodyMedium)
                    if (profile.locationLabel != null) Text("Location: ${profile.locationLabel}", style = MaterialTheme.typography.bodyMedium)
                    Text("Schedule: Issue approx. ${profile.issueDayAnchor}th · Due approx. ${profile.dueDayAnchor}th", style = MaterialTheme.typography.bodySmall)
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
                        Text("Total Paid: ${PkrMoneyInput.formatMinorUnits(totalPaidAmount)}", style = MaterialTheme.typography.titleSmall)
                    }
                }

                Text("Billing History", style = MaterialTheme.typography.titleMedium)
                if (profileOccurrences.isEmpty()) {
                    Text("No billing history found.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        profileOccurrences.forEach { occ ->
                            val monthLabel = remember(occ) {
                                YearMonth.of(occ.billingYear, occ.billingMonth).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                            }
                            Card(
                                onClick = { selectedOccurrenceForDetails = occ },
                                modifier = Modifier.fillMaxWidth().testTag("history-occ-${occ.id}"),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        val isMasked = LocalPrivacyMode.current
                                        Text(monthLabel, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            if (isMasked) "PKR ••••••"
                                            else if (occ.amountMinor != null) PkrMoneyInput.formatMinorUnits(occ.amountMinor)
                                            else "Amount not entered",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    VexelStatusChip(occ.status)
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
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Filled.Archive, "Archive")
                            Spacer(Modifier.width(4.dp))
                            Text("Archive")
                        }
                    } else {
                        Button(
                            onClick = { showAddHistorical = true },
                            modifier = Modifier.weight(1f),
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
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
        },
    )

    if (showEditProfile) {
        AddBillDialog(vm, application, profileToEdit = profile) {
            showEditProfile = false
            onDismiss()
        }
    }

    if (showAddHistorical) {
        AddHistoricalOccurrenceDialog(profile, vm, onDismiss = { showAddHistorical = false })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Utility Connection?") },
            text = { Text("This permanently deletes '${profile.name}' and all its occurrences, payments, and attachments.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteUtilityProfile(context, profile.id)
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete Everything") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
        )
    }

    selectedOccurrenceForDetails?.let { occ ->
        MonthlyOccurrenceDetailsDialog(occ, vm, application, onDismiss = { selectedOccurrenceForDetails = null })
    }
}

@Composable
private fun AddHistoricalOccurrenceDialog(
    profile: UtilityBillProfileEntity,
    vm: MainViewModel,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var year by rememberSaveable { mutableStateOf(LocalDate.now().year.toString()) }
    var month by rememberSaveable { mutableStateOf(LocalDate.now().monthValue.toString()) }
    var amount by rememberSaveable { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val occurrences by vm.monthlyOccurrences.collectAsState(initial = emptyList())

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
                    updatedAtEpochMillis = now,
                )
                vm.addMonthlyOccurrence(context, occ)
                onDismiss()
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyOccurrenceDetailsDialog(
    occurrence: MonthlyBillOccurrenceEntity,
    vm: MainViewModel,
    application: PassportApplication,
    onDismiss: () -> Unit,
) {
    val profiles by vm.utilityProfiles.collectAsState(initial = emptyList())
    val activeAccounts by vm.activeAccounts.collectAsState()
    val profile = remember(profiles, occurrence) { profiles.find { it.id == occurrence.profileId } }
    if (profile == null) {
        return
    }

    val context = LocalContext.current

    var actualIssueDate by remember { mutableStateOf(occurrence.actualIssueDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.ofEpochDay(occurrence.expectedIssueDateEpochDay)) }
    var setActualIssueDate by remember { mutableStateOf(occurrence.actualIssueDateEpochDay != null) }

    var actualDueDate by remember { mutableStateOf(occurrence.actualDueDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.ofEpochDay(occurrence.expectedDueDateEpochDay)) }
    var setActualDueDate by remember { mutableStateOf(occurrence.actualDueDateEpochDay != null) }

    var billAmount by remember { mutableStateOf(occurrence.amountMinor?.let { (it / 100).toString() } ?: "") }

    var showPayForm by remember { mutableStateOf(false) }
    var showSkipForm by remember { mutableStateOf(false) }

    var payAmount by remember { mutableStateOf(billAmount) }
    var paymentDate by remember { mutableStateOf(LocalDate.now()) }
    var paymentMode by remember { mutableStateOf("Bank Transfer") }
    var bankName by remember { mutableStateOf("") }
    var transactionReference by remember { mutableStateOf("") }
    var payNotes by remember { mutableStateOf("") }
    var paidFromAccountId by remember { mutableStateOf("") }
    var skipNotes by remember { mutableStateOf("") }

    var paymentRecord by remember { mutableStateOf<PaymentRecordEntity?>(null) }
    LaunchedEffect(occurrence) {
        paymentRecord = vm.getPaymentForOccurrence(occurrence.id)
        paymentRecord?.let { payment ->
            payAmount = (payment.amountPaidMinor / 100).toString()
            paymentDate = LocalDate.ofEpochDay(payment.paymentDateEpochDay)
            paymentMode = payment.paymentMode
            paidFromAccountId = payment.accountId.orEmpty()
            bankName = payment.bankName.orEmpty()
            transactionReference = payment.transactionReference.orEmpty()
            payNotes = payment.notes.orEmpty()
        }
        if (paidFromAccountId.isBlank()) paidFromAccountId = activeAccounts.firstOrNull()?.id.orEmpty()
    }
    LaunchedEffect(activeAccounts) {
        if (paidFromAccountId.isBlank()) paidFromAccountId = activeAccounts.firstOrNull()?.id.orEmpty()
    }

    val monthLabel = remember(occurrence) {
        YearMonth.of(occurrence.billingYear, occurrence.billingMonth).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${profile.name} - $monthLabel", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (showPayForm) {
                    Text("Record Payment", style = MaterialTheme.typography.titleMedium)
                    AmountField(payAmount, { payAmount = it }, "Amount Paid (PKR)", modifier = Modifier.testTag("pay-amount"))
                    DateField("Payment Date", paymentDate, { paymentDate = it }, testTag = "pay-date")
                    AccountPicker("Paid From", activeAccounts, paidFromAccountId) { paidFromAccountId = it }

                    Text("Payment Mode")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cash", "Bank Transfer", "Card", "Mobile Wallet", "Other").forEach { mode ->
                            FilterChip(
                                selected = paymentMode == mode,
                                onClick = { paymentMode = mode },
                                label = { Text(mode) },
                                modifier = Modifier.testTag("chip-paymode-$mode"),
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
                        Text("Expected Issue: ${LocalDate.ofEpochDay(occurrence.expectedIssueDateEpochDay).format(DateTimeFormatter.ofPattern("d MMM yyyy"))}")
                        Text("Expected Due: ${LocalDate.ofEpochDay(occurrence.expectedDueDateEpochDay).format(DateTimeFormatter.ofPattern("d MMM yyyy"))}")
                        Text("Current Status: ${occurrence.status}")
                    }

                    if (paymentRecord != null) {
                        val isMasked = LocalPrivacyMode.current
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Payment Details", style = MaterialTheme.typography.titleMedium)
                                Text("Amount Paid: ${if (isMasked) "PKR ••••••" else PkrMoneyInput.formatMinorUnits(paymentRecord!!.amountPaidMinor)}")
                                Text("Date Paid: ${LocalDate.ofEpochDay(paymentRecord!!.paymentDateEpochDay).format(DateTimeFormatter.ofPattern("d MMM yyyy"))}")
                                Text("Mode: ${paymentRecord!!.paymentMode}")
                                Text("Paid from: ${activeAccounts.firstOrNull { it.id == paymentRecord!!.accountId }?.name ?: "Cash / Unlinked"}")
                            }
                        }
                    } else {
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
                        val existing = paymentRecord
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
                            updatedAtEpochMillis = System.currentTimeMillis(),
                            accountId = paidFromAccountId,
                        )
                        if (existing == null) {
                            vm.addPayment(context, payment)
                        } else {
                            vm.updatePayment(context, existing.id, occurrence.id, amtPaidMinor, paymentDate.toEpochDay(), paymentMode, paidFromAccountId, bankName.trim().takeIf { it.isNotEmpty() }, transactionReference.trim().takeIf { it.isNotEmpty() }, payNotes.trim().takeIf { it.isNotEmpty() })
                        }
                        val finalAmtMinor = billAmount.takeIf { it.isNotBlank() }?.let { PkrMoneyInput.toMinorUnits(it, false) } ?: amtPaidMinor
                        vm.updateMonthlyOccurrence(
                            context,
                            occurrence.copy(
                                actualIssueDateEpochDay = actualIssueDate.toEpochDay().takeIf { setActualIssueDate },
                                actualDueDateEpochDay = actualDueDate.toEpochDay().takeIf { setActualDueDate },
                                amountMinor = finalAmtMinor,
                                status = "Paid",
                                updatedAtEpochMillis = System.currentTimeMillis(),
                            ),
                        )
                        onDismiss()
                    },
                    enabled = paidFromAccountId.isNotBlank() && runCatching { PkrMoneyInput.parseRupees(payAmount, false) }.isSuccess && PkrMoneyInput.toMinorUnits(payAmount, false) > 0,
                    modifier = Modifier.testTag("save-payment-button"),
                ) { Text("Save Payment") }
            } else if (showSkipForm) {
                Button(
                    onClick = {
                        vm.updateMonthlyOccurrence(
                            context,
                            occurrence.copy(
                                status = "Skipped",
                                notes = skipNotes.trim().takeIf { it.isNotEmpty() } ?: "Skipped",
                                updatedAtEpochMillis = System.currentTimeMillis(),
                            ),
                        )
                        onDismiss()
                    },
                    modifier = Modifier.testTag("save-skip-button"),
                ) { Text("Skip Occurrence") }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (paymentRecord != null) {
                        OutlinedButton(onClick = { showPayForm = true }, modifier = Modifier.weight(1f).testTag("edit-payment-button")) { Text("Edit Payment") }
                        Button(
                            onClick = {
                                vm.deletePayment(context, paymentRecord!!.id, occurrence.id)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f).testTag("delete-payment-button"),
                        ) { Text("Delete Payment") }
                    } else if (occurrence.status == "Skipped") {
                        Button(
                            onClick = {
                                vm.updateMonthlyOccurrence(
                                    context,
                                    occurrence.copy(
                                        status = "Pending",
                                        notes = null,
                                        updatedAtEpochMillis = System.currentTimeMillis(),
                                    ),
                                )
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).testTag("unskip-button"),
                        ) { Text("Revert Skip") }
                    } else {
                        Button(onClick = { showPayForm = true; payAmount = billAmount }, modifier = Modifier.weight(1f).testTag("pay-button")) { Text("Mark Paid") }
                        Button(onClick = { showSkipForm = true }, modifier = Modifier.weight(1f).testTag("skip-button")) { Text("Skip Month") }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (showPayForm) showPayForm = false
                    else if (showSkipForm) showSkipForm = false
                    else onDismiss()
                },
            ) { Text(if (showPayForm || showSkipForm) "Back" else "Cancel") }
        },
    )
}
