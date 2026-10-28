package com.suresh.finance.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suresh.finance.ui.formatCompact
import kotlin.math.max
import kotlin.math.min

data class Slice(val label: String, val value: Double, val color: Color)

/** Ring chart for a category breakdown. Draws an empty ring when there is nothing to show. */
@Composable
fun DonutChart(
    slices: List<Slice>,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit = {},
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val total = slices.sumOf { it.value }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(170.dp)) {
            val stroke = 26.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            if (total <= 0.0) {
                drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                return@Canvas
            }
            var start = -90f
            val gap = if (slices.size > 1) 1.5f else 0f
            slices.forEach { s ->
                val sweep = (s.value / total * 360.0).toFloat()
                drawArc(s.color, start + gap / 2, max(sweep - gap, 0.5f), false, topLeft, arcSize, style = Stroke(stroke))
                start += sweep
            }
        }
        center()
    }
}

@Composable
fun Legend(slices: List<Slice>, total: Double, valueText: (Double) -> String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        slices.forEach { s ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(s.color, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(s.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
                val pct = if (total > 0) (s.value / total * 100).toInt() else 0
                Text(
                    "${valueText(s.value)}  ·  $pct%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

data class BarGroup(val label: String, val values: List<Double>)

/**
 * Grouped bars, one group per period (for example income and expenses per month).
 * [colors] gives one colour per value in a group. A dashed baseline marks zero.
 */
@Composable
fun GroupedBarChart(
    groups: List<BarGroup>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    height: Int = 180,
) {
    val maxValue = max(groups.flatMap { it.values }.maxOrNull() ?: 0.0, 1.0)
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(formatCompact(maxValue), style = MaterialTheme.typography.labelSmall, color = labelColor)
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height.dp),
        ) {
            val n = max(groups.size, 1)
            val groupWidth = size.width / n
            val perGroup = max(colors.size, 1)
            val barWidth = min(groupWidth * 0.7f / perGroup, 18.dp.toPx())
            val radius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            // Light grid lines at 25%, 50%, 75% and 100%.
            for (i in 1..4) {
                val y = size.height * (1 - i / 4f)
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
            drawLine(grid, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
            groups.forEachIndexed { gi, g ->
                val groupStart = gi * groupWidth + (groupWidth - barWidth * perGroup) / 2
                g.values.forEachIndexed { vi, v ->
                    if (v <= 0) return@forEachIndexed
                    val h = (v / maxValue * size.height).toFloat()
                    drawRoundRect(
                        color = colors.getOrElse(vi) { Color.Gray },
                        topLeft = Offset(groupStart + vi * barWidth, size.height - h),
                        size = Size(barWidth - 2.dp.toPx(), h),
                        cornerRadius = radius,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            groups.forEach { g ->
                Text(
                    g.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Horizontal bars, handy for debts or assets. */
@Composable
fun HorizontalBars(items: List<Slice>, valueText: (Double) -> String, modifier: Modifier = Modifier) {
    val maxValue = max(items.maxOfOrNull { it.value } ?: 0.0, 1.0)
    val track = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            Column {
                Row {
                    Text(item.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(valueText(item.value), style = MaterialTheme.typography.bodyMedium)
                }
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                ) {
                    val r = CornerRadius(size.height / 2, size.height / 2)
                    drawRoundRect(track, cornerRadius = r)
                    val w = (item.value / maxValue).toFloat() * size.width
                    if (w > 0) drawRoundRect(item.color, size = Size(w, size.height), cornerRadius = r)
                }
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
