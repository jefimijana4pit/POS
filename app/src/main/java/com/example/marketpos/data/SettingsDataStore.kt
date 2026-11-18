package com.example.marketpos.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.PrinterType
import com.example.marketpos.model.VenueMode
import com.example.marketpos.ui.theme.FontScaleOption
import com.example.marketpos.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "marketpos_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val VENUE_MODE = stringPreferencesKey("venue_mode")
        val SELECTED_LOCATION_ID = stringPreferencesKey("selected_location_id")
        val RSD_PER_EUR = doublePreferencesKey("rsd_per_eur")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val FONT_SCALE = stringPreferencesKey("font_scale")
        val PRINTER_TYPE = stringPreferencesKey("printer_type")
        val PRINTER_NAME = stringPreferencesKey("printer_name")
        val PRINTER_ADDRESS = stringPreferencesKey("printer_address")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
            val parsedVenue = prefs[Keys.VENUE_MODE]?.let {
                runCatching { VenueMode.valueOf(it) }.getOrNull()
            } ?: VenueMode.MARKET
            AppSettings(
                language = prefs[Keys.LANGUAGE]?.let {
                    runCatching { AppLanguage.valueOf(it) }.getOrNull()
                } ?: AppLanguage.SERBIAN_LATIN,
                venueMode = parsedVenue,
                selectedLocationId = prefs[Keys.SELECTED_LOCATION_ID]
                    ?: com.example.marketpos.model.DefaultLocations.defaultIdFor(parsedVenue),
                rsdPerEur = prefs[Keys.RSD_PER_EUR] ?: 117.0,
            themeMode = prefs[Keys.THEME_MODE]?.let {
                runCatching { ThemeMode.valueOf(it) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
            fontScale = prefs[Keys.FONT_SCALE]?.let {
                runCatching { FontScaleOption.valueOf(it) }.getOrNull()
            } ?: FontScaleOption.NORMAL,
            printerType = prefs[Keys.PRINTER_TYPE]?.let {
                runCatching { PrinterType.valueOf(it) }.getOrNull()
            } ?: PrinterType.ANDROID_SYSTEM,
            selectedPrinterName = prefs[Keys.PRINTER_NAME],
            selectedPrinterAddress = prefs[Keys.PRINTER_ADDRESS]
        )
    }

    suspend fun updateLanguage(language: AppLanguage) {
        context.settingsDataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun updateVenueMode(venueMode: VenueMode) {
        context.settingsDataStore.edit {
            it[Keys.VENUE_MODE] = venueMode.name
            it[Keys.SELECTED_LOCATION_ID] = com.example.marketpos.model.DefaultLocations.defaultIdFor(venueMode)
        }
    }

    suspend fun updateLocation(venueMode: VenueMode, locationId: String) {
        context.settingsDataStore.edit {
            it[Keys.VENUE_MODE] = venueMode.name
            it[Keys.SELECTED_LOCATION_ID] = locationId
        }
    }

    suspend fun updateExchangeRate(rsdPerEur: Double) {
        context.settingsDataStore.edit { it[Keys.RSD_PER_EUR] = rsdPerEur }
    }

    suspend fun updateThemeMode(themeMode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = themeMode.name }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun updateFontScale(scale: FontScaleOption) {
        context.settingsDataStore.edit { it[Keys.FONT_SCALE] = scale.name }
    }

    suspend fun updatePrinterType(printerType: PrinterType) {
        context.settingsDataStore.edit { it[Keys.PRINTER_TYPE] = printerType.name }
    }

    suspend fun updateSelectedPrinter(name: String, address: String) {
        context.settingsDataStore.edit {
            it[Keys.PRINTER_NAME] = name
            it[Keys.PRINTER_ADDRESS] = address
        }
    }
}
