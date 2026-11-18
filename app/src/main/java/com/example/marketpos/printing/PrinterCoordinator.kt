package com.example.marketpos.printing

import android.content.Context
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.PairedPrinterDevice
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PrintResult
import com.example.marketpos.model.PrinterType
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.model.SaleLineItem
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.util.CurrencyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PrinterCoordinator(
    private val appContext: Context,
    private val appScope: CoroutineScope,
    private val systemPrinter: ReceiptPrinter = AndroidSystemPrinter(),
    private val thermalPrinter: BluetoothThermalPrinter = BluetoothThermalPrinter()
) {
    private val _pairedPrinters = MutableStateFlow<List<PairedPrinterDevice>>(emptyList())
    val pairedPrinters: StateFlow<List<PairedPrinterDevice>> = _pairedPrinters.asStateFlow()

    private val _hasBluetoothPermission = MutableStateFlow(false)
    val hasBluetoothPermission: StateFlow<Boolean> = _hasBluetoothPermission.asStateFlow()

    private val _printFeedback = MutableStateFlow<PrintResult?>(null)
    val printFeedback: StateFlow<PrintResult?> = _printFeedback.asStateFlow()

    init {
        refreshBluetoothPrinters(appContext)
    }

    fun refreshBluetoothPrinters(context: Context = appContext) {
        _hasBluetoothPermission.value = thermalPrinter.hasBluetoothPermissions(context)
        _pairedPrinters.value = thermalPrinter.getAvailablePrinters(context)
    }

    fun printTransaction(context: Context, transaction: SaleTransaction, settings: AppSettings) {
        val activePrinter: ReceiptPrinter = when (settings.printerType) {
            PrinterType.ANDROID_SYSTEM -> systemPrinter
            PrinterType.BLUETOOTH_THERMAL -> thermalPrinter
        }
        appScope.launch {
            _printFeedback.value = activePrinter.printReceipt(context, transaction, settings)
        }
    }

    fun runTestPrint(context: Context, settings: AppSettings, operatorName: String?) {
        val sampleItems = listOf(
            SaleLineItem(
                productId = "test_1",
                productNameSr = "Domaći hleb Sava 500g",
                productNameEn = "Artisan White Bread 500g",
                category = ProductCategory.BAKERY,
                unitPriceRsd = 75.0,
                quantity = 2
            ),
            SaleLineItem(
                productId = "test_2",
                productNameSr = "Sveže mleko 1L 2.8%",
                productNameEn = "Fresh Whole Milk 1L",
                category = ProductCategory.DAIRY,
                unitPriceRsd = 145.0,
                quantity = 1
            )
        )
        val subtotal = sampleItems.sumOf { it.lineTotalRsd }
        val tax = subtotal * CurrencyFormatter.VAT_RATE
        val sampleTx = SaleTransaction(
            id = "test_print",
            receiptNumber = "MP-TEST",
            workerId = "test_worker",
            workerName = operatorName ?: "MarketPOS Test",
            timestampMillis = System.currentTimeMillis(),
            items = sampleItems,
            subtotalRsd = subtotal,
            taxRsd = tax,
            totalRsd = subtotal + tax,
            paymentMethod = PaymentMethod.CASH,
            cashReceivedRsd = 500.0,
            changeRsd = (500.0 - (subtotal + tax)).coerceAtLeast(0.0)
        )
        printTransaction(context, sampleTx, settings)
    }

    fun dismissPrintFeedback() {
        _printFeedback.value = null
    }
}
