package com.katgr0up.katbudget.ui.tabs

import androidx.compose.runtime.*
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.ui.dialogs.*
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.viewmodel.BudgetViewModel
import com.katgr0up.katbudget.ui.utils.katStringResource

@Composable
fun DashboardTabWrapper(
    viewModel: BudgetViewModel,
    isEng: Boolean,
    defaultCurrency: String,
    colors: BudgetColors,
    showTransactionDialog: Boolean,
    onTransactionDialogDismiss: () -> Unit,
    selectedSourceFilter: String,
    onSourceFilterChanged: (String) -> Unit,
    createSourceRequest: Int = 0,
    onCreateSourceRequestConsumed: () -> Unit = {}
) {
    val fallbackAll = katStringResource(id = R.string.fallback_all, isEng = isEng)
    val openingBalanceStr = katStringResource(id = R.string.fallback_opening_balance, isEng = isEng)

    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())

    val displayTransactions = transactions.filter {
        it.category != "Số dư ban đầu" && it.category != "Opening Balance" && it.category != openingBalanceStr
    }

    val balance by viewModel.getCurrentBalance(defaultCurrency).collectAsState(initial = emptyMap())
    val grandTotal by viewModel.getGrandTotalConverted(defaultCurrency).collectAsState(initial = 0.0)
    val sources by viewModel.allSources.collectAsState(initial = emptyList())
    val sourceBalances by viewModel.sourceBalances.collectAsState(initial = emptyMap())
    val budgets by viewModel.allBudgets.collectAsState(initial = emptyList())
    val expenseCategories by viewModel.expenseCategories.collectAsState(initial = emptyList())
    val exchangeRates by viewModel.exchangeRates.collectAsState()
    val expenseOptions by viewModel.expenseCategoryNames.collectAsState(initial = listOf(fallbackAll))
    val incomeOptions by viewModel.incomeCategoryNames.collectAsState(initial = listOf(fallbackAll))

    val displayIncomeOptions = incomeOptions.filter {
        it != "Số dư ban đầu" && it != "Opening Balance" && it != openingBalanceStr
    }

    var searchQuery by remember { mutableStateOf("") }
    var showAllTransactions by remember { mutableStateOf(false) }

    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var sourceToEdit by remember { mutableStateOf<SourceEntity?>(null) }
    var sourceToDelete by remember { mutableStateOf<SourceEntity?>(null) }
    var imageToViewUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(createSourceRequest) {
        if (createSourceRequest > 0) {
            sourceToEdit = null
            showSourceDialog = true
            onCreateSourceRequestConsumed()
        }
    }

    LaunchedEffect(selectedSourceFilter, searchQuery) {
        showAllTransactions = false
    }

    DashboardTab(
        isEng = isEng,
        defaultCurrency = defaultCurrency,
        transactions = displayTransactions,
        allTransactions = displayTransactions,
        isShowingAllTransactions = showAllTransactions,
        balance = balance,
        grandTotal = grandTotal,
        sources = sources,
        sourceBalances = sourceBalances,
        budgets = budgets,
        categories = expenseCategories,
        selectedSourceFilter = selectedSourceFilter,
        searchQuery = searchQuery,
        colors = colors,
        exchangeRates = exchangeRates,
        onSourceFilterChanged = onSourceFilterChanged,
        onSearchChanged = { searchQuery = it },
        onCreateSource = { sourceToEdit = null; showSourceDialog = true },
        onEditSource = { sourceToEdit = it; showSourceDialog = true },
        onDeleteSource = { sourceToDelete = it },
        onEditTransaction = { transactionToEdit = it; onTransactionDialogDismiss() },
        onDeleteTransaction = { transactionToDelete = it },
        onToggleTransactionsExpanded = { showAllTransactions = !showAllTransactions },
        onViewImage = { imageToViewUri = it }
    )

    if (showSourceDialog) {
        SourceDialog(
            sourceToEdit = sourceToEdit,
            existingSources = sources,
            isEng = isEng,
            defaultCurrency = defaultCurrency,
            colors = colors,
            onDismiss = {
                showSourceDialog = false
                sourceToEdit = null
            }
        ) { name, type, include, rate, period, openingBalances ->
            viewModel.addOrUpdateSource(sourceToEdit, name, type, include, rate, period)
            if (sourceToEdit == null && openingBalances.isNotEmpty()) {
                val catOpening = openingBalanceStr
                val noteOpening = openingBalanceStr
                val timestamp = System.currentTimeMillis()
                openingBalances.forEachIndexed { index, openingBalance ->
                    viewModel.addOrUpdateTransaction(
                        id = 0,
                        amount = openingBalance.amount,
                        type = "INCOME",
                        category = catOpening,
                        note = noteOpening,
                        sourceName = name,
                        currency = openingBalance.currency,
                        timestamp = timestamp + index,
                        imageUri = null
                    )
                }
            }
            showSourceDialog = false
            sourceToEdit = null
        }
    }

    if (showTransactionDialog || transactionToEdit != null) {
        TransactionDialog(
            transactionToEdit = transactionToEdit, initialSourceName = selectedSourceFilter, isEng = isEng, sources = sources,
            expenseOptions = expenseOptions, incomeOptions = displayIncomeOptions, colors = colors, defaultCurrency = defaultCurrency,
            onDismiss = { onTransactionDialogDismiss(); transactionToEdit = null },
            onSave = { form ->
                if (form.type == "TRANSFER_OUT") viewModel.transferMoney(form.amount, form.sourceName, form.targetSourceName, form.currency, form.note, form.timestamp)
                else viewModel.addOrUpdateTransaction(transactionToEdit?.id ?: 0, form.amount, form.type, form.category, form.note, form.sourceName, form.currency, form.timestamp, form.imageUri)
                onTransactionDialogDismiss(); transactionToEdit = null
            },
            onCreateCategory = { name, type -> viewModel.addCategory(name, type) },
            onDeleteCategory = { name, type -> viewModel.deleteCategory(name, type) }
        )
    }

    sourceToDelete?.let {
        val title = katStringResource(id = R.string.dialog_delete_source_title, isEng = isEng)
        val message = katStringResource(id = R.string.dialog_delete_source_msg, isEng = isEng, it.name)

        ConfirmDeleteDialog(isEng, colors, title, message, { sourceToDelete = null }) {
            viewModel.deleteSource(it)
            if (selectedSourceFilter == it.name) onSourceFilterChanged(fallbackAll)
            sourceToDelete = null
        }
    }

    transactionToDelete?.let {
        val title = katStringResource(id = R.string.dialog_delete_tx_title, isEng = isEng)
        val message = katStringResource(id = R.string.dialog_delete_tx_msg, isEng = isEng)

        ConfirmDeleteDialog(isEng, colors, title, message, { transactionToDelete = null }) {
            viewModel.deleteTransaction(it)
            transactionToDelete = null
        }
    }

    if (imageToViewUri != null) ImagePreviewDialog(imageToViewUri, isEng) { imageToViewUri = null }
}
