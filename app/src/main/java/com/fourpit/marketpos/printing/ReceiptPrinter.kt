package com.fourpit.marketpos.printing

import android.content.Context
import com.fourpit.marketpos.model.CurrencyCode
import com.fourpit.marketpos.model.PosOrder
import com.fourpit.marketpos.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ReceiptPrintPayload(
    val businessName: String,
    val order: PosOrder,
    val currency: CurrencyCode,
    val eurRate: Double,
    val taxRatePercent: Double
) {
    fun buildPlainTextLines(): List<String> {
        val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US)
        val ts = fmt.format(Date(order.closedAtMillis ?: order.createdAtMillis))
        val lines = mutableListOf<String>()
        lines += businessName.uppercase(Locale.getDefault())
        if (order.locationName.isNotBlank()) {
            lines += order.locationName
        }
        lines += "--------------------------------"
        lines += "Receipt: ${order.receiptNumber}"
        lines += "Date:    $ts"
        lines += "Staff:   ${order.workerName}"
        order.tableLabel?.let { lines += "Table:   $it" }
        lines += "--------------------------------"
        order.items.forEach { item ->
            val lineTotal = CurrencyFormatter.format(item.lineTotalRsd, currency, eurRate)
            lines += item.productName
            lines += "  ${item.quantity} x ${CurrencyFormatter.format(item.unitPriceRsd, currency, eurRate)} = $lineTotal"
        }
        lines += "--------------------------------"
        lines += "VAT (${taxRatePercent.toInt()}%): ${CurrencyFormatter.format(order.taxAmountRsd, currency, eurRate)}"
        lines += "TOTAL: ${CurrencyFormatter.format(order.totalRsd, currency, eurRate)}"
        order.paymentMethod?.let { lines += "Payment: ${it.name}" }
        if (order.changeRsd > 0.0) {
            lines += "Received: ${CurrencyFormatter.format(order.receivedRsd, currency, eurRate)}"
            lines += "Change:   ${CurrencyFormatter.format(order.changeRsd, currency, eurRate)}"
        }
        lines += "--------------------------------"
        lines += "Thank you / Hvala na poseti!"
        return lines
    }
}

sealed interface PrintResult {
    data class Success(val message: String) : PrintResult
    data class Failure(val reason: String) : PrintResult
}

interface ReceiptPrinter {
    suspend fun print(context: Context, payload: ReceiptPrintPayload): PrintResult
}
