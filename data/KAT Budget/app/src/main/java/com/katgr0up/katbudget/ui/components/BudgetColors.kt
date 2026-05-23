package com.katgr0up.katbudget.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Stable
data class BudgetColors(
    val background: Color,
    val backgroundBrush: Brush,
    val card: Color,
    val surface: Color,
    val nav: Color,
    val text: Color,
    val subText: Color,
    val accent: Color,
    val positive: Color,
    val negative: Color,
    val border: Color,
    val warning: Color,
    val info: Color,
    val debt: Color,
    val savings: Color
)

@Composable
fun rememberBudgetColors(isDarkTheme: Boolean): BudgetColors {
    return remember(isDarkTheme) {
        if (isDarkTheme) {
            BudgetColors(
                background = Color(0xFF0D1117),
                backgroundBrush = Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D1117),
                        Color(0xFF0F2A22),
                        Color(0xFF0D1117)
                    )
                ),
                card = Color(0xFF161B22),
                surface = Color(0xFF161B22),
                nav = Color(0xFF161B22),
                text = Color(0xFFF8FAFC),
                subText = Color(0xFF9CA3AF),
                accent = Color(0xFF4ADE80),
                positive = Color(0xFF22C55E),
                negative = Color(0xFFEF4444),
                border = Color.White.copy(alpha = 0.12f),
                warning = Color(0xFFF59E0B),
                info = Color(0xFF8B5CF6),
                debt = Color(0xFFFACC15), // Vàng sáng cho nợ (Dark)
                savings = Color(0xFF38BDF8) // Xanh dương sáng cho tiết kiệm (Dark)
            )
        } else {
            BudgetColors(
                background = Color(0xFFF6F8FA),
                backgroundBrush = Brush.verticalGradient(
                    listOf(
                        Color(0xFFF6F8FA),
                        Color(0xFFEAFBF2),
                        Color(0xFFF6F8FA)
                    )
                ),
                card = Color(0xFFFFFFFF),
                surface = Color(0xFFFFFFFF),
                nav = Color(0xFFFFFFFF),
                text = Color(0xFF111827),
                subText = Color(0xFF667085),
                accent = Color(0xFF16A34A),
                positive = Color(0xFF22C55E),
                negative = Color(0xFFEF4444),
                border = Color(0xFFDDE5E0),
                warning = Color(0xFFF59E0B),
                info = Color(0xFF8B5CF6),
                debt = Color(0xFFEAB308), // Vàng đậm cho nợ (Light)
                savings = Color(0xFF0EA5E9) // Xanh dương cho tiết kiệm (Light)
            )
        }
    }
}