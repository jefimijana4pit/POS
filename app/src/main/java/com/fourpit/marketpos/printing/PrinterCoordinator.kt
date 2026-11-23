package com.fourpit.marketpos.printing

import android.content.Context
import com.fourpit.marketpos.model.PosSettings
import com.fourpit.marketpos.model.PrinterMode

class PrinterCoordinator(
    private val systemPrinter: AndroidSystemPrinter = AndroidSystemPrinter(),
    val bluetoothPrinter: BluetoothThermalPrinter = BluetoothThermalPrinter()
) {
    suspend fun printReceipt(
        context: Context,
        settings: PosSettings,
        payload: ReceiptPrintPayload
    ): PrintResult {
        return when (settings.printerMode) {
            PrinterMode.ANDROID_SYSTEM -> systemPrinter.print(context, payload)
            PrinterMode.BLUETOOTH_THERMAL -> bluetoothPrinter.printToDevice(
                context = context,
                macAddress = settings.bluetoothPrinterAddress,
                payload = payload
            )
        }
    }
}
