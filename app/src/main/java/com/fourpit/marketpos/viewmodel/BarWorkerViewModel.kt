package com.fourpit.marketpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fourpit.marketpos.data.CatalogLocationRepository
import com.fourpit.marketpos.data.OrderSalesRepository
import com.fourpit.marketpos.model.BarTable
import com.fourpit.marketpos.model.PaymentMethod
import com.fourpit.marketpos.model.PosLocation
import com.fourpit.marketpos.model.PosOrder
import com.fourpit.marketpos.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BarWorkerUiState(
    val location: PosLocation? = null,
    val tables: List<BarTable> = emptyList(),
    val products: List<Product> = emptyList(),
    val openOrders: List<PosOrder> = emptyList(),
    val paidBills: List<PosOrder> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null
) {
    val categories: List<String>
        get() = products.map { it.category }.distinct().sorted()

    val filteredProducts: List<Product>
        get() = products.filter { p ->
            val matchesCat = selectedCategory == null || p.category == selectedCategory
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                p.name.lowercase().contains(q) ||
                p.sku.lowercase().contains(q)
            matchesCat && matchesQuery
        }
}

class BarWorkerViewModel(
    private val locationId: Long,
    private val catalogRepo: CatalogLocationRepository,
    private val orderSalesRepo: OrderSalesRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<String?>(null)

    val uiState: StateFlow<BarWorkerUiState> = combine(
        catalogRepo.observeLocation(locationId),
        catalogRepo.observeTablesForLocation(locationId),
        catalogRepo.observeProductsForLocation(locationId),
        combine(
            orderSalesRepo.observeOpenOrdersForLocation(locationId),
            orderSalesRepo.observePaidOrdersForLocation(locationId),
            searchQuery,
            selectedCategory
        ) { open, paid, q, cat ->
            BarDynamicSlice(open, paid, q, cat)
        }
    ) { loc, tables, products, dyn ->
        BarWorkerUiState(
            location = loc,
            tables = tables,
            products = products,
            openOrders = dyn.openOrders,
            paidBills = dyn.paidBills,
            searchQuery = dyn.searchQuery,
            selectedCategory = dyn.selectedCategory
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BarWorkerUiState()
    )

    fun observeOrder(orderId: Long): Flow<PosOrder?> = orderSalesRepo.observeOrderById(orderId)

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        selectedCategory.value = category
    }

    fun openTable(tableId: Long, workerName: String, onOrderReady: (Long) -> Unit) {
        viewModelScope.launch {
            val orderId = orderSalesRepo.openOrGetTableOrder(
                locationId = locationId,
                tableId = tableId,
                workerName = workerName
            )
            onOrderReady(orderId)
        }
    }

    fun addProductToOrder(orderId: Long, product: Product) {
        viewModelScope.launch {
            orderSalesRepo.addProductToOpenOrder(orderId, product, deltaQty = 1)
        }
    }

    fun updateItemQuantity(orderId: Long, itemId: Long, newQuantity: Int) {
        viewModelScope.launch {
            orderSalesRepo.updateOrderItemQuantity(orderId, itemId, newQuantity)
        }
    }

    fun settleOrder(
        orderId: Long,
        paymentMethod: PaymentMethod,
        receivedRsd: Double,
        taxRatePercent: Double,
        workerName: String,
        onSettled: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val settledId = orderSalesRepo.settleOpenOrder(
                orderId = orderId,
                paymentMethod = paymentMethod,
                receivedRsd = receivedRsd,
                taxRatePercent = taxRatePercent,
                workerName = workerName
            )
            onSettled(settledId)
        }
    }

    fun cancelOrder(orderId: Long, workerName: String, onCancelled: () -> Unit) {
        viewModelScope.launch {
            orderSalesRepo.cancelOpenOrder(orderId, workerName)
            onCancelled()
        }
    }

    private data class BarDynamicSlice(
        val openOrders: List<PosOrder>,
        val paidBills: List<PosOrder>,
        val searchQuery: String,
        val selectedCategory: String?
    )

    class Factory(
        private val locationId: Long,
        private val catalogRepo: CatalogLocationRepository,
        private val orderSalesRepo: OrderSalesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BarWorkerViewModel(locationId, catalogRepo, orderSalesRepo) as T
        }
    }
}
