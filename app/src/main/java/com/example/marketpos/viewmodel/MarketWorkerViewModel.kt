package com.example.marketpos.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.marketpos.data.CatalogLocationRepository
import com.example.marketpos.data.OrderSalesRepository
import com.example.marketpos.di.PosAppContainer
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.CartItem
import com.example.marketpos.model.DefaultLocations
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.Product
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.WorkerProfile
import com.example.marketpos.printing.PrinterCoordinator
import com.example.marketpos.session.PosSessionManager
import com.example.marketpos.util.CurrencyFormatter
import com.example.marketpos.util.PosAnalyticsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MarketWorkerUiState(
    val settings: AppSettings = AppSettings(),
    val currentUser: WorkerProfile? = null,
    val activeLocation: VenueLocation? = null,
    val activeLocationId: String = DefaultLocations.MARKET_CENTRAL_ID,
    val products: List<Product> = emptyList(),
    val orders: List<PosOrder> = emptyList(),
    val transactions: List<SaleTransaction> = emptyList(),
    val cartItems: List<CartItem> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: ProductCategory? = null,
    val showOutOfStockOnly: Boolean = false,
    val selectedOrderId: String? = null,
    val editingOrderId: String? = null,
    val orderSavedNotice: Boolean = false,
    val stockWarningProduct: Product? = null
) {
    val venueCategories: List<ProductCategory>
        get() = ProductCategory.forVenue(settings.venueMode)

    val selectedOrder: PosOrder?
        get() = selectedOrderId?.let { id -> orders.firstOrNull { it.id == id } }

    val editingOrder: PosOrder?
        get() = editingOrderId?.let { id -> orders.firstOrNull { it.id == id } }

    val currentWorkerOrders: List<PosOrder>
        get() = PosAnalyticsCalculator.filterWorkerOrders(orders, currentUser)

    val currentWorkerTransactions: List<SaleTransaction>
        get() = PosAnalyticsCalculator.filterWorkerTransactions(transactions, currentUser)

    val pendingOrdersCount: Int
        get() = currentWorkerOrders.count { it.status == OrderStatus.PENDING }

    val outOfStockProducts: List<Product>
        get() = PosAnalyticsCalculator.outOfStockProducts(products, settings.language)

    val filteredCatalogProducts: List<Product>
        get() = PosAnalyticsCalculator.filterCatalogProducts(
            products = products,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
            showOutOfStockOnly = showOutOfStockOnly,
            language = settings.language
        )

    val cartSubtotalRsd: Double
        get() = cartItems.sumOf { it.product.priceRsd * it.quantity }

    val cartTaxRsd: Double
        get() = cartSubtotalRsd * CurrencyFormatter.VAT_RATE

    val cartTotalRsd: Double
        get() = cartSubtotalRsd + cartTaxRsd

    val cartTotalQuantity: Int
        get() = cartItems.sumOf { it.quantity }
}

class MarketWorkerViewModel(
    private val sessionManager: PosSessionManager,
    catalogLocationRepository: CatalogLocationRepository,
    private val orderSalesRepository: OrderSalesRepository,
    private val printerCoordinator: PrinterCoordinator
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<ProductCategory?>(null)
    private val showOutOfStockOnly = MutableStateFlow(false)
    private val cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    private val selectedOrderId = MutableStateFlow<String?>(null)
    private val editingOrderId = MutableStateFlow<String?>(null)
    private val orderSavedNotice = MutableStateFlow(false)
    private val stockWarningProduct = MutableStateFlow<Product?>(null)

    private val catalogFilterFlow = combine(
        searchQuery,
        selectedCategory,
        showOutOfStockOnly,
        cartItems,
        stockWarningProduct
    ) { query, cat, outOnly, cart, warning ->
        CatalogSlice(query, cat, outOnly, cart, warning)
    }

    private val orderUiFlow = combine(
        selectedOrderId,
        editingOrderId,
        orderSavedNotice
    ) { selId, editId, saved ->
        OrderSlice(selId, editId, saved)
    }

    private val dataFlow = combine(
        catalogLocationRepository.locationsFlow,
        catalogLocationRepository.productsFlow,
        orderSalesRepository.ordersFlow,
        orderSalesRepository.transactionsFlow
    ) { locations, products, orders, transactions ->
        RepoSlice(locations, products, orders, transactions)
    }

    val uiState: StateFlow<MarketWorkerUiState> = combine(
        sessionManager.effectiveSettingsFlow,
        sessionManager.currentUser,
        dataFlow,
        catalogFilterFlow,
        orderUiFlow
    ) { settings, user, repo, catalog, orderUi ->
        val activeLocId = settings.selectedLocationId
        val activeLoc = repo.locations.firstOrNull { it.id == activeLocId }
            ?: repo.locations.firstOrNull { it.venueMode == settings.venueMode }
        MarketWorkerUiState(
            settings = settings,
            currentUser = user,
            activeLocation = activeLoc,
            activeLocationId = activeLocId,
            products = repo.products.filter { it.locationId == activeLocId },
            orders = repo.orders.filter { it.locationId == activeLocId },
            transactions = repo.transactions.filter { it.locationId == activeLocId },
            cartItems = catalog.cart,
            searchQuery = catalog.query,
            selectedCategory = catalog.category,
            showOutOfStockOnly = catalog.outOnly,
            selectedOrderId = orderUi.selectedId,
            editingOrderId = orderUi.editingId,
            orderSavedNotice = orderUi.savedNotice,
            stockWarningProduct = catalog.warning
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MarketWorkerUiState()
    )

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun selectCategory(category: ProductCategory?) {
        showOutOfStockOnly.value = false
        selectedCategory.value = category
    }

    fun toggleOutOfStockFilter() {
        val next = !showOutOfStockOnly.value
        showOutOfStockOnly.value = next
        if (next) selectedCategory.value = null
    }

    fun addToCart(product: Product) {
        val latest = uiState.value.products.firstOrNull { it.id == product.id } ?: product
        if (!latest.isInStock) {
            stockWarningProduct.value = latest
            return
        }
        val currentQty = cartItems.value.firstOrNull { it.product.id == latest.id }?.quantity ?: 0
        if (currentQty >= latest.stockQuantity) {
            stockWarningProduct.value = latest
            return
        }
        cartItems.update { list ->
            val existing = list.find { it.product.id == latest.id }
            if (existing != null) {
                list.map {
                    if (it.product.id == latest.id) it.copy(product = latest, quantity = it.quantity + 1) else it
                }
            } else {
                list + CartItem(product = latest, quantity = 1)
            }
        }
    }

    fun decrementCartItem(productId: String) {
        cartItems.update { list ->
            list.mapNotNull { item ->
                if (item.product.id == productId) {
                    if (item.quantity > 1) item.copy(quantity = item.quantity - 1) else null
                } else {
                    item
                }
            }
        }
    }

    fun removeFromCart(productId: String) {
        cartItems.update { list -> list.filterNot { it.product.id == productId } }
    }

    fun clearCart() {
        cartItems.value = emptyList()
    }

    fun dismissStockWarning() {
        stockWarningProduct.value = null
    }

    fun saveCartAsPendingOrder() {
        val worker = sessionManager.currentUser.value ?: sessionManager.workers.first()
        val items = cartItems.value
        if (items.isEmpty()) return

        viewModelScope.launch {
            orderSalesRepository.createPendingOrder(
                worker = worker,
                venueMode = uiState.value.settings.venueMode,
                locationId = uiState.value.activeLocationId,
                cartItems = items
            )
            cartItems.value = emptyList()
            orderSavedNotice.value = true
        }
    }

    fun dismissOrderSavedNotice() {
        orderSavedNotice.value = false
    }

    fun completeSale(
        paymentMethod: PaymentMethod,
        cashReceivedRsd: Double?,
        onComplete: (SaleTransaction) -> Unit
    ) {
        val worker = sessionManager.currentUser.value ?: sessionManager.workers.first()
        val items = cartItems.value
        if (items.isEmpty()) return

        viewModelScope.launch {
            val tx = orderSalesRepository.recordSale(
                worker = worker,
                cartItems = items,
                paymentMethod = paymentMethod,
                cashReceivedRsd = cashReceivedRsd,
                venueMode = uiState.value.settings.venueMode,
                locationId = uiState.value.activeLocationId
            )
            cartItems.value = emptyList()
            onComplete(tx)
        }
    }

    fun openOrderDetails(orderId: String) {
        val user = sessionManager.currentUser.value
        val order = uiState.value.orders.firstOrNull { it.id == orderId } ?: return
        if (user?.role?.isWorker == true && order.workerId != user.id) return
        selectedOrderId.value = orderId
    }

    fun closeOrderDetails() {
        selectedOrderId.value = null
    }

    fun startEditingOrder(orderId: String) {
        val user = sessionManager.currentUser.value
        val order = uiState.value.orders.firstOrNull { it.id == orderId } ?: return
        if (user?.role?.isWorker == true && order.workerId != user.id) return
        if (!order.isEditable) return
        selectedOrderId.value = null
        editingOrderId.value = order.id
    }

    fun finishEditingOrder(reopenDetails: Boolean = true) {
        val currentId = editingOrderId.value
        editingOrderId.value = null
        if (reopenDetails && currentId != null) {
            selectedOrderId.value = currentId
        }
    }

    fun addProductToEditingOrder(product: Product) {
        val order = uiState.value.editingOrder ?: return
        if (!order.isEditable) return
        val latest = uiState.value.products.firstOrNull { it.id == product.id } ?: product
        if (!latest.isInStock) {
            stockWarningProduct.value = latest
            return
        }
        val activeQty = order.activeItems.filter { it.productId == latest.id }.sumOf { it.quantity }
        if (activeQty >= latest.stockQuantity) {
            stockWarningProduct.value = latest
            return
        }
        viewModelScope.launch {
            orderSalesRepository.addProductToPendingOrder(order.id, latest)
        }
    }

    fun incrementOrderItem(orderId: String, itemRowId: Long) {
        val order = uiState.value.orders.firstOrNull { it.id == orderId } ?: return
        if (!order.isEditable) return
        val item = order.activeItems.firstOrNull { it.id == itemRowId } ?: return
        val latest = uiState.value.products.firstOrNull { it.id == item.productId }
        if (latest != null && item.quantity >= latest.stockQuantity) {
            stockWarningProduct.value = latest
            return
        }
        viewModelScope.launch {
            orderSalesRepository.adjustOrderItemQuantity(orderId, itemRowId, +1)
        }
    }

    fun decrementOrderItem(orderId: String, itemRowId: Long) {
        viewModelScope.launch {
            orderSalesRepository.adjustOrderItemQuantity(orderId, itemRowId, -1)
        }
    }

    fun cancelOrderItem(orderId: String, itemRowId: Long) {
        viewModelScope.launch {
            orderSalesRepository.cancelOrderItem(orderId, itemRowId)
        }
    }

    fun cancelFullOrder(orderId: String) {
        viewModelScope.launch {
            orderSalesRepository.cancelOrder(orderId)
            if (editingOrderId.value == orderId) {
                editingOrderId.value = null
            }
        }
    }

    fun completeOrderPayment(
        context: Context,
        order: PosOrder,
        paymentMethod: PaymentMethod,
        cashReceivedRsd: Double?,
        onComplete: (SaleTransaction) -> Unit
    ) {
        if (!order.isEditable || order.activeItems.isEmpty()) return
        val worker = sessionManager.currentUser.value ?: sessionManager.workers.first()
        val settings = uiState.value.settings
        viewModelScope.launch {
            val tx = orderSalesRepository.completeOrderSale(
                worker = worker,
                order = order,
                paymentMethod = paymentMethod,
                cashReceivedRsd = cashReceivedRsd
            )
            sessionManager.refreshNowTimestamp()
            if (editingOrderId.value == order.id) {
                editingOrderId.value = null
            }
            printerCoordinator.printTransaction(context, tx, settings)
            onComplete(tx)
        }
    }

    fun logout() {
        sessionManager.logout()
    }

    private data class CatalogSlice(
        val query: String,
        val category: ProductCategory?,
        val outOnly: Boolean,
        val cart: List<CartItem>,
        val warning: Product?
    )

    private data class OrderSlice(
        val selectedId: String?,
        val editingId: String?,
        val savedNotice: Boolean
    )

    private data class RepoSlice(
        val locations: List<VenueLocation>,
        val products: List<Product>,
        val orders: List<PosOrder>,
        val transactions: List<SaleTransaction>
    )

    companion object {
        fun factory(container: PosAppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MarketWorkerViewModel(
                        sessionManager = container.sessionManager,
                        catalogLocationRepository = container.catalogLocationRepository,
                        orderSalesRepository = container.orderSalesRepository,
                        printerCoordinator = container.printerCoordinator
                    ) as T
                }
            }
    }
}
