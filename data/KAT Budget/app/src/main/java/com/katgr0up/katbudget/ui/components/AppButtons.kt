package com.katgr0up.katbudget.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.katgr0up.katbudget.ui.utils.KatSpringSpec
import com.katgr0up.katbudget.ui.utils.normalizeCurrency
import com.katgr0up.katbudget.utils.ALL_CURRENCIES

@Composable
fun ChoiceChip(
    text: String,
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
        label = "choice_chip_scale"
    )
    val chipShape = RoundedCornerShape(20.dp)

    FilterChip(
        selected = selected,
        onClick = onClick,
        interactionSource = interactionSource,
        label = {
            Text(
                text = text,
                color = if (selected) colors.accent else colors.text,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.card.copy(alpha = 0.78f),
            labelColor = colors.text,
            selectedContainerColor = colors.accent.copy(alpha = 0.13f),
            selectedLabelColor = colors.accent
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.border.copy(alpha = 0.50f),
            selectedBorderColor = colors.accent.copy(alpha = 0.50f)
        ),
        shape = chipShape,
        modifier = modifier
            .scale(scale)
            .height(38.dp)
    )
}

@Composable
fun CurrencyRow(
    selectedCurrency: String,
    colors: BudgetColors,
    onCurrencyChanged: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(
            items = ALL_CURRENCIES,
            key = { it }
        ) { currency ->
            ChoiceChip(
                text = currency,
                selected = normalizeCurrency(selectedCurrency) == currency,
                colors = colors,
                onClick = { onCurrencyChanged(currency) }
            )
        }
    }
}

@Composable
fun OutlinedButtonText(
    text: String,
    colors: BudgetColors,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = KatSpringSpec,
        label = "outlined_button_text_scale"
    )

    val contentColor = if (danger) colors.negative else colors.accent
    val buttonShape = RoundedCornerShape(20.dp)

    TextButton(
        onClick = onClick,
        interactionSource = interactionSource,
        border = BorderStroke(
            width = 1.dp,
            color = contentColor.copy(alpha = if (danger) 0.72f else 0.48f)
        ),
        shape = buttonShape,
        colors = ButtonDefaults.textButtonColors(
            containerColor = contentColor.copy(alpha = 0.08f),
            contentColor = contentColor
        ),
        modifier = Modifier
            .scale(scale)
            .height(46.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
