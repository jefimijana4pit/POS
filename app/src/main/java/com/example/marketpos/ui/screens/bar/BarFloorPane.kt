package com.example.marketpos.ui.screens.bar

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.TableRestaurant
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.TableRestaurant
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.BarTable
import com.example.marketpos.model.sortedByAvailability
import com.example.marketpos.ui.components.EmptyStateView
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.ui.theme.LocalSemanticColors
import com.example.marketpos.util.CurrencyFormatter
import com.example.marketpos.viewmodel.BarWorkerUiState

internal enum class BarWorkerTab {
    TABLES,
    ORDERS,
    BILLS
}

private enum class TableFloorFilter {
    ALL,
    FREE,
    MINE,
    BUSY
}

@Composable
internal fun BarWorkerBottomNav(
    activeTab: BarWorkerTab,
    pendingOrdersCount: Int,
    onSelectTab: (BarWorkerTab) -> Unit
) {
    val strings = LocalStrings.current
    NavigationBar {
        BarWorkerTab.entries.forEach { tab ->
            val selected = activeTab == tab
            val label = when (tab) {
                BarWorkerTab.TABLES -> strings.navTables
                BarWorkerTab.ORDERS -> strings.navOrders
                BarWorkerTab.BILLS -> strings.navBills
            }
            val icon = when (tab) {
                BarWorkerTab.TABLES -> if (selected) Icons.Rounded.TableRestaurant else Icons.Outlined.TableRestaurant
                BarWorkerTab.ORDERS -> if (selected) Icons.Rounded.RestaurantMenu else Icons.Outlined.RestaurantMenu
                BarWorkerTab.BILLS -> if (selected) Icons.AutoMirrored.Rounded.ReceiptLong else Icons.Outlined.ReceiptLong
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    if (tab == BarWorkerTab.ORDERS && pendingOrdersCount > 0) {
                        BadgedBox(badge = { Badge { Text(pendingOrdersCount.toString()) } }) {
                            Icon(icon, contentDescription = label)
                        }
                    } else {
                        Icon(icon, contentDescription = label)
                    }
                },
                label = { Text(label, maxLines = 1) },
                modifier = Modifier.testTag("bar_worker_nav_${tab.name.lowercase()}")
            )
        }
    }
}

@Composable
internal fun BarTablesFloorContent(
    uiState: BarWorkerUiState,
    currentWorkerId: String?,
    onOpenOrLockTable: (BarTable) -> Unit,
    onUnlockTable: (String) -> Unit,
    onStartNewOrderOnTable: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val settings = uiState.settings
    var filter by rememberSaveable { mutableStateOf(TableFloorFilter.ALL) }

    val tables = uiState.tables
    val freeCount = remember(tables) { tables.count { !it.isLocked } }
    val mineCount = remember(tables, currentWorkerId) { tables.count { it.isLockedBy(currentWorkerId) } }
    val busyCount = remember(tables) { tables.count { it.isLocked } }

    val filteredTables = remember(tables, filter, currentWorkerId) {
        val matching = when (filter) {
            TableFloorFilter.ALL -> tables
            TableFloorFilter.FREE -> tables.filter { !it.isLocked }
            TableFloorFilter.MINE -> tables.filter { it.isLockedBy(currentWorkerId) }
            TableFloorFilter.BUSY -> tables.filter { it.isLocked }
        }
        matching.sortedByAvailability(currentWorkerId)
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
            OccupancySummaryPill(
                label = strings.tableFilterFree,
                count = freeCount,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            OccupancySummaryPill(
                label = strings.tableFilterMine,
                count = mineCount,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            OccupancySummaryPill(
                label = strings.tableFilterBusy,
                count = busyCount,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            item {
                FilterChip(
                    selected = filter == TableFloorFilter.ALL,
                    onClick = { filter = TableFloorFilter.ALL },
                    label = { Text("${strings.all} (${tables.size})") }
                )
            }
            item {
                FilterChip(
                    selected = filter == TableFloorFilter.FREE,
                    onClick = { filter = TableFloorFilter.FREE },
                    label = { Text("${strings.tableFilterFree} ($freeCount)") }
                )
            }
            item {
                FilterChip(
                    selected = filter == TableFloorFilter.MINE,
                    onClick = { filter = TableFloorFilter.MINE },
                    label = { Text("${strings.tableFilterMine} ($mineCount)") }
                )
            }
            item {
                FilterChip(
                    selected = filter == TableFloorFilter.BUSY,
                    onClick = { filter = TableFloorFilter.BUSY },
                    label = { Text("${strings.tableFilterBusy} ($busyCount)") }
                )
            }
        }

        if (filteredTables.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.TableRestaurant,
                title = strings.noTablesDefined,
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
                items(filteredTables, key = { it.id }) { table ->
                    BarTableRowItem(
                        table = table,
                        currentWorkerId = currentWorkerId,
                        nowMillis = uiState.nowMillis,
                        language = settings.language,
                        rsdPerEur = settings.rsdPerEur,
                        onTapTable = { onOpenOrLockTable(table) },
                        onUnlockTable = { onUnlockTable(table.id) },
                        onStartNewOrder = { onStartNewOrderOnTable(table.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OccupancySummaryPill(
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
