package com.example.marketpos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.Product
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.ui.components.CatalogPane
import com.example.marketpos.ui.components.CheckoutPaymentDialog
import com.example.marketpos.ui.components.PosTopAppBar
import com.example.marketpos.ui.components.StickyCompactCartBar
import com.example.marketpos.ui.screens.orders.BillsListPane
import com.example.marketpos.ui.screens.orders.OrderDetailsScreen
import com.example.marketpos.ui.screens.orders.OrdersListPane
import com.example.marketpos.ui.screens.worker.CartPanelContent
import com.example.marketpos.ui.screens.worker.EditingOrderHeaderBanner
import com.example.marketpos.ui.screens.worker.EditingOrderSidePanel
import com.example.marketpos.ui.screens.worker.MarketWorkerBottomNav
import com.example.marketpos.ui.screens.worker.MarketWorkerTab
import com.example.marketpos.ui.theme.LocalSemanticColors
import com.example.marketpos.util.CurrencyFormatter
import com.example.marketpos.viewmodel.MarketWorkerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerPosScreen(
    uiState: MarketWorkerUiState,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (ProductCategory?) -> Unit,
    onToggleOutOfStockFilter: () -> Unit,
    onAddProduct: (Product) -> Unit,
    onDecrementProduct: (String) -> Unit,
    onRemoveCartItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onSavePendingOrder: () -> Unit,
    onDismissOrderSavedNotice: () -> Unit,
    onOpenOrderDetails: (String) -> Unit,
    onCloseOrderDetails: () -> Unit,
    onEditOrder: (String) -> Unit,
    onFinishEditingOrder: () -> Unit,
    onAddProductToEditingOrder: (Product) -> Unit,
    onIncrementOrderItem: (orderId: String, itemRowId: Long) -> Unit,
    onDecrementOrderItem: (orderId: String, itemRowId: Long) -> Unit,
    onCancelOrderItem: (orderId: String, itemRowId: Long) -> Unit,
    onCancelFullOrder: (String) -> Unit,
    onCompleteOrderPayment: (PosOrder, PaymentMethod, Double?) -> Unit,
    onOpenBill: (String) -> Unit,
    onDismissStockWarning: () -> Unit,
    onCompleteSale: (PaymentMethod, Double?) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalStrings.current
    val settings = uiState.settings
    val semanticColors = LocalSemanticColors.current
    var activeTab by rememberSaveable { mutableStateOf(MarketWorkerTab.REGISTER) }
    var showCartSheet by rememberSaveable { mutableStateOf(false) }
    var showCheckoutDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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

    LaunchedEffect(uiState.orderSavedNotice) {
        if (uiState.orderSavedNotice) {
            snackbarHostState.showSnackbar(strings.orderSavedMessage)
            onDismissOrderSavedNotice()
        }
    }

    val editingOrder = uiState.editingOrder
    val selectedOrder = uiState.selectedOrder

    if (selectedOrder != null) {
        OrderDetailsScreen(
            settings = settings,
            products = uiState.products,
            order = selectedOrder,
            canEdit = true,
            snackbarHostState = snackbarHostState,
            onBack = onCloseOrderDetails,
            onEditOrder = { orderId ->
                activeTab = MarketWorkerTab.REGISTER
                onEditOrder(orderId)
            },
            onIncrementItem = onIncrementOrderItem,
            onDecrementItem = onDecrementOrderItem,
            onCancelItem = onCancelOrderItem,
            onCancelFullOrder = onCancelFullOrder,
            onCompletePayment = onCompleteOrderPayment,
            onOpenBill = onOpenBill
        )
        return
    }

    if (editingOrder != null) {
        BackHandler { onFinishEditingOrder() }
    }

    val cartQuantities = remember(uiState.cartItems, editingOrder) {
        if (editingOrder != null) {
            editingOrder.activeItems.groupBy { it.productId }
                .mapValues { (_, items) -> items.sumOf { it.quantity } }
        } else {
            uiState.cartItems.associate { it.product.id to it.quantity }
        }
    }

    val screenTitle = when (activeTab) {
        MarketWorkerTab.REGISTER -> strings.posRegister
        MarketWorkerTab.ORDERS -> strings.ordersTitle
        MarketWorkerTab.BILLS -> strings.billsTitle
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp
        val isSplitPane = maxWidth >= 960.dp

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
                if (!useRail && activeTab != MarketWorkerTab.REGISTER) {
                    MarketWorkerBottomNav(
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
                        MarketWorkerTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            val label = when (tab) {
                                MarketWorkerTab.REGISTER -> strings.posRegister
                                MarketWorkerTab.ORDERS -> strings.navOrders
                                MarketWorkerTab.BILLS -> strings.navBills
                            }
                            val icon = when (tab) {
                                MarketWorkerTab.REGISTER -> if (selected) Icons.Rounded.PointOfSale else Icons.Outlined.PointOfSale
                                MarketWorkerTab.ORDERS -> if (selected) Icons.Rounded.RestaurantMenu else Icons.Outlined.RestaurantMenu
                                MarketWorkerTab.BILLS -> if (selected) Icons.AutoMirrored.Rounded.ReceiptLong else Icons.Outlined.ReceiptLong
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = {
                                    if (tab == MarketWorkerTab.ORDERS && uiState.pendingOrdersCount > 0) {
                                        BadgedBox(
                                            badge = { Badge { Text(uiState.pendingOrdersCount.toString()) } }
                                        ) {
                                            Icon(icon, contentDescription = label)
                                        }
                                    } else {
                                        Icon(icon, contentDescription = label)
                                    }
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag("market_worker_rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                when (activeTab) {
                    MarketWorkerTab.REGISTER -> {
                        val activeCatalogAdd: (Product) -> Unit = { prod ->
                            if (editingOrder != null) {
                                onAddProductToEditingOrder(prod)
                            } else {
                                onAddProduct(prod)
                            }
                        }
                        val activeCatalogDec: (String) -> Unit = { productId ->
                            if (editingOrder != null) {
                                editingOrder.activeItems.firstOrNull { it.productId == productId }?.let {
                                    onDecrementOrderItem(editingOrder.id, it.id)
                                }
                            } else {
                                onDecrementProduct(productId)
                            }
                        }

                        if (isSplitPane) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(0.60f)
                                        .fillMaxHeight()
                                ) {
                                    if (editingOrder != null) {
                                        EditingOrderHeaderBanner(
                                            orderNumber = editingOrder.orderNumber,
                                            warningBg = semanticColors.warningContainer,
                                            warningFg = semanticColors.onWarningContainer,
                                            onDone = onFinishEditingOrder
                                        )
                                    }
                                    CatalogPane(
                                        settings = settings,
                                        searchQuery = uiState.searchQuery,
                                        selectedCategory = uiState.selectedCategory,
                                        showOutOfStockOnly = uiState.showOutOfStockOnly,
                                        venueCategories = uiState.venueCategories,
                                        products = uiState.filteredCatalogProducts,
                                        outOfStockProducts = uiState.outOfStockProducts,
                                        cartQuantities = cartQuantities,
                                        onSearchQueryChange = onSearchQueryChange,
                                        onSelectCategory = onSelectCategory,
                                        onToggleOutOfStockFilter = onToggleOutOfStockFilter,
                                        onAddProduct = activeCatalogAdd,
                                        onDecrementProduct = activeCatalogDec,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier
                                        .weight(0.40f)
                                        .fillMaxHeight()
                                        .windowInsetsPadding(WindowInsets.navigationBars)
                                ) {
                                    if (editingOrder != null) {
                                        EditingOrderSidePanel(
                                            order = editingOrder,
                                            language = settings.language,
                                            rsdPerEur = settings.rsdPerEur,
                                            onDone = onFinishEditingOrder,
                                            onPayClick = { showCheckoutDialog = true }
                                        )
                                    } else {
                                        CartPanelContent(
                                            uiState = uiState,
                                            onAddProduct = onAddProduct,
                                            onDecrementProduct = onDecrementProduct,
                                            onRemoveCartItem = onRemoveCartItem,
                                            onClearCart = onClearCart,
                                            onSavePendingOrder = onSavePendingOrder,
                                            onCheckoutClick = { showCheckoutDialog = true }
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                if (editingOrder != null) {
                                    EditingOrderHeaderBanner(
                                        orderNumber = editingOrder.orderNumber,
                                        warningBg = semanticColors.warningContainer,
                                        warningFg = semanticColors.onWarningContainer,
                                        onDone = onFinishEditingOrder,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                CatalogPane(
                                    settings = settings,
                                    searchQuery = uiState.searchQuery,
                                    selectedCategory = uiState.selectedCategory,
                                    showOutOfStockOnly = uiState.showOutOfStockOnly,
                                    venueCategories = uiState.venueCategories,
                                    products = uiState.filteredCatalogProducts,
                                    outOfStockProducts = uiState.outOfStockProducts,
                                    cartQuantities = cartQuantities,
                                    onSearchQueryChange = onSearchQueryChange,
                                    onSelectCategory = onSelectCategory,
                                    onToggleOutOfStockFilter = onToggleOutOfStockFilter,
                                    onAddProduct = activeCatalogAdd,
                                    onDecrementProduct = activeCatalogDec,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp)
                                )

                                val stickyCount = editingOrder?.activeItemCount ?: uiState.cartTotalQuantity
                                val stickyTotalRsd = editingOrder?.totalRsd ?: uiState.cartTotalRsd

                                StickyCompactCartBar(
                                    itemCount = stickyCount,
                                    totalFormatted = CurrencyFormatter.format(
                                        stickyTotalRsd,
                                        settings.language,
                                        settings.rsdPerEur
                                    ),
                                    onOpenCart = {
                                        if (editingOrder != null) {
                                            onFinishEditingOrder()
                                        } else {
                                            showCartSheet = true
                                        }
                                    },
                                    onPendingOrder = if (editingOrder != null) {
                                        onFinishEditingOrder
                                    } else {
                                        onSavePendingOrder
                                    },
                                    pendingOrderLabel = if (editingOrder != null) {
                                        strings.doneEditingBtn
                                    } else {
                                        strings.takeOrderBtn
                                    },
                                    onQuickCheckout = {
                                        if (editingOrder != null) {
                                            if (editingOrder.activeItems.isNotEmpty()) {
                                                showCheckoutDialog = true
                                            } else {
                                                onFinishEditingOrder()
                                            }
                                        } else if (uiState.cartItems.isNotEmpty()) {
                                            showCheckoutDialog = true
                                        } else {
                                            showCartSheet = true
                                        }
                                    },
                                    actionLabel = if (stickyCount > 0) strings.checkoutBtn else strings.openCartBtn,
                                    applyNavBarInsets = useRail
                                )

                                if (!useRail) {
                                    MarketWorkerBottomNav(
                                        activeTab = activeTab,
                                        pendingOrdersCount = uiState.pendingOrdersCount,
                                        onSelectTab = { activeTab = it }
                                    )
                                }
                            }
                        }
                    }

                    MarketWorkerTab.ORDERS -> OrdersListPane(
                        orders = uiState.currentWorkerOrders,
                        workers = emptyList(),
                        language = settings.language,
                        rsdPerEur = settings.rsdPerEur,
                        isAdminView = false,
                        currentWorkerId = uiState.currentUser?.id,
                        onOpenOrderDetails = onOpenOrderDetails,
                        modifier = Modifier.weight(1f)
                    )

                    MarketWorkerTab.BILLS -> BillsListPane(
                        transactions = uiState.currentWorkerTransactions,
                        currentWorkerId = uiState.currentUser?.id,
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

    if (showCartSheet && editingOrder == null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = sheetState
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
            ) {
                CartPanelContent(
                    uiState = uiState,
                    onAddProduct = onAddProduct,
                    onDecrementProduct = onDecrementProduct,
                    onRemoveCartItem = onRemoveCartItem,
                    onClearCart = onClearCart,
                    onSavePendingOrder = {
                        showCartSheet = false
                        onSavePendingOrder()
                    },
                    onCheckoutClick = {
                        showCartSheet = false
                        showCheckoutDialog = true
                    }
                )
            }
        }
    }

    val dialogTotalRsd = editingOrder?.totalRsd ?: uiState.cartTotalRsd
    val hasCheckoutItems = editingOrder?.activeItems?.isNotEmpty() ?: uiState.cartItems.isNotEmpty()
    if (showCheckoutDialog && hasCheckoutItems) {
        CheckoutPaymentDialog(
            totalRsd = dialogTotalRsd,
            language = settings.language,
            rsdPerEur = settings.rsdPerEur,
            onDismiss = { showCheckoutDialog = false },
            onConfirmSale = { method, cashRsd ->
                showCheckoutDialog = false
                if (editingOrder != null) {
                    onCompleteOrderPayment(editingOrder, method, cashRsd)
                } else {
                    onCompleteSale(method, cashRsd)
                }
            }
        )
    }
}
