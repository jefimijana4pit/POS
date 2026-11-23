package com.fourpit.marketpos.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.PosSettings
import com.fourpit.marketpos.ui.screens.bar.BarFloorPane
import com.fourpit.marketpos.ui.screens.orders.BillsListPane
import com.fourpit.marketpos.ui.screens.orders.OrdersListPane
import com.fourpit.marketpos.viewmodel.BarWorkerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarWorkerScreen(
    viewModel: BarWorkerViewModel,
    workerName: String,
    settings: PosSettings,
    onOpenTableOrder: (tableId: Long, orderId: Long) -> Unit,
    onOpenOrderDetails: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.location?.name ?: strings.venueBar,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${strings.roleBarWorker}: $workerName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("bar_settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = strings.settings)
                    }
                    IconButton(onClick = onLogout, modifier = Modifier.testTag("bar_logout_button")) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = strings.logout)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.TableRestaurant, contentDescription = strings.floorPlan) },
                    label = { Text(strings.floorPlan) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.openOrders.isNotEmpty()) {
                                    Badge { Text(uiState.openOrders.size.toString()) }
                                }
                            }
                        ) {
                            Icon(Icons.Default.PendingActions, contentDescription = strings.openOrders)
                        }
                    },
                    label = { Text(strings.openOrders) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = strings.paidBills) },
                    label = { Text(strings.paidBills) }
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> BarFloorPane(
                tables = uiState.tables,
                currency = settings.currency,
                eurRate = settings.eurExchangeRate,
                onTableClick = { table ->
                    viewModel.openTable(table.id, workerName) { orderId ->
                        onOpenTableOrder(table.id, orderId)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            1 -> OrdersListPane(
                orders = uiState.openOrders,
                currency = settings.currency,
                eurRate = settings.eurExchangeRate,
                onOrderClick = { order ->
                    onOpenTableOrder(order.tableId ?: 0L, order.id)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            else -> BillsListPane(
                bills = uiState.paidBills,
                currency = settings.currency,
                eurRate = settings.eurExchangeRate,
                onBillClick = onOpenOrderDetails,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}
