package com.fourpit.marketpos.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fourpit.marketpos.model.AppLanguage
import com.fourpit.marketpos.model.CurrencyCode
import com.fourpit.marketpos.model.PosSettings
import com.fourpit.marketpos.model.PrinterMode
import com.fourpit.marketpos.ui.theme.FontScaleOption
import com.fourpit.marketpos.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.posSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "marketpos_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val CURRENCY = stringPreferencesKey("currency")
        val EUR_RATE = doublePreferencesKey("eur_rate")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val BUSINESS_NAME = stringPreferencesKey("business_name")
        val TAX_RATE = doublePreferencesKey("tax_rate")
        val PRINTER_MODE = stringPreferencesKey("printer_mode")
        val BT_PRINTER_NAME = stringPreferencesKey("bt_printer_name")
        val BT_PRINTER_ADDRESS = stringPreferencesKey("bt_printer_address")
    }

    val settingsFlow: Flow<PosSettings> = context.posSettingsDataStore.data.map { prefs ->
        PosSettings(
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.SR_LATIN,
            currency = prefs[Keys.CURRENCY]?.let { runCatching { CurrencyCode.valueOf(it) }.getOrNull() }
                ?: CurrencyCode.RSD,
            eurExchangeRate = prefs[Keys.EUR_RATE] ?: 117.2,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            fontScaleOption = prefs[Keys.FONT_SCALE]?.let { runCatching { FontScaleOption.valueOf(it) }.getOrNull() }
                ?: FontScaleOption.NORMAL,
            businessName = prefs[Keys.BUSINESS_NAME] ?: "MarketPOS Retail",
            taxRatePercent = prefs[Keys.TAX_RATE] ?: 20.0,
            printerMode = prefs[Keys.PRINTER_MODE]?.let { runCatching { PrinterMode.valueOf(it) }.getOrNull() }
                ?: PrinterMode.ANDROID_SYSTEM,
            bluetoothPrinterName = prefs[Keys.BT_PRINTER_NAME] ?: "",
            bluetoothPrinterAddress = prefs[Keys.BT_PRINTER_ADDRESS] ?: ""
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.posSettingsDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setCurrency(currency: CurrencyCode) {
        context.posSettingsDataStore.edit { it[Keys.CURRENCY] = currency.name }
    }

    suspend fun setEurExchangeRate(rate: Double) {
        if (rate > 0.0) {
            context.posSettingsDataStore.edit { it[Keys.EUR_RATE] = rate }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.posSettingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.posSettingsDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setFontScale(scale: FontScaleOption) {
        context.posSettingsDataStore.edit { it[Keys.FONT_SCALE] = scale.name }
    }

    suspend fun setBusinessName(name: String) {
        context.posSettingsDataStore.edit { it[Keys.BUSINESS_NAME] = name.trim().ifBlank { "MarketPOS Retail" } }
    }

    suspend fun setTaxRatePercent(taxRate: Double) {
        context.posSettingsDataStore.edit { it[Keys.TAX_RATE] = taxRate.coerceIn(0.0, 50.0) }
    }

    suspend fun setPrinterMode(mode: PrinterMode) {
        context.posSettingsDataStore.edit { it[Keys.PRINTER_MODE] = mode.name }
    }

    suspend fun setBluetoothPrinter(name: String, address: String) {
        context.posSettingsDataStore.edit {
            it[Keys.BT_PRINTER_NAME] = name
            it[Keys.BT_PRINTER_ADDRESS] = address
        }
    }
}
