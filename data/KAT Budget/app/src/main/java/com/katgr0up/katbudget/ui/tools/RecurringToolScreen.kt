package com.katgr0up.katbudget.ui.tools

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.RecurringEntity
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.ui.components.*
import com.katgr0up.katbudget.ui.utils.formatCurrency
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.ui.utils.katStringResource

@Composable
fun RecurringToolScreen(
    isEng: Boolean,
    defaultCurrency: String,
    exchangeRates: Map<String, Double>,
    recurrings: List<RecurringEntity>,
    sources: List<SourceEntity>,
    colors: BudgetColors,
    onBack: () -> Unit,
    onCreateRecurring: () -> Unit,
    onEditRecurring: (RecurringEntity) -> Unit,
    onDeleteRecurring: (RecurringEntity) -> Unit
) {
    fun getConverted(amount: Double, fromCur: String): Double {
        val normFrom = normalizeCurrency(fromCur)
        val normTo = normalizeCurrency(defaultCurrency)
        if (normFrom == normTo) return amount
        val rateFrom = exchangeRates[normFrom] ?: 1.0
        val rateTo = exchangeRates[normTo] ?: 1.0
        return (amount * rateFrom) / rateTo
    }

    val totalIncome = recurrings.filter { it.type == "INCOME" }.sumOf { getConverted(it.amount, it.currency) }
    val totalExpense = recurrings.filter { it.type == "EXPENSE" }.sumOf { getConverted(it.amount, it.currency) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            ToolHeader(
                title = katStringResource(id = R.string.recurring_tool_title, isEng = isEng),
                colors = colors,
                onBack = onBack
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppCard(colors = colors, modifier = Modifier.weight(1f).animateContentSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = katStringResource(id = R.string.recurring_summary_income, isEng = isEng), color = colors.positive, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = formatCurrency(totalIncome, defaultCurrency), color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
                AppCard(colors = colors, modifier = Modifier.weight(1f).animateContentSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = katStringResource(id = R.string.recurring_summary_expense, isEng = isEng), color = colors.negative, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = formatCurrency(totalExpense, defaultCurrency), color = colors.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = katStringResource(id = R.string.recurring_schedule_title, isEng = isEng),
                action = katStringResource(id = R.string.btn_create, isEng = isEng),
                colors = colors,
                onAction = onCreateRecurring
            )
        }

        if (recurrings.isEmpty()) {
            item { EmptyState(text = katStringResource(id = R.string.recurring_empty_state, isEng = isEng), colors = colors) }
        } else {
            items(recurrings, key = { it.id }) { recurring ->
                RecurringItemRow(
                    recurring = recurring,
                    isEng = isEng,
                    colors = colors,
                    onEdit = { onEditRecurring(recurring) },
                    onDelete = { onDeleteRecurring(recurring) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun RecurringItemRow(
    recurring: RecurringEntity,
    isEng: Boolean,
    colors: BudgetColors,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                label = "recurringSwipeBg"
            )
            Box(Modifier.fillMaxSize().padding(vertical = 2.dp).clip(RoundedCornerShape(16.dp)).background(bgColor))
        }
    ) {
        AppCard(
            colors = colors,
            modifier = Modifier.fillMaxWidth().animateContentSize().combinedClickable(onClick = {}, onLongClick = { onEdit() })
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(1.dp, if (recurring.type == "EXPENSE") colors.negative else colors.positive, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = recurring.dayOfMonth.toString(), color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val displayCategory = if (!isEng && recurring.category == "Other") "Khác" else recurring.category
                    Text(text = displayCategory, color = colors.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(text = recurring.sourceName, color = colors.subText, fontSize = 13.sp)
                }
                Text(
                    text = "${if (recurring.type == "EXPENSE") "-" else "+"}${formatCurrency(recurring.amount, recurring.currency)}",
                    color = if (recurring.type == "EXPENSE") colors.negative else colors.positive,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
