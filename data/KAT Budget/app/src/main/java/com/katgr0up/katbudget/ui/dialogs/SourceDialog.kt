package com.katgr0up.katbudget.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.components.ChoiceChip
import com.katgr0up.katbudget.ui.components.MoneyInputRow
import com.katgr0up.katbudget.ui.components.appTextFieldColors
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.parseMoney

@Composable
fun SourceDialog(
    sourceToEdit: SourceEntity?,
    isEng: Boolean,
    colors: BudgetColors,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, Double, String, Double) -> Unit
) {
    var name by remember(sourceToEdit) { mutableStateOf(sourceToEdit?.name.orEmpty()) }
    var type by remember(sourceToEdit) { mutableStateOf(sourceToEdit?.type ?: SourceEntity.SourceType.BANK) }
    var includeInTotal by remember(sourceToEdit) { mutableStateOf(sourceToEdit?.includeInTotal ?: true) }
    var initialBalanceInput by remember { mutableStateOf("") }

    var interestRateInput by remember(sourceToEdit) {
        mutableStateOf(sourceToEdit?.interestRate?.takeIf { it > 0.0 }?.toString()?.replace(".", ",") ?: "")
    }

    var termMonths by remember(sourceToEdit) {
        mutableStateOf(sourceToEdit?.interestPeriod?.takeIf { it.startsWith("TERM_") }?.removePrefix("TERM_") ?: "1")
    }

    val isCreateMode = sourceToEdit == null
    val canSave = name.isNotBlank()
    val sourceTypeOptions = listOf(
        SourceEntity.SourceType.BANK to katStringResource(id = R.string.source_type_bank, isEng = isEng),
        SourceEntity.SourceType.WALLET to katStringResource(id = R.string.source_type_wallet, isEng = isEng),
        SourceEntity.SourceType.CASH to katStringResource(id = R.string.source_type_cash, isEng = isEng),
        SourceEntity.SourceType.SAVINGS to katStringResource(id = R.string.source_type_savings, isEng = isEng)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = if (isCreateMode) {
                    katStringResource(id = R.string.source_title_create, isEng = isEng)
                } else {
                    katStringResource(id = R.string.source_title_edit, isEng = isEng)
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
                DialogSectionLabel(
                    text = katStringResource(id = R.string.source_label_type, isEng = isEng),
                    colors = colors
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 1.dp)
                ) {
                    items(sourceTypeOptions) { (value, label) ->
                        ChoiceChip(
                            text = label,
                            selected = type == value,
                            colors = colors,
                            onClick = { type = value }
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text(
                            text = katStringResource(id = R.string.source_label_name, isEng = isEng),
                            color = colors.subText
                        )
                    },
                    enabled = isCreateMode,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = appTextFieldColors(colors),
                    shape = RoundedCornerShape(20.dp)
                )

                if (!isCreateMode) {
                    Text(
                        text = katStringResource(id = R.string.source_initial_balance_locked, isEng = isEng),
                        color = colors.subText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isCreateMode) {
                    MoneyInputRow(
                        value = initialBalanceInput,
                        onValueChange = { initialBalanceInput = it },
                        label = katStringResource(id = R.string.source_label_initial_balance, isEng = isEng),
                        colors = colors
                    )
                }

                if (type == SourceEntity.SourceType.SAVINGS) {
                    SavingsFields(
                        isEng = isEng,
                        colors = colors,
                        interestRateInput = interestRateInput,
                        onInterestRateChange = { interestRateInput = it },
                        termMonths = termMonths,
                        onTermChanged = { termMonths = it }
                    )
                }

                IncludeInTotalRow(
                    isEng = isEng,
                    checked = includeInTotal,
                    colors = colors,
                    onCheckedChange = { includeInTotal = it }
                )
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.background,
                    disabledContainerColor = colors.accent.copy(alpha = 0.24f),
                    disabledContentColor = colors.text.copy(alpha = 0.42f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = {
                    if (name.isNotBlank()) {
                        val finalInterestPeriod = if (type == SourceEntity.SourceType.SAVINGS) {
                            "TERM_${termMonths.ifBlank { "1" }}"
                        } else {
                            SourceEntity.InterestPeriod.NONE
                        }

                        val safeRate = interestRateInput
                            .replace(",", ".")
                            .toDoubleOrNull()
                            ?: 0.0

                        val safeInitialBalance = if (initialBalanceInput.isBlank()) {
                            0.0
                        } else {
                            runCatching { parseMoney(initialBalanceInput) }.getOrDefault(0.0)
                        }

                        onSave(
                            name.trim(),
                            type,
                            includeInTotal,
                            safeRate,
                            finalInterestPeriod,
                            safeInitialBalance
                        )
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
private fun SavingsFields(
    isEng: Boolean,
    colors: BudgetColors,
    interestRateInput: String,
    onInterestRateChange: (String) -> Unit,
    termMonths: String,
    onTermChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = interestRateInput,
            onValueChange = {
                if (it.isEmpty() || it.matches(Regex("^\\d*([.,]\\d*)?$"))) {
                    onInterestRateChange(it)
                }
            },
            label = {
                Text(
                    text = katStringResource(id = R.string.source_savings_rate, isEng = isEng),
                    color = colors.subText
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = appTextFieldColors(colors),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            trailingIcon = {
                Text(
                    text = katStringResource(id = R.string.source_savings_rate_suffix, isEng = isEng),
                    color = colors.subText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 16.dp)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp)
        )

        DialogSectionLabel(
            text = katStringResource(id = R.string.source_savings_term, isEng = isEng),
            colors = colors
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 1.dp)
        ) {
            items(listOf("1", "3", "6", "12")) { term ->
                ChoiceChip(
                    text = "$term ${katStringResource(id = R.string.source_savings_month_suffix, isEng = isEng)}",
                    selected = termMonths == term,
                    colors = colors,
                    onClick = { onTermChanged(term) }
                )
            }
        }
    }
}

@Composable
private fun IncludeInTotalRow(
    isEng: Boolean,
    checked: Boolean,
    colors: BudgetColors,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = katStringResource(id = R.string.source_net_worth_title, isEng = isEng),
                color = colors.text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = katStringResource(id = R.string.source_net_worth_desc, isEng = isEng),
                color = colors.subText,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.subText,
                uncheckedTrackColor = colors.card.copy(alpha = 0.72f),
                uncheckedBorderColor = colors.border.copy(alpha = 0.72f)
            )
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
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
