package com.fourpit.marketpos.ui.components

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.CurrencyCode
import com.fourpit.marketpos.model.PaymentMethod
import com.fourpit.marketpos.util.BranchRevenueStat
import com.fourpit.marketpos.util.CategoryBreakdownItem
import com.fourpit.marketpos.util.CurrencyFormatter
import com.fourpit.marketpos.util.PaymentBreakdownItem

@Composable
fun CategoryBreakdownCard(
    title: String,
    items: List<CategoryBreakdownItem>,
    currency: CurrencyCode,
    eurRate: Double,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (items.isEmpty()) {
                Text(strings.noDataYet, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                items.take(6).forEach { item ->
                    BreakdownBarRow(
                        label = "${item.category} (${item.quantitySold})",
                        valueText = CurrencyFormatter.formatCompact(item.revenueRsd, currency, eurRate),
                        progress = item.share,
                        barColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentBreakdownCard(
    title: String,
    items: List<PaymentBreakdownItem>,
    currency: CurrencyCode,
    eurRate: Double,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (items.isEmpty()) {
                Text(strings.noDataYet, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                items.forEach { item ->
                    val label = when (item.method) {
                        PaymentMethod.CASH -> "${strings.cash} (${item.orderCount})"
                        PaymentMethod.CARD -> "${strings.card} (${item.orderCount})"
                    }
                    val color = if (item.method == PaymentMethod.CASH) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    }
                    BreakdownBarRow(
                        label = label,
                        valueText = "${CurrencyFormatter.formatCompact(item.revenueRsd, currency, eurRate)} · ${(item.share * 100).toInt()}%",
                        progress = item.share,
                        barColor = color
                    )
                }
            }
        }
    }
}

@Composable
fun BranchComparisonCard(
    title: String,
    items: List<BranchRevenueStat>,
    currency: CurrencyCode,
    eurRate: Double,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (items.isEmpty()) {
                Text(strings.noDataYet, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                items.forEach { branch ->
                    BreakdownBarRow(
                        label = "${branch.locationName} (${branch.orderCount})",
                        valueText = CurrencyFormatter.formatCompact(branch.revenueRsd, currency, eurRate),
                        progress = branch.share,
                        barColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun BreakdownBarRow(
    label: String,
    valueText: String,
    progress: Float,
    barColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            MonoPriceText(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        )
    }
}
