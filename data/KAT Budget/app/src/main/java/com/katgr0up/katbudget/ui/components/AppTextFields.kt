package com.katgr0up.katbudget.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.katgr0up.katbudget.ui.utils.KatSpringSpec
import com.katgr0up.katbudget.ui.utils.NumberDotTransformation
import com.katgr0up.katbudget.ui.utils.append000
import com.katgr0up.katbudget.ui.utils.cleanMoneyInputForEditing

@Composable
fun MoneyInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    colors: BudgetColors
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = KatSpringSpec,
        label = "append000_button_scale"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(cleanMoneyInputForEditing(it)) },
            label = {
                Text(
                    text = label,
                    color = colors.subText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            visualTransformation = remember(value) { NumberDotTransformation() },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            colors = appTextFieldColors(colors)
        )

        Button(
            onClick = { onValueChange(append000(value)) },
            interactionSource = interactionSource,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accent.copy(alpha = 0.12f),
                contentColor = colors.accent
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            modifier = Modifier
                .scale(scale)
                .height(56.dp)
        ) {
            Text(
                text = "+000",
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Stable
@Composable
fun appTextFieldColors(colors: BudgetColors) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = colors.accent.copy(alpha = 0.82f),
    unfocusedBorderColor = colors.border.copy(alpha = 0.58f),
    focusedTextColor = colors.text,
    unfocusedTextColor = colors.text,
    cursorColor = colors.accent,
    focusedLabelColor = colors.accent,
    unfocusedLabelColor = colors.subText,
    focusedContainerColor = colors.card.copy(alpha = 0.72f),
    unfocusedContainerColor = colors.card.copy(alpha = 0.56f),
    disabledTextColor = colors.text.copy(alpha = 0.55f),
    disabledBorderColor = colors.border.copy(alpha = 0.36f),
    disabledLabelColor = colors.subText.copy(alpha = 0.58f),
    disabledContainerColor = colors.card.copy(alpha = 0.38f),
    errorBorderColor = colors.negative,
    errorCursorColor = colors.negative,
    errorLabelColor = colors.negative
)