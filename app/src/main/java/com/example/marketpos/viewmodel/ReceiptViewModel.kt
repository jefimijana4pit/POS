package com.example.marketpos.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.marketpos.data.OrderSalesRepository
import com.example.marketpos.di.PosAppContainer
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.PrintResult
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.printing.PrinterCoordinator
import com.example.marketpos.session.PosSessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ReceiptUiState(
    val transaction: SaleTransaction? = null,
    val settings: AppSettings = AppSettings(),
    val printFeedback: PrintResult? = null
)

class ReceiptViewModel(
    private val transactionId: String,
    private val sessionManager: PosSessionManager,
    orderSalesRepository: OrderSalesRepository,
    private val printerCoordinator: PrinterCoordinator
) : ViewModel() {

    val uiState: StateFlow<ReceiptUiState> = combine(
        orderSalesRepository.transactionsFlow,
        sessionManager.effectiveSettingsFlow,
        printerCoordinator.printFeedback
    ) { transactions, settings, feedback ->
        ReceiptUiState(
            transaction = transactions.firstOrNull { it.id == transactionId },
            settings = settings,
            printFeedback = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReceiptUiState()
    )

    fun printReceipt(context: Context) {
        val tx = uiState.value.transaction ?: return
        printerCoordinator.printTransaction(context, tx, uiState.value.settings)
    }

    fun dismissPrintFeedback() {
        printerCoordinator.dismissPrintFeedback()
    }

    fun refreshBluetoothPrinters(context: Context) {
        printerCoordinator.refreshBluetoothPrinters(context)
    }

    companion object {
        fun factory(container: PosAppContainer, transactionId: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ReceiptViewModel(
                        transactionId = transactionId,
                        sessionManager = container.sessionManager,
                        orderSalesRepository = container.orderSalesRepository,
                        printerCoordinator = container.printerCoordinator
                    ) as T
                }
            }
    }
}
