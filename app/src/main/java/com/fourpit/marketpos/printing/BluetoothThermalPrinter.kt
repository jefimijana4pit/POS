package com.fourpit.marketpos.printing

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.util.UUID

data class PairedBluetoothDevice(
    val name: String,
    val address: String
)

class BluetoothThermalPrinter : ReceiptPrinter {

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    fun hasConnectPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(context: Context): List<PairedBluetoothDevice> {
        if (!hasConnectPermission(context)) return emptyList()
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter ?: return emptyList()
        return runCatching {
            adapter.bondedDevices
                .orEmpty()
                .map { PairedBluetoothDevice(name = it.name ?: it.address, address = it.address) }
                .sortedBy { it.name }
        }.getOrDefault(emptyList())
    }

    @SuppressLint("MissingPermission")
    suspend fun printToDevice(
        context: Context,
        macAddress: String,
        payload: ReceiptPrintPayload
    ): PrintResult = withContext(Dispatchers.IO) {
        if (!hasConnectPermission(context)) {
            return@withContext PrintResult.Failure("Bluetooth permission not granted")
        }
        if (macAddress.isBlank()) {
            return@withContext PrintResult.Failure("No Bluetooth printer selected in Settings")
        }
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = manager?.adapter
            ?: return@withContext PrintResult.Failure("Bluetooth adapter unavailable")

        runCatching {
            val device = adapter.getRemoteDevice(macAddress)
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.use { rfcomm ->
                rfcomm.connect()
                val out = rfcomm.outputStream
                // ESC/POS init + text + feed + partial cut
                out.write(byteArrayOf(0x1B, 0x40))
                val body = payload.buildPlainTextLines().joinToString("\n") + "\n\n\n"
                out.write(body.toByteArray(StandardCharsets.UTF_8))
                out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))
                out.flush()
            }
            PrintResult.Success("Printed via Bluetooth")
        }.getOrElse { err ->
            PrintResult.Failure(err.message ?: "Bluetooth thermal printer connection failed")
        }
    }

    override suspend fun print(context: Context, payload: ReceiptPrintPayload): PrintResult {
        return PrintResult.Failure("Select a Bluetooth printer address")
    }
}
