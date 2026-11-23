package com.fourpit.marketpos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.PosSettings
import com.fourpit.marketpos.ui.components.EmptyStatePlaceholder
import com.fourpit.marketpos.ui.components.MonoPriceText
import com.fourpit.marketpos.ui.components.formatPosDateTime
import com.fourpit.marketpos.ui.theme.JetBrainsMonoFamily
import com.fourpit.marketpos.util.CurrencyFormatter
import com.fourpit.marketpos.viewmodel.ReceiptViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    viewModel: ReceiptViewModel,
    settings: PosSettings,
    onDone: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.printStatusMessage) {
        uiState.printStatusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearPrintStatus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.receiptTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDone, modifier = Modifier.testTag("receipt_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        }
    ) { innerPadding ->
        val order = uiState.order
        if (order == null) {
            EmptyStatePlaceholder(
                icon = Icons.Default.ReceiptLong,
                title = strings.noDataYet,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("receipt_paper_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = settings.businessName.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (order.locationName.isNotBlank()) {
                            Text(
                                text = order.locationName,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        ReceiptMetaRow(strings.receiptNumber, order.receiptNumber)
                        ReceiptMetaRow(strings.dateAndTime, formatPosDateTime(order.closedAtMillis ?: order.createdAtMillis))
                        ReceiptMetaRow(strings.cashier, order.workerName)
                        order.tableLabel?.let { ReceiptMetaRow(strings.tables, it) }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        order.items.forEach { item ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = item.productName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMonoFamily),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${item.quantity} x ${CurrencyFormatter.format(item.unitPriceRsd, settings.currency, settings.eurExchangeRate)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    MonoPriceText(
                                        text = CurrencyFormatter.format(item.lineTotalRsd, settings.currency, settings.eurExchangeRate),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                        ReceiptMetaRow(
                            "${strings.tax} (${settings.taxRatePercent.toInt()}%)",
                            CurrencyFormatter.format(order.taxAmountRsd, settings.currency, settings.eurExchangeRate)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.total.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFamily),
                                fontWeight = FontWeight.Bold
                            )
                            MonoPriceText(
                                text = CurrencyFormatter.format(order.totalRsd, settings.currency, settings.eurExchangeRate),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        order.paymentMethod?.let { method ->
                            ReceiptMetaRow(strings.paymentMethod, method.name)
                        }
                        if (order.changeRsd > 0.0) {
                            ReceiptMetaRow(
                                strings.cashReceived,
                                CurrencyFormatter.format(order.receivedRsd, settings.currency, settings.eurExchangeRate)
                            )
                            ReceiptMetaRow(
                                strings.changeDue,
                                CurrencyFormatter.format(order.changeRsd, settings.currency, settings.eurExchangeRate)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.printReceipt(context, settings) },
                        enabled = !uiState.isPrinting,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("print_receipt_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text(if (uiState.isPrinting) strings.printing else strings.printReceipt)
                    }
                    Button(
                        onClick = onDone,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("new_sale_button")
                    ) {
                        Text(strings.newSale)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptMetaRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
            fontWeight = FontWeight.Medium
        )
    }
}
