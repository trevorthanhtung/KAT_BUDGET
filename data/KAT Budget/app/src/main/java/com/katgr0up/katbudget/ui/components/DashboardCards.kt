package com.katgr0up.katbudget.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.ui.utils.convertCurrency
import com.katgr0up.katbudget.ui.utils.formatSmartCurrency
import com.katgr0up.katbudget.ui.utils.katStringResource
import kotlin.math.abs

@Composable
fun AssetSummaryCard(
    isEng: Boolean,
    defaultCurrency: String,
    selectedSourceFilter: String,
    balance: Map<String, Double>,
    grandTotal: Double,
    sourceBalances: Map<String, Map<String, Double>>,
    colors: BudgetColors,
    exchangeRates: Map<String, Double> = emptyMap()
) {
    val fallbackAll = katStringResource(id = R.string.fallback_all, isEng = isEng)
    val isAllSources = selectedSourceFilter == fallbackAll ||
            selectedSourceFilter == "Tất cả" ||
            selectedSourceFilter == "All"

    val title = if (isAllSources) {
        katStringResource(id = R.string.card_net_worth, isEng = isEng)
    } else {
        "${katStringResource(id = R.string.tx_label_source, isEng = isEng)}: $selectedSourceFilter"
    }

    val displayAmount = if (isAllSources) {
        grandTotal
    } else {
        sourceBalances[selectedSourceFilter]
            .orEmpty()
            .entries
            .sumOf { (currency, amount) ->
                convertCurrency(amount, currency, defaultCurrency, exchangeRates)
            }
    }

    val animatedDisplayAmount by animateFloatAsState(
        targetValue = displayAmount.toFloat(),
        animationSpec = tween(durationMillis = 700, easing = LinearOutSlowInEasing),
        label = "asset_summary_amount"
    )

    val balancesToShow = if (isAllSources) {
        balance
    } else {
        sourceBalances[selectedSourceFilter].orEmpty()
    }

    AppCard(
        colors = colors,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = colors.subText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatSmartCurrency(animatedDisplayAmount.toDouble(), defaultCurrency, isEng, com.katgr0up.katbudget.ui.utils.LocalPrivacyMode.current),
                color = if (displayAmount < 0.0) colors.negative else colors.text,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            )

            if (balancesToShow.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = balancesToShow.toList().sortedBy { it.first },
                        key = { (currency, _) -> currency }
                    ) { (currency, amount) ->
                        BalanceCurrencyChip(
                            text = formatSmartCurrency(amount, currency, isEng, com.katgr0up.katbudget.ui.utils.LocalPrivacyMode.current),
                            amount = amount,
                            colors = colors
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceCurrencyChip(
    text: String,
    amount: Double,
    colors: BudgetColors
) {
    val chipColor = if (amount < 0.0) colors.negative else colors.accent

    Text(
        text = text,
        color = chipColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(chipColor.copy(alpha = 0.10f))
            .border(
                width = 1.dp,
                color = chipColor.copy(alpha = 0.20f),
                shape = RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 11.dp, vertical = 6.dp)
    )
}

@Composable
fun SummaryMiniCard(
    title: String,
    amount: Double,
    currency: String,
    color: Color,
    colors: BudgetColors,
    modifier: Modifier = Modifier
) {
    AppCard(
        colors = colors,
        modifier = modifier.height(88.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(7.dp))

            Text(
                text = formatSmartCurrency(amount, currency, isEng = true, isPrivacyMode = com.katgr0up.katbudget.ui.utils.LocalPrivacyMode.current),
                color = colors.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CashFlowOverviewCard(
    isEng: Boolean,
    income: Double,
    expense: Double,
    currency: String,
    colors: BudgetColors,
    balanceBreakdown: Map<String, Double> = emptyMap()
) {
    val maxValue = maxOf(income, expense, 1.0)
    val netIncome = income - expense
    val isSurplus = netIncome >= 0.0
    val netColor = if (isSurplus) colors.positive else colors.negative

    val animatedNetIncome by animateFloatAsState(
        targetValue = netIncome.toFloat(),
        animationSpec = tween(durationMillis = 700, easing = LinearOutSlowInEasing),
        label = "net_income_anim"
    )

    AppCard(
        colors = colors,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = katStringResource(id = R.string.card_net_cash_flow, isEng = isEng),
                color = colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            NetCashFlowBox(
                isEng = isEng,
                amount = animatedNetIncome.toDouble(),
                currency = currency,
                color = netColor,
                colors = colors,
                balanceBreakdown = balanceBreakdown
            )

            Spacer(modifier = Modifier.height(20.dp))

            CashFlowBar(
                label = katStringResource(id = R.string.tab_report_chip_income, isEng = isEng),
                amount = income,
                maxValue = maxValue,
                currency = currency,
                color = colors.positive,
                textColor = colors.text,
                subTextColor = colors.subText
            )

            Spacer(modifier = Modifier.height(12.dp))

            CashFlowBar(
                label = katStringResource(id = R.string.tab_report_chip_expense, isEng = isEng),
                amount = expense,
                maxValue = maxValue,
                currency = currency,
                color = colors.negative,
                textColor = colors.text,
                subTextColor = colors.subText
            )
        }
    }
}

@Composable
private fun NetCashFlowBox(
    isEng: Boolean,
    amount: Double,
    currency: String,
    color: Color,
    colors: BudgetColors,
    balanceBreakdown: Map<String, Double> = emptyMap()
) {
    val sign = when {
        amount > 0.0 -> "+"
        amount < 0.0 -> "-"
        else -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.09f))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.22f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = katStringResource(id = R.string.card_remaining_balance, isEng = isEng),
                color = colors.subText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "$sign${formatSmartCurrency(abs(amount), currency, isEng = true, isPrivacyMode = com.katgr0up.katbudget.ui.utils.LocalPrivacyMode.current)}",
                color = color,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )

            if (balanceBreakdown.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = balanceBreakdown.toList().sortedBy { it.first },
                        key = { (currencyCode, _) -> currencyCode }
                    ) { (currencyCode, amountValue) ->
                        BalanceCurrencyChip(
                            text = formatSmartCurrency(
                                amountValue,
                                currencyCode,
                                isEng,
                                com.katgr0up.katbudget.ui.utils.LocalPrivacyMode.current
                            ),
                            amount = amountValue,
                            colors = colors
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CashFlowBar(
    label: String,
    amount: Double,
    maxValue: Double,
    currency: String,
    color: Color,
    textColor: Color,
    subTextColor: Color
) {
    val progress = (amount / maxValue).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 550, easing = LinearOutSlowInEasing),
        label = "cash_flow_progress"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = subTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = formatSmartCurrency(amount, currency, isEng = true, isPrivacyMode = com.katgr0up.katbudget.ui.utils.LocalPrivacyMode.current),
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .widthIn(min = 88.dp)
                    .horizontalScroll(rememberScrollState())
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(subTextColor.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}
