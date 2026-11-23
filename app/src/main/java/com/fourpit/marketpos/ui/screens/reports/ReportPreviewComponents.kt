package com.fourpit.marketpos.ui.screens.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.CurrencyCode
import com.fourpit.marketpos.ui.components.MonoPriceText
import com.fourpit.marketpos.util.AggregatedReport
import com.fourpit.marketpos.util.CurrencyFormatter

@Composable
fun ReportPreviewCard(
    report: AggregatedReport,
    currency: CurrencyCode,
    eurRate: Double,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = strings.reportPreview,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            PreviewMetricRow(strings.todayRevenue, CurrencyFormatter.format(report.totalRevenueRsd, currency, eurRate))
            PreviewMetricRow(strings.cash, CurrencyFormatter.format(report.cashRevenueRsd, currency, eurRate))
            PreviewMetricRow(strings.card, CurrencyFormatter.format(report.cardRevenueRsd, currency, eurRate))
            PreviewMetricRow(strings.ordersCount, report.paidOrderCount.toString())
            PreviewMetricRow(strings.avgTicket, CurrencyFormatter.format(report.averageTicketRsd, currency, eurRate))
        }
    }
}

@Composable
private fun PreviewMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MonoPriceText(text = value, style = MaterialTheme.typography.labelLarge)
    }
}
