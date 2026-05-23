package com.katgr0up.katbudget.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.ui.components.ChoiceChip
import com.katgr0up.katbudget.ui.components.CurrencyRow
import com.katgr0up.katbudget.ui.components.MoneyInputRow
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.ui.utils.parseMoney

@Composable
fun BudgetDialog(
    isEng: Boolean,
    colors: BudgetColors,
    expenseOptions: List<String>,
    defaultCurrency: String,
    initialCategory: String? = null,
    initialAmount: Double? = null,
    initialCurrency: String? = null,
    onDismiss: () -> Unit,
    onSave: (String, Double, String) -> Unit
) {
    val fallbackOther = katStringResource(id = R.string.fallback_other, isEng = isEng)
    val fallbackAll = katStringResource(id = R.string.fallback_all, isEng = isEng)

    val categoryOptions = expenseOptions.filterNot {
        it == fallbackAll || it == "Tất cả" || it == "All"
    }

    var category by remember(categoryOptions, fallbackOther, initialCategory) {
        mutableStateOf(
            initialCategory
                ?.takeIf { it in categoryOptions }
                ?: categoryOptions.firstOrNull()
                ?: fallbackOther
        )
    }
    var amountInput by remember(initialAmount) {
        mutableStateOf(initialAmount?.toBudgetInput().orEmpty())
    }
    var currency by remember(defaultCurrency, initialCurrency) {
        mutableStateOf(normalizeCurrency(initialCurrency ?: defaultCurrency))
    }

    val parsedAmount = remember(amountInput) { parseMoney(amountInput) }
    val canSave = category.isNotBlank() && parsedAmount > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = katStringResource(id = R.string.budget_dialog_title, isEng = isEng),
                color = colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DialogSectionLabel(
                    text = katStringResource(id = R.string.recurring_label_category, isEng = isEng),
                    colors = colors
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(categoryOptions.ifEmpty { listOf(category) }) { option ->
                        ChoiceChip(
                            text = option,
                            selected = category == option,
                            colors = colors,
                            onClick = { category = option }
                        )
                    }
                }

                CurrencyRow(
                    selectedCurrency = currency,
                    colors = colors,
                    onCurrencyChanged = { currency = it }
                )

                MoneyInputRow(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = katStringResource(id = R.string.budget_label_limit, isEng = isEng),
                    colors = colors
                )
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
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
                onClick = {
                    if (canSave) {
                        onSave(category, parsedAmount, currency)
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

private fun Double.toBudgetInput(): String {
    return if (this % 1.0 == 0.0) {
        toLong().toString()
    } else {
        toString()
    }
}
