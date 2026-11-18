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
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.TableRestaurant
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.TableRestaurant
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.BarTable
import com.example.marketpos.model.LocationReportSummary
import com.example.marketpos.model.Product
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.model.ReportPeriodType
import com.example.marketpos.model.VenueMode
import com.example.marketpos.ui.components.PosTopAppBar
import com.example.marketpos.ui.screens.admin.AdminDashboardDatePickerDialog
import com.example.marketpos.ui.screens.admin.AdminDashboardTab
import com.example.marketpos.ui.screens.admin.AdminProductsTab
import com.example.marketpos.ui.screens.admin.AdminWorkerLogTab
import com.example.marketpos.ui.screens.admin.DeleteProductConfirmDialog
import com.example.marketpos.ui.screens.admin.DeleteTableConfirmDialog
import com.example.marketpos.ui.screens.admin.ProductEditorDialog
import com.example.marketpos.ui.screens.orders.OrderDetailsScreen
import com.example.marketpos.ui.screens.orders.OrdersListPane
import com.example.marketpos.viewmodel.AdminBranchUiState

private enum class AdminTab {
    DASHBOARD,
    ORDERS,
    WORKER_LOG,
    PRODUCTS,
    TABLES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    uiState: AdminBranchUiState,
    onSelectWorkerFilter: (String?) -> Unit,
    onSelectDashboardDay: (Long?, Boolean) -> Unit,
    onReprintReceipt: (String) -> Unit,
    onOpenOrderDetails: (String) -> Unit,
    onCloseOrderDetails: () -> Unit,
    onSaveProduct: (
        existingId: String?,
        sku: String,
        nameSr: String,
        nameEn: String,
        category: ProductCategory,
        priceRsd: Double,
        unitSr: String,
        unitEn: String,
        stockQuantity: Int,
        lowStockThreshold: Int,
        isAvailable: Boolean
    ) -> Unit,
    onAdjustStock: (String, Int) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onSaveTable: (existingId: String?, name: String, zone: String, capacity: Int) -> Unit,
    onDeleteTable: (String) -> Unit,
    onForceUnlockTable: (String) -> Unit,
    onBuildReportSummary: (
        locationId: String?,
        periodType: ReportPeriodType,
        startMillis: Long,
        endMillis: Long
    ) -> LocationReportSummary,
    onExportPdfReport: (LocationReportSummary) -> Unit,
    onDismissPdfResult: () -> Unit,
    onOpenSettings: () -> Unit,
    onBackToVenuePicker: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalStrings.current
    val isBarMode = uiState.settings.venueMode == VenueMode.BAR_RESTAURANT
    val visibleTabs = remember(isBarMode) {
        if (isBarMode) {
            AdminTab.entries
        } else {
            listOf(AdminTab.DASHBOARD, AdminTab.ORDERS, AdminTab.WORKER_LOG, AdminTab.PRODUCTS)
        }
    }
    var activeTab by rememberSaveable { mutableStateOf(AdminTab.DASHBOARD) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showCreateProductDialog by remember { mutableStateOf(false) }
    var deletingProduct by remember { mutableStateOf<Product?>(null) }
    var editingTable by remember { mutableStateOf<BarTable?>(null) }
    var showCreateTableDialog by remember { mutableStateOf(false) }
    var deletingTable by remember { mutableStateOf<BarTable?>(null) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showReportDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isBarMode) {
        if (!isBarMode && activeTab == AdminTab.TABLES) {
            activeTab = AdminTab.DASHBOARD
        }
    }

    val selectedOrder = uiState.selectedOrder
    if (selectedOrder != null) {
        OrderDetailsScreen(
            settings = uiState.settings,
            products = uiState.products,
            order = selectedOrder,
            canEdit = false,
            snackbarHostState = snackbarHostState,
            onBack = onCloseOrderDetails,
            onEditOrder = {},
            onIncrementItem = { _, _ -> },
            onDecrementItem = { _, _ -> },
            onCancelItem = { _, _ -> },
            onCancelFullOrder = {},
            onCompletePayment = { _, _, _ -> },
            onOpenBill = onReprintReceipt
        )
        return
    }

    BackHandler { onBackToVenuePicker() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp

        Scaffold(
            topBar = {
                val activeLoc = uiState.activeLocation
                val barTitle = activeLoc?.name ?: strings.venueName(uiState.settings.venueMode)
                val barSubtitle = listOfNotNull(
                    strings.adminPanelTitle,
                    activeLoc?.address?.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                PosTopAppBar(
                    title = barTitle,
                    subtitle = barSubtitle,
                    currencyBadge = strings.activeCurrencyLabel,
                    onOpenSettings = onOpenSettings,
                    onLogout = onLogout,
                    onBack = onBackToVenuePicker,
                    extraActions = {
                        IconButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier.testTag("admin_open_pdf_report")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = strings.pdfReportTitle,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        visibleTabs.forEach { tab ->
                            val selected = activeTab == tab
                            val label = adminTabLabel(tab, strings)
                            val icon = adminTabIcon(tab, selected)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    AdminTabIconWithBadge(
                                        tab = tab,
                                        icon = icon,
                                        label = label,
                                        cancelledCount = uiState.cancelledOrdersCount
                                    )
                                },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("admin_nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                when (activeTab) {
                    AdminTab.PRODUCTS -> {
                        FloatingActionButton(
                            onClick = { showCreateProductDialog = true },
                            modifier = Modifier.testTag("admin_add_product_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = strings.addProductTitle
                            )
                        }
                    }
                    AdminTab.TABLES -> {
                        FloatingActionButton(
                            onClick = { showCreateTableDialog = true },
                            modifier = Modifier.testTag("admin_add_table_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = strings.addTableTitle
                            )
                        }
                    }
                    else -> Unit
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
                        visibleTabs.forEach { tab ->
                            val selected = activeTab == tab
                            val label = adminTabLabel(tab, strings)
                            val icon = adminTabIcon(tab, selected)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    AdminTabIconWithBadge(
                                        tab = tab,
                                        icon = icon,
                                        label = label,
                                        cancelledCount = uiState.cancelledOrdersCount
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("admin_rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                when (activeTab) {
                    AdminTab.DASHBOARD -> AdminDashboardTab(
                        uiState = uiState,
                        isWide = useRail,
                        onSelectDay = { dayMillis -> onSelectDashboardDay(dayMillis, true) },
                        onOpenDatePicker = { showDatePicker = true },
                        onOpenReportDialog = { showReportDialog = true },
                        onReprintReceipt = onReprintReceipt,
                        onQuickRestock = { productId -> onAdjustStock(productId, 10) },
                        modifier = Modifier.weight(1f)
                    )
                    AdminTab.ORDERS -> OrdersListPane(
                        orders = uiState.orders,
                        workers = uiState.venueWorkers,
                        language = uiState.settings.language,
                        rsdPerEur = uiState.settings.rsdPerEur,
                        isAdminView = true,
                        onOpenOrderDetails = onOpenOrderDetails,
                        modifier = Modifier.weight(1f)
                    )
                    AdminTab.WORKER_LOG -> AdminWorkerLogTab(
                        uiState = uiState,
                        onSelectWorkerFilter = onSelectWorkerFilter,
                        onReprintReceipt = onReprintReceipt,
                        modifier = Modifier.weight(1f)
                    )
                    AdminTab.PRODUCTS -> AdminProductsTab(
                        uiState = uiState,
                        onAdjustStock = onAdjustStock,
                        onEditProduct = { editingProduct = it },
                        onDeleteProduct = { deletingProduct = it },
                        modifier = Modifier.weight(1f)
                    )
                    AdminTab.TABLES -> AdminTablesTab(
                        uiState = uiState,
                        onEditTable = { if (!it.isLocked) editingTable = it },
                        onDeleteTable = { if (!it.isLocked) deletingTable = it },
                        onForceUnlockTable = onForceUnlockTable,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showCreateProductDialog || editingProduct != null) {
        ProductEditorDialog(
            existing = editingProduct,
            venueMode = uiState.settings.venueMode,
            language = uiState.settings.language,
            rsdPerEur = uiState.settings.rsdPerEur,
            onDismiss = {
                showCreateProductDialog = false
                editingProduct = null
            },
            onSave = { sku, nameSr, nameEn, category, priceRsd, unitSr, unitEn, stockQty, threshold, available ->
                onSaveProduct(
                    editingProduct?.id,
                    sku,
                    nameSr,
                    nameEn,
                    category,
                    priceRsd,
                    unitSr,
                    unitEn,
                    stockQty,
                    threshold,
                    available
                )
                showCreateProductDialog = false
                editingProduct = null
            }
        )
    }

    if (showCreateTableDialog || editingTable != null) {
        TableEditorDialog(
            existing = editingTable,
            onDismiss = {
                showCreateTableDialog = false
                editingTable = null
            },
            onSave = { name, zone, capacity ->
                onSaveTable(editingTable?.id, name, zone, capacity)
                showCreateTableDialog = false
                editingTable = null
            }
        )
    }

    deletingTable?.let { target ->
        DeleteTableConfirmDialog(
            table = target,
            onDismiss = { deletingTable = null },
            onConfirmDelete = { tableId ->
                onDeleteTable(tableId)
                deletingTable = null
            }
        )
    }

    deletingProduct?.let { target ->
        DeleteProductConfirmDialog(
            product = target,
            productDisplayName = target.displayName(uiState.settings.language),
            onDismiss = { deletingProduct = null },
            onConfirmDelete = { productId ->
                onDeleteProduct(productId)
                deletingProduct = null
            }
        )
    }

    if (showDatePicker) {
        AdminDashboardDatePickerDialog(
            selectedDashboardDayMillis = uiState.selectedDashboardDayMillis,
            onDismiss = { showDatePicker = false },
            onSelectDate = { localDay -> onSelectDashboardDay(localDay, false) }
        )
    }

    if (showReportDialog) {
        ReportExportDialog(
            settings = uiState.settings,
            locations = uiState.locations,
            allTransactions = uiState.allTransactions,
            allOrders = uiState.allOrders,
            pdfExportResult = uiState.pdfExportResult,
            initialLocationId = uiState.activeLocationId,
            allowLocationSwitch = true,
            onDismiss = { showReportDialog = false },
            onBuildSummary = onBuildReportSummary,
            onExportPdf = onExportPdfReport,
            onDismissResult = onDismissPdfResult
        )
    }
}

private fun adminTabLabel(tab: AdminTab, strings: com.example.marketpos.i18n.AppStrings): String = when (tab) {
    AdminTab.DASHBOARD -> strings.navDashboard
    AdminTab.ORDERS -> strings.navOrders
    AdminTab.WORKER_LOG -> strings.navWorkerLog
    AdminTab.PRODUCTS -> strings.navProducts
    AdminTab.TABLES -> strings.navTables
}

private fun adminTabIcon(tab: AdminTab, selected: Boolean) = when (tab) {
    AdminTab.DASHBOARD -> if (selected) Icons.Rounded.Analytics else Icons.Outlined.Analytics
    AdminTab.ORDERS -> if (selected) Icons.Rounded.RestaurantMenu else Icons.Outlined.RestaurantMenu
    AdminTab.WORKER_LOG -> if (selected) Icons.AutoMirrored.Rounded.ReceiptLong else Icons.Outlined.ReceiptLong
    AdminTab.PRODUCTS -> if (selected) Icons.Rounded.Inventory2 else Icons.Outlined.Inventory2
    AdminTab.TABLES -> if (selected) Icons.Rounded.TableRestaurant else Icons.Outlined.TableRestaurant
}

@Composable
private fun AdminTabIconWithBadge(
    tab: AdminTab,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    cancelledCount: Int
) {
    if (tab == AdminTab.ORDERS && cancelledCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Text(cancelledCount.toString())
                }
            }
        ) {
            Icon(icon, contentDescription = label)
        }
    } else {
        Icon(icon, contentDescription = label)
    }
}
