package com.fourpit.marketpos.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.CurrencyCode
import com.fourpit.marketpos.model.PaymentMethod
import com.fourpit.marketpos.util.CurrencyFormatter
import kotlin.math.ceil

@Composable
fun CheckoutPaymentDialog(
    totalRsd: Double,
    taxRatePercent: Double,
    currency: CurrencyCode,
    eurRate: Double,
    onDismiss: () -> Unit,
    onConfirm: (PaymentMethod, Double) -> Unit
) {
    val strings = LocalAppStrings.current
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var cashInputText by remember { mutableStateOf(ceil(totalRsd).toInt().toString()) }

    val receivedRsd = if (paymentMethod == PaymentMethod.CARD) {
        totalRsd
    } else {
        cashInputText.replace(",", ".").toDoubleOrNull() ?: 0.0
    }
    val changeRsd = (receivedRsd - totalRsd).coerceAtLeast(0.0)
    val canComplete = paymentMethod == PaymentMethod.CARD || receivedRsd >= totalRsd - 0.01

    val quickAmounts = remember(totalRsd) {
        listOf(
            ceil(totalRsd).toInt(),
            ((ceil(totalRsd / 500.0) * 500).toInt()).coerceAtLeast(500),
            ((ceil(totalRsd / 1000.0) * 1000).toInt()).coerceAtLeast(1000),
            ((ceil(totalRsd / 2000.0) * 2000).toInt()).coerceAtLeast(2000)
        ).distinct().take(4)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.checkout, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = strings.total,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        MonoPriceText(
                            text = CurrencyFormatter.format(totalRsd, currency, eurRate),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (taxRatePercent > 0.0) {
                            val includedVat = totalRsd - (totalRsd / (1.0 + taxRatePercent / 100.0))
                            Text(
                                text = "${strings.tax} (${taxRatePercent.toInt()}%): ${CurrencyFormatter.format(includedVat, currency, eurRate)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Text(strings.paymentMethod, style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = paymentMethod == PaymentMethod.CASH,
                        onClick = { paymentMethod = PaymentMethod.CASH },
                        label = { Text(strings.cash) },
                        leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payment_cash_chip")
                    )
                    FilterChip(
                        selected = paymentMethod == PaymentMethod.CARD,
                        onClick = { paymentMethod = PaymentMethod.CARD },
                        label = { Text(strings.card) },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("payment_card_chip")
                    )
                }

                if (paymentMethod == PaymentMethod.CASH) {
                    OutlinedTextField(
                        value = cashInputText,
                        onValueChange = { cashInputText = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                        label = { Text("${strings.cashReceived} (RSD)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cash_received_input")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        quickAmounts.forEach { amt ->
                            OutlinedButton(
                                onClick = { cashInputText = amt.toString() },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (amt == quickAmounts.first()) strings.exactAmount else amt.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.changeDue, style = MaterialTheme.typography.titleSmall)
                        MonoPriceText(
                            text = CurrencyFormatter.format(changeRsd, currency, eurRate),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(paymentMethod, receivedRsd) },
                enabled = canComplete,
                modifier = Modifier.testTag("confirm_checkout_button")
            ) {
                Text(strings.completeSale)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
