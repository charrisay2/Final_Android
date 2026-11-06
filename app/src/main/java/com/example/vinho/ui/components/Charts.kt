package com.example.vinho.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class DonutSlice(val color: Color, val value: Float, val label: String)

/**
 * Biểu đồ tròn dạng vành khuyên, vẽ bằng Canvas (không cần thư viện chart bên ngoài).
 * Các nhát được vẽ nối tiếp nhau từ -90° (đỉnh), mỗi nhát có khe 1.5° cho dễ phân biệt.
 */
@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    centerLabel: String,
    centerValue: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 20.dp,
) {
    val total = slices.sumOf { it.value.toDouble() }.toFloat().takeIf { it > 0f } ?: 1f
    val appear by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "donutSweep",
    )

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            var startAngle = -90f

            if (slices.isEmpty()) {
                drawArc(
                    color = Color.Gray.copy(alpha = 0.2f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
            }

            slices.forEach { slice ->
                val sweep = (slice.value / total) * 360f * appear
                val gap = if (slices.size > 1) 1.5f else 0f
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.5f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = centerValue, style = MaterialTheme.typography.titleMedium)
        }
    }
}

data class MonthBar(val label: String, val income: Long, val expense: Long)

/**
 * Biểu đồ cột 6 tháng: mỗi tháng 2 cột (thu = primary, chi = error).
 * Dựng bằng composable (Box + fillMaxHeight) thay vì Canvas để chữ nhãn tự động
 * theo theme, theo font scale của người dùng và không phải tự đo text.
 */
@Composable
fun MonthlyBarChart(
    bars: List<MonthBar>,
    modifier: Modifier = Modifier,
    height: Dp = 168.dp,
) {
    val maxValue = bars.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1L) ?: 1L
    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        bars.forEach { bar ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Bar(
                        fraction = (bar.income.toFloat() / maxValue).coerceIn(0.02f, 1f),
                        color = incomeColor,
                    )
                    Bar(
                        fraction = (bar.expense.toFloat() / maxValue).coerceIn(0.02f, 1f),
                        color = expenseColor,
                    )
                }
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(
        Modifier
            .width(12.dp)
            .fillMaxHeight(fraction)
            .background(color, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)),
    )
}
