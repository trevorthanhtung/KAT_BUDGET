package com.katgr0up.katbudget.ui.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.SourceEntity
import com.katgr0up.katbudget.ui.components.ChoiceChip
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.utils.katStringResource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportCsvDialog(
    isEng: Boolean,
    colors: BudgetColors,
    sources: List<SourceEntity>,
    onDismiss: () -> Unit,
    onConfirm: (timeRange: String, source: String, type: String) -> Unit
) {
    val fallbackAll = katStringResource(id = R.string.fallback_all, isEng = isEng)
    var showDatePicker by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()

    var selectedSource by remember(fallbackAll) { mutableStateOf(fallbackAll) }
    var selectedType by remember { mutableStateOf("ALL") }

    val formatter = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    val startDateMillis = dateRangePickerState.selectedStartDateMillis
    val endDateMillis = dateRangePickerState.selectedEndDateMillis

    val selectedDateText = when {
        startDateMillis != null && endDateMillis != null -> {
            val start = formatter.format(Date(startDateMillis))
            val end = formatter.format(Date(endDateMillis))
            "$start - $end"
        }
        startDateMillis != null -> formatter.format(Date(startDateMillis))
        else -> katStringResource(id = R.string.csv_fallback_date_range, isEng = isEng)
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            shape = RoundedCornerShape(28.dp),
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(
                        text = katStringResource(id = R.string.btn_done, isEng = isEng),
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
            },
            colors = DatePickerDefaults.colors(
                containerColor = colors.surface
            )
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = {
                    Text(
                        text = katStringResource(id = R.string.csv_title_range, isEng = isEng),
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                        color = colors.text,
                        fontWeight = FontWeight.Bold
                    )
                },
                headline = {
                    DateRangePickerDefaults.DateRangePickerHeadline(
                        selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis,
                        selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis,
                        displayMode = dateRangePickerState.displayMode,
                        dateFormatter = DatePickerDefaults.dateFormatter(),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                },
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    dayContentColor = colors.text,
                    selectedDayContainerColor = colors.accent,
                    selectedDayContentColor = colors.background,
                    todayDateBorderColor = colors.accent,
                    todayContentColor = colors.accent
                )
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = katStringResource(id = R.string.csv_title_options, isEng = isEng),
                color = colors.text,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExportSectionLabel(
                    text = katStringResource(id = R.string.csv_label_time, isEng = isEng),
                    colors = colors
                )

                DateRangeSelector(
                    text = selectedDateText,
                    colors = colors,
                    onClick = { showDatePicker = true }
                )

                ExportSectionLabel(
                    text = katStringResource(id = R.string.csv_label_account, isEng = isEng),
                    colors = colors
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 1.dp)
                ) {
                    item {
                        ChoiceChip(
                            text = fallbackAll,
                            selected = selectedSource == fallbackAll,
                            colors = colors,
                            onClick = { selectedSource = fallbackAll }
                        )
                    }

                    items(sources, key = { it.id }) { source ->
                        ChoiceChip(
                            text = source.name,
                            selected = selectedSource == source.name,
                            colors = colors,
                            onClick = { selectedSource = source.name }
                        )
                    }
                }

                ExportSectionLabel(
                    text = katStringResource(id = R.string.csv_label_type, isEng = isEng),
                    colors = colors
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 1.dp)
                ) {
                    item {
                        ChoiceChip(
                            text = fallbackAll,
                            selected = selectedType == "ALL",
                            colors = colors,
                            onClick = { selectedType = "ALL" }
                        )
                    }
                    item {
                        ChoiceChip(
                            text = katStringResource(id = R.string.csv_chip_expense, isEng = isEng),
                            selected = selectedType == "EXPENSE",
                            colors = colors,
                            onClick = { selectedType = "EXPENSE" }
                        )
                    }
                    item {
                        ChoiceChip(
                            text = katStringResource(id = R.string.csv_chip_income, isEng = isEng),
                            selected = selectedType == "INCOME",
                            colors = colors,
                            onClick = { selectedType = "INCOME" }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.background
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(48.dp),
                onClick = {
                    val finalStartMillis = dateRangePickerState.selectedStartDateMillis

                    if (finalStartMillis == null) {
                        onConfirm("ALL", selectedSource, selectedType)
                    } else {
                        val finalEndMillis = dateRangePickerState.selectedEndDateMillis ?: finalStartMillis
                        onConfirm("CUSTOM:$finalStartMillis:$finalEndMillis", selectedSource, selectedType)
                    }
                }
            ) {
                Text(
                    text = katStringResource(id = R.string.btn_export, isEng = isEng),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = katStringResource(id = R.string.btn_cancel, isEng = isEng),
                    color = colors.subText,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
private fun DateRangeSelector(
    text: String,
    colors: BudgetColors,
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
            .padding(horizontal = 14.dp, vertical = 15.dp)
    ) {
        Text(
            text = text,
            color = colors.text,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ExportSectionLabel(
    text: String,
    colors: BudgetColors
) {
    Text(
        text = text,
        color = colors.subText,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
    )
}