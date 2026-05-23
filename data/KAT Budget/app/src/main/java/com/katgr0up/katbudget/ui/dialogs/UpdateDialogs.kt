package com.katgr0up.katbudget.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.utils.katStringResource

@Composable
fun UpdateLoadingDialog(
    isEng: Boolean,
    colors: BudgetColors
) {
    AlertDialog(
        onDismissRequest = {},
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        confirmButton = {},
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = colors.accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = katStringResource(id = R.string.settings_update_checking, isEng = isEng),
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
fun UpdateAvailableDialog(
    isEng: Boolean,
    colors: BudgetColors,
    versionName: String,
    releaseNotes: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = katStringResource(id = R.string.settings_update_available_title, isEng = isEng),
                color = colors.text,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = versionName,
                    color = colors.accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = releaseNotes,
                    color = colors.subText,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
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
                onClick = onConfirm
            ) {
                Text(
                    text = katStringResource(id = R.string.settings_update_btn_action, isEng = isEng),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = katStringResource(id = R.string.btn_later, isEng = isEng),
                    color = colors.subText,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
fun SettingsUpdateNoticeDialog(
    isEng: Boolean,
    colors: BudgetColors,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val title = if (isError) {
        katStringResource(id = R.string.settings_notice_title_unable, isEng = isEng)
    } else {
        katStringResource(id = R.string.settings_notice_title_latest, isEng = isEng)
    }
    val message = if (isError) {
        katStringResource(id = R.string.settings_notice_msg_error, isEng = isEng)
    } else {
        katStringResource(id = R.string.settings_notice_msg_latest, isEng = isEng)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = title,
                color = if (isError) colors.negative else colors.text,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = message,
                color = colors.subText,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.background
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(48.dp),
                onClick = onDismiss
            ) {
                Text(
                    text = katStringResource(id = R.string.btn_close, isEng = isEng),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
