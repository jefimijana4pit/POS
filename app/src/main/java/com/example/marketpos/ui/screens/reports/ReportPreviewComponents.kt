package com.example.marketpos.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.LocationReportSummary
import com.example.marketpos.model.WorkerReportRow
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReportSummaryHeaderCard(
    summary: LocationReportSummary,
    language: AppLanguage,
    rsdPerEur: Double
) {
    val strings = LocalStrings.current
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.location?.name ?: strings.globalOverviewTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    val subtitle = summary.location?.let { loc ->
                        "${strings.venueName(loc.venueMode)} • ${loc.address}"
                    } ?: strings.globalReportSubtitle
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ReportMetricPill(
                    label = strings.kpiRevenue,
                    value = CurrencyFormatter.format(summary.totalRevenueRsd, language, rsdPerEur),
                    highlight = true
                )
                ReportMetricPill(
                    label = strings.reportFinishedBillsLabel,
                    value = summary.finishedBillsCount.toString()
                )
                ReportMetricPill(
                    label = strings.reportCancelledBillsLabel,
                    value = summary.cancelledOrdersCount.toString()
                )
                ReportMetricPill(
                    label = strings.reportTablesHandledLabel,
                    value = if (summary.tablesHandledCount > 0) summary.tablesHandledCount.toString() else "—"
                )
                ReportMetricPill(
                    label = strings.reportUnitsSoldLabel,
                    value = summary.totalUnitsSold.toString()
                )
            }
        }
    }
}

@Composable
internal fun ReportMetricPill(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        }
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlight) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = if (highlight) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
internal fun WorkerReportRowCard(
    row: WorkerReportRow,
    language: AppLanguage,
    rsdPerEur: Double
) {
    val strings = LocalStrings.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.workerName,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = CurrencyFormatter.format(row.revenueRsd, language, rsdPerEur),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = "${strings.reportFinShort}: ${row.finishedBillsCount}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (row.cancelledOrdersCount > 0 || row.cancelledItemsCount > 0) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    }
                ) {
                    val cancelDetail = if (row.cancelledItemsCount > 0) {
                        "${strings.reportCancShort}: ${row.cancelledOrdersCount} (${row.cancelledItemsCount} ${strings.reportItemsShort})"
                    } else {
                        "${strings.reportCancShort}: ${row.cancelledOrdersCount}"
                    }
                    Text(
                        text = cancelDetail,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (row.cancelledOrdersCount > 0 || row.cancelledItemsCount > 0) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${strings.reportColTables}: ${if (row.tablesHandledCount > 0) row.tablesHandledCount else "—"}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (row.productsSold.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val breakdown = row.productsSold.joinToString(", ") { stat ->
                    "${stat.quantity}× ${stat.displayName(language)}"
                }
                Text(
                    text = "${row.totalUnitsSold} ${strings.reportUnitsShort} — $breakdown",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

internal fun shiftMonth(anchorMillis: Long, deltaMonths: Int): Long {
    val cal = Calendar.getInstance()
    cal.timeInMillis = anchorMillis
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.add(Calendar.MONTH, deltaMonths)
    return CurrencyFormatter.localDayStartMillis(cal.timeInMillis)
}

internal fun formatMonthYear(millis: Long, language: AppLanguage): String {
    val pattern = if (language == AppLanguage.SERBIAN_LATIN) "LLLL yyyy" else "MMMM yyyy"
    val locale = if (language == AppLanguage.SERBIAN_LATIN) {
        Locale.forLanguageTag("sr-Latn")
    } else {
        Locale.ENGLISH
    }
    return SimpleDateFormat(pattern, locale)
        .format(Date(millis))
        .replaceFirstChar { it.uppercase(locale) }
}

internal fun formatShortDate(millis: Long, language: AppLanguage): String {
    val pattern = if (language == AppLanguage.SERBIAN_LATIN) "dd.MM.yyyy." else "MMM dd, yyyy"
    val locale = if (language == AppLanguage.SERBIAN_LATIN) {
        Locale.forLanguageTag("sr-Latn")
    } else {
        Locale.ENGLISH
    }
    return SimpleDateFormat(pattern, locale).format(Date(millis))
}
