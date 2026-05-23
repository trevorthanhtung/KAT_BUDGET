package com.katgr0up.katbudget.ui.tools

import androidx.compose.runtime.*
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.BudgetEntity
import com.katgr0up.katbudget.data.local.entity.CategoryEntity
import com.katgr0up.katbudget.ui.dialogs.*
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.viewmodel.BudgetViewModel
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.normalizeCurrency

@Composable
fun BudgetToolWrapper(
    viewModel: BudgetViewModel,
    isEng: Boolean,
    defaultCurrency: String,
    exchangeRates: Map<String, Double>,
    colors: BudgetColors,
    onBack: () -> Unit
) {
    val fallbackAll = katStringResource(id = R.string.fallback_all, isEng = isEng)
    val budgets by viewModel.allBudgets.collectAsState(initial = emptyList())
    val transactions by viewModel.allTransactions.collectAsState(initial = emptyList())
    val expenseCategories by viewModel.expenseCategories.collectAsState(initial = emptyList())
    val expenseOptions by viewModel.expenseCategoryNames.collectAsState(initial = listOf(fallbackAll))

    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<BudgetEntity?>(null) }
    var budgetToDelete by remember { mutableStateOf<BudgetEntity?>(null) }

    BudgetToolScreen(
        isEng = isEng,
        defaultCurrency = defaultCurrency,
        exchangeRates = exchangeRates,
        transactions = transactions,
        budgets = budgets,
        categories = expenseCategories,
        colors = colors,
        onBack = onBack,
        onAddBudgetClick = { budgetToEdit = null; showBudgetDialog = true },
        onEditBudget = { budgetToEdit = it; showBudgetDialog = true },
        onDeleteBudget = { budgetToDelete = it }
    )

    if (showBudgetDialog) {
        val editingCategory = budgetToEdit?.let { budget ->
            expenseCategories.find { it.id == budget.categoryId }?.name
        }

        BudgetDialog(
            isEng = isEng,
            colors = colors,
            expenseOptions = expenseOptions,
            defaultCurrency = defaultCurrency,
            initialCategory = editingCategory,
            initialAmount = budgetToEdit?.toMajorAmount(),
            initialCurrency = budgetToEdit?.currencyCode,
            onDismiss = { showBudgetDialog = false; budgetToEdit = null }
        ) { cat, amt, curr ->
            if (budgetToEdit != null) {
                viewModel.updateBudget(budgetToEdit!!.id, cat, amt, curr)
            } else {
                viewModel.addBudget(cat, amt, curr)
            }
            showBudgetDialog = false
            budgetToEdit = null
        }
    }

    budgetToDelete?.let {
        val title = katStringResource(id = R.string.dialog_delete_budget_title, isEng = isEng)
        val categoryName = expenseCategories.find { category -> category.id == it.categoryId }?.name.orEmpty()
        val message = katStringResource(id = R.string.dialog_delete_budget_msg, isEng = isEng, categoryName)

        ConfirmDeleteDialog(
            isEng = isEng,
            colors = colors,
            title = title,
            message = message,
            onDismiss = { budgetToDelete = null }
        ) {
            viewModel.deleteBudget(it)
            budgetToDelete = null
        }
    }
}

private fun BudgetEntity.toMajorAmount(): Double {
    return if (normalizeCurrency(currencyCode) in listOf("VND", "JPY", "KRW")) {
        limitAmountMinor.toDouble()
    } else {
        limitAmountMinor / 100.0
    }
}
