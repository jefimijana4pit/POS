package com.example.marketpos.session

import com.example.marketpos.data.CatalogLocationRepository
import com.example.marketpos.data.OrderSalesRepository
import com.example.marketpos.data.SettingsDataStore
import com.example.marketpos.data.seed.CatalogSeedData
import com.example.marketpos.data.seed.PosDatabaseSeeder
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.BarTable
import com.example.marketpos.model.DefaultLocations
import com.example.marketpos.model.PairedPrinterDevice
import com.example.marketpos.model.PrinterType
import com.example.marketpos.model.UserRole
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.VenueMode
import com.example.marketpos.model.WorkerProfile
import com.example.marketpos.ui.theme.FontScaleOption
import com.example.marketpos.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PosSessionManager(
    private val appScope: CoroutineScope,
    private val settingsDataStore: SettingsDataStore,
    private val catalogLocationRepository: CatalogLocationRepository,
    private val orderSalesRepository: OrderSalesRepository,
    private val databaseSeeder: PosDatabaseSeeder
) {
    val workers: List<WorkerProfile> = CatalogSeedData.workers
    val adminProfile: WorkerProfile = CatalogSeedData.adminProfile

    private val _currentUser = MutableStateFlow<WorkerProfile?>(null)
    val currentUser: StateFlow<WorkerProfile?> = _currentUser.asStateFlow()

    private val _nowMillisFlow = MutableStateFlow(System.currentTimeMillis())
    val nowMillisFlow: StateFlow<Long> = _nowMillisFlow.asStateFlow()

    val effectiveSettingsFlow: Flow<AppSettings> = combine(
        settingsDataStore.settingsFlow,
        catalogLocationRepository.locationsFlow
    ) { settings, allLocations ->
        val resolvedLocation = allLocations.firstOrNull {
            it.id == settings.selectedLocationId && it.venueMode == settings.venueMode
        } ?: allLocations.firstOrNull {
            it.id == settings.selectedLocationId
        } ?: allLocations.firstOrNull {
            it.venueMode == settings.venueMode
        }
        val activeLocId = resolvedLocation?.id ?: settings.selectedLocationId
        val activeVenueMode = resolvedLocation?.venueMode ?: settings.venueMode
        if (activeVenueMode != settings.venueMode || activeLocId != settings.selectedLocationId) {
            settings.copy(venueMode = activeVenueMode, selectedLocationId = activeLocId)
        } else {
            settings
        }
    }

    val effectiveSettingsState: StateFlow<AppSettings> = effectiveSettingsFlow.stateIn(
        scope = appScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    init {
        appScope.launch {
            databaseSeeder.ensureSeedData()
        }

        appScope.launch {
            var latestTables: List<BarTable> = emptyList()
            launch {
                orderSalesRepository.tablesFlow.collect { tables ->
                    latestTables = tables
                    val now = System.currentTimeMillis()
                    if (tables.any { it.awaitingCleanup && it.remainingCleanupMillis(now) == 0L }) {
                        orderSalesRepository.autoUnlockExpiredTables(now)
                    }
                }
            }
            while (true) {
                val hasPendingCleanup = latestTables.any { it.awaitingCleanup }
                if (hasPendingCleanup) {
                    val now = System.currentTimeMillis()
                    _nowMillisFlow.value = now
                    if (latestTables.any { it.awaitingCleanup && it.remainingCleanupMillis(now) == 0L }) {
                        orderSalesRepository.autoUnlockExpiredTables(now)
                    }
                }
                delay(1_000L)
            }
        }
    }

    fun authenticate(role: UserRole, workerId: String?, pin: String): Boolean {
        val profile = when (role) {
            UserRole.MARKET_WORKER,
            UserRole.RESTAURANT_WORKER -> {
                workers.firstOrNull { it.id == workerId && it.role == role }
                    ?: workers.firstOrNull { it.role == role }
                    ?: workers.first()
            }
            UserRole.ADMIN -> adminProfile
        }
        return if (profile.pin == pin.trim()) {
            _currentUser.value = profile
            profile.role.assignedVenueMode()?.let { assignedVenue ->
                selectLocation(assignedVenue, profile.locationId)
            }
            true
        } else {
            false
        }
    }

    fun quickDemoLogin(role: UserRole) {
        val profile = when (role) {
            UserRole.MARKET_WORKER -> workers.firstOrNull { it.role == UserRole.MARKET_WORKER }
                ?: workers.first()
            UserRole.RESTAURANT_WORKER -> workers.firstOrNull { it.role == UserRole.RESTAURANT_WORKER }
                ?: workers.first()
            UserRole.ADMIN -> adminProfile
        }
        _currentUser.value = profile
        profile.role.assignedVenueMode()?.let { assignedVenue ->
            selectLocation(assignedVenue, profile.locationId)
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    fun selectLocation(venueMode: VenueMode, locationId: String) {
        appScope.launch {
            settingsDataStore.updateLocation(venueMode, locationId)
        }
    }

    fun selectAdminLocation(location: VenueLocation) {
        selectLocation(location.venueMode, location.id)
    }

    fun updateLanguage(language: AppLanguage) {
        appScope.launch { settingsDataStore.updateLanguage(language) }
    }

    fun updateVenueMode(mode: VenueMode) {
        appScope.launch {
            val locations = catalogLocationRepository.locationsFlow.first()
            val firstOfMode = locations.firstOrNull { it.venueMode == mode }?.id
                ?: DefaultLocations.defaultIdFor(mode)
            settingsDataStore.updateLocation(mode, firstOfMode)
        }
    }

    fun updateExchangeRate(rate: Double) {
        appScope.launch { settingsDataStore.updateExchangeRate(rate) }
    }

    fun updateThemeMode(mode: ThemeMode) {
        appScope.launch { settingsDataStore.updateThemeMode(mode) }
    }

    fun updateDynamicColor(enabled: Boolean) {
        appScope.launch { settingsDataStore.updateDynamicColor(enabled) }
    }

    fun updateFontScale(scale: FontScaleOption) {
        appScope.launch { settingsDataStore.updateFontScale(scale) }
    }

    fun updatePrinterType(type: PrinterType) {
        appScope.launch { settingsDataStore.updatePrinterType(type) }
    }

    fun updateSelectedPrinter(device: PairedPrinterDevice) {
        appScope.launch { settingsDataStore.updateSelectedPrinter(device.name, device.address) }
    }

    fun refreshNowTimestamp() {
        _nowMillisFlow.value = System.currentTimeMillis()
    }
}
