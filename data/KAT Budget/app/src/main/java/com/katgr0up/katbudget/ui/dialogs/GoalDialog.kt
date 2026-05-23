package com.katgr0up.katbudget.ui.dialogs

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.SavingGoalEntity
import com.katgr0up.katbudget.ui.components.CurrencyRow
import com.katgr0up.katbudget.ui.components.MoneyInputRow
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.components.appTextFieldColors
import com.katgr0up.katbudget.ui.utils.formatInputNumber
import com.katgr0up.katbudget.ui.utils.katStringResource
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.ui.utils.parseMoney

@Composable
fun GoalDialog(
    goalToEdit: SavingGoalEntity?,
    isEng: Boolean,
    colors: BudgetColors,
    defaultCurrency: String,
    onDismiss: () -> Unit,
    onSave: (name: String, target: Double, curr: String) -> Unit
) {
    val context = LocalContext.current

    var name by remember(goalToEdit) { mutableStateOf(goalToEdit?.name ?: "") }
    var targetInput by remember(goalToEdit) {
        mutableStateOf(
            goalToEdit?.targetAmount?.let {
                if (it % 1.0 == 0.0) {
                    formatInputNumber(it.toLong().toString())
                } else {
                    formatInputNumber(it.toString().replace(".", ","))
                }
            }.orEmpty()
        )
    }
    var currency by remember(goalToEdit) {
        mutableStateOf(goalToEdit?.currency ?: normalizeCurrency(defaultCurrency))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = if (goalToEdit == null) {
                    katStringResource(id = R.string.goal_dialog_title_add, isEng = isEng)
                } else {
                    katStringResource(id = R.string.goal_dialog_title_edit, isEng = isEng)
                },
                color = colors.text,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text(
                            text = katStringResource(id = R.string.goal_label_name, isEng = isEng),
                            color = colors.subText
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = appTextFieldColors(colors),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp)
                )

                MoneyInputRow(
                    value = targetInput,
                    onValueChange = { targetInput = it },
                    label = katStringResource(id = R.string.goal_label_target, isEng = isEng),
                    colors = colors
                )

                CurrencyRow(
                    selectedCurrency = currency,
                    colors = colors,
                    onCurrencyChanged = { currency = it }
                )
            }
        },
        confirmButton = {
            val toastWarningMsg = katStringResource(id = R.string.toast_goal_fill_all_info, isEng = isEng)

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
                    val target = parseMoney(targetInput)
                    if (name.isBlank() || target <= 0.0) {
                        Toast.makeText(context, toastWarningMsg, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onSave(name.trim(), target, currency)
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