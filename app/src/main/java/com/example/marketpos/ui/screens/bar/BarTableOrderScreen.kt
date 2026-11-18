package com.example.marketpos.ui.screens.bar

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.BarTable
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.Product
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.ui.components.CatalogPane
import com.example.marketpos.ui.components.CheckoutPaymentDialog
import com.example.marketpos.ui.components.StickyCompactCartBar
import com.example.marketpos.util.CurrencyFormatter
import com.example.marketpos.viewmodel.BarWorkerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BarTableOrderScreen(
    uiState: BarWorkerUiState,
    table: BarTable,
    pendingOrderId: String?,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onKeepPendingOrder: () -> Unit,
    onOpenOrderDetails: (String) -> Unit,
    onUnlockTable: () -> Unit,
    onStartNewOrder: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (ProductCategory?) -> Unit,
    onToggleOutOfStockFilter: () -> Unit,
    onAddProduct: (Product) -> Unit,
    onDecrementItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearOrder: () -> Unit,
    onCompleteSale: (PaymentMethod, Double?) -> Unit
) {
    val strings = LocalStrings.current
    val settings = uiState.settings
    var showOrderSheet by rememberSaveable { mutableStateOf(false) }
    var showCheckoutDialog by rememberSaveable { mutableStateOf(false) }
    var showCancelOrderDialog by rememberSaveable { mutableStateOf(false) }

    val orderQuantities = remember(table.orderItems) {
        table.orderItems.associate { it.productId to it.quantity }
    }
    val countdownText = CurrencyFormatter.formatCountdown(table.remainingCleanupMillis(uiState.nowMillis))

    val currentWorkerId = uiState.currentUser?.id
    val subtitleText = when {
        table.isLockedBy(currentWorkerId) -> "${table.zone} • ${strings.tableStatusLockedByYou}"
        table.isLockedByOther(currentWorkerId) -> "${table.zone} • ${
            strings.tableStatusLockedByOther(table.lockedByWorkerName ?: "")
        }"
        else -> "${table.zone} • ${strings.tableStatusFree}"
    }
    val subtitleColor = when {
        table.isLockedBy(currentWorkerId) -> MaterialTheme.colorScheme.primary
        table.isLockedByOther(currentWorkerId) -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.tableOrderTitle(table.name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.labelSmall,
                            color = subtitleColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("table_order_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                },
                actions = {
                    if (pendingOrderId != null) {
                        TextButton(
                            onClick = { onOpenOrderDetails(pendingOrderId) },
                            modifier = Modifier.testTag("table_open_order_details_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.doneEditingBtn)
                        }
                    }
                    if (table.isLockedBy(currentWorkerId) && table.orderItems.isEmpty() && !table.awaitingCleanup) {
                        TextButton(
                            onClick = onUnlockTable,
                            modifier = Modifier.testTag("unlock_empty_table_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.unlockEmptyTableBtn)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isSplitPane = maxWidth >= 840.dp

            Column(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = table.awaitingCleanup) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("table_cleanup_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CleaningServices,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = strings.tableCleanupBannerTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            Text(
                                text = strings.tableCleanupBannerSubtitle(countdownText),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onUnlockTable,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("banner_unlock_table_btn")
                                ) {
                                    Text(
                                        text = strings.tableCleanedUnlockBtn,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = onStartNewOrder,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("banner_new_guests_btn")
                                ) {
                                    Text(
                                        text = strings.newGuestsOrderBtn,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (isSplitPane) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                            cartQuantities = orderQuantities,
                            onSearchQueryChange = onSearchQueryChange,
                            onSelectCategory = onSelectCategory,
                            onToggleOutOfStockFilter = onToggleOutOfStockFilter,
                            onAddProduct = onAddProduct,
                            onDecrementProduct = onDecrementItem,
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
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            TableOrderPanelContent(
                                table = table,
                                products = uiState.products,
                                language = settings.language,
                                rsdPerEur = settings.rsdPerEur,
                                onIncrementProduct = onAddProduct,
                                onDecrementItem = onDecrementItem,
                                onRemoveItem = onRemoveItem,
                                onClearOrder = { showCancelOrderDialog = true },
                                onUnlockEmpty = onUnlockTable,
                                onKeepPendingOrder = onKeepPendingOrder,
                                onPayAndPrint = { showCheckoutDialog = true }
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        CatalogPane(
                            settings = settings,
                            searchQuery = uiState.searchQuery,
                            selectedCategory = uiState.selectedCategory,
                            showOutOfStockOnly = uiState.showOutOfStockOnly,
                            venueCategories = uiState.venueCategories,
                            products = uiState.filteredCatalogProducts,
                            outOfStockProducts = uiState.outOfStockProducts,
                            cartQuantities = orderQuantities,
                            onSearchQueryChange = onSearchQueryChange,
                            onSelectCategory = onSelectCategory,
                            onToggleOutOfStockFilter = onToggleOutOfStockFilter,
                            onAddProduct = onAddProduct,
                            onDecrementProduct = onDecrementItem,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        )

                        StickyCompactCartBar(
                            itemCount = table.itemCount,
                            totalFormatted = CurrencyFormatter.format(
                                table.orderTotalRsd,
                                settings.language,
                                settings.rsdPerEur
                            ),
                            onOpenCart = { showOrderSheet = true },
                            onPendingOrder = onKeepPendingOrder,
                            pendingOrderLabel = strings.takeOrderBtn,
                            onQuickCheckout = {
                                if (table.orderItems.isNotEmpty()) {
                                    showCheckoutDialog = true
                                } else {
                                    showOrderSheet = true
                                }
                            },
                            actionLabel = if (table.orderItems.isNotEmpty()) strings.checkoutBtn else strings.openCartBtn
                        )
                    }
                }
            }
        }
    }

    if (showOrderSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showOrderSheet = false },
            sheetState = sheetState
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
            ) {
                TableOrderPanelContent(
                    table = table,
                    products = uiState.products,
                    language = settings.language,
                    rsdPerEur = settings.rsdPerEur,
                    onIncrementProduct = onAddProduct,
                    onDecrementItem = onDecrementItem,
                    onRemoveItem = onRemoveItem,
                    onClearOrder = {
                        showOrderSheet = false
                        showCancelOrderDialog = true
                    },
                    onUnlockEmpty = {
                        showOrderSheet = false
                        onUnlockTable()
                    },
                    onKeepPendingOrder = {
                        showOrderSheet = false
                        onKeepPendingOrder()
                    },
                    onPayAndPrint = {
                        showOrderSheet = false
                        showCheckoutDialog = true
                    }
                )
            }
        }
    }

    if (showCancelOrderDialog) {
        AlertDialog(
            onDismissRequest = { showCancelOrderDialog = false },
            title = { Text(strings.cancelOrderConfirmTitle) },
            text = { Text(strings.cancelOrderConfirmMessage(table.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelOrderDialog = false
                        onClearOrder()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(strings.confirmCancelBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelOrderDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showCheckoutDialog && table.orderItems.isNotEmpty()) {
        CheckoutPaymentDialog(
            totalRsd = table.orderTotalRsd,
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
