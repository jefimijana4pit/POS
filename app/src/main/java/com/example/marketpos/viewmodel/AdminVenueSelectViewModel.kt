package com.example.marketpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.marketpos.data.CatalogLocationRepository
import com.example.marketpos.data.OrderSalesRepository
import com.example.marketpos.di.PosAppContainer
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.LocationStats
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.VenueMode
import com.example.marketpos.model.WorkerProfile
import com.example.marketpos.session.PosSessionManager
import com.example.marketpos.util.PosAnalyticsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class AdminVenueSelectUiState(
    val settings: AppSettings = AppSettings(),
    val currentUser: WorkerProfile? = null,
    val adminProfile: WorkerProfile? = null,
    val locations: List<VenueLocation> = emptyList(),
    val locationStats: List<LocationStats> = emptyList(),
    val allProductsCount: Int = 0,
    val globalRevenueRsd: Double = 0.0,
    val globalLowOrOutStockCount: Int = 0,
    val locationErrorNotice: Boolean = false
) {
    val marketLocations: List<VenueLocation>
        get() = locations.filter { it.venueMode == VenueMode.MARKET }

    val barLocations: List<VenueLocation>
        get() = locations.filter { it.venueMode == VenueMode.BAR_RESTAURANT }
}

class AdminVenueSelectViewModel(
    private val sessionManager: PosSessionManager,
    private val catalogLocationRepository: CatalogLocationRepository,
    orderSalesRepository: OrderSalesRepository
) : ViewModel() {

    private val locationErrorNotice = MutableStateFlow(false)

    private val dataFlow = combine(
        catalogLocationRepository.locationsFlow,
        catalogLocationRepository.productsFlow,
        orderSalesRepository.transactionsFlow,
        orderSalesRepository.ordersFlow,
        orderSalesRepository.tablesFlow
    ) { locations, products, transactions, orders, tables ->
        VenueDataSlice(locations, products, transactions, orders, tables)
    }

    val uiState: StateFlow<AdminVenueSelectUiState> = combine(
        sessionManager.effectiveSettingsFlow,
        sessionManager.currentUser,
        dataFlow,
        locationErrorNotice
    ) { settings, user, data, errorNotice ->
        val stats = PosAnalyticsCalculator.computeLocationStats(
            locations = data.locations,
            products = data.products,
            transactions = data.transactions,
            orders = data.orders,
            tables = data.tables
        )
        AdminVenueSelectUiState(
            settings = settings,
            currentUser = user,
            adminProfile = sessionManager.adminProfile,
            locations = data.locations,
            locationStats = stats,
            allProductsCount = data.products.size,
            globalRevenueRsd = data.transactions.sumOf { it.totalRsd },
            globalLowOrOutStockCount = PosAnalyticsCalculator.lowOrOutOfStockProducts(data.products).size,
            locationErrorNotice = errorNotice
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AdminVenueSelectUiState(adminProfile = sessionManager.adminProfile)
    )

    fun selectAdminLocation(location: VenueLocation) {
        sessionManager.selectAdminLocation(location)
    }

    fun saveLocation(
        existingId: String?,
        name: String,
        address: String,
        venueMode: VenueMode,
        copyStarterData: Boolean
    ) {
        val current = uiState.value.locations
        val existing = existingId?.let { id -> current.firstOrNull { it.id == id } }
        val loc = VenueLocation(
            id = existingId ?: "loc_${if (venueMode == VenueMode.MARKET) "mkt" else "bar"}_${UUID.randomUUID().toString().take(6)}",
            name = name.trim(),
            address = address.trim().ifEmpty { "Beograd" },
            venueMode = venueMode,
            sortOrder = existing?.sortOrder ?: (current.size + 1)
        )
        viewModelScope.launch {
            catalogLocationRepository.saveLocation(loc, copyStarterData = copyStarterData && existing == null)
        }
    }

    fun deleteLocation(locationId: String) {
        viewModelScope.launch {
            val deleted = catalogLocationRepository.deleteLocation(locationId)
            if (!deleted) {
                locationErrorNotice.value = true
            } else if (uiState.value.settings.selectedLocationId == locationId) {
                val fallback = uiState.value.locations.firstOrNull { it.id != locationId }
                if (fallback != null) {
                    sessionManager.selectAdminLocation(fallback)
                }
            }
        }
    }

    fun dismissLocationErrorNotice() {
        locationErrorNotice.value = false
    }

    fun logout() {
        sessionManager.logout()
    }

    private data class VenueDataSlice(
        val locations: List<VenueLocation>,
        val products: List<com.example.marketpos.model.Product>,
        val transactions: List<com.example.marketpos.model.SaleTransaction>,
        val orders: List<com.example.marketpos.model.PosOrder>,
        val tables: List<com.example.marketpos.model.BarTable>
    )

    companion object {
        fun factory(container: PosAppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AdminVenueSelectViewModel(
                        sessionManager = container.sessionManager,
                        catalogLocationRepository = container.catalogLocationRepository,
                        orderSalesRepository = container.orderSalesRepository
                    ) as T
                }
            }
    }
}
