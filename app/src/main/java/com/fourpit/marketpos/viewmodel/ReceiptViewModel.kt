package com.fourpit.marketpos.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fourpit.marketpos.data.OrderSalesRepository
import com.fourpit.marketpos.model.PosOrder
import com.fourpit.marketpos.model.PosSettings
import com.fourpit.marketpos.printing.PrintResult
import com.fourpit.marketpos.printing.PrinterCoordinator
import com.fourpit.marketpos.printing.ReceiptPrintPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReceiptUiState(
    val order: PosOrder? = null,
    val isPrinting: Boolean = false,
    val printStatusMessage: String? = null,
    val printSucceeded: Boolean? = null
)

class ReceiptViewModel(
    private val orderId: Long,
    private val orderSalesRepo: OrderSalesRepository,
    private val printerCoordinator: PrinterCoordinator
) : ViewModel() {

    private val isPrinting = MutableStateFlow(false)
    private val printResult = MutableStateFlow<PrintResult?>(null)

    val uiState: StateFlow<ReceiptUiState> = combine(
        orderSalesRepo.observeOrderById(orderId),
        isPrinting,
        printResult
    ) { order, printing, result ->
        ReceiptUiState(
            order = order,
            isPrinting = printing,
            printStatusMessage = when (result) {
                is PrintResult.Success -> result.message
                is PrintResult.Failure -> result.reason
                null -> null
            },
            printSucceeded = when (result) {
                is PrintResult.Success -> true
                is PrintResult.Failure -> false
                null -> null
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReceiptUiState()
    )

    fun printReceipt(context: Context, settings: PosSettings) {
        val currentOrder = uiState.value.order ?: return
        if (isPrinting.value) return
        viewModelScope.launch {
            isPrinting.value = true
            try {
                val payload = ReceiptPrintPayload(
                    businessName = settings.businessName,
                    order = currentOrder,
                    currency = settings.currency,
                    eurRate = settings.eurExchangeRate,
                    taxRatePercent = settings.taxRatePercent
                )
                printResult.value = printerCoordinator.printReceipt(context, settings, payload)
            } finally {
                isPrinting.value = false
            }
        }
    }

    fun clearPrintStatus() {
        printResult.value = null
    }

    class Factory(
        private val orderId: Long,
        private val orderSalesRepo: OrderSalesRepository,
        private val printerCoordinator: PrinterCoordinator
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReceiptViewModel(orderId, orderSalesRepo, printerCoordinator) as T
        }
    }
}
