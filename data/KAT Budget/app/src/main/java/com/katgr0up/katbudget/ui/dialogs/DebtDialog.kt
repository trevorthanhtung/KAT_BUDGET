package com.katgr0up.katbudget.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.DebtEntity
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.ui.components.ChoiceChip
import com.katgr0up.katbudget.ui.components.CurrencyRow
import com.katgr0up.katbudget.ui.components.MoneyInputRow
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.components.appTextFieldColors
import com.katgr0up.katbudget.ui.utils.formatDateOnly
import com.katgr0up.katbudget.ui.utils.formatInputNumber
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.ui.utils.parseMoney
import com.katgr0up.katbudget.utils.TagPrefix
import com.katgr0up.katbudget.utils.TxType

@Composable
fun DebtDialog(
    debtToEdit: DebtEntity?,
    sources: List<SourceEntity>,
    transactions: List<TransactionEntity>,
    isEng: Boolean,
    colors: BudgetColors,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, String, Long?, String, Boolean) -> Unit
) {
    val isCreateMode = debtToEdit == null
    val openingTransaction = remember(debtToEdit, transactions) {
        debtToEdit?.let { debt ->
            transactions.firstOrNull { transaction ->
                transaction.projectTag == "${TagPrefix.DEBT_OPENING}${debt.timestamp}"
            }
        }
    }
    var personName by remember(debtToEdit) { mutableStateOf(debtToEdit?.personName.orEmpty()) }
    var amountInput by remember(debtToEdit) {
        mutableStateOf(
            debtToEdit?.amount?.let {
                if (it % 1.0 == 0.0) {
                    formatInputNumber(it.toLong().toString())
                } else {
                    formatInputNumber(it.toString().replace(".", ","))
                }
            }.orEmpty()
        )
    }
    var currency by remember(debtToEdit) { mutableStateOf(normalizeCurrency(debtToEdit?.currency ?: "VND")) }
    var type by remember(debtToEdit) { mutableStateOf(debtToEdit?.type ?: DebtEntity.TYPE_LOAN) }
    var note by remember(debtToEdit) { mutableStateOf(debtToEdit?.note.orEmpty()) }
    var dueDate by remember(debtToEdit) { mutableStateOf(debtToEdit?.dueDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var sourceName by remember(sources) { mutableStateOf(sources.firstOrNull()?.name.orEmpty()) }
    var includeInCashFlow by remember(debtToEdit, openingTransaction) {
        mutableStateOf(
            openingTransaction?.let { it.type == TxType.INCOME || it.type == TxType.EXPENSE }
                ?: true
        )
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueDate)
    val parsedAmount = remember(amountInput) { parseMoney(amountInput) }
    val isFormValid = personName.isNotBlank() &&
            parsedAmount > 0.0 &&
            (!isCreateMode || sourceName.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = if (isCreateMode) {
                    katStringResource(id = R.string.debt_dialog_title_add, isEng = isEng)
                } else {
                    katStringResource(id = R.string.debt_dialog_title_edit, isEng = isEng)
                },
                color = colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip(
                        text = katStringResource(id = R.string.debt_chip_lent, isEng = isEng),
                        selected = type == DebtEntity.TYPE_LOAN,
                        colors = colors,
                        onClick = { type = DebtEntity.TYPE_LOAN }
                    )

                    ChoiceChip(
                        text = katStringResource(id = R.string.debt_chip_borrowed, isEng = isEng),
                        selected = type == DebtEntity.TYPE_DEBT,
                        colors = colors,
                        onClick = { type = DebtEntity.TYPE_DEBT }
                    )
                }

                CurrencyRow(
                    selectedCurrency = currency,
                    colors = colors,
                    onCurrencyChanged = { currency = it }
                )

                if (isCreateMode) {
                    Text(
                        text = katStringResource(id = R.string.debt_label_account, isEng = isEng),
                        color = colors.subText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (sources.isEmpty()) {
                        Text(
                            text = katStringResource(id = R.string.toast_create_account_first, isEng = isEng),
                            color = colors.negative,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(sources, key = { it.id }) { source ->
                                ChoiceChip(
                                    text = source.name,
                                    selected = sourceName == source.name,
                                    colors = colors,
                                    onClick = { sourceName = source.name }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.card.copy(alpha = 0.56f))
                        .border(
                            width = 1.dp,
                            color = colors.border.copy(alpha = 0.58f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = katStringResource(id = R.string.debt_label_include_cash_flow, isEng = isEng),
                            color = colors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = katStringResource(id = R.string.debt_hint_include_cash_flow, isEng = isEng),
                            color = colors.subText,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }

                    Switch(
                        checked = includeInCashFlow,
                        onCheckedChange = { includeInCashFlow = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.background,
                            checkedTrackColor = colors.accent,
                            uncheckedThumbColor = colors.subText,
                            uncheckedTrackColor = colors.border
                        )
                    )
                }

                OutlinedTextField(
                    value = personName,
                    onValueChange = { personName = it },
                    label = {
                        Text(
                            text = katStringResource(id = R.string.debt_label_name, isEng = isEng) + " (*)",
                            color = colors.subText
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = appTextFieldColors(colors)
                )

                MoneyInputRow(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = katStringResource(id = R.string.debt_label_amount, isEng = isEng),
                    colors = colors
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = {
                        Text(
                            text = katStringResource(id = R.string.debt_label_note, isEng = isEng),
                            color = colors.subText
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = appTextFieldColors(colors)
                )

                DebtDueDateSelector(
                    isEng = isEng,
                    colors = colors,
                    dueDate = dueDate,
                    onClick = { showDatePicker = true }
                )

                if (dueDate != null) {
                    TextButton(onClick = { dueDate = null }) {
                        Text(
                            text = katStringResource(id = R.string.debt_btn_clear_due_date, isEng = isEng),
                            color = colors.negative,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = isFormValid,
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
                    onSave(personName.trim(), parsedAmount, currency, type, note.trim(), dueDate, sourceName, includeInCashFlow)
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

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueDate = datePickerState.selectedDateMillis
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = katStringResource(id = R.string.btn_select, isEng = isEng),
                        color = colors.accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(
                        text = katStringResource(id = R.string.btn_cancel, isEng = isEng),
                        color = colors.subText
                    )
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DebtDueDateSelector(
    isEng: Boolean,
    colors: BudgetColors,
    dueDate: Long?,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card.copy(alpha = 0.56f))
            .border(
                width = 1.dp,
                color = colors.border.copy(alpha = 0.58f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp)
    ) {
        Text(
            text = dueDate?.let {
                "${katStringResource(id = R.string.debt_label_due_date, isEng = isEng)} ${formatDateOnly(it)}"
            } ?: katStringResource(id = R.string.debt_hint_select_due_date, isEng = isEng),
            color = if (dueDate != null) colors.text else colors.subText,
            fontSize = 14.sp,
            fontWeight = if (dueDate != null) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
