package com.katgr0up.katbudget.ui.theme

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_bold, FontWeight.Bold)
)

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold)
)

val AppTypography = Typography(
    displayMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PlusJakartaSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF34D399),
    onPrimary = Color(0xFF062015),
    primaryContainer = Color(0xFF123B2A),
    onPrimaryContainer = Color(0xFFD5F8E6),
    secondary = Color(0xFF8DD8B1),
    onSecondary = Color(0xFF092015),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF061923),
    background = Color(0xFF0B0F0E),
    onBackground = Color(0xFFF4F7F5),
    surface = Color(0xFF151C19),
    onSurface = Color(0xFFF4F7F5),
    surfaceVariant = Color(0xFF1B2420),
    onSurfaceVariant = Color(0xFFA8B3AD),
    outline = Color(0xFF2B3732),
    error = Color(0xFFF43F5E),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF16A34A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF7E7),
    onPrimaryContainer = Color(0xFF052E16),
    secondary = Color(0xFF4F9F72),
    onSecondary = Color.White,
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = Color(0xFFF7FAF8),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFFAFCFB),
    onSurfaceVariant = Color(0xFF5B6670),
    outline = Color(0xFFD9E2DD),
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun KatBudgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = rememberColorScheme(
        context = context,
        darkTheme = darkTheme,
        dynamicColor = dynamicColor
    )

    ConfigureSystemBars(darkTheme = darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

@Composable
private fun rememberColorScheme(
    context: Context,
    darkTheme: Boolean,
    dynamicColor: Boolean
): ColorScheme {
    return when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
}

@Composable
private fun ConfigureSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    val context = LocalContext.current

    if (view.isInEditMode) return

    SideEffect {
        val activity = context.findActivity() ?: return@SideEffect
        val transparent = Color.Transparent.toArgb()
        val systemBarStyle = if (darkTheme) {
            SystemBarStyle.dark(transparent)
        } else {
            SystemBarStyle.light(transparent, transparent)
        }

        activity.enableEdgeToEdge(
            statusBarStyle = systemBarStyle,
            navigationBarStyle = systemBarStyle
        )
    }
}

private tailrec fun Context.findActivity(): ComponentActivity? {
    return when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
