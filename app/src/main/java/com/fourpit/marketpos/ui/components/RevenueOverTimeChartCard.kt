package com.fourpit.marketpos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.CurrencyCode
import com.fourpit.marketpos.util.CurrencyFormatter
import com.fourpit.marketpos.util.DailyRevenuePoint

@Composable
fun RevenueOverTimeChartCard(
    title: String,
    points: List<DailyRevenuePoint>,
    currency: CurrencyCode,
    eurRate: Double,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val peak = points.maxOfOrNull { it.revenueRsd } ?: 0.0

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (peak > 0.0) {
                    MonoPriceText(
                        text = CurrencyFormatter.formatCompact(peak, currency, eurRate),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (points.isEmpty() || peak <= 0.0) {
                Text(
                    text = strings.noDataYet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(136.dp)
                ) {
                    val count = points.size.coerceAtLeast(1)
                    val gap = 12.dp.toPx()
                    val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(8f)
                    val radius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

                    points.forEachIndexed { idx, pt ->
                        val left = idx * (barWidth + gap)
                        drawRoundRect(
                            color = trackColor,
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, size.height),
                            cornerRadius = radius
                        )
                        val fraction = (pt.revenueRsd / peak).toFloat().coerceIn(0f, 1f)
                        val barHeight = (size.height * fraction).coerceAtLeast(if (pt.revenueRsd > 0) 6.dp.toPx() else 0f)
                        if (barHeight > 0f) {
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(left, size.height - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = radius
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    points.forEach { pt ->
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
