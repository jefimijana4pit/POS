package com.example.marketpos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddShoppingCart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.TableRestaurant
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PrintResult
import com.example.marketpos.model.PrinterType
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.ui.components.EmptyStateView
import com.example.marketpos.ui.components.PrintFeedbackDialog
import com.example.marketpos.ui.theme.JetBrainsMonoFamily
import com.example.marketpos.util.CurrencyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    transaction: SaleTransaction?,
    settings: AppSettings,
    printFeedback: PrintResult?,
    onPrintReceipt: () -> Unit,
    onDismissPrintFeedback: () -> Unit,
    onBluetoothPermissionsResult: (Boolean) -> Unit,
    onNewSale: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current

    PrintFeedbackDialog(
        printResult = printFeedback,
        onDismiss = onDismissPrintFeedback,
        onBluetoothPermissionsResult = onBluetoothPermissionsResult
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(strings.receiptTitle) },
                navigationIcon = {
                    IconButton(
                        onClick = onNewSale,
                        modifier = Modifier.testTag("receipt_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            )
        },
        bottomBar = {
            if (transaction != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onPrintReceipt,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("print_receipt_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Print,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.printReceiptBtn)
                        }

                        Button(
                            onClick = onNewSale,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("new_sale_btn")
                        ) {
                            Icon(
                                imageVector = if (transaction.tableName != null) {
                                    Icons.Rounded.TableRestaurant
                                } else {
                                    Icons.Rounded.AddShoppingCart
                                },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (transaction.tableName != null) {
                                    strings.backToTableBtn
                                } else {
                                    strings.newSaleBtn
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (transaction == null) {
            EmptyStateView(
                icon = Icons.Rounded.ReceiptLong,
                title = strings.noTransactionsFound,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${transaction.receiptNumber} • ${
                            if (settings.printerType == PrinterType.ANDROID_SYSTEM) {
                                strings.printerSystemTitle
                            } else {
                                settings.selectedPrinterName ?: strings.printerThermalTitle
                            }
                        }",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                ThermalReceiptCard(
                    transaction = transaction,
                    settings = settings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                )
            }
        }
    }
}

@Composable
private fun ThermalReceiptCard(
    transaction: SaleTransaction,
    settings: AppSettings,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val lang = settings.language
    val rate = settings.rsdPerEur
    val paperColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val edgeColor = MaterialTheme.colorScheme.background

    Column(modifier = modifier) {
        Surface(
            color = paperColor,
            tonalElevation = 2.dp,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.storeNameHeader,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = strings.storeAddressHeader,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                ReceiptMetaRow(strings.receiptNumberLabel, transaction.receiptNumber)
                ReceiptMetaRow(
                    strings.dateTimeLabel,
                    CurrencyFormatter.formatDateTime(transaction.timestampMillis, lang)
                )
                ReceiptMetaRow(strings.cashierLabel, transaction.workerName)
                transaction.tableName?.let { tableName ->
                    ReceiptMetaRow(strings.receiptTableLabel, tableName)
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                transaction.items.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = item.displayName(lang),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "  ${item.quantity} × ${
                                    CurrencyFormatter.format(item.unitPriceRsd, lang, rate)
                                }",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.format(item.lineTotalRsd, lang, rate),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                ReceiptMetaRow(
                    strings.subtotalLabel,
                    CurrencyFormatter.format(transaction.subtotalRsd, lang, rate)
                )
                ReceiptMetaRow(
                    strings.pdvLabel,
                    CurrencyFormatter.format(transaction.taxRsd, lang, rate)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.totalLabel.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = CurrencyFormatter.format(transaction.totalRsd, lang, rate),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                ReceiptMetaRow(
                    strings.paymentUsedLabel,
                    strings.paymentMethodName(transaction.paymentMethod)
                )
                if (transaction.paymentMethod == PaymentMethod.CASH && transaction.cashReceivedRsd != null) {
                    ReceiptMetaRow(
                        strings.cashLabel,
                        CurrencyFormatter.format(transaction.cashReceivedRsd, lang, rate)
                    )
                    ReceiptMetaRow(
                        strings.changeDueLabel,
                        CurrencyFormatter.format(transaction.changeRsd ?: 0.0, lang, rate)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Text(
                    text = strings.thankYouFooter,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Perforated receipt saw-tooth bottom edge drawn natively in Compose Canvas
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
        ) {
            drawRect(color = paperColor)
            val teethCount = 24
            val toothWidth = size.width / teethCount
            val path = Path().apply {
                moveTo(0f, size.height)
                for (i in 0 until teethCount) {
                    val startX = i * toothWidth
                    lineTo(startX + toothWidth / 2f, 0f)
                    lineTo(startX + toothWidth, size.height)
                }
                close()
            }
            drawPath(path = path, color = edgeColor)
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
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Medium
            )
        )
    }
}
