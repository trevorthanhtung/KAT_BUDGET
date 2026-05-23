package com.katgr0up.katbudget.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.ui.utils.KatSpringSpec
import com.katgr0up.katbudget.ui.utils.katStringResource

@Composable
fun BudgetTopBar(
    title: String,
    isEng: Boolean,
    colors: BudgetColors,
    showGreeting: Boolean = false,
    onLanguageToggle: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (showGreeting) {
                Text(
                    text = stringResource(id = if (isEng) R.string.greeting_en else R.string.greeting_vi),
                    color = colors.subText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            Text(
                text = title,
                color = colors.text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.97f else 1f,
            animationSpec = KatSpringSpec,
            label = "lang_toggle_spring"
        )
        val shape = RoundedCornerShape(20.dp)

        Box(
            modifier = Modifier
                .scale(scale)
                .clip(shape)
                .background(colors.card.copy(alpha = 0.72f))
                .border(1.dp, colors.border.copy(alpha = 0.55f), shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onLanguageToggle()
                    }
                )
                .padding(horizontal = 14.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isEng) "EN" else "VI",
                color = colors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun BudgetBottomBar(
    selectedTab: Int,
    activeToolScreen: String,
    isEng: Boolean,
    colors: BudgetColors,
    hasSources: Boolean,
    onTabSelected: (Int) -> Unit,
    onMissingSources: () -> Unit,
    onAdd: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val barShape = RoundedCornerShape(30.dp)

    BottomAppBar(
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = barShape,
                        ambientColor = Color.Black.copy(alpha = 0.06f),
                        spotColor = Color.Black.copy(alpha = 0.14f),
                        clip = false
                    )
                    .clip(barShape)
                    .background(colors.nav.copy(alpha = 0.88f))
                    .border(1.dp, colors.border.copy(alpha = 0.55f), barShape)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BottomNavItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    label = katStringResource(id = R.string.nav_overview, isEng = isEng),
                    selected = selectedTab == 0 && activeToolScreen == "NONE",
                    colors = colors,
                    modifier = Modifier.weight(1f)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(0)
                }

                BottomNavItem(
                    icon = Icons.Default.Group,
                    label = katStringResource(id = R.string.nav_debts, isEng = isEng),
                    selected = selectedTab == 1 && activeToolScreen == "NONE",
                    colors = colors,
                    modifier = Modifier.weight(1f)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(1)
                }

                BudgetFab(
                    colors = colors,
                    modifier = Modifier.weight(1f)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (!hasSources) {
                        onMissingSources()
                    } else {
                        onAdd()
                    }
                }

                BottomNavItem(
                    icon = Icons.Default.PieChart,
                    label = katStringResource(id = R.string.nav_reports, isEng = isEng),
                    selected = selectedTab == 2 && activeToolScreen == "NONE",
                    colors = colors,
                    modifier = Modifier.weight(1f)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(2)
                }

                BottomNavItem(
                    icon = Icons.Default.Settings,
                    label = katStringResource(id = R.string.nav_settings, isEng = isEng),
                    selected = selectedTab == 3 || activeToolScreen != "NONE",
                    colors = colors,
                    modifier = Modifier.weight(1f)
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onTabSelected(3)
                }
            }
        }
    }
}

@Composable
private fun BudgetFab(
    colors: BudgetColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = KatSpringSpec,
        label = "fab_press_spring"
    )

    val pulse = rememberInfiniteTransition(label = "fab_pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_pulse_scale"
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.07f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fab_pulse_alpha"
    )

    Box(
        modifier = modifier.height(68.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(66.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = pulseAlpha))
        )

        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(pressScale)
                .shadow(
                    elevation = 8.dp,
                    shape = CircleShape,
                    ambientColor = colors.accent.copy(alpha = 0.14f),
                    spotColor = colors.accent.copy(alpha = 0.22f),
                    clip = false
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.accent.copy(alpha = 0.24f),
                            colors.accent.copy(alpha = 0.11f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = colors.accent.copy(alpha = 0.44f),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(id = R.string.btn_add),
                tint = colors.accent,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    colors: BudgetColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = KatSpringSpec,
        label = "nav_item_spring"
    )

    val tint = if (selected) colors.accent else colors.subText
    val itemShape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .height(58.dp)
            .scale(scale)
            .clip(itemShape)
            .background(if (selected) colors.accent.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
