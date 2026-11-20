package com.example.marketpos.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.Product
import com.example.marketpos.ui.components.AddActionButton
import com.example.marketpos.ui.components.RevenueOverTimeChartCard
import com.example.marketpos.ui.components.SalesByWorkerChartCard
import com.example.marketpos.ui.components.TopProductsChartCard
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.util.CurrencyFormatter
import com.example.marketpos.viewmodel.AdminBranchUiState

@Composable
internal fun AdminDashboardTab(
    uiState: AdminBranchUiState,
    isWide: Boolean,
    onSelectDay: (Long?) -> Unit,
    onOpenDatePicker: () -> Unit,
    onOpenReportDialog: () -> Unit,
    onReprintReceipt: (String) -> Unit,
    onAdjustStock: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val settings = uiState.settings
    val lang = settings.language
    val rate = settings.rsdPerEur
    val pickedDayMillis = uiState.selectedDashboardDayMillis
    var restockingProduct by remember { mutableStateOf<Product?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (pickedDayMillis != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.dateOverviewTitle(
                                    CurrencyFormatter.formatFullDate(pickedDayMillis, lang)
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = { onSelectDay(null) }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = strings.allDaysBtn,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.allDaysBtn)
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        label = strings.kpiRevenue,
                        value = CurrencyFormatter.format(uiState.totalRevenueRsd, lang, rate),
                        highlight = true,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        label = strings.kpiTransactions,
                        value = uiState.dashboardTransactions.size.toString(),
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        label = strings.kpiAvgBasket,
                        value = CurrencyFormatter.format(uiState.averageBasketRsd, lang, rate),
                        highlight = false,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        label = strings.kpiLowStock,
                        value = uiState.lowOrOutOfStockCount.toString(),
                        highlight = false,
                        isWarning = uiState.lowOrOutOfStockCount > 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Button(
                onClick = onOpenReportDialog,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("admin_dashboard_export_pdf_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.exportPdfReportBtn,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        item {
            RevenueOverTimeChartCard(
                points = uiState.dailyRevenue,
                selectedDayTimestampMillis = pickedDayMillis,
                onSelectDay = onSelectDay,
                onOpenDatePicker = onOpenDatePicker,
                language = lang,
                rsdPerEur = rate
            )
        }

        if (pickedDayMillis != null) {
            item {
                SelectedDateOverviewCard(
                    dayMillis = pickedDayMillis,
                    transactions = uiState.dashboardTransactions,
                    cashRevenueRsd = uiState.dashboardCashRevenueRsd,
                    cardRevenueRsd = uiState.dashboardCardRevenueRsd,
                    language = lang,
                    rsdPerEur = rate,
                    onReprintReceipt = onReprintReceipt,
                    onClearDate = { onSelectDay(null) }
                )
            }
        }

        if (uiState.lowOrOutOfStockProducts.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.lowStockAlertsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        uiState.lowOrOutOfStockProducts.forEach { product ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLowest
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.displayName(lang),
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (!product.isInStock) {
                                                strings.outOfStock
                                            } else {
                                                strings.lowStockCount(
                                                    product.stockQuantity,
                                                    product.displayUnit(lang)
                                                )
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFamily,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = if (!product.isInStock) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.tertiary
                                            }
                                        )
                                    }
                                    AddActionButton(
                                        onClick = { restockingProduct = product },
                                        label = strings.restockBtn,
                                        testTag = "dashboard_restock_product_${product.id}"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isWide) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TopProductsChartCard(
                        products = uiState.topProducts,
                        language = lang,
                        rsdPerEur = rate,
                        selectedDayTimestampMillis = pickedDayMillis,
                        onClearSelectedDay = { onSelectDay(null) },
                        modifier = Modifier.weight(1f)
                    )
                    SalesByWorkerChartCard(
                        workers = uiState.salesByWorker,
                        language = lang,
                        rsdPerEur = rate,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            item {
                TopProductsChartCard(
                    products = uiState.topProducts,
                    language = lang,
                    rsdPerEur = rate,
                    selectedDayTimestampMillis = pickedDayMillis,
                    onClearSelectedDay = { onSelectDay(null) }
                )
            }
            item {
                SalesByWorkerChartCard(
                    workers = uiState.salesByWorker,
                    language = lang,
                    rsdPerEur = rate
                )
            }
        }
    }

    restockingProduct?.let { target ->
        AddStockDialog(
            product = target,
            language = lang,
            onDismiss = { restockingProduct = null },
            onConfirmAdd = { quantityToAdd ->
                onAdjustStock(target.id, quantityToAdd)
                restockingProduct = null
            }
        )
    }
}
