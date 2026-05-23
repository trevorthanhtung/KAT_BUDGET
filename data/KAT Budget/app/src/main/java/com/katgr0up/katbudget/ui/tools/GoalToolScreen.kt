package com.katgr0up.katbudget.ui.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.SavingGoalEntity
import com.katgr0up.katbudget.ui.components.*
import com.katgr0up.katbudget.ui.utils.formatCurrency
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.ui.utils.katStringResource
import java.util.Locale

@Composable
fun GoalToolScreen(
    isEng: Boolean,
    defaultCurrency: String,
    exchangeRates: Map<String, Double>,
    goals: List<SavingGoalEntity>,
    colors: BudgetColors,
    onBack: () -> Unit,
    onCreateGoal: () -> Unit,
    onEditGoal: (SavingGoalEntity) -> Unit,
    onDeposit: (SavingGoalEntity) -> Unit,
    onDeleteGoal: (SavingGoalEntity) -> Unit
) {
    fun getConverted(amount: Double, fromCur: String): Double {
        val normFrom = normalizeCurrency(fromCur)
        val normTo = normalizeCurrency(defaultCurrency)
        if (normFrom == normTo) return amount
        val rateFrom = exchangeRates[normFrom] ?: 1.0
        val rateTo = exchangeRates[normTo] ?: 1.0
        return (amount * rateFrom) / rateTo
    }

    val totalSaved = goals.sumOf { getConverted(it.currentAmount.toDouble(), it.currency) }
    val totalTarget = goals.sumOf { getConverted(it.targetAmount.toDouble(), it.currency) }
    val totalPercentage = if (totalTarget > 0.0) (totalSaved / totalTarget).coerceIn(0.0, 1.0) else 0.0

    var isAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isAnimated = true }
    val animatedTotalPercentage by animateFloatAsState(targetValue = if (isAnimated) totalPercentage.toFloat() else 0f, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "TotalGoalProgress")

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
        item { ToolHeader(title = katStringResource(id = R.string.goal_tool_title, isEng = isEng), colors = colors, onBack = onBack) }

        item {
            AppCard(colors = colors, modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = katStringResource(id = R.string.goal_total_saved, isEng = isEng), color = colors.subText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = formatCurrency(totalSaved, defaultCurrency), color = colors.savings, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${String.format(Locale.US, "%.0f", totalPercentage * 100)}%", color = colors.savings, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.text.copy(alpha = 0.1f))) {
                        Box(modifier = Modifier.fillMaxWidth(animatedTotalPercentage).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(colors.savings))
                    }
                }
            }
        }

        item { SectionHeader(title = katStringResource(id = R.string.goal_list_title, isEng = isEng), action = katStringResource(id = R.string.btn_create, isEng = isEng), colors = colors, onAction = onCreateGoal) }

        if (goals.isEmpty()) {
            item { EmptyState(text = katStringResource(id = R.string.goal_empty_state, isEng = isEng), colors = colors) }
        } else {
            items(goals, key = { it.id }) { goal ->
                GoalItemRow(goal = goal, colors = colors, onEdit = { onEditGoal(goal) }, onDeposit = { onDeposit(goal) }, onDelete = { onDeleteGoal(goal) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun GoalItemRow(
    goal: SavingGoalEntity, colors: BudgetColors,
    onEdit: () -> Unit, onDeposit: () -> Unit, onDelete: () -> Unit
) {
    val percentage = if (goal.targetAmount > 0.0) (goal.currentAmount.toDouble() / goal.targetAmount.toDouble()).coerceIn(0.0, 1.0) else 0.0

    var isAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isAnimated = true }
    val animatedPercentage by animateFloatAsState(targetValue = if (isAnimated) percentage.toFloat() else 0f, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "GoalProgress")

    @Suppress("DEPRECATION")
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDeposit()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val bgColor by animateColorAsState(targetValue = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.StartToEnd -> colors.positive.copy(alpha = 0.25f)
                SwipeToDismissBoxValue.EndToStart -> colors.negative.copy(alpha = 0.25f)
                else -> Color.Transparent
            }, label = "goalSwipeBg")
            Box(Modifier.fillMaxSize().padding(vertical = 2.dp).clip(RoundedCornerShape(16.dp)).background(bgColor))
        }
    ) {
        AppCard(colors = colors, modifier = Modifier.fillMaxWidth().animateContentSize().combinedClickable(onClick = {}, onLongClick = { onEdit() })) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = goal.name, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${String.format(Locale.US, "%.0f", percentage * 100)}%", color = if (percentage >= 1.0) colors.positive else colors.savings, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.text.copy(alpha = 0.08f))) {
                    Box(modifier = Modifier.fillMaxWidth(animatedPercentage).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(colors.savings))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = formatCurrency(goal.currentAmount.toDouble(), goal.currency), color = colors.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = formatCurrency(goal.targetAmount.toDouble(), goal.currency), color = colors.subText, fontSize = 12.sp)
                }
            }
        }
    }
}
