package com.example.marketpos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.rounded.PointOfSale
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
import com.example.marketpos.model.Product
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.ui.components.CatalogPane
import com.example.marketpos.ui.components.CheckoutPaymentDialog
import com.example.marketpos.ui.components.PosTopAppBar
import com.example.marketpos.ui.components.StickyCompactCartBar
import com.example.marketpos.ui.screens.orders.BillsListPane
import com.example.marketpos.ui.screens.worker.CartPanelContent
import com.example.marketpos.ui.screens.worker.MarketWorkerBottomNav
import com.example.marketpos.ui.screens.worker.MarketWorkerTab
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
    onOpenBill: (String) -> Unit,
    onDismissStockWarning: () -> Unit,
    onCompleteSale: (PaymentMethod, Double?) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalStrings.current
    val settings = uiState.settings
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

    val cartQuantities = remember(uiState.cartItems) {
        uiState.cartItems.associate { it.product.id to it.quantity }
    }

    val screenTitle = when (activeTab) {
        MarketWorkerTab.REGISTER -> strings.posRegister
        MarketWorkerTab.BILLS -> strings.billsTitle
    }

    val stickyCount = uiState.cartTotalQuantity
    val stickyTotalFormatted = CurrencyFormatter.format(
        uiState.cartTotalRsd,
        settings.language,
        settings.rsdPerEur
    )
    val onStickyQuickCheckout: () -> Unit = {
        if (uiState.cartItems.isNotEmpty()) {
            showCheckoutDialog = true
        } else {
            showCartSheet = true
        }
    }
    val stickyActionLabel = if (stickyCount > 0) strings.checkoutBtn else strings.openCartBtn

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
                if (!useRail) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (activeTab == MarketWorkerTab.REGISTER) {
                            StickyCompactCartBar(
                                itemCount = stickyCount,
                                totalFormatted = stickyTotalFormatted,
                                onOpenCart = { showCartSheet = true },
                                onQuickCheckout = onStickyQuickCheckout,
                                actionLabel = stickyActionLabel,
                                applyNavBarInsets = false
                            )
                        }
                        MarketWorkerBottomNav(
                            activeTab = activeTab,
                            onSelectTab = { activeTab = it }
                        )
                    }
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
                                MarketWorkerTab.BILLS -> strings.navBills
                            }
                            val icon = when (tab) {
                                MarketWorkerTab.REGISTER -> if (selected) Icons.Rounded.PointOfSale else Icons.Outlined.PointOfSale
                                MarketWorkerTab.BILLS -> if (selected) Icons.AutoMirrored.Rounded.ReceiptLong else Icons.AutoMirrored.Outlined.ReceiptLong
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("market_worker_rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                when (activeTab) {
                    MarketWorkerTab.REGISTER -> {
                        if (isSplitPane) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
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
                                    onAddProduct = onAddProduct,
                                    onDecrementProduct = onDecrementProduct,
                                    modifier = Modifier
                                        .weight(0.60f)
                                        .fillMaxHeight()
                                )

                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier
                                        .weight(0.40f)
                                        .fillMaxHeight()
                                ) {
                                    CartPanelContent(
                                        uiState = uiState,
                                        onAddProduct = onAddProduct,
                                        onDecrementProduct = onDecrementProduct,
                                        onRemoveCartItem = onRemoveCartItem,
                                        onClearCart = onClearCart,
                                        onCheckoutClick = { showCheckoutDialog = true }
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
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
                                    onAddProduct = onAddProduct,
                                    onDecrementProduct = onDecrementProduct,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 12.dp)
                                )

                                if (useRail) {
                                    StickyCompactCartBar(
                                        itemCount = stickyCount,
                                        totalFormatted = stickyTotalFormatted,
                                        onOpenCart = { showCartSheet = true },
                                        onQuickCheckout = onStickyQuickCheckout,
                                        actionLabel = stickyActionLabel,
                                        applyNavBarInsets = false
                                    )
                                }
                            }
                        }
                    }

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

    if (showCartSheet) {
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
                    onCheckoutClick = {
                        showCartSheet = false
                        showCheckoutDialog = true
                    }
                )
            }
        }
    }

    if (showCheckoutDialog && uiState.cartItems.isNotEmpty()) {
        CheckoutPaymentDialog(
            totalRsd = uiState.cartTotalRsd,
            language = settings.language,
            rsdPerEur = settings.rsdPerEur,
            onDismiss = { showCheckoutDialog = false },
            onConfirmSale = { method, cashRsd ->
                showCheckoutDialog = false
                onCompleteSale(method, cashRsd)
            }
        )
    }
}
