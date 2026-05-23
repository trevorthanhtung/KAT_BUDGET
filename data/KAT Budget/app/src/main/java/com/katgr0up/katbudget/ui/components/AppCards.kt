package com.katgr0up.katbudget.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.katgr0up.katbudget.ui.utils.KatSpringSpec

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    colors: BudgetColors,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = remember { RoundedCornerShape(16.dp) }

    val scale by animateFloatAsState(
        targetValue = if (onClick != null && isPressed) 0.98f else 1f,
        animationSpec = KatSpringSpec,
        label = "appCardScale"
    )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.card),
        shape = shape,
        modifier = modifier
            .scale(scale)
            .border(1.dp, colors.border.copy(alpha = 0.72f), shape)
            .then(clickableModifier)
    ) {
        Column(content = content)
    }
}