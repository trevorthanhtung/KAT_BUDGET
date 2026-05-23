package com.katgr0up.katbudget.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.BudgetEntity
import com.katgr0up.katbudget.data.local.entity.CategoryEntity
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.ui.utils.KatSpringSpec
import com.katgr0up.katbudget.ui.utils.convertCurrency
import com.katgr0up.katbudget.ui.utils.formatDate
import com.katgr0up.katbudget.ui.utils.formatSmartCurrency
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.ui.utils.rememberCategoryNameLocalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SourceFilterRow(
    isEng: Boolean,
    sources: List<SourceEntity>,
    sourceBalances: Map<String, Map<String, Double>>,
    defaultCurrency: String,
    selectedSourceFilter: String,
    colors: BudgetColors,
    exchangeRates: Map<String, Double>,
    onSourceFilterChanged: (String) -> Unit,
    onEditSource: (SourceEntity) -> Unit
) {
    val fallbackAll = katStringResource(id = R.string.fallback_all, isEng = isEng)

    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(sources, key = { it.id }) { source ->
            val balance = sourceBalances[source.name].orEmpty()
            val total = balance.entries.sumOf {
                convertCurrency(it.value, it.key, defaultCurrency, exchangeRates)
            }

            SourceChip(
                modifier = Modifier.animateItem(),
                title = source.name,
                subtitle = formatSmartCurrency(total, defaultCurrency, isEng = true, isPrivacyMode = com.katgr0up.katbudget.ui.utils.LocalPrivacyMode.current),
                selected = selectedSourceFilter == source.name,
                colors = colors,
                onClick = {
                    onSourceFilterChanged(
                        if (selectedSourceFilter == source.name) fallbackAll else source.name
                    )
                },
                onLongClick = { onEditSource(source) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SourceChip(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    selected: Boolean,
    colors: BudgetColors,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = KatSpringSpec,
        label = "source_chip_spring"
    )

    val shape = RoundedCornerShape(24.dp)
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) colors.accent.copy(alpha = 0.13f) else colors.card.copy(alpha = 0.78f),
        label = "source_chip_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.accent.copy(alpha = 0.55f) else colors.border.copy(alpha = 0.54f),
        label = "source_chip_border"
    )

    Box(
        modifier = modifier
            .width(166.dp)
            .height(94.dp)
            .scale(scale)
            .shadow(
                elevation = if (selected) 8.dp else 4.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.03f),
                spotColor = Color.Black.copy(alpha = 0.08f),
                clip = false
            )
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                color = if (selected) colors.accent else colors.subText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionRow(
    modifier: Modifier = Modifier,
    transaction: TransactionEntity,
    transactions: List<TransactionEntity>,
    budgets: List<BudgetEntity>,
    categories: List<CategoryEntity>,
    isEng: Boolean,
    colors: BudgetColors,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewImage: (String) -> Unit
) {
    val monthFormatter = remember { SimpleDateFormat("yyyy-MM", Locale.US) }
    val currentMonthYear = remember { monthFormatter.format(Date()) }
    val isExpense = transaction.type == "EXPENSE"
    val categoryNamesById = remember(categories) {
        categories.associate { it.id to it.name }
    }

    val budget = budgets.find {
        categoryNamesById[it.categoryId] == transaction.category &&
                it.monthYear == currentMonthYear &&
                normalizeCurrency(it.currencyCode) == normalizeCurrency(transaction.currency)
    }

    val budgetLimitMajor = budget?.let {
        if (normalizeCurrency(it.currencyCode) in listOf("VND", "JPY", "KRW")) {
            it.limitAmountMinor.toDouble()
        } else {
            it.limitAmountMinor / 100.0
        }
    } ?: 0.0

    val budgetRatio = if (budget != null && isExpense && budgetLimitMajor > 0.0) {
        val totalSpent = transactions
            .filter {
                it.category == transaction.category &&
                        it.type == "EXPENSE" &&
                        normalizeCurrency(it.currency) == normalizeCurrency(transaction.currency) &&
                        monthFormatter.format(Date(it.timestamp)) == currentMonthYear
            }
            .sumOf { it.amount }

        totalSpent / budgetLimitMajor
    } else {
        0.0
    }

    val warningColor = when {
        budgetRatio >= 1.0 -> colors.negative
        budgetRatio >= 0.8 -> colors.warning
        else -> Color.Transparent
    }

    val animatedBorder by animateColorAsState(
        targetValue = if (warningColor == Color.Transparent) {
            colors.border.copy(alpha = 0.54f)
        } else {
            warningColor.copy(alpha = 0.76f)
        },
        label = "transaction_border"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = KatSpringSpec,
        label = "tx_row_spring"
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

    val itemColor = transactionColor(transaction.type, colors)
    val prefix = transactionPrefix(transaction.type)
    val rowShape = RoundedCornerShape(22.dp)

    SwipeToDismissBox(
        modifier = modifier,
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val bgColor by animateColorAsState(
                targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    colors.negative.copy(alpha = 0.18f)
                } else {
                    Color.Transparent
                },
                label = "swipe_bg"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .clip(rowShape)
                    .background(bgColor),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Text(
                        text = katStringResource(id = R.string.btn_delete, isEng = isEng),
                        color = colors.negative,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 22.dp)
                    )
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .shadow(
                    elevation = 5.dp,
                    shape = rowShape,
                    ambientColor = Color.Black.copy(alpha = 0.025f),
                    spotColor = Color.Black.copy(alpha = 0.055f),
                    clip = false
                )
                .clip(rowShape)
                .background(colors.card.copy(alpha = 0.86f))
                .border(1.dp, animatedBorder, rowShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onEdit
                )
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(50.dp)
                    .clip(CircleShape)
                    .background(itemColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            TransactionBody(
                transaction = transaction,
                isEng = isEng,
                colors = colors,
                budgetRatio = budgetRatio,
                onViewImage = onViewImage,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "$prefix${formatSmartCurrency(abs(transaction.amount), transaction.currency, isEng = true, isPrivacyMode = com.katgr0up.katbudget.ui.utils.LocalPrivacyMode.current)}",
                color = itemColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(min = 88.dp, max = 130.dp)
            )
        }
    }
}

@Composable
private fun TransactionBody(
    transaction: TransactionEntity,
    isEng: Boolean,
    colors: BudgetColors,
    budgetRatio: Double,
    onViewImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val localizeCategory = rememberCategoryNameLocalizer(isEng)

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = localizeCategory(transaction.category),
                color = colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            if (!transaction.imageUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = katStringResource(id = R.string.tx_receipt_attach, isEng = isEng)
                        .replace("Thêm hóa đơn", "Hóa đơn")
                        .replace("Add receipt", "Receipt"),
                    color = colors.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.clickable { onViewImage(transaction.imageUri!!) }
                )
            }
        }

        if (transaction.note.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = transaction.note,
                color = colors.subText,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = "${transaction.sourceName} • ${formatDate(transaction.timestamp)}",
            color = colors.subText,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        when {
            budgetRatio >= 1.0 -> BudgetWarningText(
                text = katStringResource(id = R.string.budget_over, isEng = isEng),
                color = colors.negative
            )

            budgetRatio >= 0.8 -> BudgetWarningText(
                text = katStringResource(id = R.string.budget_warning_80, isEng = isEng),
                color = colors.warning
            )
        }
    }
}

@Composable
private fun BudgetWarningText(
    text: String,
    color: Color
) {
    Spacer(modifier = Modifier.height(5.dp))
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private fun transactionColor(
    type: String,
    colors: BudgetColors
): Color {
    return when (type) {
        "EXPENSE" -> colors.negative
        "INCOME" -> colors.positive
        "GOAL_DEPOSIT" -> colors.savings
        "DEBT_PAYMENT", "DEBT_COLLECTION", "LENDING", "BORROWING" -> colors.debt
        "TRANSFER_IN", "TRANSFER_OUT" -> colors.info
        else -> colors.subText
    }
}

private fun transactionPrefix(type: String): String {
    return when (type) {
        "EXPENSE", "GOAL_DEPOSIT", "TRANSFER_OUT", "LENDING", "DEBT_PAYMENT" -> "-"
        "INCOME", "TRANSFER_IN", "BORROWING", "DEBT_COLLECTION" -> "+"
        else -> ""
    }
}
