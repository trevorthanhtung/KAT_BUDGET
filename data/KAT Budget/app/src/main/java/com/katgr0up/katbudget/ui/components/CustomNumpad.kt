package com.katgr0up.katbudget.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.ui.utils.KatSpringSpec
import com.katgr0up.katbudget.ui.utils.formatInputNumber

private val suggestedAmounts = listOf("37000", "10000", "3000", "50000", "100000", "200000", "500000")
private val keyShape = RoundedCornerShape(18.dp)

@Composable
fun CustomNumpad(
    colors: BudgetColors,
    onKeyClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onSuggestionClick: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val buttonHeight = 46.dp
    val spacing = 7.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(suggestedAmounts, key = { it }) { amount ->
                SuggestionChip(
                    amount = amount,
                    colors = colors,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSuggestionClick(amount)
                    }
                )
            }
        }

        NumpadRow(spacing = spacing) {
            NumpadButton("C", colors, isOperator = true, modifier = Modifier.weight(1f).height(buttonHeight), onClick = onClearClick)
            NumpadButton("÷", colors, isOperator = true, modifier = Modifier.weight(1f).height(buttonHeight)) { onKeyClick("/") }
            NumpadButton("×", colors, isOperator = true, modifier = Modifier.weight(1f).height(buttonHeight)) { onKeyClick("*") }
            NumpadIconButton(
                colors = colors,
                modifier = Modifier.weight(1f).height(buttonHeight),
                onClick = onDeleteClick
            )
        }

        NumpadRow(spacing = spacing) {
            listOf("7", "8", "9").forEach { key ->
                NumpadButton(key, colors, modifier = Modifier.weight(1f).height(buttonHeight)) { onKeyClick(key) }
            }
            NumpadButton("-", colors, isOperator = true, modifier = Modifier.weight(1f).height(buttonHeight)) { onKeyClick("-") }
        }

        NumpadRow(spacing = spacing) {
            listOf("4", "5", "6").forEach { key ->
                NumpadButton(key, colors, modifier = Modifier.weight(1f).height(buttonHeight)) { onKeyClick(key) }
            }
            NumpadButton("+", colors, isOperator = true, modifier = Modifier.weight(1f).height(buttonHeight)) { onKeyClick("+") }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Column(
                modifier = Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                NumpadRow(spacing = spacing) {
                    listOf("1", "2", "3").forEach { key ->
                        NumpadButton(key, colors, modifier = Modifier.weight(1f).height(buttonHeight)) { onKeyClick(key) }
                    }
                }
                NumpadRow(spacing = spacing) {
                    listOf("0", "000", ".").forEach { key ->
                        NumpadButton(key, colors, modifier = Modifier.weight(1f).height(buttonHeight)) { onKeyClick(key) }
                    }
                }
            }

            SubmitButton(
                colors = colors,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onClick = onSubmitClick
            )
        }
    }
}

@Composable
private fun SuggestionChip(
    amount: String,
    colors: BudgetColors,
    onClick: () -> Unit
) {
    PressableKeyContainer(
        colors = colors,
        modifier = Modifier,
        shape = RoundedCornerShape(999.dp),
        backgroundColor = colors.card.copy(alpha = 0.52f),
        borderColor = colors.border.copy(alpha = 0.46f),
        onClick = onClick
    ) {
        Text(
            text = formatInputNumber(amount),
            color = colors.subText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun NumpadRow(
    spacing: Dp,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
private fun NumpadButton(
    text: String,
    colors: BudgetColors,
    modifier: Modifier = Modifier,
    isOperator: Boolean = false,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    PressableKeyContainer(
        colors = colors,
        modifier = modifier,
        shape = keyShape,
        backgroundColor = if (isOperator) colors.accent.copy(alpha = 0.10f) else colors.card.copy(alpha = 0.50f),
        borderColor = if (isOperator) colors.accent.copy(alpha = 0.24f) else colors.border.copy(alpha = 0.46f),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    ) {
        Text(
            text = text,
            color = if (isOperator) colors.accent else colors.text,
            fontSize = if (text == "000") 16.sp else 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun NumpadIconButton(
    colors: BudgetColors,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    PressableKeyContainer(
        colors = colors,
        modifier = modifier,
        shape = keyShape,
        backgroundColor = colors.accent.copy(alpha = 0.10f),
        borderColor = colors.accent.copy(alpha = 0.24f),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = stringResource(id = R.string.accessibility_backspace),
            tint = colors.accent,
            modifier = Modifier.size(23.dp)
        )
    }
}

@Composable
private fun SubmitButton(
    colors: BudgetColors,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    PressableKeyContainer(
        colors = colors,
        modifier = modifier,
        shape = keyShape,
        backgroundColor = colors.accent,
        borderColor = colors.accent.copy(alpha = 0.42f),
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    ) {
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = stringResource(id = R.string.accessibility_submit),
            tint = colors.background,
            modifier = Modifier.size(27.dp)
        )
    }
}

@Composable
private fun PressableKeyContainer(
    colors: BudgetColors,
    modifier: Modifier,
    shape: RoundedCornerShape,
    backgroundColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = KatSpringSpec,
        label = "numpad_key_press"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}
