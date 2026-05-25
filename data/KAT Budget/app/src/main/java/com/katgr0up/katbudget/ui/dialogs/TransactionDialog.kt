package com.katgr0up.katbudget.ui.dialogs

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.ui.components.*
import com.katgr0up.katbudget.ui.utils.*
import java.util.Calendar

data class TransactionForm(
    val amount: Double, val type: String, val category: String, val note: String,
    val sourceName: String, val targetSourceName: String, val currency: String,
    val timestamp: Long, val imageUri: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDialog(
    transactionToEdit: TransactionEntity?,
    initialSourceName: String,
    isEng: Boolean,
    sources: List<SourceEntity>,
    expenseOptions: List<String>, incomeOptions: List<String>, colors: BudgetColors, defaultCurrency: String,
    initialType: String? = null,
    onDismiss: () -> Unit, onSave: (TransactionForm) -> Unit,
    onCreateCategory: (String, String) -> Unit,
    onDeleteCategory: (String, String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val fallbackOther = katStringResource(id = R.string.fallback_other, isEng = isEng)
    val fallbackOtherIncome = katStringResource(id = R.string.fallback_other_income, isEng = isEng)
    val fallbackAll = katStringResource(id = R.string.fallback_all, isEng = isEng)
    val transferLabel = katStringResource(id = R.string.tx_chip_transfer, isEng = isEng)
    val expenseLabel = katStringResource(id = R.string.tx_chip_expense, isEng = isEng)
    val incomeLabel = katStringResource(id = R.string.tx_chip_income, isEng = isEng)
    val safeInitialType = when (initialType) {
        "INCOME", "EXPENSE", "TRANSFER_OUT" -> initialType
        else -> "EXPENSE"
    }

    var type by remember(transactionToEdit, safeInitialType) { mutableStateOf(transactionToEdit?.type ?: safeInitialType) }
    var amountInput by remember(transactionToEdit) {
        mutableStateOf(transactionToEdit?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty())
    }
    var note by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.note.orEmpty()) }

    var sourceName by remember(transactionToEdit, sources, initialSourceName) {
        mutableStateOf(
            transactionToEdit?.sourceName
                ?: initialSourceName.takeIf { it.isNotBlank() && it != fallbackAll && it != "Tất cả" && it != "All" }
                ?: sources.firstOrNull()?.name.orEmpty()
        )
    }

    var targetSourceName by remember(sources) { mutableStateOf(sources.drop(1).firstOrNull()?.name ?: sources.firstOrNull()?.name.orEmpty()) }
    var currency by remember(transactionToEdit, defaultCurrency) { mutableStateOf(normalizeCurrency(transactionToEdit?.currency ?: defaultCurrency)) }
    var category by remember(transactionToEdit, type) { mutableStateOf(transactionToEdit?.category ?: if (type == "INCOME") fallbackOtherIncome else fallbackOther) }
    var imageUri by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.imageUri) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = transactionToEdit?.timestamp ?: System.currentTimeMillis())
    var timestamp by remember(transactionToEdit) { mutableLongStateOf(transactionToEdit?.timestamp ?: System.currentTimeMillis()) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            runCatching { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            imageUri = it.toString()
        }
    }

    val toastInvalidAmount = katStringResource(id = R.string.toast_tx_invalid_amount, isEng = isEng)
    val toastSelectDestination = katStringResource(id = R.string.toast_tx_select_destination, isEng = isEng)
    val toastAmountTooLarge = katStringResource(id = R.string.toast_tx_amount_too_large, isEng = isEng)

    val handleSave = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        val finalAmount = calculateExpression(amountInput)
        val safeSource = sourceName.ifBlank { sources.firstOrNull()?.name.orEmpty() }
        val safeTarget = targetSourceName.ifBlank { sources.firstOrNull { it.name != safeSource }?.name.orEmpty() }

        if (finalAmount <= 0.0 || safeSource.isBlank()) {
            Toast.makeText(context, toastInvalidAmount, Toast.LENGTH_SHORT).show()
        } else if (type == "TRANSFER_OUT" && safeTarget.isBlank()) {
            Toast.makeText(context, toastSelectDestination, Toast.LENGTH_SHORT).show()
        } else {
            val catToSave = if (type == "TRANSFER_OUT") transferLabel else category.ifBlank { if (type == "INCOME") fallbackOtherIncome else fallbackOther }
            onSave(TransactionForm(finalAmount, type, catToSave, note.trim(), safeSource, safeTarget, currency, timestamp, imageUri))
        }
    }

    AlertDialog(
        onDismissRequest = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onDismiss() },
        containerColor = colors.surface,
        title = { Text(if (transactionToEdit == null) katStringResource(id = R.string.tx_title_add, isEng = isEng) else katStringResource(id = R.string.tx_title_edit, isEng = isEng), color = colors.text, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val types = listOf("EXPENSE" to expenseLabel, "INCOME" to incomeLabel, "TRANSFER_OUT" to transferLabel)
                        items(types) { (value, label) ->
                            ChoiceChip(label, type == value, colors) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                type = value
                                category = if (value == "INCOME") fallbackOtherIncome else fallbackOther
                            }
                        }
                    }

                    CurrencyRow(currency, colors) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); currency = it }

                    if (sources.isNotEmpty()) {
                        Text(katStringResource(id = R.string.tx_label_source, isEng = isEng), color = colors.subText, fontSize = 12.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            items(sources) { source ->
                                ChoiceChip(source.name, sourceName == source.name, colors) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); sourceName = source.name
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = type == "TRANSFER_OUT" && sources.size >= 2) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(katStringResource(id = R.string.tx_label_destination, isEng = isEng), color = colors.subText, fontSize = 12.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                items(sources.filter { it.name != sourceName }) { source ->
                                    ChoiceChip(source.name, targetSourceName == source.name, colors) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); targetSourceName = source.name
                                    }
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = type != "TRANSFER_OUT") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(katStringResource(id = R.string.tx_label_category, isEng = isEng), color = colors.subText, fontSize = 12.sp)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                val options = (if (type == "INCOME") incomeOptions else expenseOptions).filterNot { it == fallbackAll || it == "Tất cả" || it == "All" }
                                items(options.ifEmpty { listOf(if (type == "INCOME") fallbackOtherIncome else fallbackOther) }) { option ->
                                    CategoryChip(
                                        label = option, isSelected = category == option, colors = colors,
                                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); category = option },
                                        onLongClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); categoryToDelete = option }
                                    )
                                }
                                item {
                                    CategoryChip(
                                        label = "+ ${katStringResource(id = R.string.btn_create, isEng = isEng)}", isSelected = false, colors = colors,
                                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); showAddCategoryDialog = true },
                                        onLongClick = {}
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(note, { note = it }, label = { Text(katStringResource(id = R.string.tx_label_note, isEng = isEng), color = colors.subText) }, modifier = Modifier.fillMaxWidth(), colors = appTextFieldColors(colors))

                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .clickable { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); showDatePicker = true }.padding(14.dp)) {
                        Text("${katStringResource(id = R.string.tx_label_date, isEng = isEng)} ${formatDateOnly(timestamp)}", color = colors.text, fontWeight = FontWeight.Medium)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButtonText(if (imageUri == null) katStringResource(id = R.string.tx_receipt_attach, isEng = isEng) else katStringResource(id = R.string.tx_receipt_change, isEng = isEng), colors) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); imagePicker.launch(arrayOf("image/*"))
                        }
                        if (imageUri != null) {
                            OutlinedButtonText(katStringResource(id = R.string.tx_receipt_remove, isEng = isEng), colors, true) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); imageUri = null
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 12.dp))

                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    Text(text = formatExpressionForDisplay(amountInput), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = colors.text, maxLines = 1, modifier = Modifier.horizontalScroll(rememberScrollState()))
                    if (amountInput.any { it in listOf('+', '-', '*', '/') }) {
                        val liveRes = calculateExpression(amountInput)
                        val formattedLiveRes = formatExpressionForDisplay(if (liveRes % 1.0 == 0.0) liveRes.toLong().toString() else liveRes.toString())
                        Text(text = "= $formattedLiveRes", fontSize = 18.sp, color = colors.accent, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
                    } else { Spacer(modifier = Modifier.height(10.dp)) }
                }

                CustomNumpad(
                    colors = colors,
                    onKeyClick = { if (amountInput.length < 15) amountInput += it },
                    onDeleteClick = { if (amountInput.isNotEmpty()) amountInput = amountInput.dropLast(1) },
                    onClearClick = { amountInput = "" },
                    onSubmitClick = { handleSave() },
                    onSuggestionClick = { amt ->
                        if (amountInput.length + amt.length <= 15) {
                            amountInput += if (amountInput.isEmpty() || amountInput.last() in listOf('+', '-', '*', '/')) amt else "+$amt"
                        } else {
                            Toast.makeText(context, toastAmountTooLarge, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        },
        confirmButton = {},
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
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onDismiss()
                }
            ) {
                Text(
                    text = katStringResource(id = R.string.btn_close, isEng = isEng),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); showAddCategoryDialog = false; newCategoryName = "" },
            containerColor = colors.surface,
            title = { Text(katStringResource(id = R.string.tx_cat_mgmt_create_title, isEng = isEng), color = colors.text, fontWeight = FontWeight.Bold) },
            text = { OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = { Text(katStringResource(id = R.string.tx_cat_mgmt_create_name, isEng = isEng), color = colors.subText) }, singleLine = true, colors = appTextFieldColors(colors)) },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.background
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (newCategoryName.isNotBlank()) {
                            onCreateCategory(newCategoryName.trim(), type)
                            category = newCategoryName.trim()
                            showAddCategoryDialog = false; newCategoryName = ""
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
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showAddCategoryDialog = false
                        newCategoryName = ""
                    }
                ) {
                    Text(
                        text = katStringResource(id = R.string.btn_close, isEng = isEng),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    if (categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); categoryToDelete = null },
            containerColor = colors.surface,
            title = { Text(katStringResource(id = R.string.tx_cat_mgmt_delete_title, isEng = isEng), color = colors.text, fontWeight = FontWeight.Bold) },
            text = { Text(katStringResource(id = R.string.tx_cat_mgmt_delete_msg, isEng, categoryToDelete!!), color = colors.text) },
            confirmButton = {
                Button(colors = ButtonDefaults.buttonColors(containerColor = Color.Red), onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val toDelete = categoryToDelete!!
                    onDeleteCategory(toDelete, type)
                    if (category == toDelete) category = if (type == "INCOME") fallbackOtherIncome else fallbackOther
                    categoryToDelete = null
                }) { Text(katStringResource(id = R.string.btn_delete, isEng = isEng), color = Color.White, fontWeight = FontWeight.Bold) }
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
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        categoryToDelete = null
                    }
                ) {
                    Text(
                        text = katStringResource(id = R.string.btn_close, isEng = isEng),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    datePickerState.selectedDateMillis?.let { millis ->
                        val currentTime = Calendar.getInstance()
                        timestamp = Calendar.getInstance().apply { timeInMillis = millis; set(Calendar.HOUR_OF_DAY, currentTime.get(Calendar.HOUR_OF_DAY)); set(Calendar.MINUTE, currentTime.get(Calendar.MINUTE)) }.timeInMillis
                    }; showDatePicker = false
                }) { Text(katStringResource(id = R.string.btn_select, isEng = isEng)) }
            },
            dismissButton = { TextButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); showDatePicker = false }) { Text(katStringResource(id = R.string.btn_cancel, isEng = isEng)) } }
        ) { DatePicker(state = datePickerState) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryChip(
    label: String, isSelected: Boolean, colors: BudgetColors,
    onClick: () -> Unit, onLongClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.92f else 1f, label = "scale")

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) colors.accent else Color.Transparent)
            .border(1.dp, if (isSelected) Color.Transparent else colors.border, RoundedCornerShape(8.dp))
            .combinedClickable(interactionSource = interactionSource, indication = null, onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text = label, color = if (isSelected) colors.background else colors.text, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal)
    }
}
