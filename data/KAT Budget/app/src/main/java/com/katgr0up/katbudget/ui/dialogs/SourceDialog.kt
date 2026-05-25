package com.katgr0up.katbudget.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.components.ChoiceChip
import com.katgr0up.katbudget.ui.components.CurrencyRow
import com.katgr0up.katbudget.ui.components.MoneyInputRow
import com.katgr0up.katbudget.ui.components.appTextFieldColors
import com.katgr0up.katbudget.ui.utils.formatSmartCurrency
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.ui.utils.parseMoney

data class SourceOpeningBalance(
    val amount: Double,
    val currency: String
)

@Composable
fun SourceDialog(
    sourceToEdit: SourceEntity?,
    existingSources: List<SourceEntity>,
    isEng: Boolean,
    defaultCurrency: String,
    colors: BudgetColors,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, Double, String, List<SourceOpeningBalance>) -> Unit
) {
    val context = LocalContext.current
    var name by remember(sourceToEdit) { mutableStateOf(sourceToEdit?.name.orEmpty()) }
    var type by remember(sourceToEdit) { mutableStateOf(sourceToEdit?.type ?: SourceEntity.SourceType.BANK) }
    var includeInTotal by remember(sourceToEdit) { mutableStateOf(sourceToEdit?.includeInTotal ?: true) }
    var showOpeningBalanceDialog by remember { mutableStateOf(false) }
    val openingBalances = remember(sourceToEdit) { mutableStateListOf<SourceOpeningBalance>() }

    var interestRateInput by remember(sourceToEdit) {
        mutableStateOf(sourceToEdit?.interestRate?.takeIf { it > 0.0 }?.toString()?.replace(".", ",") ?: "")
    }

    var termMonths by remember(sourceToEdit) {
        mutableStateOf(sourceToEdit?.interestPeriod?.takeIf { it.startsWith("TERM_") }?.removePrefix("TERM_") ?: "1")
    }

    val isCreateMode = sourceToEdit == null
    val canSave = name.isNotBlank()
    val duplicateNameMessage = katStringResource(id = R.string.toast_source_duplicate_name, isEng = isEng)
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
                    OpeningBalancesSection(
                        isEng = isEng,
                        colors = colors,
                        balances = openingBalances,
                        onAddClick = { showOpeningBalanceDialog = true },
                        onRemove = { balance -> openingBalances.remove(balance) }
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
                    val safeName = name.trim()
                    if (safeName.isNotBlank()) {
                        val hasDuplicateName = isCreateMode && existingSources.any { source ->
                            source.name.trim().equals(safeName, ignoreCase = true)
                        }

                        if (hasDuplicateName) {
                            Toast.makeText(context, duplicateNameMessage, Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val finalInterestPeriod = if (type == SourceEntity.SourceType.SAVINGS) {
                            "TERM_${termMonths.ifBlank { "1" }}"
                        } else {
                            SourceEntity.InterestPeriod.NONE
                        }

                        val safeRate = interestRateInput
                            .replace(",", ".")
                            .toDoubleOrNull()
                            ?: 0.0

                        onSave(
                            safeName,
                            type,
                            includeInTotal,
                            safeRate,
                            finalInterestPeriod,
                            openingBalances
                                .filter { it.amount > 0.0 }
                                .map { it.copy(currency = normalizeCurrency(it.currency)) }
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

    if (showOpeningBalanceDialog) {
        AddOpeningBalanceDialog(
            isEng = isEng,
            colors = colors,
            defaultCurrency = defaultCurrency,
            onDismiss = { showOpeningBalanceDialog = false },
            onAdd = { balance ->
                val normalizedCurrency = normalizeCurrency(balance.currency)
                val existingIndex = openingBalances.indexOfFirst {
                    normalizeCurrency(it.currency) == normalizedCurrency
                }

                if (existingIndex >= 0) {
                    val existing = openingBalances[existingIndex]
                    openingBalances[existingIndex] = existing.copy(
                        amount = existing.amount + balance.amount,
                        currency = normalizedCurrency
                    )
                } else {
                    openingBalances.add(balance.copy(currency = normalizedCurrency))
                }
                showOpeningBalanceDialog = false
            }
        )
    }
}

@Composable
private fun OpeningBalancesSection(
    isEng: Boolean,
    colors: BudgetColors,
    balances: List<SourceOpeningBalance>,
    onAddClick: () -> Unit,
    onRemove: (SourceOpeningBalance) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DialogSectionLabel(
            text = katStringResource(id = R.string.source_label_initial_balance, isEng = isEng),
            colors = colors
        )

        if (balances.isEmpty()) {
            Text(
                text = katStringResource(id = R.string.source_initial_balance_empty, isEng = isEng),
                color = colors.subText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        } else {
            balances.forEach { balance ->
                OpeningBalanceRow(
                    isEng = isEng,
                    colors = colors,
                    balance = balance,
                    onRemove = { onRemove(balance) }
                )
            }
        }

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent.copy(alpha = 0.12f),
                contentColor = colors.accent
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = katStringResource(id = R.string.source_initial_balance_add, isEng = isEng),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OpeningBalanceRow(
    isEng: Boolean,
    colors: BudgetColors,
    balance: SourceOpeningBalance,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card.copy(alpha = 0.58f))
            .border(
                width = 1.dp,
                color = colors.border.copy(alpha = 0.52f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(start = 14.dp, end = 6.dp, top = 9.dp, bottom = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatSmartCurrency(balance.amount, balance.currency, isEng = isEng),
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = katStringResource(id = R.string.btn_delete, isEng = isEng),
                tint = colors.subText,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun AddOpeningBalanceDialog(
    isEng: Boolean,
    colors: BudgetColors,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onAdd: (SourceOpeningBalance) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    var selectedCurrency by remember(defaultCurrency) {
        mutableStateOf(normalizeCurrency(defaultCurrency))
    }
    val amount = remember(amountInput) {
        runCatching { parseMoney(amountInput) }.getOrDefault(0.0)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = katStringResource(id = R.string.source_initial_balance_add_title, isEng = isEng),
                color = colors.text,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CurrencyRow(
                    selectedCurrency = selectedCurrency,
                    colors = colors,
                    onCurrencyChanged = { selectedCurrency = it }
                )

                MoneyInputRow(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = katStringResource(id = R.string.source_initial_balance_amount, isEng = isEng),
                    colors = colors
                )
            }
        },
        confirmButton = {
            Button(
                enabled = amount > 0.0,
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
                    onAdd(
                        SourceOpeningBalance(
                            amount = amount,
                            currency = selectedCurrency
                        )
                    )
                }
            ) {
                Text(
                    text = katStringResource(id = R.string.btn_add, isEng = isEng),
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
