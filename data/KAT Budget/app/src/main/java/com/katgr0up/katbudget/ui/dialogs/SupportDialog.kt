package com.katgr0up.katbudget.ui.dialogs

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.utils.katStringResource

private const val SUPPORT_EMAIL = "trevorthanhtung@gmail.com"

@Composable
fun SupportDialog(
    isEng: Boolean,
    colors: BudgetColors,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val emailSubject = katStringResource(id = R.string.support_email_subject, isEng = isEng)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = katStringResource(id = R.string.support_dialog_title, isEng = isEng),
                color = colors.text,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = katStringResource(id = R.string.support_dialog_message, isEng = isEng),
                    color = colors.subText,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = SUPPORT_EMAIL,
                    color = colors.accent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = "mailto:$SUPPORT_EMAIL".toUri()
                        putExtra(Intent.EXTRA_SUBJECT, emailSubject)
                    }
                    runCatching { context.startActivity(intent) }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.background
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = katStringResource(id = R.string.support_dialog_open_mail, isEng = isEng),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = katStringResource(id = R.string.btn_close, isEng = isEng),
                    color = colors.subText,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}
