package com.fourpit.marketpos.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fourpit.marketpos.data.SettingsDataStore
import com.fourpit.marketpos.model.AppLanguage
import com.fourpit.marketpos.model.CurrencyCode
import com.fourpit.marketpos.model.PosSettings
import com.fourpit.marketpos.model.PrinterMode
import com.fourpit.marketpos.printing.BluetoothThermalPrinter
import com.fourpit.marketpos.printing.PairedBluetoothDevice
import com.fourpit.marketpos.ui.theme.FontScaleOption
import com.fourpit.marketpos.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    private val bluetoothPrinter: BluetoothThermalPrinter
) : ViewModel() {

    val settings: StateFlow<PosSettings> = settingsDataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PosSettings()
    )

    private val _pairedPrinters = MutableStateFlow<List<PairedBluetoothDevice>>(emptyList())
    val pairedPrinters: StateFlow<List<PairedBluetoothDevice>> = _pairedPrinters.asStateFlow()

    fun refreshBluetoothPrinters(context: Context) {
        _pairedPrinters.value = bluetoothPrinter.getPairedDevices(context)
    }

    fun hasBluetoothPermission(context: Context): Boolean =
        bluetoothPrinter.hasConnectPermission(context)

    fun setLanguage(language: AppLanguage) = viewModelScope.launch { settingsDataStore.setLanguage(language) }
    fun setCurrency(currency: CurrencyCode) = viewModelScope.launch { settingsDataStore.setCurrency(currency) }
    fun setEurRate(rate: Double) = viewModelScope.launch { settingsDataStore.setEurExchangeRate(rate) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsDataStore.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsDataStore.setDynamicColor(enabled) }
    fun setFontScale(scale: FontScaleOption) = viewModelScope.launch { settingsDataStore.setFontScale(scale) }
    fun setBusinessName(name: String) = viewModelScope.launch { settingsDataStore.setBusinessName(name) }
    fun setTaxRate(taxRate: Double) = viewModelScope.launch { settingsDataStore.setTaxRatePercent(taxRate) }
    fun setPrinterMode(mode: PrinterMode) = viewModelScope.launch { settingsDataStore.setPrinterMode(mode) }
    fun selectBluetoothPrinter(device: PairedBluetoothDevice) = viewModelScope.launch {
        settingsDataStore.setBluetoothPrinter(device.name, device.address)
    }

    class Factory(
        private val settingsDataStore: SettingsDataStore,
        private val bluetoothPrinter: BluetoothThermalPrinter
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsDataStore, bluetoothPrinter) as T
        }
    }
}
