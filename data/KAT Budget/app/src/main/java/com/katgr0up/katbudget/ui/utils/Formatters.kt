package com.katgr0up.katbudget.ui.utils

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

val LocalPrivacyMode = androidx.compose.runtime.compositionLocalOf { false }

// ==========================================
// 1. CONSTANTS & UTILS
// ==========================================
private const val CURRENCY_VND = "VND"
private const val MAX_INTEGER_DIGITS = 15
private const val MAX_DECIMAL_DIGITS = 2

// ==========================================
// 1.5 LOCALIZATION UTILS
// ==========================================
@Composable
fun katStringResource(id: Int, isEng: Boolean, vararg formatArgs: Any): String {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val localizedContext = remember(context, configuration, isEng) {
        val newConfig = android.content.res.Configuration(configuration).apply {
            setLocale(if (isEng) Locale.ENGLISH else Locale.forLanguageTag("vi"))
        }
        context.createConfigurationContext(newConfig)
    }

    return localizedContext.getString(id, *formatArgs)
}

// ==========================================
// 2. CURRENCY & EXCHANGE
// ==========================================
fun normalizeCurrency(currency: String): String {
    return when (currency.trim().uppercase(Locale.ROOT)) {
        "", "VNĐ", "VND" -> CURRENCY_VND
        else -> currency.trim().uppercase(Locale.ROOT)
    }
}

fun getMockRateToVND(currency: String): Double {
    return when (normalizeCurrency(currency)) {
        "USD" -> 25_400.0
        "EUR" -> 27_500.0
        "JPY" -> 165.0
        "KRW" -> 18.5
        "CNY" -> 3_500.0
        else -> 1.0
    }
}

fun convertCurrency(
    amount: Double,
    from: String,
    to: String,
    exchangeRates: Map<String, Double> = emptyMap()
): Double {
    val normFrom = normalizeCurrency(from)
    val normTo = normalizeCurrency(to)
    val rateFrom = exchangeRates[normFrom] ?: getMockRateToVND(normFrom)
    val rateTo = exchangeRates[normTo] ?: getMockRateToVND(normTo)

    if (rateFrom <= 0.0 || rateTo <= 0.0) return amount

    return amount * rateFrom / rateTo
}

// ==========================================
// 3. NUMBER FORMATTING & PARSING
// ==========================================
fun formatCurrency(amount: Double, currency: String): String {
    val normalizedCurrency = normalizeCurrency(currency)
    val formatter = if (normalizedCurrency == CURRENCY_VND || amount % 1.0 == 0.0) {
        integerFormatter()
    } else {
        decimalFormatter()
    }

    val value = formatter.format(amount)
    return if (currency.isBlank()) value else "$value $normalizedCurrency"
}

fun formatCurrencySafe(amount: Double, currency: String, isPrivacyMode: Boolean = false): String {
    if (isPrivacyMode) return "***"
    return formatCurrency(amount, currency)
}

fun formatCompactCurrency(amount: Double, currency: String): String {
    val absAmount = abs(amount)
    val sign = if (amount < 0) "-" else ""
    val normalizedCurrency = normalizeCurrency(currency)
    val formatter = decimalFormatter()

    return when {
        absAmount >= 1_000_000_000 -> {
            val unit = "B"
            "$sign${formatter.format(absAmount / 1_000_000_000)} $unit $normalizedCurrency"
        }

        absAmount >= 1_000_000 -> {
            val unit = "M"
            "$sign${formatter.format(absAmount / 1_000_000)} $unit $normalizedCurrency"
        }

        else -> formatCurrency(amount, normalizedCurrency)
    }
}

fun formatSmartCurrency(amount: Double, currency: String, isEng: Boolean = true, isPrivacyMode: Boolean = false): String {
    if (isPrivacyMode) return "***"
    val absoluteAmount = abs(amount)

    if (absoluteAmount < 1_000_000_000) return formatCurrency(amount, currency)

    val locale = Locale.US
    val (value, suffix) = when {
        absoluteAmount >= 1_000_000_000_000_000 -> {
            val s = if (isEng) "Q" else "Q"
            (absoluteAmount / 1_000_000_000_000_000) to s
        }
        absoluteAmount >= 1_000_000_000_000 -> {
            val s = if (isEng) "T" else "Nghìn Tỷ"
            (absoluteAmount / 1_000_000_000_000) to s
        }
        else -> {
            val s = if (isEng) "B" else "Tỷ"
            (absoluteAmount / 1_000_000_000) to s
        }
    }

    val formattedNumber = String.format(locale, "%,.2f", value).replace(Regex("[,.]00$"), "")
    val prefix = if (amount < 0) "-" else ""

    return "$prefix$formattedNumber $suffix $currency"
}

fun parseMoney(input: String): Double {
    val normalized = normalizeMoneyInput(input)
    return normalized.toDoubleOrNull() ?: 0.0
}

fun formatInputNumber(input: String): String {
    val clean = input
        .trim()
        .replace(" ", "")
        .replace(".", "")

    val parts = clean.split(",", limit = 2)
    val intPart = parts
        .firstOrNull()
        .orEmpty()
        .filter { it.isDigit() }
        .take(MAX_INTEGER_DIGITS)

    val decPart = parts
        .getOrNull(1)
        ?.filter { it.isDigit() }
        ?.take(MAX_DECIMAL_DIGITS)

    val formattedInt = if (intPart.isNotEmpty()) {
        runCatching { integerFormatter().format(intPart.toLong()) }
            .getOrDefault(intPart)
    } else {
        ""
    }

    return if (decPart != null) "$formattedInt,$decPart" else formattedInt
}

fun append000(input: String): String {
    val clean = input
        .trim()
        .replace(" ", "")
        .replace(".", "")

    if (clean.isEmpty() || clean.contains(",") || clean.length > 12) return input

    return formatInputNumber(clean + "000")
}

fun cleanMoneyInputForEditing(input: String): String {
    val normalized = input
        .trim()
        .replace(" ", "")
        .replace(".", "")

    val builder = StringBuilder()
    var hasComma = false

    normalized.forEach { char ->
        when {
            char.isDigit() -> builder.append(char)
            char == ',' && !hasComma -> {
                builder.append(char)
                hasComma = true
            }
        }
    }

    val result = builder.toString()
    if (result == ",") return "0,"
    return result
}

private fun normalizeMoneyInput(input: String): String {
    val clean = input
        .trim()
        .replace(" ", "")

    if (clean.isBlank()) return ""

    val lastComma = clean.lastIndexOf(',')
    val lastDot = clean.lastIndexOf('.')
    val decimalSeparator = when {
        lastComma >= 0 && lastDot >= 0 -> if (lastComma > lastDot) ',' else '.'
        lastComma >= 0 -> {
            val digitsAfterComma = clean.length - lastComma - 1
            if (digitsAfterComma in 1..MAX_DECIMAL_DIGITS) ',' else null
        }
        lastDot >= 0 -> {
            val digitsAfterDot = clean.length - lastDot - 1
            if (digitsAfterDot in 1..MAX_DECIMAL_DIGITS) '.' else null
        }
        else -> null
    }

    val builder = StringBuilder()
    clean.forEachIndexed { index, char ->
        when {
            char.isDigit() -> builder.append(char)
            decimalSeparator != null && char == decimalSeparator && index == clean.lastIndexOf(decimalSeparator) ->
                builder.append('.')
        }
    }

    return builder.toString()
}

// ==========================================
// 4. TEXT TRANSFORMATIONS
// ==========================================
class NumberDotTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val formatted = formatInputNumber(original)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var transformedOffset = 0
                var originalOffset = 0

                while (originalOffset < offset && transformedOffset < formatted.length) {
                    if (formatted[transformedOffset] != '.') {
                        originalOffset++
                    }
                    transformedOffset++
                }

                return transformedOffset.coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                var originalOffset = 0

                for (index in 0 until min(offset, formatted.length)) {
                    if (formatted[index] != '.') {
                        originalOffset++
                    }
                }

                return originalOffset.coerceIn(0, original.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

// ==========================================
// 5. DATE FORMATTERS
// ==========================================
fun formatDate(timestamp: Long): String {
    return dateTimeFormatter().format(Date(timestamp))
}

fun formatDateOnly(timestamp: Long): String {
    return dateOnlyFormatter().format(Date(timestamp))
}

// ==========================================
// 6. IMAGE UTILS
// ==========================================
@Composable
fun loadBitmap(uriString: String?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uriString) {
        bitmap = null

        if (uriString.isNullOrBlank()) return@LaunchedEffect

        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                val uri = uriString.toUri()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    return bitmap
}

// ==========================================
// 7. PRIVATE FORMATTER INSTANCES
// ==========================================
private fun integerFormatter(): DecimalFormat {
    return DecimalFormat("#,##0", decimalSymbols())
}

private fun decimalFormatter(): DecimalFormat {
    return DecimalFormat("#,##0.##", decimalSymbols())
}

private fun decimalSymbols(): DecimalFormatSymbols {
    return DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
}
private fun dateTimeFormatter(): SimpleDateFormat {
    return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
}

private fun dateOnlyFormatter(): SimpleDateFormat {
    return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
}

// ==========================================
// 8. MATH EXPRESSION EVALUATORS
// ==========================================
fun calculateExpression(expr: String): Double {
    if (expr.isBlank()) return 0.0
    try {
        val cleanExpr = expr.replace(",", "").replace(" ", "")
        var currentOp = '+'
        var currentNumber = ""
        var total = 0.0

        val safeExpr = cleanExpr + "="

        for (char in safeExpr) {
            if (char.isDigit() || char == '.') {
                currentNumber += char
            } else if (char in listOf('+', '-', '*', '/', '=')) {
                val num = currentNumber.toDoubleOrNull() ?: 0.0
                when (currentOp) {
                    '+' -> total += num
                    '-' -> total -= num
                    '*' -> total *= num
                    '/' -> if (num != 0.0) total /= num
                }
                currentOp = char
                currentNumber = ""
            }
        }
        return total
    } catch (e: Exception) {
        return 0.0
    }
}

fun formatExpressionForDisplay(expr: String): String {
    if (expr.isEmpty()) return "0"

    val locale = Locale.US
    val regex = Regex("\\d+(\\.\\d+)?")

    var formattedExpr = expr.replace(regex) { matchResult ->
        val numStr = matchResult.value
        val parts = numStr.split(".")
        val value = parts[0].toDoubleOrNull() ?: 0.0

        val formattedInteger = String.format(locale, "%,.0f", value)
        if (parts.size > 1) "$formattedInteger.${parts[1]}" else formattedInteger
    }

    formattedExpr = formattedExpr
        .replace("+", " + ")
        .replace("-", " - ")
        .replace("*", " × ")
        .replace("/", " ÷ ")

    return formattedExpr
}
