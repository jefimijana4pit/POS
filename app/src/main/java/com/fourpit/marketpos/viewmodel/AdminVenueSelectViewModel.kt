package com.fourpit.marketpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fourpit.marketpos.data.CatalogLocationRepository
import com.fourpit.marketpos.data.OrderSalesRepository
import com.fourpit.marketpos.model.OrderStatus
import com.fourpit.marketpos.model.PosLocation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LocationSummaryCardData(
    val location: PosLocation,
    val totalRevenueRsd: Double,
    val paidOrdersCount: Int,
    val productCount: Int,
    val lowStockCount: Int
)

data class AdminVenueSelectUiState(
    val venues: List<LocationSummaryCardData> = emptyList(),
    val globalRevenueRsd: Double = 0.0,
    val globalOrdersCount: Int = 0
)

class AdminVenueSelectViewModel(
    private val catalogRepo: CatalogLocationRepository,
    private val orderSalesRepo: OrderSalesRepository
) : ViewModel() {

    val uiState: StateFlow<AdminVenueSelectUiState> = combine(
        catalogRepo.observeLocations(),
        catalogRepo.observeAllProducts(),
        orderSalesRepo.observeAllOrders()
    ) { locations, products, orders ->
        val paidOrders = orders.filter { it.status == OrderStatus.PAID }
        val summaries = locations.map { loc ->
            val locOrders = paidOrders.filter { it.locationId == loc.id }
            val locProducts = products.filter { it.locationId == loc.id }
            LocationSummaryCardData(
                location = loc,
                totalRevenueRsd = locOrders.sumOf { it.totalRsd },
                paidOrdersCount = locOrders.size,
                productCount = locProducts.size,
                lowStockCount = locProducts.count { it.isLowStock }
            )
        }
        AdminVenueSelectUiState(
            venues = summaries,
            globalRevenueRsd = paidOrders.sumOf { it.totalRsd },
            globalOrdersCount = paidOrders.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdminVenueSelectUiState()
    )

    fun saveLocation(location: PosLocation) {
        viewModelScope.launch {
            catalogRepo.saveLocation(location)
        }
    }

    fun deleteLocation(locationId: Long) {
        viewModelScope.launch {
            catalogRepo.deleteLocation(locationId)
        }
    }

    class Factory(
        private val catalogRepo: CatalogLocationRepository,
        private val orderSalesRepo: OrderSalesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdminVenueSelectViewModel(catalogRepo, orderSalesRepo) as T
        }
    }
}
