package com.example.marketpos.ui.screens.orders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.PosOrderItem
import com.example.marketpos.model.Product
import com.example.marketpos.ui.components.CheckoutPaymentDialog
import com.example.marketpos.ui.components.TotalsSummaryBlock
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.ui.theme.LocalSemanticColors
import com.example.marketpos.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrderDetailsScreen(
    settings: AppSettings,
    products: List<Product>,
    order: PosOrder,
    canEdit: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEditOrder: (String) -> Unit,
    onIncrementItem: (orderId: String, itemRowId: Long) -> Unit,
    onDecrementItem: (orderId: String, itemRowId: Long) -> Unit,
    onCancelItem: (orderId: String, itemRowId: Long) -> Unit,
    onCancelFullOrder: (String) -> Unit,
    onCompletePayment: (PosOrder, PaymentMethod, Double?) -> Unit,
    onOpenBill: (String) -> Unit
) {
    BackHandler { onBack() }

    val strings = LocalStrings.current
    val semanticColors = LocalSemanticColors.current
    val productMap = remember(products) { products.associateBy { it.id } }
    var showCancelConfirm by rememberSaveable { mutableStateOf(false) }
    var showPaymentDialog by rememberSaveable { mutableStateOf(false) }

    val (badgeBg, badgeFg, statusText) = when (order.status) {
        OrderStatus.PENDING -> Triple(
            semanticColors.warningContainer,
            semanticColors.onWarningContainer,
            strings.orderStatusPending
        )
        OrderStatus.FINISHED -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            strings.orderStatusFinished
        )
        OrderStatus.CANCELLED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            strings.orderStatusCancelled
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.orderDetailsTitle(order.orderNumber),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${order.tableName ?: strings.counterOrderLabel} • ${order.workerName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("order_details_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeBg,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = badgeFg,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        bottomBar = {
            when {
                order.isEditable && canEdit -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 4.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onEditOrder(order.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("order_details_edit_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.addItemsBtn,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = { showCancelConfirm = true },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("order_details_cancel_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Cancel,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.cancelOrderBtn,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (order.activeItems.isNotEmpty()) {
                                Button(
                                    onClick = { showPaymentDialog = true },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("order_details_pay_btn")
                                ) {
                                    Text(
                                        text = "${strings.payAndFinishOrderBtn} • ${
                                            CurrencyFormatter.format(order.totalRsd, settings.language, settings.rsdPerEur)
                                        }",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                order.status == OrderStatus.FINISHED && order.transactionId != null -> {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 4.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Button(
                                onClick = { onOpenBill(order.transactionId) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("order_details_view_bill_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.viewBillBtn)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.dateTimeLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.formatDateTime(order.updatedAtMillis, settings.language),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily)
                            )
                        }
                        if (order.tableName != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.receiptTableLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = order.tableName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        if (order.status == OrderStatus.CANCELLED) {
                            val canceledTotal = order.items.sumOf { it.grossLineTotalRsd } * (1.0 + CurrencyFormatter.VAT_RATE)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.orderStatusCancelled,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = CurrencyFormatter.format(canceledTotal, settings.language, settings.rsdPerEur),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            TotalsSummaryBlock(
                                subtotal = CurrencyFormatter.format(order.subtotalRsd, settings.language, settings.rsdPerEur),
                                pdv = CurrencyFormatter.format(order.taxRsd, settings.language, settings.rsdPerEur),
                                total = CurrencyFormatter.format(order.totalRsd, settings.language, settings.rsdPerEur)
                            )
                        }
                    }
                }
            }

            if (order.activeItems.isNotEmpty()) {
                item {
                    Text(
                        text = "${strings.activeItemsSection} (${order.activeItemCount})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(order.activeItems, key = { "active_${it.id}_${it.productId}" }) { item ->
                    val prod = productMap[item.productId]
                    val canIncrement = prod == null || item.quantity < prod.stockQuantity
                    OrderDetailItemRow(
                        item = item,
                        editable = order.isEditable && canEdit,
                        canIncrement = canIncrement,
                        language = settings.language,
                        rsdPerEur = settings.rsdPerEur,
                        onIncrement = { onIncrementItem(order.id, item.id) },
                        onDecrement = { onDecrementItem(order.id, item.id) },
                        onCancelItem = { onCancelItem(order.id, item.id) }
                    )
                }
            }

            if (order.cancelledItems.isNotEmpty()) {
                item {
                    Text(
                        text = "${strings.cancelledItemsSection} (${order.cancelledItemCount})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                items(order.cancelledItems, key = { "cancelled_${it.id}_${it.productId}" }) { item ->
                    OrderDetailItemRow(
                        item = item,
                        editable = false,
                        canIncrement = false,
                        language = settings.language,
                        rsdPerEur = settings.rsdPerEur,
                        onIncrement = {},
                        onDecrement = {},
                        onCancelItem = {}
                    )
                }
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text(strings.cancelOrderConfirmTitle) },
            text = { Text(strings.cancelOrderConfirmMessage(order.orderNumber)) },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirm = false
                        onCancelFullOrder(order.id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_cancel_order_btn")
                ) {
                    Text(strings.confirmCancelBtn)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showPaymentDialog && order.activeItems.isNotEmpty()) {
        CheckoutPaymentDialog(
            totalRsd = order.totalRsd,
            language = settings.language,
            rsdPerEur = settings.rsdPerEur,
            onDismiss = { showPaymentDialog = false },
            onConfirmSale = { method, cashRsd ->
                showPaymentDialog = false
                onCompletePayment(order, method, cashRsd)
            }
        )
    }
}
