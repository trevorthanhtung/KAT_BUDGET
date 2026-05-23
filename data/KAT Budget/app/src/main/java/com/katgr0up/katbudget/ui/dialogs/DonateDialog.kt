package com.katgr0up.katbudget.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.utils.KatSpringSpec
import com.katgr0up.katbudget.ui.utils.katStringResource

@Composable
fun DonateDialog(
    isEng: Boolean,
    colors: BudgetColors,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val accountNumber = "0816158215"
    val toastMsg = katStringResource(id = R.string.toast_account_copied, isEng = isEng)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = katStringResource(id = R.string.donate_title, isEng = isEng),
                color = colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = katStringResource(id = R.string.donate_description, isEng = isEng),
                    color = colors.subText,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                DonateQrCard(
                    isEng = isEng,
                    colors = colors
                )

                DonateAccountCard(
                    isEng = isEng,
                    colors = colors,
                    accountNumber = accountNumber,
                    onCopy = {
                        clipboardManager.setPrimaryClip(
                            ClipData.newPlainText("KAT Budget account number", accountNumber)
                        )
                        Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                    }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = onDismiss
            ) {
                Text(
                    text = katStringResource(id = R.string.btn_close, isEng = isEng),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    )
}

@Composable
private fun DonateQrCard(
    isEng: Boolean,
    colors: BudgetColors
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.86f)
            .widthIn(max = 260.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = colors.border.copy(alpha = 0.45f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.donates),
            contentDescription = katStringResource(id = R.string.donate_qr_label, isEng = isEng),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun DonateAccountCard(
    isEng: Boolean,
    colors: BudgetColors,
    accountNumber: String,
    onCopy: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = KatSpringSpec,
        label = "donate_account_press"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.card.copy(alpha = 0.72f))
            .border(
                width = 1.dp,
                color = colors.border.copy(alpha = 0.58f),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onCopy
            )
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = katStringResource(id = R.string.donate_bank_name, isEng = isEng),
            color = colors.subText,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = accountNumber,
            color = colors.accent,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center
        )

        Text(
            text = katStringResource(id = R.string.donate_account_name, isEng = isEng),
            color = colors.text.copy(alpha = 0.84f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
