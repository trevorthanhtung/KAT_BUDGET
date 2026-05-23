package com.katgr0up.katbudget.ui.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.components.appTextFieldColors
import com.katgr0up.katbudget.ui.utils.katStringResource

@Composable
fun PinSetupDialog(
    isEng: Boolean,
    colors: BudgetColors,
    pinValue: String,
    onPinChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = katStringResource(id = R.string.pin_title_setup, isEng = isEng),
                color = colors.text,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = pinValue,
                onValueChange = onPinChanged,
                label = {
                    Text(
                        text = katStringResource(id = R.string.pin_hint_setup, isEng = isEng),
                        color = colors.subText
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                colors = appTextFieldColors(colors),
                shape = RoundedCornerShape(20.dp)
            )
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.background,
                    disabledContainerColor = colors.accent.copy(alpha = 0.24f),
                    disabledContentColor = colors.background.copy(alpha = 0.55f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = onSave,
                enabled = pinValue.length == 4
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