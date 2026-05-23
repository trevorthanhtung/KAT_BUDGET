package com.katgr0up.katbudget.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.katgr0up.katbudget.R
import com.katgr0up.katbudget.data.local.entity.TransactionEntity
import com.katgr0up.katbudget.ui.utils.formatSmartCurrency
import com.katgr0up.katbudget.ui.utils.katStringResource
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun LineChartTrend(
    isEng: Boolean,
    transactions: List<TransactionEntity>,
    chartType: String,
    currency: String,
    filterStartDate: Long,
    filterEndDate: Long,
    textColor: Color,
    subTextColor: Color,
    primaryBrandColor: Color
) {
    val formatter = remember { SimpleDateFormat("dd/MM", Locale.US) }

    val dailyData = remember(transactions, chartType, filterStartDate, filterEndDate) {
        val result = linkedMapOf<Long, Double>()
        val calendar = Calendar.getInstance()

        calendar.timeInMillis = filterStartDate
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        while (calendar.timeInMillis <= filterEndDate) {
            result[calendar.timeInMillis] = 0.0
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        transactions
            .filter { it.type == chartType }
            .forEach { tx ->
                val dayCalendar = Calendar.getInstance().apply {
                    timeInMillis = tx.timestamp
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val dayMillis = dayCalendar.timeInMillis
                if (result.containsKey(dayMillis)) {
                    result[dayMillis] = (result[dayMillis] ?: 0.0) + tx.amount
                }
            }

        result.map { (dayMillis, amount) ->
            DailyTrendPoint(
                label = formatter.format(Date(dayMillis)),
                amount = amount
            )
        }
    }

    if (dailyData.isEmpty()) return

    val maxAmount = dailyData.maxOfOrNull { it.amount } ?: 1.0
    val maxValue = if (maxAmount <= 0.0) 1.0 else maxAmount

    var isAnimated by remember(dailyData) { mutableStateOf(false) }
    val animationProgress by animateFloatAsState(
        targetValue = if (isAnimated) 1f else 0f,
        animationSpec = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
        label = "line_chart_animation"
    )

    LaunchedEffect(dailyData) {
        isAnimated = true
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val lineColor = if (chartType == "EXPENSE") Color(0xFFEF4444) else primaryBrandColor

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LineChartHeader(
            isEng = isEng,
            chartType = chartType,
            currency = currency,
            selectedPoint = selectedIndex?.let { dailyData.getOrNull(it) },
            textColor = textColor,
            subTextColor = subTextColor,
            lineColor = lineColor
        )

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(194.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(lineColor.copy(alpha = 0.045f))
                .border(
                    width = 1.dp,
                    color = lineColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 14.dp, vertical = 16.dp)
                .pointerInput(dailyData) {
                    detectTapGestures(
                        onPress = { offset ->
                            selectedIndex = offset.toChartIndex(size.width, dailyData.size)
                            tryAwaitRelease()
                            selectedIndex = null
                        }
                    )
                }
                .pointerInput(dailyData) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedIndex = offset.toChartIndex(size.width, dailyData.size)
                        },
                        onDrag = { change, _ ->
                            selectedIndex = change.position.toChartIndex(size.width, dailyData.size)
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val spaceToX = width / (dailyData.size - 1).coerceAtLeast(1)

                repeat(5) { index ->
                    val y = height - index * (height / 4f)
                    drawLine(
                        color = subTextColor.copy(alpha = 0.13f),
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                val strokePath = Path()
                val fillPath = Path()
                var previousX = 0f
                var previousY = height

                dailyData.forEachIndexed { index, point ->
                    val x = index * spaceToX
                    val ratio = (point.amount / maxValue).toFloat().coerceIn(0f, 1f)
                    val y = height - ratio * height * animationProgress

                    if (index == 0) {
                        strokePath.moveTo(x, y)
                        fillPath.moveTo(x, height)
                        fillPath.lineTo(x, y)
                    } else {
                        val controlX = (previousX + x) / 2f
                        strokePath.cubicTo(controlX, previousY, controlX, y, x, y)
                        fillPath.cubicTo(controlX, previousY, controlX, y, x, y)
                    }

                    previousX = x
                    previousY = y

                    if (index == dailyData.lastIndex) {
                        fillPath.lineTo(x, height)
                        fillPath.close()
                    }
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.24f),
                            lineColor.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        endY = height
                    )
                )

                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                selectedIndex?.let { index ->
                    val point = dailyData.getOrNull(index) ?: return@let
                    val x = index * spaceToX
                    val ratio = (point.amount / maxValue).toFloat().coerceIn(0f, 1f)
                    val y = height - ratio * height

                    drawLine(
                        color = textColor.copy(alpha = 0.36f),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 1.4.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 9f), 0f)
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 7.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = lineColor,
                        radius = 4.5.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = dailyData.first().label, color = subTextColor, fontSize = 11.sp)
            Text(text = dailyData[dailyData.size / 2].label, color = subTextColor, fontSize = 11.sp)
            Text(text = dailyData.last().label, color = subTextColor, fontSize = 11.sp)
        }
    }
}

@Composable
private fun LineChartHeader(
    isEng: Boolean,
    chartType: String,
    currency: String,
    selectedPoint: DailyTrendPoint?,
    textColor: Color,
    subTextColor: Color,
    lineColor: Color
) {
    if (selectedPoint != null) {
        Text(
            text = selectedPoint.label,
            color = subTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = formatSmartCurrency(selectedPoint.amount, currency),
            color = lineColor,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
    } else {
        Text(
            text = if (chartType == "EXPENSE") {
                katStringResource(id = R.string.chart_daily_expense_trend, isEng = isEng)
            } else {
                katStringResource(id = R.string.chart_daily_income_trend, isEng = isEng)
            },
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = katStringResource(id = R.string.chart_touch_to_inspect, isEng = isEng),
            color = subTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private data class DailyTrendPoint(
    val label: String,
    val amount: Double
)

private fun Offset.toChartIndex(
    width: Int,
    itemCount: Int
): Int {
    val spaceToX = width / (itemCount - 1).coerceAtLeast(1).toFloat()
    return (x / spaceToX).toInt().coerceIn(0, itemCount - 1)
}