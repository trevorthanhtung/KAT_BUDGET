package com.katgr0up.katbudget.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.utils.katStringResource

@Composable
fun AboutDialog(
    isEng: Boolean,
    colors: BudgetColors,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val packageInfo = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }.getOrNull()
    val versionName = packageInfo?.versionName ?: "1.0.3"

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = katStringResource(id = R.string.about_dialog_title, isEng = isEng),
                    color = colors.text,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(
                            width = 1.dp,
                            color = colors.border.copy(alpha = 0.42f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = katStringResource(id = R.string.accessibility_app_logo, isEng = isEng),
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = katStringResource(id = R.string.app_name, isEng = isEng),
                    color = colors.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = katStringResource(id = R.string.about_version, isEng = isEng, versionName),
                    color = colors.subText,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = katStringResource(id = R.string.about_slogan, isEng = isEng),
                    color = colors.text,
                    fontSize = 15.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = katStringResource(id = R.string.about_developer, isEng = isEng),
                    color = colors.subText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            SettingsDialogPrimaryButton(
                text = katStringResource(id = R.string.btn_close, isEng = isEng),
                colors = colors,
                onClick = onDismiss
            )
        }
    )
}
