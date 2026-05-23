package com.katgr0up.katbudget.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.BudgetEntity
import com.katgr0up.katbudget.data.local.entity.CategoryEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.ui.utils.formatCompactCurrency
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.ui.utils.rememberCategoryNameLocalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BudgetReportCard(
    isEng: Boolean,
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    transactions: List<TransactionEntity>,
    filterStartDate: Long,
    displayCurrency: String,
    defaultCurrency: String,
    exchangeRates: Map<String, Double>,
    colors: BudgetColors
) {
    val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(filterStartDate))
    val activeBudgets = budgets.filter { it.monthYear == currentMonthYear }
    val categoryNamesById = remember(categories) {
        categories.associate { it.id to it.name }
    }
    val localizeCategory = rememberCategoryNameLocalizer(isEng)
    val unknownCategory = katStringResource(id = R.string.fallback_unknown, isEng = isEng)

    if (activeBudgets.isEmpty()) return

    fun getMajorAmount(minor: Long, currencyCode: String): Double {
        val normCur = normalizeCurrency(currencyCode)
        return if (normCur in listOf("VND", "JPY", "KRW")) minor.toDouble() else minor / 100.0
    }

    fun convertToDisplay(amount: Double, fromCurrency: String): Double {
        val targetCurrency = if (displayCurrency.isEmpty()) defaultCurrency else displayCurrency
        if (normalizeCurrency(fromCurrency) == normalizeCurrency(targetCurrency)) return amount

        val sourceRate = exchangeRates[normalizeCurrency(fromCurrency)] ?: 1.0
        val targetRate = exchangeRates[normalizeCurrency(targetCurrency)] ?: 1.0
        return (amount * sourceRate) / targetRate
    }

    val targetCurrency = if (displayCurrency.isEmpty()) defaultCurrency else displayCurrency

    val expensesByCategory = transactions
        .filter { it.type == "EXPENSE" }
        .groupBy { it.category }
        .mapValues { entry ->
            entry.value.sumOf { convertToDisplay(it.amount, it.currency) }
        }

    var totalBudgetAmount = 0.0
    var totalSpentInBudgets = 0.0

    val budgetItems = activeBudgets
        .map { budget ->
            val budgetMajor = getMajorAmount(budget.limitAmountMinor, budget.currencyCode)
            val budgetInDisplayCurrency = convertToDisplay(budgetMajor, budget.currencyCode)
            val categoryName = categoryNamesById[budget.categoryId] ?: unknownCategory
            val actualSpent = expensesByCategory[categoryName] ?: 0.0

            totalBudgetAmount += budgetInDisplayCurrency
            totalSpentInBudgets += actualSpent

            BudgetReportItem(
                category = localizeCategory(categoryName),
                limit = budgetInDisplayCurrency,
                spent = actualSpent
            )
        }
        .sortedByDescending { it.spent / maxOf(it.limit, 1.0) }

    val overallProgress = (totalSpentInBudgets / maxOf(totalBudgetAmount, 1.0)).toFloat().coerceIn(0f, 1f)

    var isAnimated by remember(budgetItems) { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (isAnimated) overallProgress else 0f,
        animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing),
        label = "budget_progress_anim"
    )

    LaunchedEffect(budgetItems) {
        isAnimated = true
    }

    val overBudget = totalSpentInBudgets > totalBudgetAmount
    val warning = totalSpentInBudgets > totalBudgetAmount * 0.8
    val barColor = when {
        overBudget -> colors.negative
        warning -> colors.warning
        else -> colors.positive
    }

    AppCard(colors = colors, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = katStringResource(id = R.string.budget_report_title, isEng = isEng),
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            BudgetOverviewBox(
                isEng = isEng,
                spent = totalSpentInBudgets,
                limit = totalBudgetAmount,
                currency = targetCurrency,
                progress = animatedProgress,
                color = barColor,
                colors = colors
            )

            Spacer(modifier = Modifier.height(14.dp))

            budgetItems.take(4).forEachIndexed { index, item ->
                BudgetReportRow(
                    item = item,
                    currency = targetCurrency,
                    colors = colors,
                    animated = isAnimated
                )

                if (index != budgetItems.take(4).lastIndex) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun BudgetOverviewBox(
    isEng: Boolean,
    spent: Double,
    limit: Double,
    currency: String,
    progress: Float,
    color: androidx.compose.ui.graphics.Color,
    colors: BudgetColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.22f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 15.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = katStringResource(id = R.string.budget_report_total_spent, isEng = isEng),
                color = colors.subText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "${formatCompactCurrency(spent, currency)} / ${formatCompactCurrency(limit, currency)}",
                color = colors.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        ProgressTrack(
            progress = progress,
            color = color,
            trackColor = colors.subText.copy(alpha = 0.13f),
            height = 9
        )
    }
}

@Composable
private fun BudgetReportRow(
    item: BudgetReportItem,
    currency: String,
    colors: BudgetColors,
    animated: Boolean
) {
    val rawProgress = (item.spent / maxOf(item.limit, 1.0)).toFloat()
    val progress = rawProgress.coerceIn(0f, 1f)
    val color = when {
        item.spent > item.limit -> colors.negative
        rawProgress > 0.8f -> colors.warning
        else -> colors.positive
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animated) progress else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "budget_row_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card.copy(alpha = 0.52f))
            .border(
                width = 1.dp,
                color = colors.border.copy(alpha = 0.42f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.category,
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${(rawProgress * 100).toInt()}%",
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        ProgressTrack(
            progress = animatedProgress,
            color = color,
            trackColor = colors.subText.copy(alpha = 0.12f),
            height = 6
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "${formatCompactCurrency(item.spent, currency)} / ${formatCompactCurrency(item.limit, currency)}",
            color = colors.subText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProgressTrack(
    progress: Float,
    color: androidx.compose.ui.graphics.Color,
    trackColor: androidx.compose.ui.graphics.Color,
    height: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(CircleShape)
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color)
        )
    }
}

private data class BudgetReportItem(
    val category: String,
    val limit: Double,
    val spent: Double
)
