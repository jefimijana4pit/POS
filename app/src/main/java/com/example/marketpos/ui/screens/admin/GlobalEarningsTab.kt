package com.example.marketpos.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.VenueMode
import com.example.marketpos.ui.components.RevenueOverTimeChartCard
import com.example.marketpos.ui.components.TopProductsChartCard
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.ui.theme.LocalSemanticColors
import com.example.marketpos.util.CurrencyFormatter
import com.example.marketpos.util.PosAnalyticsCalculator
import com.example.marketpos.viewmodel.AdminGlobalUiState

private enum class GlobalVenueFilter {
    ALL,
    MARKETS,
    BARS
}

@Composable
internal fun GlobalEarningsTab(
    uiState: AdminGlobalUiState,
    onOpenLocationAdmin: (VenueLocation) -> Unit,
    onOpenReportDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val semanticColors = LocalSemanticColors.current
    val settings = uiState.settings
    val lang = settings.language
    val rate = settings.rsdPerEur

    var venueFilter by rememberSaveable { mutableStateOf(GlobalVenueFilter.ALL) }
    var selectedDayMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val filteredStats = remember(uiState.locationStats, venueFilter) {
        when (venueFilter) {
            GlobalVenueFilter.ALL -> uiState.locationStats
            GlobalVenueFilter.MARKETS -> uiState.locationStats.filter { it.location.venueMode == VenueMode.MARKET }
            GlobalVenueFilter.BARS -> uiState.locationStats.filter { it.location.venueMode == VenueMode.BAR_RESTAURANT }
        }
    }

    val filteredTransactions = remember(uiState.allTransactions, venueFilter) {
        when (venueFilter) {
            GlobalVenueFilter.ALL -> uiState.allTransactions
            GlobalVenueFilter.MARKETS -> uiState.allTransactions.filter { it.venueMode == VenueMode.MARKET }
            GlobalVenueFilter.BARS -> uiState.allTransactions.filter { it.venueMode == VenueMode.BAR_RESTAURANT }
        }
    }

    val filteredCancelledCount = remember(uiState.allOrders, venueFilter) {
        when (venueFilter) {
            GlobalVenueFilter.ALL -> uiState.allOrders
            GlobalVenueFilter.MARKETS -> uiState.allOrders.filter { it.venueMode == VenueMode.MARKET }
            GlobalVenueFilter.BARS -> uiState.allOrders.filter { it.venueMode == VenueMode.BAR_RESTAURANT }
        }.count { it.status == OrderStatus.CANCELLED }
    }

    val totalRev = filteredTransactions.sumOf { it.totalRsd }
    val cashRev = filteredTransactions.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalRsd }
    val cardRev = filteredTransactions.filter { it.paymentMethod == PaymentMethod.CARD }.sumOf { it.totalRsd }
    val avgBasket = if (filteredTransactions.isEmpty()) 0.0 else totalRev / filteredTransactions.size

    val dailyPoints = remember(filteredTransactions) {
        PosAnalyticsCalculator.computeDailyRevenue(filteredTransactions)
    }

    val topProductsTransactions = remember(filteredTransactions, selectedDayMillis) {
        if (selectedDayMillis == null) {
            filteredTransactions
        } else {
            filteredTransactions.filter {
                CurrencyFormatter.localDayStartMillis(it.timestampMillis) == selectedDayMillis
            }
        }
    }

    val topProducts = remember(topProductsTransactions) {
        PosAnalyticsCalculator.computeTopProducts(topProductsTransactions)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = venueFilter == GlobalVenueFilter.ALL,
                        onClick = { venueFilter = GlobalVenueFilter.ALL },
                        label = { Text(strings.globalFilterAllLocations) },
                        modifier = Modifier.testTag("global_filter_all")
                    )
                }
                item {
                    FilterChip(
                        selected = venueFilter == GlobalVenueFilter.MARKETS,
                        onClick = { venueFilter = GlobalVenueFilter.MARKETS },
                        label = { Text(strings.globalFilterMarketsOnly) },
                        modifier = Modifier.testTag("global_filter_markets")
                    )
                }
                item {
                    FilterChip(
                        selected = venueFilter == GlobalVenueFilter.BARS,
                        onClick = { venueFilter = GlobalVenueFilter.BARS },
                        label = { Text(strings.globalFilterBarsOnly) },
                        modifier = Modifier.testTag("global_filter_bars")
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlobalKpiTile(
                        label = strings.kpiRevenue,
                        value = CurrencyFormatter.format(totalRev, lang, rate),
                        highlight = true,
                        modifier = Modifier.weight(1f)
                    )
                    GlobalKpiTile(
                        label = strings.kpiTransactions,
                        value = filteredTransactions.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlobalKpiTile(
                        label = strings.kpiAvgBasket,
                        value = CurrencyFormatter.format(avgBasket, lang, rate),
                        modifier = Modifier.weight(1f)
                    )
                    GlobalKpiTile(
                        label = strings.reportCancelledBillsLabel,
                        value = filteredCancelledCount.toString(),
                        isWarning = filteredCancelledCount > 0,
                        modifier = Modifier.weight(1f)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentBreakdownRow(
                            icon = Icons.Rounded.Payments,
                            label = strings.cashLabel,
                            value = CurrencyFormatter.format(cashRev, lang, rate)
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        )
                        PaymentBreakdownRow(
                            icon = Icons.Rounded.CreditCard,
                            label = strings.cardLabel,
                            value = CurrencyFormatter.format(cardRev, lang, rate)
                        )
                    }
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
                    .height(50.dp)
                    .testTag("global_export_pdf_cta")
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
            Text(
                text = strings.earningsByLocationTitle,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        items(filteredStats, key = { it.location.id }) { stats ->
            val isMarket = stats.location.venueMode == VenueMode.MARKET
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onOpenLocationAdmin(stats.location) }
                    .testTag("global_location_card_${stats.location.id}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isMarket) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.tertiaryContainer
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isMarket) Icons.Rounded.Storefront else Icons.Rounded.LocalBar,
                                    contentDescription = null,
                                    tint = if (isMarket) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stats.location.name,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${strings.venueName(stats.location.venueMode)} • ${stats.location.address}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = CurrencyFormatter.format(stats.revenueRsd, lang, rate),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${stats.transactionCount} ${strings.navWorkerLog.lowercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                val summaryText = if (isMarket) {
                                    strings.activeProductsSummary(stats.productsCount)
                                } else {
                                    strings.freeTablesSummary(stats.freeTablesCount, stats.totalTablesCount)
                                }
                                Text(
                                    text = summaryText,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (stats.lowOrOutStockCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = semanticColors.warningContainer
                                ) {
                                    Text(
                                        text = "${strings.kpiLowStock}: ${stats.lowOrOutStockCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = semanticColors.onWarningContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = strings.openLocationAdminBtn,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            RevenueOverTimeChartCard(
                points = dailyPoints,
                selectedDayTimestampMillis = selectedDayMillis,
                onSelectDay = { dayMillis ->
                    selectedDayMillis = if (dayMillis != null && selectedDayMillis == dayMillis) {
                        null
                    } else {
                        dayMillis
                    }
                },
                onOpenDatePicker = { showDatePicker = true },
                language = lang,
                rsdPerEur = rate
            )
        }

        item {
            TopProductsChartCard(
                products = topProducts,
                language = lang,
                rsdPerEur = rate,
                selectedDayTimestampMillis = selectedDayMillis,
                onClearSelectedDay = { selectedDayMillis = null }
            )
        }
    }

    if (showDatePicker) {
        AdminDashboardDatePickerDialog(
            selectedDashboardDayMillis = selectedDayMillis,
            onDismiss = { showDatePicker = false },
            onSelectDate = { localDay -> selectedDayMillis = localDay }
        )
    }
}

@Composable
private fun PaymentBreakdownRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
