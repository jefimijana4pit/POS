package com.fourpit.marketpos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.PosSettings
import com.fourpit.marketpos.ui.components.CheckoutPaymentDialog
import com.fourpit.marketpos.ui.components.MonoPriceText
import com.fourpit.marketpos.ui.components.PosCatalogPane
import com.fourpit.marketpos.ui.screens.orders.BillsListPane
import com.fourpit.marketpos.ui.screens.worker.MarketCartPanel
import com.fourpit.marketpos.util.CurrencyFormatter
import com.fourpit.marketpos.viewmodel.MarketWorkerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerPosScreen(
    viewModel: MarketWorkerViewModel,
    workerName: String,
    settings: PosSettings,
    onOrderCompleted: (Long) -> Unit,
    onOpenOrderDetails: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCartSheet by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    val cartQuantities = remember(uiState.cart) {
        uiState.cart.associate { it.product.id to it.quantity }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.location?.name ?: strings.appName,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${strings.cashier}: $workerName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("worker_settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = strings.settings)
                    }
                    IconButton(onClick = onLogout, modifier = Modifier.testTag("worker_logout_button")) {
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
                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = strings.posRegister) },
                    label = { Text(strings.posRegister) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = strings.recentBills) },
                    label = { Text(strings.recentBills) }
                )
            }
        }
    ) { innerPadding ->
        if (selectedTab == 1) {
            BillsListPane(
                bills = uiState.paidBills,
                currency = settings.currency,
                eurRate = settings.eurExchangeRate,
                onBillClick = onOpenOrderDetails,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                val isWide = maxWidth >= 720.dp
                if (isWide) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        PosCatalogPane(
                            products = uiState.filteredProducts,
                            categories = uiState.categories,
                            selectedCategory = uiState.selectedCategory,
                            searchQuery = uiState.searchQuery,
                            currency = settings.currency,
                            eurRate = settings.eurExchangeRate,
                            cartQuantities = cartQuantities,
                            onSearchChange = viewModel::updateSearchQuery,
                            onSelectCategory = viewModel::selectCategory,
                            onProductClick = viewModel::addToCart,
                            modifier = Modifier.weight(1f)
                        )
                        MarketCartPanel(
                            cart = uiState.cart,
                            totalRsd = uiState.cartTotalRsd,
                            currency = settings.currency,
                            eurRate = settings.eurExchangeRate,
                            onIncrement = { viewModel.addToCart(it.product) },
                            onDecrement = viewModel::decrementFromCart,
                            onRemove = viewModel::removeFromCart,
                            onClear = viewModel::clearCart,
                            onCheckoutClick = { showCheckoutDialog = true },
                            modifier = Modifier
                                .width(340.dp)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        PosCatalogPane(
                            products = uiState.filteredProducts,
                            categories = uiState.categories,
                            selectedCategory = uiState.selectedCategory,
                            searchQuery = uiState.searchQuery,
                            currency = settings.currency,
                            eurRate = settings.eurExchangeRate,
                            cartQuantities = cartQuantities,
                            onSearchChange = viewModel::updateSearchQuery,
                            onSelectCategory = viewModel::selectCategory,
                            onProductClick = viewModel::addToCart,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            onClick = { showCartSheet = true },
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("compact_cart_summary_bar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (uiState.cartItemCount > 0) {
                                                Badge { Text(uiState.cartItemCount.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = strings.cart)
                                    }
                                    Text(
                                        text = "${strings.cart} (${uiState.cartItemCount})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                MonoPriceText(
                                    text = CurrencyFormatter.format(uiState.cartTotalRsd, settings.currency, settings.eurExchangeRate),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCartSheet) {
        ModalBottomSheet(onDismissRequest = { showCartSheet = false }) {
            MarketCartPanel(
                cart = uiState.cart,
                totalRsd = uiState.cartTotalRsd,
                currency = settings.currency,
                eurRate = settings.eurExchangeRate,
                onIncrement = { viewModel.addToCart(it.product) },
                onDecrement = viewModel::decrementFromCart,
                onRemove = viewModel::removeFromCart,
                onClear = viewModel::clearCart,
                onCheckoutClick = {
                    showCartSheet = false
                    showCheckoutDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
            )
        }
    }

    if (showCheckoutDialog) {
        CheckoutPaymentDialog(
            totalRsd = uiState.cartTotalRsd,
            taxRatePercent = settings.taxRatePercent,
            currency = settings.currency,
            eurRate = settings.eurExchangeRate,
            onDismiss = { showCheckoutDialog = false },
            onConfirm = { method, received ->
                showCheckoutDialog = false
                viewModel.completeSale(
                    workerName = workerName,
                    paymentMethod = method,
                    receivedRsd = received,
                    taxRatePercent = settings.taxRatePercent,
                    onOrderCompleted = onOrderCompleted
                )
            }
        )
    }
}
