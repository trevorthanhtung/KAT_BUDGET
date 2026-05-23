package com.katgr0up.katbudget.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.katgr0up.katbudget.ui.utils.KatSpringSpec

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = 24.dp,
    accentColor: Color = Color(0xFF4ADE80),
    surfaceColor: Color = Color(0xFF102117).copy(alpha = 0.52f),
    highlightColor: Color = Color(0xFFF0FFF4),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = KatSpringSpec,
        label = "liquidGlassCardScale"
    )

    val shape = RoundedCornerShape(cornerRadius)

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.10f),
                clip = false
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.68f),
                        surfaceColor.copy(alpha = 0.54f)
                    )
                )
            )
            .drawWithContent {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(size.width, 0f),
                        radius = size.width / 1.45f
                    ),
                    center = Offset(size.width, 0f)
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            highlightColor.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        center = Offset(0f, size.height),
                        radius = size.width * 0.95f
                    ),
                    center = Offset(0f, size.height)
                )

                drawContent()

                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            highlightColor.copy(alpha = 0.20f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, 1f),
                    end = Offset(size.width, 1f),
                    strokeWidth = 1.6f
                )

                drawLine(
                    color = highlightColor.copy(alpha = 0.035f),
                    start = Offset(0f, size.height - 1f),
                    end = Offset(size.width, size.height - 1f),
                    strokeWidth = 1.2f
                )
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        highlightColor.copy(alpha = 0.14f),
                        highlightColor.copy(alpha = 0.06f),
                        Color.Transparent
                    )
                ),
                shape = shape
            )
            .then(clickModifier)
            .padding(20.dp)
    ) {
        content()
    }
}