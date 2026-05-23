package com.katgr0up.katbudget.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.DebtEntity
import com.katgr0up.katbudget.ui.components.ChoiceChip
import com.katgr0up.katbudget.ui.components.CurrencyRow
import com.katgr0up.katbudget.ui.components.MoneyInputRow
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.utils.formatCurrency
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.parseMoney

@Composable
fun DebtPaymentDialog(
    isEng: Boolean,
    debt: DebtEntity,
    colors: BudgetColors,
    payAmountInput: String,
    onPayAmountChange: (String) -> Unit,
    payCurrency: String,
    onPayCurrencyChange: (String) -> Unit,
    accountOptions: List<String>,
    selectedSourceName: String,
    onSourceSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val inputAmount = parseMoney(payAmountInput)
    val canSubmit = inputAmount > 0.0 && selectedSourceName.isNotBlank()
    val remainingAmount = (debt.amount - debt.paidAmount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = if (debt.type == DebtEntity.TYPE_DEBT) {
                    katStringResource(id = R.string.debt_pay_title_pay, isEng = isEng)
                } else {
                    katStringResource(id = R.string.debt_pay_title_collect, isEng = isEng)
                },
                color = colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DebtPaymentPreviewCard(
                    label = katStringResource(id = R.string.debt_pay_label_remaining, isEng = isEng),
                    value = formatCurrency(remainingAmount, debt.currency),
                    colors = colors
                )

                Column {
                    DialogSectionLabel(
                        text = katStringResource(id = R.string.debt_pay_label_account, isEng = isEng),
                        colors = colors
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(accountOptions) { sourceName ->
                            ChoiceChip(
                                text = sourceName,
                                selected = selectedSourceName == sourceName,
                                colors = colors,
                                onClick = { onSourceSelected(sourceName) }
                            )
                        }
                    }
                }

                CurrencyRow(
                    selectedCurrency = payCurrency,
                    colors = colors,
                    onCurrencyChanged = onPayCurrencyChange
                )

                MoneyInputRow(
                    value = payAmountInput,
                    onValueChange = onPayAmountChange,
                    label = katStringResource(id = R.string.debt_pay_label_amount_add, isEng = isEng),
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
                enabled = canSubmit,
                onClick = onConfirm
            ) {
                Text(
                    text = katStringResource(id = R.string.btn_confirm, isEng = isEng),
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
private fun DebtPaymentPreviewCard(
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
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