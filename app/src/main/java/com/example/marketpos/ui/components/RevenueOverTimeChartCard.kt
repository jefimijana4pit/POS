package com.example.marketpos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.util.CurrencyFormatter
import com.example.marketpos.util.DailyRevenuePoint
import kotlin.math.abs

@Composable
fun RevenueOverTimeChartCard(
    points: List<DailyRevenuePoint>,
    selectedDayTimestampMillis: Long?,
    onSelectDay: (Long?) -> Unit,
    onOpenDatePicker: () -> Unit,
    language: AppLanguage,
    rsdPerEur: Double,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val currentOnSelectDay by rememberUpdatedState(onSelectDay)
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceBg = MaterialTheme.colorScheme.surfaceContainerLowest

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.chartRevenueOverTime,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (selectedDayTimestampMillis != null) {
                            CurrencyFormatter.formatFullDate(selectedDayTimestampMillis, language)
                        } else {
                            strings.tapDotHint
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedDayTimestampMillis != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedDayTimestampMillis != null) {
                        AssistChip(
                            onClick = { onSelectDay(null) },
                            label = { Text(strings.allDaysBtn) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = strings.allDaysBtn,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("clear_selected_date_chip")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    IconButton(
                        onClick = onOpenDatePicker,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_date_picker_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = strings.pickDateLabel,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (points.isEmpty()) {
                Text(
                    text = strings.noChartData,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxRevenue = (points.maxOfOrNull { it.revenueRsd } ?: 1.0).coerceAtLeast(1.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .pointerInput(points) {
                            detectTapGestures { tapOffset ->
                                val leftPad = 16.dp.toPx()
                                val rightPad = 16.dp.toPx()
                                val chartW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                                val stepX = if (points.size > 1) chartW / (points.size - 1) else 0f

                                val nearestIndex = points.indices.minByOrNull { idx ->
                                    val x = if (points.size == 1) {
                                        leftPad + chartW / 2f
                                    } else {
                                        leftPad + idx * stepX
                                    }
                                    abs(tapOffset.x - x)
                                }
                                if (nearestIndex != null) {
                                    currentOnSelectDay(points[nearestIndex].dayTimestampMillis)
                                }
                            }
                        }
                        .testTag("revenue_chart_canvas")
                ) {
                    val leftPad = 16.dp.toPx()
                    val rightPad = 16.dp.toPx()
                    val topPad = 18.dp.toPx()
                    val bottomPad = 14.dp.toPx()
                    val chartW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                    val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)

                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    for (step in 0..3) {
                        val y = topPad + chartH * (step / 3f)
                        drawLine(
                            color = gridColor.copy(alpha = 0.55f),
                            start = Offset(leftPad, y),
                            end = Offset(leftPad + chartW, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = if (step == 3) null else dashEffect
                        )
                    }

                    val stepX = if (points.size > 1) chartW / (points.size - 1) else 0f
                    val coords = points.mapIndexed { idx, pt ->
                        val x = if (points.size == 1) leftPad + chartW / 2f else leftPad + idx * stepX
                        val ratio = (pt.revenueRsd / maxRevenue).toFloat().coerceIn(0f, 1f)
                        val y = topPad + chartH * (1f - ratio)
                        Offset(x, y)
                    }

                    val barW = (chartW / (points.size * 2.4f)).coerceIn(10.dp.toPx(), 28.dp.toPx())
                    coords.forEachIndexed { idx, pt ->
                        val isSelected = points[idx].dayTimestampMillis == selectedDayTimestampMillis
                        val barH = (topPad + chartH) - pt.y
                        if (isSelected) {
                            drawLine(
                                color = primaryColor.copy(alpha = 0.45f),
                                start = Offset(pt.x, topPad),
                                end = Offset(pt.x, topPad + chartH),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = dashEffect
                            )
                        }
                        drawRoundRect(
                            color = if (isSelected) {
                                primaryColor.copy(alpha = 0.30f)
                            } else {
                                secondaryColor.copy(alpha = 0.14f)
                            },
                            topLeft = Offset(pt.x - barW / 2f, pt.y),
                            size = Size(barW, barH.coerceAtLeast(0f)),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    if (coords.size >= 2) {
                        val areaPath = Path().apply {
                            moveTo(coords.first().x, topPad + chartH)
                            coords.forEach { lineTo(it.x, it.y) }
                            lineTo(coords.last().x, topPad + chartH)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.28f),
                                    primaryColor.copy(alpha = 0.02f)
                                ),
                                startY = topPad,
                                endY = topPad + chartH
                            )
                        )

                        val linePath = Path().apply {
                            moveTo(coords.first().x, coords.first().y)
                            for (i in 1 until coords.size) {
                                val prev = coords[i - 1]
                                val curr = coords[i]
                                val midX = (prev.x + curr.x) / 2f
                                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                            }
                        }
                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    coords.forEachIndexed { idx, pt ->
                        val isSelected = points[idx].dayTimestampMillis == selectedDayTimestampMillis
                        if (isSelected) {
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.25f),
                                radius = 12.dp.toPx(),
                                center = pt
                            )
                        }
                        drawCircle(
                            color = primaryColor,
                            radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = surfaceBg,
                            radius = if (isSelected) 3.dp.toPx() else 2.5.dp.toPx(),
                            center = pt
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    points.forEachIndexed { idx, pt ->
                        val isSelected = pt.dayTimestampMillis == selectedDayTimestampMillis
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectDay(pt.dayTimestampMillis) }
                                .testTag("chart_day_pill_$idx")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = CurrencyFormatter.formatCompact(pt.revenueRsd, language, rsdPerEur),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                                Text(
                                    text = CurrencyFormatter.formatDayLabel(pt.dayTimestampMillis, language),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
