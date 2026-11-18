package com.example.marketpos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.util.CurrencyFormatter
import java.util.Locale

@Composable
internal fun StickyCompactCartBar(
    itemCount: Int,
    totalFormatted: String,
    onOpenCart: () -> Unit,
    onQuickCheckout: () -> Unit,
    actionLabel: String? = null,
    onPendingOrder: (() -> Unit)? = null,
    pendingOrderLabel: String? = null,
    applyNavBarInsets: Boolean = true
) {
    val strings = LocalStrings.current
    val showTwoOptions = itemCount > 0 && onPendingOrder != null

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (applyNavBarInsets) Modifier.windowInsetsPadding(WindowInsets.navigationBars) else Modifier)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenCart)
                        .padding(4.dp)
                        .testTag("compact_cart_summary_tap")
                ) {
                    BadgedBox(
                        badge = {
                            if (itemCount > 0) {
                                Badge {
                                    Text(itemCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ShoppingCart,
                            contentDescription = strings.cartTitle,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.itemsCount(itemCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = totalFormatted,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                if (!showTwoOptions) {
                    Button(
                        onClick = onQuickCheckout,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("compact_checkout_btn")
                    ) {
                        Text(
                            text = actionLabel ?: if (itemCount > 0) strings.checkoutBtn else strings.openCartBtn,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (showTwoOptions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onPendingOrder?.invoke() },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("compact_pending_order_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pendingOrderLabel ?: strings.takeOrderBtn,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Button(
                        onClick = onQuickCheckout,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("compact_checkout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = actionLabel ?: strings.checkoutBtn,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TotalsSummaryBlock(
    subtotal: String,
    pdv: String,
    total: String
) {
    val strings = LocalStrings.current

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.subtotalLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtotal,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.pdvLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = pdv,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.totalLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = total,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CheckoutPaymentDialog(
    totalRsd: Double,
    language: AppLanguage,
    rsdPerEur: Double,
    onDismiss: () -> Unit,
    onConfirmSale: (PaymentMethod, Double?) -> Unit
) {
    val strings = LocalStrings.current
    var paymentMethod by rememberSaveable { mutableStateOf(PaymentMethod.CASH) }
    val totalInActive = CurrencyFormatter.convertFromRsd(totalRsd, language, rsdPerEur)
    var cashInputText by rememberSaveable {
        mutableStateOf(String.format(Locale.US, "%.2f", totalInActive))
    }

    val parsedCashActive = cashInputText.replace(',', '.').toDoubleOrNull()
    val parsedCashRsd = parsedCashActive?.let {
        CurrencyFormatter.convertToRsd(it, language, rsdPerEur)
    }
    val isCashValid = paymentMethod == PaymentMethod.CARD ||
        (parsedCashRsd != null && parsedCashRsd + 0.01 >= totalRsd)
    val changeRsd = if (parsedCashRsd != null && parsedCashRsd > totalRsd) {
        parsedCashRsd - totalRsd
    } else {
        0.0
    }

    val quickAmounts = remember(language, totalInActive) {
        if (language == AppLanguage.SERBIAN_LATIN) {
            listOf(500.0, 1000.0, 2000.0, 5000.0)
        } else {
            listOf(10.0, 20.0, 50.0, 100.0)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.paymentTitle) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.amountDue,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = CurrencyFormatter.format(totalRsd, language, rsdPerEur),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = strings.paymentMethodLabel,
                    style = MaterialTheme.typography.labelLarge
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = paymentMethod == PaymentMethod.CASH,
                        onClick = { paymentMethod = PaymentMethod.CASH },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(Icons.Rounded.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.testTag("payment_cash_btn")
                    ) {
                        Text(strings.cashLabel)
                    }
                    SegmentedButton(
                        selected = paymentMethod == PaymentMethod.CARD,
                        onClick = { paymentMethod = PaymentMethod.CARD },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(Icons.Rounded.CreditCard, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.testTag("payment_card_btn")
                    ) {
                        Text(strings.cardLabel)
                    }
                }

                AnimatedVisibility(visible = paymentMethod == PaymentMethod.CASH) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = cashInputText,
                            onValueChange = { cashInputText = it },
                            label = { Text(strings.cashReceivedLabel) },
                            singleLine = true,
                            isError = !isCashValid,
                            supportingText = if (!isCashValid) {
                                { Text(strings.insufficientCashError) }
                            } else {
                                null
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cash_received_input")
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                AssistChip(
                                    onClick = {
                                        cashInputText = String.format(Locale.US, "%.2f", totalInActive)
                                    },
                                    label = { Text(strings.exactAmount) }
                                )
                            }
                            items(quickAmounts) { preset ->
                                AssistChip(
                                    onClick = {
                                        cashInputText = String.format(Locale.US, "%.2f", preset)
                                    },
                                    label = {
                                        Text(
                                            if (language == AppLanguage.SERBIAN_LATIN) {
                                                "${preset.toInt()} RSD"
                                            } else {
                                                "€${preset.toInt()}"
                                            }
                                        )
                                    }
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.changeDueLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = CurrencyFormatter.format(changeRsd, language, rsdPerEur),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = paymentMethod == PaymentMethod.CARD) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = strings.cardReadyNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmSale(
                        paymentMethod,
                        if (paymentMethod == PaymentMethod.CASH) parsedCashRsd else null
                    )
                },
                enabled = isCashValid,
                modifier = Modifier.testTag("confirm_sale_btn")
            ) {
                Text(strings.completeSaleBtn)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
