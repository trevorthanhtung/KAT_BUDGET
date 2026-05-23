package com.katgr0up.katbudget.ui.tools

import androidx.compose.runtime.*
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.RecurringEntity
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.ui.dialogs.*
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.viewmodel.BudgetViewModel
import com.katgr0up.katbudget.ui.utils.katStringResource

@Composable
fun RecurringToolWrapper(
    viewModel: BudgetViewModel,
    isEng: Boolean,
    defaultCurrency: String,
    exchangeRates: Map<String, Double>, // ĐÃ THÊM: Biến tỷ giá
    colors: BudgetColors,
    sources: List<SourceEntity>,
    onBack: () -> Unit
) {
    val fallbackAll = katStringResource(id = R.string.fallback_all, isEng = isEng)
    val openingBalanceStr = katStringResource(id = R.string.fallback_opening_balance, isEng = isEng)

    val recurrings by viewModel.allRecurrings.collectAsState(initial = emptyList())
    val expenseOptions by viewModel.expenseCategoryNames.collectAsState(initial = listOf(fallbackAll))
    val incomeOptionsRaw by viewModel.incomeCategoryNames.collectAsState(initial = listOf(fallbackAll))

    val incomeOptions = incomeOptionsRaw.filter {
        it != "Số dư ban đầu" && it != "Opening Balance" && it != openingBalanceStr
    }

    var showRecurringDialog by remember { mutableStateOf(false) }
    var recurringToEdit by remember { mutableStateOf<RecurringEntity?>(null) }
    var recurringToDelete by remember { mutableStateOf<RecurringEntity?>(null) }

    RecurringToolScreen(
        isEng = isEng,
        defaultCurrency = defaultCurrency,
        exchangeRates = exchangeRates, // TRUYỀN TỶ GIÁ
        recurrings = recurrings,
        sources = sources,
        colors = colors,
        onBack = onBack,
        onCreateRecurring = { recurringToEdit = null; showRecurringDialog = true },
        onEditRecurring = { recurringToEdit = it; showRecurringDialog = true },
        onDeleteRecurring = { recurringToDelete = it }
    )

    if (showRecurringDialog) {
        RecurringDialog(
            recurringToEdit = recurringToEdit,
            isEng = isEng,
            colors = colors,
            sources = sources,
            expenseOptions = expenseOptions,
            incomeOptions = incomeOptions,
            defaultCurrency = defaultCurrency,
            onDismiss = { showRecurringDialog = false; recurringToEdit = null }
        ) { amt, type, cat, note, src, curr, day ->
            if (recurringToEdit != null) {
                viewModel.updateRecurring(recurringToEdit!!.id, amt, type, cat, note, src, curr, day)
            } else {
                viewModel.addRecurring(amt, type, cat, note, src, curr, day)
            }
            showRecurringDialog = false
            recurringToEdit = null
        }
    }

    recurringToDelete?.let {
        ConfirmDeleteDialog(
            isEng = isEng,
            colors = colors,
            title = katStringResource(id = R.string.dialog_delete_recurring_title, isEng = isEng),
            message = katStringResource(id = R.string.dialog_delete_recurring_msg, isEng = isEng, it.category),
            onDismiss = { recurringToDelete = null }
        ) {
            viewModel.deleteRecurring(it)
            recurringToDelete = null
        }
    }
}