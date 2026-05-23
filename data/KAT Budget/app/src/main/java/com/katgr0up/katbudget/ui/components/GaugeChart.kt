package com.katgr0up.katbudget.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.ui.utils.formatCompactCurrency
import com.katgr0up.katbudget.ui.utils.formatCurrency
import com.katgr0up.katbudget.ui.utils.katStringResource
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.hypot

@Composable
fun GaugeChartAndTopList(
    isEng: Boolean,
    dataByCategory: Map<String, Double>,
    centerLabel: String,
    isExpense: Boolean,
    currency: String,
    textColor: Color,
    subTextColor: Color,
    accentColor: Color,
    primaryBrandColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    onCategoryClick: (String) -> Unit = {}
) {
    val total = dataByCategory.values.sum()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var isAnimated by remember(dataByCategory) { mutableStateOf(false) }

    val animationProgress by animateFloatAsState(
        targetValue = if (isAnimated) 1f else 0f,
        animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing),
        label = "gauge_animation"
    )

    LaunchedEffect(dataByCategory) {
        isAnimated = true
    }

    val segmentColors = remember(isExpense) {
        if (isExpense) {
            listOf(
                Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF8B5CF6),
                Color(0xFFF97316), Color(0xFFEC4899), Color(0xFFA855F7),
                Color(0xFFDC2626), Color(0xFFEAB308), Color(0xFFFB7185)
            )
        } else {
            listOf(
                Color(0xFF22C55E), Color(0xFF38BDF8), Color(0xFF14B8A6),
                Color(0xFFA3E635), Color(0xFF4ADE80), Color(0xFF06B6D4),
                Color(0xFF16A34A), Color(0xFF0EA5E9), Color(0xFF84CC16)
            )
        }
    }

    if (total <= 0.0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = katStringResource(id = R.string.tab_report_no_data, isEng = isEng),
                color = subTextColor,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        GaugeChart(
            dataByCategory = dataByCategory,
            total = total,
            selectedCategory = selectedCategory,
            centerLabel = centerLabel,
            currency = currency,
            isExpense = isExpense,
            textColor = textColor,
            subTextColor = subTextColor,
            amountColor = if (isExpense) Color(0xFFEF4444) else primaryBrandColor,
            borderColor = borderColor,
            segmentColors = segmentColors,
            animationProgress = animationProgress,
            onSelected = { category ->
                selectedCategory = if (selectedCategory == category) null else category
                category?.let(onCategoryClick)
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isExpense) {
                katStringResource(id = R.string.tab_report_chip_expense, isEng = isEng)
            } else {
                katStringResource(id = R.string.tab_report_chip_income, isEng = isEng)
            },
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        dataByCategory.entries
            .sortedByDescending { it.value }
            .take(5)
            .forEachIndexed { index, entry ->
                val category = entry.key
                val amount = entry.value
                val percent = (amount / total).coerceIn(0.0, 1.0)
                val color = segmentColors[index % segmentColors.size]
                val isSelected = selectedCategory == category

                TopCategoryRow(
                    category = category,
                    amount = amount,
                    percent = percent,
                    currency = currency,
                    color = color,
                    selected = isSelected,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    cardBgColor = cardBgColor,
                    borderColor = borderColor,
                    animationProgress = animationProgress,
                    onClick = {
                        selectedCategory = if (isSelected) null else category
                        onCategoryClick(category)
                    }
                )

                if (index != dataByCategory.entries.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
    }
}

@Composable
private fun GaugeChart(
    dataByCategory: Map<String, Double>,
    total: Double,
    selectedCategory: String?,
    centerLabel: String,
    currency: String,
    isExpense: Boolean,
    textColor: Color,
    subTextColor: Color,
    amountColor: Color,
    borderColor: Color,
    segmentColors: List<Color>,
    animationProgress: Float,
    onSelected: (String?) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 250.dp)
                .fillMaxWidth(0.78f)
                .aspectRatio(2f)
                .pointerInput(dataByCategory, animationProgress) {
                    detectTapGestures { offset ->
                        val radius = size.width / 2f
                        val centerX = radius
                        val centerY = size.height.toFloat()
                        val dx = offset.x - centerX
                        val dy = offset.y - centerY
                        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                        val strokeWidth = 52f

                        if (distance in (radius - strokeWidth)..(radius + strokeWidth * 0.18f)) {
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            var currentAngle = 180f
                            var tappedCategory: String? = null

                            for ((category, amount) in dataByCategory) {
                                val sweep = (amount.toFloat() / total.toFloat()) * 180f
                                if (angle >= currentAngle && angle <= currentAngle + sweep) {
                                    tappedCategory = category
                                    break
                                }
                                currentAngle += sweep
                            }

                            onSelected(tappedCategory)
                        } else {
                            onSelected(null)
                        }
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseStrokeWidth = 38f

                drawArc(
                    color = borderColor.copy(alpha = 0.26f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = Stroke(width = baseStrokeWidth, cap = StrokeCap.Round),
                    size = Size(size.width, size.width)
                )

                var startAngle = 180f

                dataByCategory.entries.toList().forEachIndexed { index, entry ->
                    val rawSweep = (entry.value / total).toFloat() * 180f
                    val sweepAngle = rawSweep * animationProgress
                    val isSelected = selectedCategory == entry.key
                    val strokeWidth = if (isSelected) baseStrokeWidth + 8f else baseStrokeWidth
                    val alpha = if (selectedCategory == null || isSelected) 1f else 0.26f

                    drawArc(
                        color = segmentColors[index % segmentColors.size].copy(alpha = alpha),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        size = Size(size.width, size.width)
                    )

                    startAngle += rawSweep
                }
            }

            GaugeCenterLabel(
                selectedCategory = selectedCategory,
                centerLabel = centerLabel,
                displayAmount = selectedCategory?.let { dataByCategory[it] } ?: total,
                total = total,
                currency = currency,
                textColor = textColor,
                subTextColor = subTextColor,
                amountColor = amountColor
            )
        }
    }
}

@Composable
private fun GaugeCenterLabel(
    selectedCategory: String?,
    centerLabel: String,
    displayAmount: Double,
    total: Double,
    currency: String,
    textColor: Color,
    subTextColor: Color,
    amountColor: Color
) {
    val percentAmount = if (selectedCategory != null && total > 0.0) {
        (displayAmount / total) * 100.0
    } else {
        100.0
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.offset(y = 10.dp)
    ) {
        Text(
            text = selectedCategory ?: centerLabel,
            color = subTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = formatCompactCurrency(displayAmount, currency),
            color = amountColor,
            fontWeight = FontWeight.Bold,
            fontSize = 23.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 220.dp)
        )

        if (selectedCategory != null) {
            Text(
                text = String.format(Locale.US, "%.1f%%", percentAmount),
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(textColor.copy(alpha = 0.08f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun TopCategoryRow(
    category: String,
    amount: Double,
    percent: Double,
    currency: String,
    color: Color,
    selected: Boolean,
    textColor: Color,
    subTextColor: Color,
    cardBgColor: Color,
    borderColor: Color,
    animationProgress: Float,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) color.copy(alpha = 0.12f) else cardBgColor.copy(alpha = 0.48f))
            .border(
                width = 1.dp,
                color = if (selected) color.copy(alpha = 0.42f) else borderColor.copy(alpha = 0.42f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )

                Spacer(modifier = Modifier.width(9.dp))

                Text(
                    text = category,
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "${String.format(Locale.US, "%.1f", percent * 100)}%",
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(9.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape)
                .background(borderColor.copy(alpha = 0.32f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((percent * animationProgress).toFloat())
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(color)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = formatCurrency(amount, currency),
            color = subTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
