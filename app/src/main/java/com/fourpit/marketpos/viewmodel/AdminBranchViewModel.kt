package com.fourpit.marketpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fourpit.marketpos.data.CatalogLocationRepository
import com.fourpit.marketpos.data.OrderSalesRepository
import com.fourpit.marketpos.model.BarTable
import com.fourpit.marketpos.model.PosLocation
import com.fourpit.marketpos.model.PosOrder
import com.fourpit.marketpos.model.Product
import com.fourpit.marketpos.model.WorkerActivityLog
import com.fourpit.marketpos.util.CategoryBreakdownItem
import com.fourpit.marketpos.util.DailyRevenuePoint
import com.fourpit.marketpos.util.PaymentBreakdownItem
import com.fourpit.marketpos.util.PosAnalyticsCalculator
import com.fourpit.marketpos.util.TopProductStat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminBranchUiState(
    val location: PosLocation? = null,
    val products: List<Product> = emptyList(),
    val tables: List<BarTable> = emptyList(),
    val orders: List<PosOrder> = emptyList(),
    val workerLogs: List<WorkerActivityLog> = emptyList(),
    val totalRevenueRsd: Double = 0.0,
    val paidOrderCount: Int = 0,
    val averageTicketRsd: Double = 0.0,
    val lowStockCount: Int = 0,
    val revenueSeries: List<DailyRevenuePoint> = emptyList(),
    val categoryBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val paymentBreakdown: List<PaymentBreakdownItem> = emptyList(),
    val topProducts: List<TopProductStat> = emptyList()
)

class AdminBranchViewModel(
    private val locationId: Long,
    private val catalogRepo: CatalogLocationRepository,
    private val orderSalesRepo: OrderSalesRepository
) : ViewModel() {

    val uiState: StateFlow<AdminBranchUiState> = combine(
        catalogRepo.observeLocation(locationId),
        catalogRepo.observeProductsForLocation(locationId),
        catalogRepo.observeTablesForLocation(locationId),
        orderSalesRepo.observeOrdersForLocation(locationId),
        orderSalesRepo.observeWorkerLogsForLocation(locationId)
    ) { loc, products, tables, orders, logs ->
        val paid = PosAnalyticsCalculator.paidOrders(orders)
        AdminBranchUiState(
            location = loc,
            products = products,
            tables = tables,
            orders = orders,
            workerLogs = logs,
            totalRevenueRsd = PosAnalyticsCalculator.totalRevenueRsd(paid),
            paidOrderCount = paid.size,
            averageTicketRsd = PosAnalyticsCalculator.averageTicketRsd(paid),
            lowStockCount = products.count { it.isLowStock },
            revenueSeries = PosAnalyticsCalculator.revenueLastDays(paid, days = 7),
            categoryBreakdown = PosAnalyticsCalculator.categoryBreakdown(paid),
            paymentBreakdown = PosAnalyticsCalculator.paymentBreakdown(paid),
            topProducts = PosAnalyticsCalculator.topProducts(paid, limit = 6)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AdminBranchUiState()
    )

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            catalogRepo.saveProduct(product.copy(locationId = locationId))
        }
    }

    fun restockProduct(productId: Long, delta: Int = 10) {
        viewModelScope.launch {
            catalogRepo.restockProduct(productId, delta)
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            catalogRepo.deleteProduct(productId)
        }
    }

    fun saveTable(label: String, seats: Int, existingId: Long = 0L) {
        viewModelScope.launch {
            catalogRepo.saveTable(
                BarTable(
                    id = existingId,
                    locationId = locationId,
                    label = label,
                    seats = seats
                )
            )
        }
    }

    fun deleteTable(tableId: Long) {
        viewModelScope.launch {
            catalogRepo.deleteTable(tableId)
        }
    }

    class Factory(
        private val locationId: Long,
        private val catalogRepo: CatalogLocationRepository,
        private val orderSalesRepo: OrderSalesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdminBranchViewModel(locationId, catalogRepo, orderSalesRepo) as T
        }
    }
}
