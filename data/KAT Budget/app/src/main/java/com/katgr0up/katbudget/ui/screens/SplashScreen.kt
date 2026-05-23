package com.katgr0up.katbudget.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.ui.components.BudgetColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    colors: BudgetColors,
    onTimeout: () -> Unit
) {
    var targetAlpha by remember { mutableFloatStateOf(0f) }
    val progress by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "splashProgress"
    )

    LaunchedEffect(Unit) {
        targetAlpha = 1f
        delay(1600)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundBrush)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        colors.accent.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    radius = 760f
                )
            )
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = progress
                scaleX = 0.96f + 0.04f * progress
                scaleY = 0.96f + 0.04f * progress
                translationY = 18f * (1f - progress)
            }
        ) {
            SplashLogoOrb(
                colors = colors,
                modifier = Modifier.size(142.dp)
            )

            Spacer(Modifier.size(22.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                color = colors.text,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(id = R.string.splash_slogan),
                color = colors.subText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Text(
            text = stringResource(id = R.string.splash_credits),
            color = colors.subText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .graphicsLayer {
                    alpha = progress
                    translationY = 14f * (1f - progress)
                }
        )
    }
}

@Composable
private fun SplashLogoOrb(
    colors: BudgetColors,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splashLogoMotion")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnimation"
    )
    val floatY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatAnimation"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .blur(26.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colors.accent.copy(alpha = 0.26f),
                            colors.accent.copy(alpha = 0.10f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize(0.76f)
                .graphicsLayer {
                    translationY = floatY
                    scaleX = 0.99f + (pulse - 1f) * 0.28f
                    scaleY = 0.99f + (pulse - 1f) * 0.28f
                }
                .clip(RoundedCornerShape(28.dp))
                .background(colors.card.copy(alpha = 0.88f))
                .border(
                    width = 1.dp,
                    color = colors.accent.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = stringResource(id = R.string.splash_accessibility_logo),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(22.dp))
            )
        }
    }
}
