package com.katgr0up.katbudget.ui.tabs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.BudgetEntity
import com.katgr0up.katbudget.data.local.entity.CategoryEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.ui.components.AppCard
import com.katgr0up.katbudget.ui.components.BarChartComparison
import com.katgr0up.katbudget.ui.components.BudgetReportCard
import com.katgr0up.katbudget.ui.components.CashFlowOverviewCard
import com.katgr0up.katbudget.ui.components.ChoiceChip
import com.katgr0up.katbudget.ui.components.EmptyState
import com.katgr0up.katbudget.ui.components.GaugeChartAndTopList
import com.katgr0up.katbudget.ui.components.LineChartTrend
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.dialogs.CategoryDetailsDialog
import com.katgr0up.katbudget.ui.utils.KatSpringSpec
import com.katgr0up.katbudget.ui.utils.formatDateOnly
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.ui.utils.rememberCategoryNameLocalizer
import kotlin.math.abs

private const val REPORT_CURRENCY_ALL = "ALL"

@Composable
fun ReportsTab(
    isEng: Boolean,
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    expenseOptions: List<String>,
    incomeOptions: List<String>,
    selectedTag: String,
    chartType: String,
    chartCurrencyFilter: String,
    filterStartDate: Long,
    filterEndDate: Long,
    colors: BudgetColors,
    availableCurrencies: List<String>,
    exchangeRates: Map<String, Double>,
    defaultCurrency: String,
    onChartTypeChanged: (String) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onTagChanged: (String) -> Unit,
    onDateRangeClick: () -> Unit
) {
    val fallbackAll = katStringResource(id = R.string.fallback_all, isEng = isEng)
    val localizeCategory = rememberCategoryNameLocalizer(isEng)
    val allConvertedLabel = fallbackAll
    val extendedCurrencies = remember(allConvertedLabel, availableCurrencies) {
        listOf(allConvertedLabel) + availableCurrencies
    }
    val isAllConverted = isAllCurrencyFilter(chartCurrencyFilter, allConvertedLabel)

    fun getConvertedAmount(tx: TransactionEntity): Double {
        if (!isAllConverted || normalizeCurrency(tx.currency) == normalizeCurrency(defaultCurrency)) {
            return tx.amount
        }

        val txRate = exchangeRates[normalizeCurrency(tx.currency)] ?: 1.0
        val defaultRate = exchangeRates[normalizeCurrency(defaultCurrency)] ?: 1.0
        return (tx.amount * txRate) / defaultRate
    }

    val transactionsInDate = transactions.filter { it.timestamp in filterStartDate..filterEndDate }
    val transactionsInRange = if (isAllConverted) {
        transactionsInDate
    } else {
        transactionsInDate.filter {
            normalizeCurrency(it.currency) == normalizeCurrency(chartCurrencyFilter)
        }
    }

    val totalIncome = transactionsInRange
        .filter { it.type == "INCOME" }
        .sumOf { getConvertedAmount(it) }

    val totalExpense = transactionsInRange
        .filter { it.type == "EXPENSE" }
        .sumOf { getConvertedAmount(it) }

    val netBreakdownByCurrency = if (isAllConverted) {
        transactionsInRange
            .groupBy { normalizeCurrency(it.currency) }
            .mapValues { (_, currencyTransactions) ->
                currencyTransactions.sumOf { tx ->
                    when (tx.type) {
                        "INCOME" -> tx.amount
                        "EXPENSE" -> -tx.amount
                        else -> 0.0
                    }
                }
            }
            .filterValues { abs(it) > 0.000001 }
            .toSortedMap()
    } else {
        emptyMap()
    }

    val chartTransactions = transactionsInRange
        .filter { it.type == chartType }
        .filter {
            selectedTag == fallbackAll ||
                    selectedTag == "Tất cả" ||
                    selectedTag == "All" ||
                    it.category == selectedTag ||
                    localizeCategory(it.category) == selectedTag
        }

    val chartGroups = chartTransactions.groupBy { localizeCategory(it.category) }
    val chartData = chartGroups.mapValues { (_, categoryTransactions) ->
        categoryTransactions.sumOf { getConvertedAmount(it) }
    }

    val displayCurrency = if (isAllConverted) defaultCurrency else chartCurrencyFilter
    var drillDownCategory by remember { mutableStateOf<String?>(null) }

    fun isAllTag(value: String): Boolean {
        return value == fallbackAll || value == "Tất cả" || value == "All"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 2.dp, bottom = 112.dp)
    ) {
        item {
            ReportDateRangeCard(
                isEng = isEng,
                startDate = filterStartDate,
                endDate = filterEndDate,
                colors = colors,
                onClick = onDateRangeClick
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                item {
                    ChoiceChip(
                        text = katStringResource(id = R.string.tab_report_chip_expense, isEng = isEng),
                        selected = chartType == "EXPENSE",
                        colors = colors,
                        onClick = { onChartTypeChanged("EXPENSE") }
                    )
                }

                item {
                    ChoiceChip(
                        text = katStringResource(id = R.string.tab_report_chip_income, isEng = isEng),
                        selected = chartType == "INCOME",
                        colors = colors,
                        onClick = { onChartTypeChanged("INCOME") }
                    )
                }
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(items = extendedCurrencies, key = { it }) { currency ->
                    val isAllCurrency = currency == allConvertedLabel
                    ChoiceChip(
                        text = currency,
                        selected = if (isAllCurrency) {
                            isAllConverted
                        } else {
                            normalizeCurrency(chartCurrencyFilter) == normalizeCurrency(currency)
                        },
                        colors = colors,
                        onClick = {
                            onCurrencyChanged(if (isAllCurrency) REPORT_CURRENCY_ALL else currency)
                        }
                    )
                }
            }
        }

        item {
            CashFlowOverviewCard(
                isEng = isEng,
                income = totalIncome,
                expense = totalExpense,
                currency = displayCurrency,
                colors = colors,
                balanceBreakdown = netBreakdownByCurrency
            )
        }

        item {
            val categoryOptions = (if (chartType == "EXPENSE") expenseOptions else incomeOptions)
                .map { option -> if (isAllTag(option)) fallbackAll else localizeCategory(option) }
                .distinct()
                .filterNot { isAllTag(it) }
            val options = listOf(fallbackAll) + categoryOptions

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(items = options, key = { it }) { option ->
                    val selected = if (isAllTag(option)) {
                        isAllTag(selectedTag)
                    } else {
                        selectedTag == option || localizeCategory(selectedTag) == option
                    }

                    ChoiceChip(
                        text = option,
                        selected = selected,
                        colors = colors,
                        onClick = { onTagChanged(option) }
                    )
                }
            }
        }

        if (chartData.isEmpty()) {
            item {
                EmptyState(
                    text = katStringResource(id = R.string.tab_report_no_data, isEng = isEng),
                    colors = colors
                )
            }
        } else {
            item {
                val centerLabel = if (chartType == "EXPENSE") {
                    katStringResource(id = R.string.tab_report_center_expense, isEng = isEng)
                } else {
                    katStringResource(id = R.string.tab_report_center_income, isEng = isEng)
                }

                AppCard(colors = colors, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        GaugeChartAndTopList(
                            isEng = isEng,
                            dataByCategory = chartData,
                            centerLabel = centerLabel,
                            isExpense = chartType == "EXPENSE",
                            currency = displayCurrency,
                            textColor = colors.text,
                            subTextColor = colors.subText,
                            accentColor = colors.accent,
                            primaryBrandColor = colors.positive,
                            cardBgColor = colors.card,
                            borderColor = colors.border,
                            onCategoryClick = { category -> drillDownCategory = category }
                        )
                    }
                }
            }

            if (chartType == "EXPENSE") {
                item {
                    BudgetReportCard(
                        isEng = isEng,
                        budgets = budgets,
                        categories = categories,
                        transactions = transactionsInRange,
                        filterStartDate = filterStartDate,
                        displayCurrency = displayCurrency,
                        defaultCurrency = defaultCurrency,
                        exchangeRates = exchangeRates,
                        colors = colors
                    )
                }
            }

            item {
                AppCard(colors = colors, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        LineChartTrend(
                            isEng = isEng,
                            transactions = transactionsInRange,
                            chartType = chartType,
                            currency = displayCurrency,
                            filterStartDate = filterStartDate,
                            filterEndDate = filterEndDate,
                            textColor = colors.text,
                            subTextColor = colors.subText,
                            primaryBrandColor = colors.positive
                        )
                    }
                }
            }

            item {
                AppCard(colors = colors, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        BarChartComparison(
                            isEng = isEng,
                            transactions = transactions,
                            chartType = chartType,
                            currency = chartCurrencyFilter,
                            defaultCurrency = defaultCurrency,
                            exchangeRates = exchangeRates,
                            filterStartDate = filterStartDate,
                            filterEndDate = filterEndDate,
                            textColor = colors.text,
                            subTextColor = colors.subText,
                            primaryBrandColor = colors.positive
                        )
                    }
                }
            }
        }
    }

    drillDownCategory?.let { categoryName ->
        CategoryDetailsDialog(
            isEng = isEng,
            categoryName = categoryName,
            currency = displayCurrency,
            transactions = transactionsInRange,
            colors = colors,
            onDismiss = { drillDownCategory = null }
        )
    }
}

private fun isAllCurrencyFilter(
    currencyFilter: String,
    localizedAllLabel: String
): Boolean {
    return currencyFilter == REPORT_CURRENCY_ALL ||
            currencyFilter == localizedAllLabel ||
            currencyFilter == "Tất cả" ||
            currencyFilter == "All"
}

@Composable
private fun ReportDateRangeCard(
    isEng: Boolean,
    startDate: Long,
    endDate: Long,
    colors: BudgetColors,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = KatSpringSpec,
        label = "reportDateRangeScale"
    )

    AppCard(
        colors = colors,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                )
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = katStringResource(id = R.string.report_date_range, isEng = isEng),
                    color = colors.subText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${formatDateOnly(startDate)} - ${formatDateOnly(endDate)}",
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
