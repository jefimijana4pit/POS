package com.example.marketpos.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.marketpos.di.PosAppContainer
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.PairedPrinterDevice
import com.example.marketpos.model.PrintResult
import com.example.marketpos.model.PrinterType
import com.example.marketpos.model.VenueMode
import com.example.marketpos.model.WorkerProfile
import com.example.marketpos.printing.PrinterCoordinator
import com.example.marketpos.session.PosSessionManager
import com.example.marketpos.ui.theme.FontScaleOption
import com.example.marketpos.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val currentUser: WorkerProfile? = null,
    val pairedPrinters: List<PairedPrinterDevice> = emptyList(),
    val hasBluetoothPermission: Boolean = false,
    val printFeedback: PrintResult? = null
)

class SettingsViewModel(
    private val sessionManager: PosSessionManager,
    private val printerCoordinator: PrinterCoordinator
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        sessionManager.effectiveSettingsFlow,
        sessionManager.currentUser,
        printerCoordinator.pairedPrinters,
        printerCoordinator.hasBluetoothPermission,
        printerCoordinator.printFeedback
    ) { settings, user, printers, hasBtPerm, feedback ->
        SettingsUiState(
            settings = settings,
            currentUser = user,
            pairedPrinters = printers,
            hasBluetoothPermission = hasBtPerm,
            printFeedback = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun updateVenueMode(mode: VenueMode) = sessionManager.updateVenueMode(mode)
    fun updateLanguage(language: AppLanguage) = sessionManager.updateLanguage(language)
    fun updateExchangeRate(rate: Double) = sessionManager.updateExchangeRate(rate)
    fun updateThemeMode(mode: ThemeMode) = sessionManager.updateThemeMode(mode)
    fun updateDynamicColor(enabled: Boolean) = sessionManager.updateDynamicColor(enabled)
    fun updateFontScale(scale: FontScaleOption) = sessionManager.updateFontScale(scale)
    fun updatePrinterType(type: PrinterType) = sessionManager.updatePrinterType(type)
    fun updateSelectedPrinter(device: PairedPrinterDevice) = sessionManager.updateSelectedPrinter(device)
    fun refreshBluetoothPrinters(context: Context) = printerCoordinator.refreshBluetoothPrinters(context)
    fun runTestPrint(context: Context) {
        printerCoordinator.runTestPrint(
            context = context,
            settings = uiState.value.settings,
            operatorName = uiState.value.currentUser?.name
        )
    }

    fun dismissPrintFeedback() = printerCoordinator.dismissPrintFeedback()

    companion object {
        fun factory(container: PosAppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        sessionManager = container.sessionManager,
                        printerCoordinator = container.printerCoordinator
                    ) as T
                }
            }
    }
}
