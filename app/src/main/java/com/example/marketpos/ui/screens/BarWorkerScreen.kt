package com.example.marketpos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.TableRestaurant
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.TableRestaurant
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.BarTable
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.Product
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.ui.components.PosTopAppBar
import com.example.marketpos.ui.screens.bar.BarTableOrderScreen
import com.example.marketpos.ui.screens.bar.BarTablesFloorContent
import com.example.marketpos.ui.screens.bar.BarWorkerBottomNav
import com.example.marketpos.ui.screens.bar.BarWorkerTab
import com.example.marketpos.ui.screens.orders.BillsListPane
import com.example.marketpos.ui.screens.orders.OrderDetailsScreen
import com.example.marketpos.ui.screens.orders.OrdersListPane
import com.example.marketpos.viewmodel.BarWorkerUiState

@Composable
fun BarWorkerScreen(
    uiState: BarWorkerUiState,
    onOpenOrLockTable: (BarTable) -> Unit,
    onCloseTable: () -> Unit,
    onUnlockTable: (String) -> Unit,
    onStartNewOrderOnTable: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (ProductCategory?) -> Unit,
    onToggleOutOfStockFilter: () -> Unit,
    onAddProductToTable: (Product) -> Unit,
    onDecrementTableItem: (String) -> Unit,
    onRemoveTableItem: (String) -> Unit,
    onClearTableOrder: () -> Unit,
    onCompleteTableSale: (PaymentMethod, Double?) -> Unit,
    onOpenOrderDetails: (String) -> Unit,
    onCloseOrderDetails: () -> Unit,
    onEditOrder: (String) -> Unit,
    onIncrementOrderItem: (orderId: String, itemRowId: Long) -> Unit,
    onDecrementOrderItem: (orderId: String, itemRowId: Long) -> Unit,
    onCancelOrderItem: (orderId: String, itemRowId: Long) -> Unit,
    onCancelFullOrder: (String) -> Unit,
    onCompleteOrderPayment: (PosOrder, PaymentMethod, Double?) -> Unit,
    onOpenBill: (String) -> Unit,
    onDismissStockWarning: () -> Unit,
    onDismissTableLockFeedback: () -> Unit,
    onKeepPendingOrder: () -> Unit,
    onDismissOrderSavedNotice: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalStrings.current
    val settings = uiState.settings
    val currentWorkerId = uiState.currentUser?.id
    val activeTable = uiState.activeBarTable
    val selectedOrder = uiState.selectedOrder
    val snackbarHostState = remember { SnackbarHostState() }
    var activeTab by rememberSaveable { mutableStateOf(BarWorkerTab.TABLES) }

    LaunchedEffect(uiState.stockWarningProduct) {
        val warned = uiState.stockWarningProduct ?: return@LaunchedEffect
        val message = if (!warned.isInStock) {
            "${warned.displayName(settings.language)}: ${strings.outOfStock}"
        } else {
            strings.maxStockReached(warned.displayName(settings.language), warned.stockQuantity)
        }
        snackbarHostState.showSnackbar(message)
        onDismissStockWarning()
    }

    LaunchedEffect(uiState.tableLockFeedbackWorkerName) {
        val ownerName = uiState.tableLockFeedbackWorkerName ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(strings.tableLockedCannotInteract(ownerName))
        onDismissTableLockFeedback()
    }

    LaunchedEffect(uiState.orderSavedNotice) {
        if (uiState.orderSavedNotice) {
            snackbarHostState.showSnackbar(strings.orderSavedMessage)
            onDismissOrderSavedNotice()
        }
    }

    if (selectedOrder != null) {
        val linkedTable = selectedOrder.tableId?.let { id -> uiState.tables.firstOrNull { it.id == id } }
        val canEdit = linkedTable == null || !linkedTable.isLockedByOther(currentWorkerId)
        OrderDetailsScreen(
            settings = settings,
            products = uiState.products,
            order = selectedOrder,
            canEdit = canEdit,
            snackbarHostState = snackbarHostState,
            onBack = onCloseOrderDetails,
            onEditOrder = { orderId ->
                activeTab = BarWorkerTab.TABLES
                onEditOrder(orderId)
            },
            onIncrementItem = onIncrementOrderItem,
            onDecrementItem = onDecrementOrderItem,
            onCancelItem = onCancelOrderItem,
            onCancelFullOrder = onCancelFullOrder,
            onCompletePayment = onCompleteOrderPayment,
            onOpenBill = onOpenBill
        )
    } else if (activeTable != null) {
        BackHandler { onCloseTable() }
        val pendingOrderId = uiState.currentWorkerOrders.firstOrNull {
            it.tableId == activeTable.id && it.status == OrderStatus.PENDING
        }?.id
        BarTableOrderScreen(
            uiState = uiState,
            table = activeTable,
            pendingOrderId = pendingOrderId,
            snackbarHostState = snackbarHostState,
            onBack = onCloseTable,
            onKeepPendingOrder = onKeepPendingOrder,
            onOpenOrderDetails = onOpenOrderDetails,
            onUnlockTable = { onUnlockTable(activeTable.id) },
            onStartNewOrder = { onStartNewOrderOnTable(activeTable.id) },
            onSearchQueryChange = onSearchQueryChange,
            onSelectCategory = onSelectCategory,
            onToggleOutOfStockFilter = onToggleOutOfStockFilter,
            onAddProduct = onAddProductToTable,
            onDecrementItem = onDecrementTableItem,
            onRemoveItem = onRemoveTableItem,
            onClearOrder = onClearTableOrder,
            onCompleteSale = onCompleteTableSale
        )
    } else {
        val screenTitle = when (activeTab) {
            BarWorkerTab.TABLES -> strings.barFloorTitle
            BarWorkerTab.ORDERS -> strings.ordersTitle
            BarWorkerTab.BILLS -> strings.billsTitle
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 840.dp
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    PosTopAppBar(
                        title = screenTitle,
                        subtitle = uiState.currentUser?.name,
                        currencyBadge = strings.activeCurrencyLabel,
                        onOpenSettings = onOpenSettings,
                        onLogout = onLogout
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        BarWorkerBottomNav(
                            activeTab = activeTab,
                            pendingOrdersCount = uiState.pendingOrdersCount,
                            onSelectTab = { activeTab = it }
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
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
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { activeTab = tab },
                                    icon = {
                                        if (tab == BarWorkerTab.ORDERS && uiState.pendingOrdersCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge { Text(uiState.pendingOrdersCount.toString()) }
                                                }
                                            ) {
                                                Icon(icon, contentDescription = label)
                                            }
                                        } else {
                                            Icon(icon, contentDescription = label)
                                        }
                                    },
                                    label = { Text(label) },
                                    modifier = Modifier.testTag("bar_worker_rail_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    when (activeTab) {
                        BarWorkerTab.TABLES -> BarTablesFloorContent(
                            uiState = uiState,
                            currentWorkerId = currentWorkerId,
                            onOpenOrLockTable = onOpenOrLockTable,
                            onUnlockTable = onUnlockTable,
                            onStartNewOrderOnTable = onStartNewOrderOnTable,
                            modifier = Modifier.weight(1f)
                        )
                        BarWorkerTab.ORDERS -> OrdersListPane(
                            orders = uiState.currentWorkerOrders,
                            workers = emptyList(),
                            language = settings.language,
                            rsdPerEur = settings.rsdPerEur,
                            isAdminView = false,
                            currentWorkerId = currentWorkerId,
                            onOpenOrderDetails = onOpenOrderDetails,
                            modifier = Modifier.weight(1f)
                        )
                        BarWorkerTab.BILLS -> BillsListPane(
                            transactions = uiState.currentWorkerTransactions,
                            currentWorkerId = currentWorkerId,
                            language = settings.language,
                            rsdPerEur = settings.rsdPerEur,
                            onOpenReceipt = onOpenBill,
                            isAdminView = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
