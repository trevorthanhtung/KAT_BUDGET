package com.katgr0up.katbudget.ui.screens

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.DebtEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.managers.CsvReportManager
import com.katgr0up.katbudget.ui.components.*
import com.katgr0up.katbudget.ui.dialogs.*
import com.katgr0up.katbudget.ui.tabs.*
import com.katgr0up.katbudget.ui.tools.*
import com.katgr0up.katbudget.ui.utils.*
import com.katgr0up.katbudget.viewmodel.DebtViewModel
import com.katgr0up.katbudget.viewmodel.GoalViewModel
import com.katgr0up.katbudget.viewmodel.SettingsViewModel
import com.katgr0up.katbudget.viewmodel.BudgetViewModel
import com.katgr0up.katbudget.utils.ALL_CURRENCIES
import java.io.IOException
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREF_ONBOARDING = "has_seen_onboarding"
private const val PREFS_NAME = "katbudget_prefs"
private const val DASHBOARD_TAG = "DashboardScreen"
private const val BACKUP_MIME_TYPE = "application/octet-stream"
private const val CSV_MIME_TYPE = "text/csv"
private const val TRUNCATE_WRITE_MODE = "w"
private const val REPORT_CURRENCY_ALL = "ALL"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BudgetViewModel,
    settingsViewModel: SettingsViewModel,
    debtViewModel: DebtViewModel,
    goalViewModel: GoalViewModel,
    isDarkTheme: Boolean,
    openQuickAddOnStart: Boolean = false,
    quickAddInitialType: String? = null,
    onQuickAddConsumed: () -> Unit = {},
    onToggleTheme: () -> Unit
) {
    val languageCode by settingsViewModel.languageCode.collectAsState()
    val isEng = languageCode != "vi"
    val defaultCurrency by settingsViewModel.defaultCurrency.collectAsState()
    val hasPinSetup by settingsViewModel.hasPin.collectAsState()
    val isAutoBackupEnabled by settingsViewModel.isAutoBackupEnabled.collectAsState()
    val autoBackupPath by settingsViewModel.autoBackupPath.collectAsState()
    val exchangeRates by settingsViewModel.exchangeRates.collectAsState()
    val isBiometricEnabled by settingsViewModel.isBiometricEnabled.collectAsState()
    val isPrivacyModeEnabled by settingsViewModel.isPrivacyModeEnabled.collectAsState()

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val colors = rememberBudgetColors(isDarkTheme)
    val scope = rememberCoroutineScope()

    var showSplash by remember { mutableStateOf(true) }
    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean(PREF_ONBOARDING, false)) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var activeToolScreen by remember { mutableStateOf("NONE") }

    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionDialogInitialType by remember { mutableStateOf<String?>(null) }
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())

    val fallbackAll = katStringResource(id = R.string.fallback_all, isEng = isEng)
    var selectedSourceFilter by remember { mutableStateOf("") }
    LaunchedEffect(fallbackAll) {
        if (selectedSourceFilter.isBlank()) {
            selectedSourceFilter = fallbackAll
        }
    }
    val activeSourceFilter = selectedSourceFilter.ifBlank { fallbackAll }

    val openingBalanceStr = katStringResource(id = R.string.fallback_opening_balance, isEng = isEng)
    val displayTransactions = transactions.filter {
        it.category != "Số dư ban đầu" && it.category != "Opening Balance" && it.category != openingBalanceStr
    }

    val expenseOptions by viewModel.expenseCategoryNames.collectAsState(initial = listOf(fallbackAll))
    val incomeOptions by viewModel.incomeCategoryNames.collectAsState(initial = listOf(fallbackAll))
    val displayIncomeOptions = incomeOptions.filter {
        it != "Số dư ban đầu" && it != "Opening Balance" && it != openingBalanceStr
    }

    val selectedTag by viewModel.selectedTag.collectAsState()
    val sources by viewModel.allSources.collectAsState(initial = emptyList())

    val hasSources = sources.isNotEmpty()

    val sourceBalances by viewModel.sourceBalances.collectAsState(initial = emptyMap())
    val debts by debtViewModel.allDebts.collectAsState(initial = emptyList())
    val budgets by viewModel.allBudgets.collectAsState(initial = emptyList())
    val expenseCategories by viewModel.expenseCategories.collectAsState(initial = emptyList())
    val recurrings by viewModel.allRecurrings.collectAsState(initial = emptyList())
    val goals by goalViewModel.allGoals.collectAsState(initial = emptyList())

    var showDateRangePicker by remember { mutableStateOf(false) }
    var filterStartDate by remember {
        mutableLongStateOf(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis
        )
    }
    var filterEndDate by remember {
        mutableLongStateOf(
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis
        )
    }
    var chartType by remember { mutableStateOf("EXPENSE") }
    var chartCurrencyFilter by remember { mutableStateOf(REPORT_CURRENCY_ALL) }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }
    var showExportCsvDialog by remember { mutableStateOf(false) }
    var showCreateAccountRequiredDialog by remember { mutableStateOf(false) }
    var createSourceRequest by remember { mutableIntStateOf(0) }
    var askEnableBiometric by remember { mutableStateOf(false) }
    var exportTimeRange by remember { mutableStateOf("ALL") }
    var exportSource by remember { mutableStateOf(fallbackAll) }
    var exportType by remember { mutableStateOf("ALL") }

    var showDebtDialog by remember { mutableStateOf(false) }
    var debtToEdit by remember { mutableStateOf<DebtEntity?>(null) }
    var debtToDelete by remember { mutableStateOf<DebtEntity?>(null) }

    var isAppUnlocked by remember(hasPinSetup) { mutableStateOf(!hasPinSetup) }
    var inputPin by remember { mutableStateOf("") }
    var isPinSetupMode by remember { mutableStateOf(false) }
    var pinSetupValue by remember { mutableStateOf("") }

    val backupFileName = katStringResource(id = R.string.file_backup_name, isEng = isEng)
    val reportFileName = katStringResource(id = R.string.file_report_name, isEng = isEng)
    val msgAutoBackupConfigSuccess = katStringResource(id = R.string.toast_config_backup_success, isEng = isEng)
    val msgBackupExportSuccess = katStringResource(id = R.string.toast_backup_export_success, isEng = isEng)
    val msgBackupExportError = katStringResource(id = R.string.toast_backup_export_error, isEng = isEng)
    val msgBackupProcessing = katStringResource(id = R.string.toast_backup_processing, isEng = isEng)
    val msgRestoreSuccess = katStringResource(id = R.string.toast_restore_success, isEng = isEng)
    val msgRestoreError = katStringResource(id = R.string.toast_restore_error, isEng = isEng)
    val msgCsvSuccess = katStringResource(id = R.string.toast_csv_export_success, isEng = isEng)
    val msgCsvError = katStringResource(id = R.string.toast_csv_export_error, isEng = isEng)
    val msgFilePickerError = katStringResource(id = R.string.toast_file_picker_error, isEng = isEng)

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            runCatching {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
                settingsViewModel.setAutoBackupPath(it.toString())
                settingsViewModel.toggleAutoBackup(true)
            }.onSuccess {
                Toast.makeText(context, msgAutoBackupConfigSuccess, Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                Log.e(DASHBOARD_TAG, "Unable to persist auto backup folder permission.", error)
                Toast.makeText(context, msgBackupExportError, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val fileExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)
    ) { uri ->
        uri?.let {
            Toast.makeText(context, msgBackupProcessing, Toast.LENGTH_SHORT).show()
            scope.launch {
                runCatching {
                    val bytes = withContext(Dispatchers.IO) {
                        viewModel.exportFullBackupData(
                            sources,
                            transactions,
                            debts,
                            budgets,
                            recurrings,
                            goals
                        )
                    }
                    writeBytesToUri(context, it, bytes, TRUNCATE_WRITE_MODE)
                }.onSuccess {
                    Toast.makeText(context, msgBackupExportSuccess, Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Log.e(DASHBOARD_TAG, "Unable to export backup.", error)
                    Toast.makeText(context, msgBackupExportError, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                runCatching {
                    readBytesFromUri(context, it)
                }.onSuccess { bytes ->
                    viewModel.importBackupDataFromBytes(
                        bytes,
                        onSuccess = {
                            Toast.makeText(context, msgRestoreSuccess, Toast.LENGTH_SHORT).show()
                        },
                        onError = {
                            Toast.makeText(context, msgRestoreError, Toast.LENGTH_SHORT).show()
                        }
                    )
                }.onFailure { error ->
                    Log.e(DASHBOARD_TAG, "Unable to read backup file.", error)
                    Toast.makeText(context, msgRestoreError, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(CSV_MIME_TYPE)
    ) { uri ->
        uri?.let {
            scope.launch {
                runCatching {
                    val filteredTransactions = filterTransactionsForCsv(
                        transactions = displayTransactions,
                        exportTimeRange = exportTimeRange,
                        exportSource = exportSource,
                        exportType = exportType,
                        fallbackAll = fallbackAll
                    )
                    val csvData = CsvReportManager.generateCsvReport(filteredTransactions, isEng)
                    writeBytesToUri(
                        context = context,
                        uri = it,
                        bytes = csvData.toByteArray(Charsets.UTF_8),
                        mode = TRUNCATE_WRITE_MODE
                    )
                }.onSuccess {
                    Toast.makeText(context, msgCsvSuccess, Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Log.e(DASHBOARD_TAG, "Unable to export CSV report.", error)
                    Toast.makeText(context, msgCsvError, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> settingsViewModel.setAppBackgroundedTime()
                Lifecycle.Event.ON_RESUME -> if (settingsViewModel.shouldLockApp()) isAppUnlocked = false
                Lifecycle.Event.ON_STOP -> settingsViewModel.triggerInstantBackup()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var backPressedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(2000)
            backPressedOnce = false
        }
    }

    val exitToastMsg = katStringResource(id = R.string.toast_back_press_exit, isEng = isEng)
    BackHandler(enabled = true) {
        when {
            activeToolScreen != "NONE" -> activeToolScreen = "NONE"
            selectedTab != 0 -> {
                selectedTab = 0
                activeToolScreen = "NONE"
            }
            backPressedOnce -> (context as? android.app.Activity)?.finish()
            else -> {
                backPressedOnce = true
                Toast.makeText(context, exitToastMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(openQuickAddOnStart, quickAddInitialType, hasSources, isAppUnlocked, showSplash, showOnboarding) {
        if (!openQuickAddOnStart || showSplash || showOnboarding || !isAppUnlocked) return@LaunchedEffect

        if (hasSources) {
            selectedTab = 0
            activeToolScreen = "NONE"
            transactionDialogInitialType = quickAddInitialType
            showTransactionDialog = true
        }
        onQuickAddConsumed()
    }

    if (showSplash) {
        SplashScreen(colors = colors, onTimeout = { showSplash = false })
        return
    }

    if (showOnboarding) {
        OnboardingScreen(
            colors = colors,
            onFinish = {
                prefs.edit {
                    putBoolean(PREF_ONBOARDING, true)
                }
                showOnboarding = false
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundBrush)
            .drawWithContent {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.accent.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(0f, 0f),
                        radius = size.width * 0.92f
                    ),
                    center = Offset(0f, 0f)
                )
                drawContent()
            }
    ) {
        if (!isAppUnlocked) {
            PinLockScreen(
                isEng = isEng,
                colors = colors,
                isBiometricEnabled = isBiometricEnabled,
                inputPin = inputPin,
                onPinChange = { pin -> inputPin = pin },
                onUnlock = {
                    inputPin = ""
                    settingsViewModel.unlockApp()
                    isAppUnlocked = true
                },
                getSavedPin = { settingsViewModel.getPin() }
            )
        } else {
            androidx.compose.runtime.CompositionLocalProvider(com.katgr0up.katbudget.ui.utils.LocalPrivacyMode provides isPrivacyModeEnabled) {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
                bottomBar = {
                    BudgetBottomBar(
                        selectedTab = selectedTab,
                        activeToolScreen = activeToolScreen,
                        isEng = isEng,
                        colors = colors,
                        hasSources = hasSources,
                        onTabSelected = { tab ->
                            selectedTab = tab
                            activeToolScreen = "NONE"
                        },
                        onMissingSources = { showCreateAccountRequiredDialog = true },
                        onAdd = {
                            if (hasSources) {
                                transactionDialogInitialType = null
                                showTransactionDialog = true
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(paddingValues)
                        .padding(horizontal = 20.dp)
                ) {
                    if (activeToolScreen == "NONE") {
                        BudgetTopBar(
                            title = when (selectedTab) {
                                0 -> katStringResource(id = R.string.screen_title_overview, isEng = isEng)
                                1 -> katStringResource(id = R.string.screen_title_debt, isEng = isEng)
                                2 -> katStringResource(id = R.string.screen_title_report, isEng = isEng)
                                else -> katStringResource(id = R.string.screen_title_settings, isEng = isEng)
                            },
                            isEng = isEng,
                            colors = colors,
                            showGreeting = selectedTab == 0,
                            isPrivacyModeEnabled = isPrivacyModeEnabled,
                            onPrivacyToggle = { settingsViewModel.togglePrivacyMode(it) },
                            onLanguageToggle = { settingsViewModel.cycleLanguage() }
                        )
                    }

                    when (activeToolScreen) {
                        "BUDGET" -> BudgetToolWrapper(
                            viewModel = viewModel,
                            isEng = isEng,
                            defaultCurrency = defaultCurrency,
                            exchangeRates = exchangeRates,
                            colors = colors,
                            onBack = { activeToolScreen = "NONE" }
                        )
                        "RECURRING" -> RecurringToolWrapper(
                            viewModel = viewModel,
                            isEng = isEng,
                            defaultCurrency = defaultCurrency,
                            exchangeRates = exchangeRates,
                            colors = colors,
                            sources = sources,
                            onBack = { activeToolScreen = "NONE" }
                        )
                        "GOAL" -> GoalToolWrapper(
                            goalViewModel = goalViewModel,
                            isEng = isEng,
                            defaultCurrency = defaultCurrency,
                            exchangeRates = exchangeRates,
                            colors = colors,
                            sources = sources,
                            sourceBalances = sourceBalances,
                            onBack = { activeToolScreen = "NONE" }
                        )
                        else -> when (selectedTab) {
                            0 -> DashboardTabWrapper(
                                viewModel = viewModel,
                                isEng = isEng,
                                defaultCurrency = defaultCurrency,
                                colors = colors,
                                showTransactionDialog = false,
                                onTransactionDialogDismiss = { },
                                selectedSourceFilter = activeSourceFilter,
                                onSourceFilterChanged = { selectedSourceFilter = it },
                                createSourceRequest = createSourceRequest,
                                onCreateSourceRequestConsumed = { createSourceRequest = 0 }
                            )
                            1 -> DebtTab(
                                isEng = isEng,
                                colors = colors,
                                debts = debts,
                                sources = sources,
                                onCreateDebt = {
                                    debtToEdit = null
                                    showDebtDialog = true
                                },
                                onEditDebt = { debt ->
                                    debtToEdit = debt
                                    showDebtDialog = true
                                },
                                onDeleteDebt = { debt -> debtToDelete = debt },
                                onPayDebt = { debt, amount, source ->
                                    debtViewModel.payDebt(debt, amount, source)
                                }
                            )
                            2 -> ReportsTab(
                                isEng = isEng,
                                transactions = displayTransactions,
                                budgets = budgets,
                                categories = expenseCategories,
                                expenseOptions = expenseOptions,
                                incomeOptions = displayIncomeOptions,
                                selectedTag = selectedTag,
                                chartType = chartType,
                                chartCurrencyFilter = chartCurrencyFilter,
                                filterStartDate = filterStartDate,
                                filterEndDate = filterEndDate,
                                colors = colors,
                                availableCurrencies = ALL_CURRENCIES,
                                exchangeRates = exchangeRates,
                                defaultCurrency = defaultCurrency,
                                onChartTypeChanged = {
                                    chartType = it
                                    viewModel.setFilterTag(fallbackAll)
                                },
                                onCurrencyChanged = { chartCurrencyFilter = it },
                                onTagChanged = { viewModel.setFilterTag(it) },
                                onDateRangeClick = { showDateRangePicker = true }
                            )
                            else -> SettingsTab(
                                isEng = isEng,
                                isDarkTheme = isDarkTheme,
                                defaultCurrency = defaultCurrency,
                                hasPinSetup = hasPinSetup,
                                isBiometricEnabled = isBiometricEnabled,
                                isPrivacyModeEnabled = isPrivacyModeEnabled,
                                isAutoBackupEnabled = isAutoBackupEnabled,
                                autoBackupPath = autoBackupPath,
                                colors = colors,
                                availableCurrencies = ALL_CURRENCIES,
                                exchangeRates = exchangeRates,
                                onCurrencyChanged = { settingsViewModel.setDefaultCurrency(it) },
                                onBudgetClick = { activeToolScreen = "BUDGET" },
                                onRecurringClick = { activeToolScreen = "RECURRING" },
                                onGoalClick = { activeToolScreen = "GOAL" },
                                onExportCsv = { showExportCsvDialog = true },
                                onBackup = {
                                    try {
                                        fileExportLauncher.launch(backupFileName)
                                    } catch (error: Throwable) {
                                        Log.e(DASHBOARD_TAG, "Unable to launch backup file picker.", error)
                                        Toast.makeText(context, msgFilePickerError, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onAutoBackupToggle = { settingsViewModel.toggleAutoBackup(it) },
                                onSelectBackupFolder = {
                                    try {
                                        folderPickerLauncher.launch(null)
                                    } catch (error: Exception) {
                                        Log.e(DASHBOARD_TAG, "Unable to launch backup folder picker.", error)
                                        Toast.makeText(context, msgFilePickerError, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onRestore = {
                                    try {
                                        fileImportLauncher.launch(arrayOf("*/*"))
                                    } catch (error: Exception) {
                                        Log.e(DASHBOARD_TAG, "Unable to launch restore file picker.", error)
                                        Toast.makeText(context, msgFilePickerError, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onPinToggle = { enabled ->
                                    if (enabled) isPinSetupMode = true else settingsViewModel.clearPin()
                                },
                                onBiometricToggle = { enabled -> settingsViewModel.toggleBiometric(enabled) },
                                onPrivacyToggle = { enabled -> settingsViewModel.togglePrivacyMode(enabled) },
                                onThemeToggle = { onToggleTheme() },
                                onSupportClick = { showSupportDialog = true },
                                onAboutClick = { showAboutDialog = true },
                                onDonateClick = { showDonateDialog = true },
                                onRefreshRates = { settingsViewModel.fetchExchangeRates(context) }
                            )
                        }
                    }
                }
            }
        }
        }

        if (showTransactionDialog) {
            TransactionDialog(
                transactionToEdit = null,
                initialSourceName = activeSourceFilter,
                isEng = isEng,
                sources = sources,
                expenseOptions = expenseOptions,
                incomeOptions = displayIncomeOptions,
                colors = colors,
                defaultCurrency = defaultCurrency,
                initialType = transactionDialogInitialType,
                onDismiss = {
                    showTransactionDialog = false
                    transactionDialogInitialType = null
                },
                onSave = { form ->
                    if (form.type == "TRANSFER_OUT") {
                        viewModel.transferMoney(
                            form.amount,
                            form.sourceName,
                            form.targetSourceName,
                            form.currency,
                            form.note,
                            form.timestamp
                        )
                    } else {
                        viewModel.addOrUpdateTransaction(
                            0,
                            form.amount,
                            form.type,
                            form.category,
                            form.note,
                            form.sourceName,
                            form.currency,
                            form.timestamp,
                            form.imageUri
                        )
                    }
                    showTransactionDialog = false
                    transactionDialogInitialType = null
                },
                onCreateCategory = { name, type -> viewModel.addCategory(name, type) },
                onDeleteCategory = { name, type -> viewModel.deleteCategory(name, type) }
            )
        }

        if (showExportCsvDialog) {
            ExportCsvDialog(
                isEng = isEng,
                colors = colors,
                sources = sources,
                onDismiss = { showExportCsvDialog = false },
                onConfirm = { time, src, type ->
                    exportTimeRange = time
                    exportSource = src
                    exportType = type
                    showExportCsvDialog = false
                    try {
                        csvExportLauncher.launch(reportFileName)
                    } catch (error: Throwable) {
                        Log.e(DASHBOARD_TAG, "Unable to launch CSV file picker.", error)
                        Toast.makeText(context, msgFilePickerError, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (isPinSetupMode) {
            PinSetupDialog(
                isEng = isEng,
                colors = colors,
                pinValue = pinSetupValue,
                onPinChanged = { value ->
                    if (value.length <= 4 && value.all { it.isDigit() }) pinSetupValue = value
                },
                onDismiss = {
                    isPinSetupMode = false
                    pinSetupValue = ""
                },
                onSave = {
                    if (pinSetupValue.length == 4) {
                        settingsViewModel.savePin(pinSetupValue)
                        isPinSetupMode = false
                        pinSetupValue = ""
                        isAppUnlocked = true
                        
                        val biometricStatus = androidx.biometric.BiometricManager.from(context)
                            .canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK)
                        if (biometricStatus == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
                            askEnableBiometric = true
                        }
                    }
                }
            )
        }

        if (askEnableBiometric) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { askEnableBiometric = false },
                title = { Text(katStringResource(id = R.string.dialog_biometric_prompt_title, isEng = isEng), color = colors.text) },
                text = { Text(katStringResource(id = R.string.dialog_biometric_prompt_msg, isEng = isEng), color = colors.subText) },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            settingsViewModel.toggleBiometric(true)
                            askEnableBiometric = false
                        }
                    ) {
                        Text(katStringResource(id = R.string.btn_confirm, isEng = isEng), color = colors.accent)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { askEnableBiometric = false }
                    ) {
                        Text(katStringResource(id = R.string.btn_cancel, isEng = isEng), color = colors.accent)
                    }
                },
                containerColor = colors.surface
            )
        }

        if (showCreateAccountRequiredDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showCreateAccountRequiredDialog = false },
                containerColor = colors.surface,
                shape = RoundedCornerShape(28.dp),
                title = {
                    Text(
                        text = katStringResource(id = R.string.dialog_create_account_required_title, isEng = isEng),
                        color = colors.text,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = katStringResource(id = R.string.dialog_create_account_required_msg, isEng = isEng),
                        color = colors.subText
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedTab = 0
                            activeToolScreen = "NONE"
                            createSourceRequest += 1
                            showCreateAccountRequiredDialog = false
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.background
                        )
                    ) {
                        Text(
                            text = katStringResource(id = R.string.dialog_create_account_required_action, isEng = isEng),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateAccountRequiredDialog = false }) {
                        Text(
                            text = katStringResource(id = R.string.btn_close, isEng = isEng),
                            color = colors.subText
                        )
                    }
                }
            )
        }

        if (showAboutDialog) {
            AboutDialog(
                isEng = isEng,
                colors = colors,
                onDismiss = { showAboutDialog = false }
            )
        }

        if (showSupportDialog) {
            SupportDialog(
                isEng = isEng,
                colors = colors,
                onDismiss = { showSupportDialog = false }
            )
        }

        if (showDebtDialog) {
            DebtDialog(
                debtToEdit = debtToEdit,
                sources = sources,
                transactions = transactions,
                isEng = isEng,
                colors = colors,
                onDismiss = {
                    showDebtDialog = false
                    debtToEdit = null
                },
                onSave = { name, amount, currency, type, note, dueDate, sourceName, includeInCashFlow ->
                    debtViewModel.addOrUpdateDebt(
                        debtToEdit?.id ?: 0,
                        name,
                        amount,
                        currency,
                        type,
                        note,
                        dueDate,
                        sourceName,
                        includeInCashFlow
                    )
                    showDebtDialog = false
                    debtToEdit = null
                }
            )
        }

        debtToDelete?.let { debt ->
            val title = katStringResource(id = R.string.dialog_delete_debt_title, isEng = isEng)
            val message = katStringResource(
                id = R.string.dialog_delete_debt_msg,
                isEng = isEng,
                debt.personName
            )

            ConfirmDeleteDialog(
                isEng = isEng,
                colors = colors,
                title = title,
                message = message,
                onDismiss = { debtToDelete = null }
            ) {
                debtViewModel.deleteDebt(debt)
                debtToDelete = null
            }
        }

        if (showDonateDialog) {
            DonateDialog(isEng = isEng, colors = colors) {
                showDonateDialog = false
            }
        }

        if (showDateRangePicker) {
            val dateRangePickerState = rememberDateRangePickerState(
                initialSelectedStartDateMillis = filterStartDate,
                initialSelectedEndDateMillis = filterEndDate
            )

            DatePickerDialog(
                onDismissRequest = { showDateRangePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dateRangePickerState.selectedStartDateMillis?.let {
                                filterStartDate = it
                            }
                            dateRangePickerState.selectedEndDateMillis?.let {
                                filterEndDate = it + 86_399_999L
                            }
                            showDateRangePicker = false
                        }
                    ) {
                        Text(katStringResource(id = R.string.btn_apply, isEng = isEng))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDateRangePicker = false }) {
                        Text(katStringResource(id = R.string.btn_cancel, isEng = isEng))
                    }
                }
            ) {
                DateRangePicker(state = dateRangePickerState)
            }
        }
    }
}

private suspend fun writeBytesToUri(
    context: Context,
    uri: Uri,
    bytes: ByteArray,
    mode: String
) = withContext(Dispatchers.IO) {
    val outputStream = context.contentResolver.openOutputStream(uri, mode)
        ?: throw IOException("Unable to open output stream for $uri")

    outputStream.use { output ->
        output.write(bytes)
        output.flush()
    }
}

private suspend fun readBytesFromUri(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw IOException("Unable to open input stream for $uri")

    inputStream.use { input ->
        input.readBytes().also { bytes ->
            if (bytes.isEmpty()) throw IOException("Selected file is empty: $uri")
        }
    }
}

private fun filterTransactionsForCsv(
    transactions: List<TransactionEntity>,
    exportTimeRange: String,
    exportSource: String,
    exportType: String,
    fallbackAll: String
): List<TransactionEntity> {
    val (startMillis, endMillis) = resolveExportDateRange(exportTimeRange)

    return transactions.filter { transaction ->
        val matchesTime = transaction.timestamp in startMillis..endMillis
        val matchesSource = exportSource == fallbackAll || transaction.sourceName == exportSource
        val matchesType = when (exportType) {
            "EXPENSE" -> transaction.type == "EXPENSE"
            "INCOME" -> transaction.type == "INCOME"
            else -> true
        }

        matchesTime && matchesSource && matchesType
    }
}

private fun resolveExportDateRange(exportTimeRange: String): Pair<Long, Long> {
    return when {
        exportTimeRange == "CURRENT_MONTH" -> monthRange(offsetMonths = 0)
        exportTimeRange == "LAST_MONTH" -> monthRange(offsetMonths = -1)
        exportTimeRange.startsWith("CUSTOM:") -> {
            val parts = exportTimeRange.split(":")
            val startMillis = parts.getOrNull(1)?.toLongOrNull() ?: 0L
            val endMillis = parts.getOrNull(2)?.toLongOrNull()
                ?.let { it + 86_399_999L }
                ?: Long.MAX_VALUE
            startMillis to endMillis
        }
        else -> 0L to Long.MAX_VALUE
    }
}

private fun monthRange(offsetMonths: Int): Pair<Long, Long> {
    val calendar = Calendar.getInstance().apply {
        add(Calendar.MONTH, offsetMonths)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startMillis = calendar.timeInMillis

    calendar.apply {
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    return startMillis to calendar.timeInMillis
}
