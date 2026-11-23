package com.fourpit.marketpos.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.CurrencyCode
import com.fourpit.marketpos.ui.components.BranchComparisonCard
import com.fourpit.marketpos.ui.components.CategoryBreakdownCard
import com.fourpit.marketpos.ui.components.PaymentBreakdownCard
import com.fourpit.marketpos.ui.components.RevenueOverTimeChartCard
import com.fourpit.marketpos.util.CurrencyFormatter
import com.fourpit.marketpos.viewmodel.AdminGlobalUiState

@Composable
fun GlobalEarningsTab(
    uiState: AdminGlobalUiState,
    currency: CurrencyCode,
    eurRate: Double,
    onExportPdfClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlobalKpiTile(
                    label = strings.todayRevenue,
                    value = CurrencyFormatter.formatCompact(uiState.totalRevenueRsd, currency, eurRate),
                    icon = Icons.Default.AttachMoney,
                    modifier = Modifier.weight(1f)
                )
                GlobalKpiTile(
                    label = strings.ordersCount,
                    value = uiState.totalPaidOrders.toString(),
                    icon = Icons.Default.Receipt,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlobalKpiTile(
                    label = strings.avgTicket,
                    value = CurrencyFormatter.formatCompact(uiState.averageTicketRsd, currency, eurRate),
                    icon = Icons.Default.ShoppingBag,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                GlobalKpiTile(
                    label = strings.lowStockItems,
                    value = uiState.lowStockCount.toString(),
                    icon = Icons.Default.WarningAmber,
                    tint = if (uiState.lowStockCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Button(
                onClick = onExportPdfClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_export_pdf_button")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Text("  ${strings.exportPdfReport}")
            }
        }
        item {
            RevenueOverTimeChartCard(
                title = strings.revenueOverTime,
                points = uiState.revenueSeries,
                currency = currency,
                eurRate = eurRate
            )
        }
        item {
            BranchComparisonCard(
                title = strings.branchComparison,
                items = uiState.branchComparison,
                currency = currency,
                eurRate = eurRate
            )
        }
        item {
            PaymentBreakdownCard(
                title = strings.paymentBreakdown,
                items = uiState.paymentBreakdown,
                currency = currency,
                eurRate = eurRate
            )
        }
        item {
            CategoryBreakdownCard(
                title = strings.categoryBreakdown,
                items = uiState.categoryBreakdown,
                currency = currency,
                eurRate = eurRate
            )
        }
    }
}
