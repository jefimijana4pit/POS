package com.example.marketpos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.util.CurrencyFormatter
import com.example.marketpos.util.ProductSalesStat
import com.example.marketpos.util.WorkerSalesStat

@Composable
fun TopProductsChartCard(
    products: List<ProductSalesStat>,
    language: AppLanguage,
    rsdPerEur: Double,
    modifier: Modifier = Modifier,
    selectedDayTimestampMillis: Long? = null,
    onClearSelectedDay: (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("top_products_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = if (selectedDayTimestampMillis != null) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.chartTopProducts,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (selectedDayTimestampMillis != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = CurrencyFormatter.formatFullDate(selectedDayTimestampMillis, language),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (selectedDayTimestampMillis != null && onClearSelectedDay != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AssistChip(
                        onClick = onClearSelectedDay,
                        label = { Text(strings.allDaysBtn) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = strings.allDaysBtn,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("top_products_clear_date_chip")
                    )
                }
            }

            if (products.isEmpty()) {
                Text(
                    text = if (selectedDayTimestampMillis != null) {
                        strings.noSalesOnDate
                    } else {
                        strings.noChartData
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxUnits = (products.maxOfOrNull { it.unitsSold } ?: 1).coerceAtLeast(1)
                products.forEach { stat ->
                    val fraction = (stat.unitsSold.toFloat() / maxUnits.toFloat()).coerceIn(0.05f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stat.displayName(language),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${strings.soldUnitsSuffix(stat.unitsSold)} • ${
                                    CurrencyFormatter.format(stat.revenueRsd, language, rsdPerEur)
                                }",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        ) {
                            drawRoundRect(
                                color = trackColor,
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                            )
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(primaryColor.copy(alpha = 0.75f), primaryColor)
                                ),
                                size = Size(size.width * fraction, size.height),
                                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SalesByWorkerChartCard(
    workers: List<WorkerSalesStat>,
    language: AppLanguage,
    rsdPerEur: Double,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.chartSalesByWorker,
                style = MaterialTheme.typography.titleMedium
            )

            if (workers.isEmpty()) {
                Text(
                    text = strings.noChartData,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val maxRev = (workers.maxOfOrNull { it.revenueRsd } ?: 1.0).coerceAtLeast(1.0)
                workers.forEachIndexed { idx, stat ->
                    val barColor: Color = palette[idx % palette.size]
                    val fraction = (stat.revenueRsd / maxRev).toFloat().coerceIn(0.05f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${stat.workerName} (${stat.transactionCount})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = CurrencyFormatter.format(stat.revenueRsd, language, rsdPerEur),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = barColor
                            )
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                        ) {
                            drawRoundRect(
                                color = trackColor,
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                            drawRoundRect(
                                color = barColor,
                                size = Size(size.width * fraction, size.height),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}
