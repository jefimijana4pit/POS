package com.fourpit.marketpos.ui.screens.bar

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.fourpit.marketpos.util.CurrencyFormatter
import com.fourpit.marketpos.viewmodel.BarWorkerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarTableOrderScreen(
    orderId: Long,
    viewModel: BarWorkerViewModel,
    workerName: String,
    settings: PosSettings,
    onSettled: (Long) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val order by remember(orderId) { viewModel.observeOrder(orderId) }
        .collectAsStateWithLifecycle(initialValue = null)

    var showOrderSheet by remember { mutableStateOf(false) }
    var showSettleDialog by remember { mutableStateOf(false) }

    val orderQuantities = remember(order) {
        order?.items?.associate { it.productId to it.quantity } ?: emptyMap()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = order?.tableLabel ?: strings.orderDetails,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = order?.receiptNumber ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("bar_order_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        }
    ) { innerPadding ->
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
                        cartQuantities = orderQuantities,
                        onSearchChange = viewModel::updateSearchQuery,
                        onSelectCategory = viewModel::selectCategory,
                        onProductClick = { product -> viewModel.addProductToOrder(orderId, product) },
                        modifier = Modifier.weight(1f)
                    )
                    BarTableOrderPanel(
                        order = order,
                        currency = settings.currency,
                        eurRate = settings.eurExchangeRate,
                        onChangeQuantity = { itemId, qty -> viewModel.updateItemQuantity(orderId, itemId, qty) },
                        onSettleClick = { showSettleDialog = true },
                        onCancelOrderClick = {
                            viewModel.cancelOrder(orderId, workerName, onCancelled = onBack)
                        },
                        modifier = Modifier
                            .width(350.dp)
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
                        cartQuantities = orderQuantities,
                        onSearchChange = viewModel::updateSearchQuery,
                        onSelectCategory = viewModel::selectCategory,
                        onProductClick = { product -> viewModel.addProductToOrder(orderId, product) },
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        onClick = { showOrderSheet = true },
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bar_order_summary_bar")
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
                                val count = order?.itemCount ?: 0
                                BadgedBox(
                                    badge = { if (count > 0) Badge { Text(count.toString()) } }
                                ) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null)
                                }
                                Text(
                                    text = "${order?.tableLabel ?: strings.orderDetails} ($count)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            MonoPriceText(
                                text = CurrencyFormatter.format(order?.totalRsd ?: 0.0, settings.currency, settings.eurExchangeRate),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showOrderSheet) {
        ModalBottomSheet(onDismissRequest = { showOrderSheet = false }) {
            BarTableOrderPanel(
                order = order,
                currency = settings.currency,
                eurRate = settings.eurExchangeRate,
                onChangeQuantity = { itemId, qty -> viewModel.updateItemQuantity(orderId, itemId, qty) },
                onSettleClick = {
                    showOrderSheet = false
                    showSettleDialog = true
                },
                onCancelOrderClick = {
                    showOrderSheet = false
                    viewModel.cancelOrder(orderId, workerName, onCancelled = onBack)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
            )
        }
    }

    if (showSettleDialog) {
        CheckoutPaymentDialog(
            totalRsd = order?.totalRsd ?: 0.0,
            taxRatePercent = settings.taxRatePercent,
            currency = settings.currency,
            eurRate = settings.eurExchangeRate,
            onDismiss = { showSettleDialog = false },
            onConfirm = { method, received ->
                showSettleDialog = false
                viewModel.settleOrder(
                    orderId = orderId,
                    paymentMethod = method,
                    receivedRsd = received,
                    taxRatePercent = settings.taxRatePercent,
                    workerName = workerName,
                    onSettled = onSettled
                )
            }
        )
    }
}
