package com.katgr0up.katbudget.ui.tools

import androidx.compose.runtime.*
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.SavingGoalEntity
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.ui.dialogs.*
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.viewmodel.GoalViewModel

@Composable
fun GoalToolWrapper(
    goalViewModel: GoalViewModel,
    isEng: Boolean,
    defaultCurrency: String,
    exchangeRates: Map<String, Double>, // ĐÃ THÊM BIẾN TỶ GIÁ
    colors: BudgetColors,
    sources: List<SourceEntity>,
    sourceBalances: Map<String, Map<String, Double>>,
    onBack: () -> Unit
) {
    val goals by goalViewModel.allGoals.collectAsState(initial = emptyList())
    var showGoalDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<SavingGoalEntity?>(null) }
    var goalToDelete by remember { mutableStateOf<SavingGoalEntity?>(null) }
    var goalForDeposit by remember { mutableStateOf<SavingGoalEntity?>(null) }

    GoalToolScreen(
        isEng = isEng,
        defaultCurrency = defaultCurrency,
        exchangeRates = exchangeRates, // TRUYỀN TỶ GIÁ XUỐNG SCREEN
        goals = goals,
        colors = colors,
        onBack = onBack,
        onCreateGoal = { goalToEdit = null; showGoalDialog = true },
        onEditGoal = { goalToEdit = it; showGoalDialog = true },
        onDeposit = { goalForDeposit = it },
        onDeleteGoal = { goalToDelete = it }
    )

    if (showGoalDialog) {
        GoalDialog(
            goalToEdit = goalToEdit, isEng = isEng, colors = colors, defaultCurrency = defaultCurrency,
            onDismiss = { showGoalDialog = false; goalToEdit = null }
        ) { name, target, curr ->
            goalToEdit?.let { goalViewModel.deleteGoal(it) }
            goalViewModel.addGoal(name, target, curr)
            showGoalDialog = false; goalToEdit = null
        }
    }

    goalForDeposit?.let { goal ->
        DepositGoalDialog(
            goal = goal, isEng = isEng, colors = colors, sources = sources, sourceBalances = sourceBalances,
            onDismiss = { goalForDeposit = null }
        ) { amt, src, curr ->
            goalViewModel.depositToGoal(goal, amt, src, curr)
            goalForDeposit = null
        }
    }

    goalToDelete?.let {
        ConfirmDeleteDialog(
            isEng = isEng, colors = colors,
            title = katStringResource(id = R.string.dialog_delete_goal_title, isEng = isEng),
            message = katStringResource(id = R.string.dialog_delete_goal_msg, isEng = isEng, it.name),
            onDismiss = { goalToDelete = null }
        ) { goalViewModel.deleteGoal(it); goalToDelete = null }
    }
}
