package com.example.marketpos.ui.screens.orders

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.WorkerProfile
import com.example.marketpos.ui.components.EmptyStateView
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.ui.theme.LocalSemanticColors
import com.example.marketpos.util.CurrencyFormatter

enum class OrderFilterOption {
    ALL,
    PENDING,
    FINISHED,
    CANCELLED
}

@Composable
internal fun OrdersListPane(
    orders: List<PosOrder>,
    workers: List<WorkerProfile>,
    language: AppLanguage,
    rsdPerEur: Double,
    isAdminView: Boolean,
    onOpenOrderDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    currentWorkerId: String? = null,
    initialFilter: OrderFilterOption = OrderFilterOption.ALL
) {
    val strings = LocalStrings.current
    val semanticColors = LocalSemanticColors.current
    var statusFilter by rememberSaveable { mutableStateOf(initialFilter) }
    var selectedWorkerId by rememberSaveable { mutableStateOf<String?>(null) }

    val scopedOrders = remember(orders, isAdminView, currentWorkerId) {
        if (!isAdminView && currentWorkerId != null) {
            orders.filter { it.workerId == currentWorkerId }
        } else {
            orders
        }
    }

    val workerFiltered = remember(scopedOrders, isAdminView, selectedWorkerId) {
        if (!isAdminView || selectedWorkerId == null) {
            scopedOrders
        } else {
            scopedOrders.filter { it.workerId == selectedWorkerId }
        }
    }

    val pendingCount = remember(workerFiltered) { workerFiltered.count { it.status == OrderStatus.PENDING } }
    val finishedCount = remember(workerFiltered) { workerFiltered.count { it.status == OrderStatus.FINISHED } }
    val cancelledCount = remember(workerFiltered) { workerFiltered.count { it.status == OrderStatus.CANCELLED } }

    val displayedOrders = remember(workerFiltered, statusFilter) {
        when (statusFilter) {
            OrderFilterOption.ALL -> workerFiltered
            OrderFilterOption.PENDING -> workerFiltered.filter { it.status == OrderStatus.PENDING }
            OrderFilterOption.FINISHED -> workerFiltered.filter { it.status == OrderStatus.FINISHED }
            OrderFilterOption.CANCELLED -> workerFiltered.filter { it.status == OrderStatus.CANCELLED }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrderSummaryMetricPill(
                label = strings.orderStatusPending,
                count = pendingCount,
                containerColor = semanticColors.warningContainer,
                contentColor = semanticColors.onWarningContainer,
                modifier = Modifier.weight(1f)
            )
            OrderSummaryMetricPill(
                label = strings.orderStatusFinished,
                count = finishedCount,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            OrderSummaryMetricPill(
                label = strings.orderStatusCancelled,
                count = cancelledCount,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            item {
                FilterChip(
                    selected = statusFilter == OrderFilterOption.ALL,
                    onClick = { statusFilter = OrderFilterOption.ALL },
                    label = { Text("${strings.all} (${workerFiltered.size})") },
                    modifier = Modifier.testTag("orders_filter_all")
                )
            }
            item {
                FilterChip(
                    selected = statusFilter == OrderFilterOption.PENDING,
                    onClick = { statusFilter = OrderFilterOption.PENDING },
                    label = { Text("${strings.orderStatusPending} ($pendingCount)") },
                    modifier = Modifier.testTag("orders_filter_pending")
                )
            }
            item {
                FilterChip(
                    selected = statusFilter == OrderFilterOption.FINISHED,
                    onClick = { statusFilter = OrderFilterOption.FINISHED },
                    label = { Text("${strings.orderStatusFinished} ($finishedCount)") },
                    modifier = Modifier.testTag("orders_filter_finished")
                )
            }
            item {
                FilterChip(
                    selected = statusFilter == OrderFilterOption.CANCELLED,
                    onClick = { statusFilter = OrderFilterOption.CANCELLED },
                    label = { Text("${strings.orderStatusCancelled} ($cancelledCount)") },
                    modifier = Modifier.testTag("orders_filter_cancelled")
                )
            }
        }

        if (isAdminView && workers.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedWorkerId == null,
                        onClick = { selectedWorkerId = null },
                        label = { Text(strings.allWorkers) }
                    )
                }
                items(workers, key = { it.id }) { worker ->
                    FilterChip(
                        selected = selectedWorkerId == worker.id,
                        onClick = {
                            selectedWorkerId = if (selectedWorkerId == worker.id) null else worker.id
                        },
                        label = { Text(worker.name) }
                    )
                }
            }
        }

        if (displayedOrders.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.RestaurantMenu,
                title = strings.noOrdersFound,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .widthIn(max = 840.dp)
                    .align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(displayedOrders, key = { it.id }) { order ->
                    OrderRowCard(
                        order = order,
                        language = language,
                        rsdPerEur = rsdPerEur,
                        onClick = { onOpenOrderDetails(order.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderSummaryMetricPill(
    label: String,
    count: Int,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun OrderRowCard(
    order: PosOrder,
    language: AppLanguage,
    rsdPerEur: Double,
    onClick: () -> Unit
) {
    val strings = LocalStrings.current
    val semanticColors = LocalSemanticColors.current

    val (containerColor, borderColor) = when (order.status) {
        OrderStatus.PENDING -> semanticColors.warningContainer.copy(alpha = 0.42f) to semanticColors.warning
        OrderStatus.FINISHED -> MaterialTheme.colorScheme.surfaceContainerLow to MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f) to MaterialTheme.colorScheme.error.copy(alpha = 0.65f)
    }

    val (badgeBg, badgeFg, statusLabel) = when (order.status) {
        OrderStatus.PENDING -> Triple(
            semanticColors.warningContainer,
            semanticColors.onWarningContainer,
            strings.orderStatusPending
        )
        OrderStatus.FINISHED -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            strings.orderStatusFinished
        )
        OrderStatus.CANCELLED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            strings.orderStatusCancelled
        )
    }

    val statusIcon = when (order.status) {
        OrderStatus.PENDING -> Icons.Rounded.Schedule
        OrderStatus.FINISHED -> Icons.Rounded.CheckCircle
        OrderStatus.CANCELLED -> Icons.Rounded.Cancel
    }

    val previewText = remember(order.items, language) {
        val source = if (order.activeItems.isNotEmpty()) order.activeItems else order.items
        source.take(3).joinToString(", ") { "${it.quantity}× ${it.displayName(language)}" } +
            if (source.size > 3) " +${source.size - 3}" else ""
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.2.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("order_card_${order.id}")
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = badgeBg,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = badgeFg,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = order.orderNumber,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "• ${order.tableName ?: strings.counterOrderLabel}",
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${order.workerName} • ${
                                CurrencyFormatter.formatDateTime(order.updatedAtMillis, language)
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = badgeFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (previewText.isNotEmpty()) {
                Text(
                    text = previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (order.activeItemCount > 0) {
                        Text(
                            text = strings.itemsCount(order.activeItemCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (order.cancelledItemCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = strings.canceledOrdersSummary(order.cancelledItemCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (order.status == OrderStatus.CANCELLED) {
                    val canceledGross = order.items.sumOf { it.grossLineTotalRsd } * (1.0 + CurrencyFormatter.VAT_RATE)
                    Text(
                        text = CurrencyFormatter.format(canceledGross, language, rsdPerEur),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = TextDecoration.LineThrough
                        ),
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = CurrencyFormatter.format(order.totalRsd, language, rsdPerEur),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
