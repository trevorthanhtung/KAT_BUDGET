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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.ui.utils.formatCompactCurrency
import com.katgr0up.katbudget.ui.utils.formatSmartCurrency
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import java.util.Calendar
import java.util.Locale

@Composable
fun BarChartComparison(
    isEng: Boolean,
    transactions: List<TransactionEntity>,
    chartType: String,
    currency: String,
    defaultCurrency: String,
    exchangeRates: Map<String, Double>,
    filterStartDate: Long,
    filterEndDate: Long,
    textColor: Color,
    subTextColor: Color,
    primaryBrandColor: Color
) {
    val isAllConverted = currency == "All (Converted)" ||
            currency == "Tất cả (Quy đổi)" ||
            currency == "Tất cả" ||
            currency == "All" ||
            currency == "ALL"

    fun isMatchCurrency(tx: TransactionEntity): Boolean {
        return isAllConverted || normalizeCurrency(tx.currency) == normalizeCurrency(currency)
    }

    fun amountForDisplay(tx: TransactionEntity): Double {
        if (!isAllConverted) return tx.amount

        val sourceRate = exchangeRates[normalizeCurrency(tx.currency)] ?: 1.0
        val targetRate = exchangeRates[normalizeCurrency(defaultCurrency)] ?: 1.0
        return (tx.amount * sourceRate) / targetRate
    }

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = filterStartDate
    calendar.add(Calendar.MONTH, -1)
    val lastMonthStart = calendar.timeInMillis

    calendar.timeInMillis = filterEndDate
    calendar.add(Calendar.MONTH, -1)
    val lastMonthEnd = calendar.timeInMillis

    val sumThisPeriod = transactions
        .filter { it.timestamp in filterStartDate..filterEndDate && isMatchCurrency(it) && it.type == chartType }
        .sumOf { amountForDisplay(it) }

    val sumPreviousPeriod = transactions
        .filter { it.timestamp in lastMonthStart..lastMonthEnd && isMatchCurrency(it) && it.type == chartType }
        .sumOf { amountForDisplay(it) }

    val maxValue = maxOf(sumThisPeriod, sumPreviousPeriod, 1.0)
    val displayCurrency = if (isAllConverted) defaultCurrency else currency
    val currentColor = if (chartType == "EXPENSE") Color(0xFFEF4444) else primaryBrandColor

    var isAnimated by remember(sumThisPeriod, sumPreviousPeriod) { mutableStateOf(false) }
    val animationProgress by animateFloatAsState(
        targetValue = if (isAnimated) 1f else 0f,
        animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing),
        label = "bar_chart_animation"
    )

    LaunchedEffect(sumThisPeriod, sumPreviousPeriod) {
        isAnimated = true
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (chartType == "EXPENSE") {
                katStringResource(id = R.string.chart_compare_expense_title, isEng = isEng)
            } else {
                katStringResource(id = R.string.chart_compare_income_title, isEng = isEng)
            },
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        TrendPill(
            isEng = isEng,
            chartType = chartType,
            current = sumThisPeriod,
            previous = sumPreviousPeriod,
            positiveColor = primaryBrandColor,
            negativeColor = Color(0xFFEF4444),
            subTextColor = subTextColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            ComparisonBar(
                amount = sumPreviousPeriod,
                maxValue = maxValue,
                currency = displayCurrency,
                label = katStringResource(id = R.string.chart_label_previous_period, isEng = isEng),
                barColor = subTextColor.copy(alpha = 0.32f),
                amountColor = subTextColor.copy(alpha = 0.74f),
                labelColor = subTextColor,
                animationProgress = animationProgress,
                modifier = Modifier.weight(1f)
            )

            ComparisonBar(
                amount = sumThisPeriod,
                maxValue = maxValue,
                currency = displayCurrency,
                label = katStringResource(id = R.string.chart_label_current_period, isEng = isEng),
                barColor = currentColor,
                amountColor = currentColor,
                labelColor = textColor,
                animationProgress = animationProgress,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TrendPill(
    isEng: Boolean,
    chartType: String,
    current: Double,
    previous: Double,
    positiveColor: Color,
    negativeColor: Color,
    subTextColor: Color
) {
    if (previous <= 0.0 && current <= 0.0) {
        Box(
            modifier = Modifier
                .height(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(subTextColor.copy(alpha = 0.08f))
                .border(
                    width = 1.dp,
                    color = subTextColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = katStringResource(id = R.string.barchart_no_comparison, isEng = isEng),
                color = subTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    val diff = current - previous
    val percent = if (previous > 0.0) (diff / previous) * 100.0 else 100.0
    val isPositiveTrend = if (chartType == "EXPENSE") percent <= 0.0 else percent >= 0.0
    val trendColor = if (isPositiveTrend) positiveColor else negativeColor
    val trendIcon = when {
        percent > 0.0 -> "▲"
        percent < 0.0 -> "▼"
        else -> "▬"
    }
    val trendSign = if (percent > 0.0) "+" else ""

    Box(
        modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trendColor.copy(alpha = 0.12f))
            .border(
                width = 1.dp,
                color = trendColor.copy(alpha = 0.24f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$trendIcon $trendSign${String.format(Locale.US, "%.1f", percent)}%",
            color = trendColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ComparisonBar(
    amount: Double,
    maxValue: Double,
    currency: String,
    label: String,
    barColor: Color,
    amountColor: Color,
    labelColor: Color,
    animationProgress: Float,
    modifier: Modifier = Modifier
) {
    val barHeight = ((amount / maxValue).toFloat().coerceIn(0.04f, 1f)) * animationProgress

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxHeight()
    ) {
        Text(
            text = formatCompactCurrency(amount, currency),
            color = amountColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .width(46.dp)
                    .fillMaxHeight(barHeight)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                    .background(barColor)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            color = labelColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
