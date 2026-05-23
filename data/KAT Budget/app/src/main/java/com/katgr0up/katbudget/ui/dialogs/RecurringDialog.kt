package com.katgr0up.katbudget.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.RecurringEntity
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.ui.components.ChoiceChip
import com.katgr0up.katbudget.ui.components.CurrencyRow
import com.katgr0up.katbudget.ui.components.MoneyInputRow
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.components.appTextFieldColors
import com.katgr0up.katbudget.ui.utils.formatInputNumber
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.ui.utils.parseMoney
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringDialog(
    recurringToEdit: RecurringEntity?,
    isEng: Boolean,
    colors: BudgetColors,
    sources: List<SourceEntity>,
    expenseOptions: List<String>,
    incomeOptions: List<String>,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (amt: Double, type: String, cat: String, note: String, src: String, curr: String, day: Int) -> Unit
) {
    val context = LocalContext.current

    val fallbackOther = katStringResource(id = R.string.fallback_other, isEng = isEng)
    val fallbackOtherIncome = katStringResource(id = R.string.fallback_other_income, isEng = isEng)
    val fallbackAll = katStringResource(id = R.string.fallback_all, isEng = isEng)

    var type by remember(recurringToEdit) { mutableStateOf(recurringToEdit?.type ?: "EXPENSE") }
    var sourceName by remember(recurringToEdit, sources) {
        mutableStateOf(recurringToEdit?.sourceName ?: sources.firstOrNull()?.name.orEmpty())
    }
    var currency by remember(recurringToEdit, defaultCurrency) {
        mutableStateOf(recurringToEdit?.currency ?: normalizeCurrency(defaultCurrency))
    }
    var dayInput by remember(recurringToEdit) {
        mutableStateOf(recurringToEdit?.dayOfMonth?.toString() ?: "1")
    }
    var amountInput by remember(recurringToEdit) {
        mutableStateOf(
            recurringToEdit?.amount?.let {
                if (it % 1.0 == 0.0) {
                    formatInputNumber(it.toLong().toString())
                } else {
                    formatInputNumber(it.toString().replace(".", ","))
                }
            }.orEmpty()
        )
    }
    var note by remember(recurringToEdit) { mutableStateOf(recurringToEdit?.note.orEmpty()) }
    var category by remember(recurringToEdit, type) {
        mutableStateOf(recurringToEdit?.category ?: if (type == "INCOME") fallbackOtherIncome else fallbackOther)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = if (recurringToEdit == null) {
                    katStringResource(id = R.string.recurring_dialog_title_add, isEng = isEng)
                } else {
                    katStringResource(id = R.string.recurring_dialog_title_edit, isEng = isEng)
                },
                color = colors.text,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(
                        text = katStringResource(id = R.string.recurring_chip_expense, isEng = isEng),
                        selected = type == "EXPENSE",
                        colors = colors
                    ) {
                        type = "EXPENSE"
                        category = fallbackOther
                    }

                    ChoiceChip(
                        text = katStringResource(id = R.string.recurring_chip_income, isEng = isEng),
                        selected = type == "INCOME",
                        colors = colors
                    ) {
                        type = "INCOME"
                        category = fallbackOtherIncome
                    }
                }

                DialogSectionLabel(
                    text = katStringResource(id = R.string.recurring_label_account, isEng = isEng),
                    colors = colors
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 1.dp)
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

                CurrencyRow(currency, colors) { currency = it }

                DialogSectionLabel(
                    text = katStringResource(id = R.string.recurring_label_category, isEng = isEng),
                    colors = colors
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 1.dp)
                ) {
                    val options = (if (type == "INCOME") incomeOptions else expenseOptions)
                        .filterNot { it == fallbackAll || it == "Tất cả" || it == "All" }
                    val defaultOptions = listOf(if (type == "INCOME") fallbackOtherIncome else fallbackOther)

                    items(options.ifEmpty { defaultOptions }, key = { it }) { option ->
                        ChoiceChip(
                            text = option,
                            selected = category == option,
                            colors = colors,
                            onClick = { category = option }
                        )
                    }
                }

                OutlinedTextField(
                    value = dayInput,
                    onValueChange = { value ->
                        dayInput = value.filter { char -> char.isDigit() }.take(2)
                    },
                    label = {
                        Text(
                            text = katStringResource(id = R.string.recurring_label_day_hint, isEng = isEng),
                            color = colors.subText
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = appTextFieldColors(colors),
                    shape = RoundedCornerShape(20.dp)
                )

                MoneyInputRow(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = katStringResource(id = R.string.recurring_label_amount, isEng = isEng),
                    colors = colors
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = {
                        Text(
                            text = katStringResource(id = R.string.recurring_label_note, isEng = isEng),
                            color = colors.subText
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = appTextFieldColors(colors),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        },
        confirmButton = {
            val toastInvalidInfo = katStringResource(id = R.string.toast_recurring_invalid_info, isEng = isEng)

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
                onClick = {
                    val amount = parseMoney(amountInput)
                    val day = dayInput.toIntOrNull() ?: 0

                    if (amount <= 0.0 || day !in 1..31 || sourceName.isBlank()) {
                        Toast.makeText(context, toastInvalidInfo, Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    onSave(amount, type, category, note.trim(), sourceName, currency, day)
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
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
