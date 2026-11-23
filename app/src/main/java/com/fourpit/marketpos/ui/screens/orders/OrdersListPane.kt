package com.fourpit.marketpos.ui.screens.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.CurrencyCode
import com.fourpit.marketpos.model.PosOrder
import com.fourpit.marketpos.ui.components.EmptyStatePlaceholder
import com.fourpit.marketpos.ui.components.MonoPriceText
import com.fourpit.marketpos.ui.components.StatusPill
import com.fourpit.marketpos.ui.components.formatPosDateTime
import com.fourpit.marketpos.util.CurrencyFormatter

@Composable
fun OrdersListPane(
    orders: List<PosOrder>,
    currency: CurrencyCode,
    eurRate: Double,
    onOrderClick: (PosOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    if (orders.isEmpty()) {
        EmptyStatePlaceholder(
            icon = Icons.Default.TableRestaurant,
            title = strings.noOpenOrders,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(orders, key = { it.id }) { order ->
            Card(
                onClick = { onOrderClick(order) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_order_card_${order.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = order.tableLabel ?: order.receiptNumber,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            StatusPill(
                                text = "${order.itemCount} ${strings.itemsLabel}",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = "${order.workerName} · ${formatPosDateTime(order.createdAtMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MonoPriceText(
                        text = CurrencyFormatter.format(order.totalRsd, currency, eurRate),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
