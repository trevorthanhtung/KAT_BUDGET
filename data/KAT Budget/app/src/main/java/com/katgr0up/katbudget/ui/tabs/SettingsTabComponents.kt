package com.katgr0up.katbudget.ui.tabs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.ui.components.AppCard
import com.katgr0up.katbudget.ui.components.BudgetColors
import com.katgr0up.katbudget.ui.utils.KatSpringSpec

@Composable
internal fun SettingsSectionTitle(
    title: String,
    colors: BudgetColors
) {
    Text(
        text = title,
        color = colors.subText,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}

@Composable
internal fun SettingsGroup(
    colors: BudgetColors,
    content: @Composable ColumnScope.() -> Unit
) {
    AppCard(
        colors = colors,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
internal fun SettingsRow(
    title: String,
    subtitle: String,
    colors: BudgetColors,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed) 0.97f else 1f,
        animationSpec = KatSpringSpec,
        label = "settingsRowScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) colors.text else colors.subText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                color = colors.subText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
internal fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    colors: BudgetColors,
    onRowClick: (() -> Unit)?,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (enabled && onRowClick != null && isPressed) 0.97f else 1f,
        animationSpec = KatSpringSpec,
        label = "settingsSwitchRowScale"
    )

    val rowClickModifier = if (enabled && onRowClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onRowClick
        )
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(rowScale)
            .then(rowClickModifier)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                color = if (enabled) colors.text else colors.subText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                color = colors.subText,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.subText,
                uncheckedTrackColor = colors.card.copy(alpha = 0.72f),
                uncheckedBorderColor = colors.border.copy(alpha = 0.72f),
                disabledCheckedThumbColor = colors.subText,
                disabledCheckedTrackColor = colors.card.copy(alpha = 0.72f),
                disabledUncheckedThumbColor = colors.subText.copy(alpha = 0.72f),
                disabledUncheckedTrackColor = colors.card.copy(alpha = 0.48f)
            )
        )
    }
}

@Composable
internal fun SettingsDivider(colors: BudgetColors) {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 18.dp),
        color = colors.border.copy(alpha = 0.42f)
    )
}
