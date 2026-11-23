package com.fourpit.marketpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fourpit.marketpos.data.CatalogLocationRepository
import com.fourpit.marketpos.data.OrderSalesRepository
import com.fourpit.marketpos.model.CartItem
import com.fourpit.marketpos.model.PaymentMethod
import com.fourpit.marketpos.model.PosLocation
import com.fourpit.marketpos.model.PosOrder
import com.fourpit.marketpos.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketWorkerUiState(
    val location: PosLocation? = null,
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val cart: List<CartItem> = emptyList(),
    val paidBills: List<PosOrder> = emptyList(),
    val isCheckingOut: Boolean = false
) {
    val categories: List<String>
        get() = products.map { it.category }.distinct().sorted()

    val filteredProducts: List<Product>
        get() = products.filter { p ->
            val matchesCat = selectedCategory == null || p.category == selectedCategory
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isEmpty() ||
                p.name.lowercase().contains(q) ||
                p.sku.lowercase().contains(q) ||
                p.category.lowercase().contains(q)
            matchesCat && matchesQuery
        }

    val cartTotalRsd: Double
        get() = cart.sumOf { it.lineTotalRsd }

    val cartItemCount: Int
        get() = cart.sumOf { it.quantity }
}

class MarketWorkerViewModel(
    private val locationId: Long,
    private val catalogRepo: CatalogLocationRepository,
    private val orderSalesRepo: OrderSalesRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<String?>(null)
    private val cartQuantities = MutableStateFlow<Map<Long, Int>>(emptyMap())
    private val isCheckingOut = MutableStateFlow(false)

    val uiState: StateFlow<MarketWorkerUiState> = combine(
        catalogRepo.observeLocation(locationId),
        catalogRepo.observeProductsForLocation(locationId),
        orderSalesRepo.observePaidOrdersForLocation(locationId),
        combine(searchQuery, selectedCategory, cartQuantities, isCheckingOut) { q, cat, cart, busy ->
            Quad(q, cat, cart, busy)
        }
    ) { loc, products, bills, controls ->
        val byId = products.associateBy { it.id }
        val cartItems = controls.third.mapNotNull { (prodId, qty) ->
            val prod = byId[prodId]
            if (prod != null && qty > 0) CartItem(prod, qty) else null
        }
        MarketWorkerUiState(
            location = loc,
            products = products,
            searchQuery = controls.first,
            selectedCategory = controls.second,
            cart = cartItems,
            paidBills = bills,
            isCheckingOut = controls.fourth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MarketWorkerUiState()
    )

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectCategory(category: String?) {
        selectedCategory.value = category
    }

    fun addToCart(product: Product) {
        if (product.stockQuantity <= 0) return
        cartQuantities.update { current ->
            val next = (current[product.id] ?: 0) + 1
            if (next <= product.stockQuantity) current + (product.id to next) else current
        }
    }

    fun decrementFromCart(productId: Long) {
        cartQuantities.update { current ->
            val qty = (current[productId] ?: 0) - 1
            if (qty <= 0) current - productId else current + (productId to qty)
        }
    }

    fun removeFromCart(productId: Long) {
        cartQuantities.update { it - productId }
    }

    fun clearCart() {
        cartQuantities.value = emptyMap()
    }

    fun completeSale(
        workerName: String,
        paymentMethod: PaymentMethod,
        receivedRsd: Double,
        taxRatePercent: Double,
        onOrderCompleted: (Long) -> Unit
    ) {
        val currentCart = uiState.value.cart
        if (currentCart.isEmpty() || isCheckingOut.value) return

        viewModelScope.launch {
            isCheckingOut.value = true
            try {
                val orderId = orderSalesRepo.completeMarketCheckout(
                    locationId = locationId,
                    workerName = workerName,
                    items = currentCart,
                    paymentMethod = paymentMethod,
                    receivedRsd = receivedRsd,
                    taxRatePercent = taxRatePercent
                )
                cartQuantities.value = emptyMap()
                onOrderCompleted(orderId)
            } finally {
                isCheckingOut.value = false
            }
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    class Factory(
        private val locationId: Long,
        private val catalogRepo: CatalogLocationRepository,
        private val orderSalesRepo: OrderSalesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MarketWorkerViewModel(locationId, catalogRepo, orderSalesRepo) as T
        }
    }
}
