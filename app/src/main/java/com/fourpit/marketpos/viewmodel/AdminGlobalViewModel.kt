package com.fourpit.marketpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fourpit.marketpos.data.CatalogLocationRepository
import com.fourpit.marketpos.data.OrderSalesRepository
import com.fourpit.marketpos.model.PosLocation
import com.fourpit.marketpos.model.PosOrder
import com.fourpit.marketpos.model.Product
import com.fourpit.marketpos.util.BranchRevenueStat
import com.fourpit.marketpos.util.CategoryBreakdownItem
import com.fourpit.marketpos.util.DailyRevenuePoint
import com.fourpit.marketpos.util.PaymentBreakdownItem
import com.fourpit.marketpos.util.PosAnalyticsCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminGlobalUiState(
    val locations: List<PosLocation> = emptyList(),
    val allProducts: List<Product> = emptyList(),
    val allOrders: List<PosOrder> = emptyList(),
    val totalRevenueRsd: Double = 0.0,
    val totalPaidOrders: Int = 0,
    val averageTicketRsd: Double = 0.0,
    val lowStockCount: Int = 0,
    val revenueSeries: List<DailyRevenuePoint> = emptyList(),
    val branchComparison: List<BranchRevenueStat> = emptyList(),
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val paymentBreakdown: List<PaymentBreakdownItem> = emptyList()
)

class AdminGlobalViewModel(
    private val catalogRepo: CatalogLocationRepository,
    private val orderSalesRepo: OrderSalesRepository
) : ViewModel() {

    val uiState: StateFlow<AdminGlobalUiState> = combine(
        catalogRepo.observeLocations(),
        catalogRepo.observeAllProducts(),
        orderSalesRepo.observeAllOrders()
    ) { locations, products, orders ->
        val paid = PosAnalyticsCalculator.paidOrders(orders)
        val locNames = locations.associate { it.id to it.name }
        AdminGlobalUiState(
            locations = locations,
            allProducts = products,
            allOrders = orders,
            totalRevenueRsd = PosAnalyticsCalculator.totalRevenueRsd(paid),
            totalPaidOrders = paid.size,
            averageTicketRsd = PosAnalyticsCalculator.averageTicketRsd(paid),
            lowStockCount = products.count { it.isLowStock },
            revenueSeries = PosAnalyticsCalculator.revenueLastDays(paid, days = 7),
            branchComparison = PosAnalyticsCalculator.branchComparison(paid, locNames),
            categoryBreakdown = PosAnalyticsCalculator.categoryBreakdown(paid),
            paymentBreakdown = PosAnalyticsCalculator.paymentBreakdown(paid)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdminGlobalUiState()
    )

    fun restockProduct(productId: Long, delta: Int = 10) {
        viewModelScope.launch {
            catalogRepo.restockProduct(productId, delta)
        }
    }

    class Factory(
        private val catalogRepo: CatalogLocationRepository,
        private val orderSalesRepo: OrderSalesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdminGlobalViewModel(catalogRepo, orderSalesRepo) as T
        }
    }
}
