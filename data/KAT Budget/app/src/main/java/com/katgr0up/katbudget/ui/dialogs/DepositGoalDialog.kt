package com.katgr0up.katbudget.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.SavingGoalEntity
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.ui.components.ChoiceChip
import com.katgr0up.katbudget.ui.components.CurrencyRow
import com.katgr0up.katbudget.ui.components.MoneyInputRow
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.utils.formatCurrency
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.ui.utils.parseMoney

@Composable
fun DepositGoalDialog(
    goal: SavingGoalEntity,
    isEng: Boolean,
    colors: BudgetColors,
    sources: List<SourceEntity>,
    sourceBalances: Map<String, Map<String, Double>>,
    onDismiss: () -> Unit,
    onSave: (Double, String, String) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var sourceName by remember(sources) { mutableStateOf(sources.firstOrNull()?.name.orEmpty()) }
    var currency by remember(goal) { mutableStateOf(normalizeCurrency(goal.currency)) }

    val balances = sourceBalances[sourceName].orEmpty()
    val balanceText = if (balances.isEmpty()) {
        "0 VND"
    } else {
        balances.entries.sortedBy { it.key }.joinToString(", ") { (currency, amount) ->
            formatCurrency(amount, currency)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = katStringResource(id = R.string.goal_deposit_title, isEng = isEng, goal.name),
                color = colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                lineHeight = 24.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DialogSectionLabel(
                    text = katStringResource(id = R.string.goal_deposit_account, isEng = isEng),
                    colors = colors
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(sources, key = { it.id }) { source ->
                        ChoiceChip(
                            text = source.name,
                            selected = sourceName == source.name,
                            colors = colors,
                            onClick = { sourceName = source.name }
                        )
                    }
                }

                if (sourceName.isNotBlank()) {
                    BalancePreviewCard(
                        label = katStringResource(id = R.string.goal_deposit_current_balance, isEng = isEng),
                        value = balanceText,
                        colors = colors
                    )
                }

                DialogSectionLabel(
                    text = katStringResource(id = R.string.goal_deposit_currency, isEng = isEng),
                    colors = colors
                )

                CurrencyRow(currency, colors) { currency = it }

                MoneyInputRow(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = katStringResource(id = R.string.goal_deposit_amount, isEng = isEng),
                    colors = colors
                )
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.background,
                    disabledContainerColor = colors.accent.copy(alpha = 0.28f),
                    disabledContentColor = colors.background.copy(alpha = 0.55f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = parseMoney(amountInput) > 0.0 && sourceName.isNotBlank(),
                onClick = {
                    val amount = parseMoney(amountInput)
                    if (amount > 0.0 && sourceName.isNotBlank()) {
                        onSave(amount, sourceName, currency)
                    }
                }
            ) {
                Text(
                    text = katStringResource(id = R.string.btn_save, isEng = isEng),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.text.copy(alpha = 0.08f),
                    contentColor = colors.text
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = onDismiss
            ) {
                Text(
                    text = katStringResource(id = R.string.btn_close, isEng = isEng),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}

@Composable
private fun BalancePreviewCard(
    label: String,
    value: String,
    colors: BudgetColors
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                color = colors.border.copy(alpha = 0.56f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = colors.subText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = value,
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 19.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        )
    }
}

@Composable
private fun DialogSectionLabel(
    text: String,
    colors: BudgetColors
) {
    Text(
        text = text,
        color = colors.subText,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}
