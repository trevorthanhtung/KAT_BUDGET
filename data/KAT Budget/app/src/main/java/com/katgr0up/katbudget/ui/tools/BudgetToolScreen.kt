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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.BudgetEntity
import com.katgr0up.katbudget.data.local.entity.CategoryEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.ui.components.*
import com.katgr0up.katbudget.ui.utils.formatCurrency
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.ui.utils.katStringResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BudgetToolScreen(
    isEng: Boolean,
    defaultCurrency: String,
    exchangeRates: Map<String, Double>,
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    colors: BudgetColors,
    onBack: () -> Unit,
    onAddBudgetClick: () -> Unit,
    onEditBudget: (BudgetEntity) -> Unit,
    onDeleteBudget: (BudgetEntity) -> Unit
) {
    val currentMonthYear = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
    val categoryNamesById = remember(categories) {
        categories.associate { it.id to it.name }
    }

    fun getConverted(amount: Double, fromCur: String, toCur: String): Double {
        val normFrom = normalizeCurrency(fromCur)
        val normTo = normalizeCurrency(toCur)
        if (normFrom == normTo) return amount
        val rateFrom = exchangeRates[normFrom] ?: 1.0
        val rateTo = exchangeRates[normTo] ?: 1.0
        return (amount * rateFrom) / rateTo
    }

    val currentBudgets = budgets.filter { it.monthYear == currentMonthYear }

    val totalBudgetAmount = currentBudgets.sumOf { budget ->
        val normCur = normalizeCurrency(budget.currencyCode)
        val majorAmount = if (normCur in listOf("VND", "JPY", "KRW")) budget.limitAmountMinor.toDouble() else budget.limitAmountMinor / 100.0
        getConverted(majorAmount, budget.currencyCode, defaultCurrency)
    }

    val totalSpent = transactions.filter { tx ->
        tx.type == "EXPENSE" &&
                SimpleDateFormat("yyyy-MM", Locale.US).format(Date(tx.timestamp)) == currentMonthYear &&
                currentBudgets.any { categoryNamesById[it.categoryId] == tx.category }
    }.sumOf { getConverted(it.amount, it.currency, defaultCurrency) }

    val totalPercentage = if (totalBudgetAmount > 0.0) (totalSpent / totalBudgetAmount).coerceIn(0.0, 1.0) else 0.0

    var isAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isAnimated = true }

    val animatedTotalPercentage by animateFloatAsState(
        targetValue = if (isAnimated) totalPercentage.toFloat() else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "TotalBudgetProgress"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            ToolHeader(
                title = katStringResource(id = R.string.budget_tool_title, isEng = isEng),
                colors = colors,
                onBack = onBack
            )
        }

        item {
            AppCard(colors = colors, modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = katStringResource(id = R.string.budget_total_title, isEng = isEng),
                        color = colors.subText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = formatCurrency(totalBudgetAmount, defaultCurrency), color = colors.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${String.format(Locale.US, "%.0f", totalPercentage * 100)}%", color = if (totalPercentage >= 1.0) colors.negative else colors.positive, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(colors.text.copy(alpha = 0.1f))) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedTotalPercentage)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (totalPercentage >= 1.0) colors.negative else colors.positive)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${katStringResource(id = R.string.budget_prefix_spent, isEng = isEng)} ${formatCurrency(totalSpent, defaultCurrency)}",
                        color = colors.subText,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item {
            SectionHeader(
                title = katStringResource(id = R.string.budget_list_title, isEng = isEng),
                action = katStringResource(id = R.string.btn_create, isEng = isEng),
                colors = colors,
                onAction = onAddBudgetClick
            )
        }

        if (currentBudgets.isEmpty()) {
            item {
                EmptyState(
                    text = katStringResource(id = R.string.budget_empty_state, isEng = isEng),
                    colors = colors
                )
            }
        } else {
            items(currentBudgets, key = { it.id }) { budget ->
                val categoryName = categoryNamesById[budget.categoryId]
                    ?: katStringResource(id = R.string.fallback_unknown, isEng = isEng)
                val limitAmount = if (normalizeCurrency(budget.currencyCode) in listOf("VND", "JPY", "KRW")) {
                    budget.limitAmountMinor.toDouble()
                } else {
                    budget.limitAmountMinor / 100.0
                }

                val spentForCategory = transactions.filter {
                    it.category == categoryName &&
                            it.type == "EXPENSE" &&
                            SimpleDateFormat("yyyy-MM", Locale.US).format(Date(it.timestamp)) == currentMonthYear
                }.sumOf { getConverted(it.amount, it.currency, budget.currencyCode) }

                BudgetItemRow(
                    budget = budget,
                    categoryName = categoryName,
                    limitAmount = limitAmount,
                    spentForCategory = spentForCategory,
                    colors = colors,
                    onEdit = { onEditBudget(budget) },
                    onDelete = { onDeleteBudget(budget) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun BudgetItemRow(
    budget: BudgetEntity,
    categoryName: String,
    limitAmount: Double,
    spentForCategory: Double,
    colors: BudgetColors,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val percentage = if (limitAmount > 0.0) (spentForCategory / limitAmount).coerceIn(0.0, 1.0) else 0.0

    var isAnimated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isAnimated = true }

    val animatedPercentage by animateFloatAsState(
        targetValue = if (isAnimated) percentage.toFloat() else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "BudgetProgress"
    )

    @Suppress("DEPRECATION")
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            value == SwipeToDismissBoxValue.Settled
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val bgColor by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) colors.negative.copy(alpha = 0.25f) else Color.Transparent,
                label = "budgetSwipeBg"
            )
            Box(Modifier.fillMaxSize().padding(vertical = 2.dp).clip(RoundedCornerShape(16.dp)).background(bgColor))
        }
    ) {
        AppCard(
            colors = colors,
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .combinedClickable(
                    onClick = { },
                    onLongClick = { onEdit() }
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = categoryName, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = "${String.format(Locale.US, "%.0f", percentage * 100)}%", color = if (percentage >= 1.0) colors.negative else colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${formatCurrency(spentForCategory, budget.currencyCode)} / ${formatCurrency(limitAmount, budget.currencyCode)}",
                    color = colors.subText,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.text.copy(alpha = 0.08f))) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedPercentage)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (percentage >= 1.0) colors.negative else colors.accent)
                    )
                }
            }
        }
    }
}
