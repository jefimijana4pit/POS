package com.example.marketpos.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.marketpos.data.CatalogLocationRepository
import com.example.marketpos.data.OrderSalesRepository
import com.example.marketpos.di.PosAppContainer
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.BarTable
import com.example.marketpos.model.DefaultLocations
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.Product
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.model.UserRole
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.WorkerProfile
import com.example.marketpos.printing.PrinterCoordinator
import com.example.marketpos.session.PosSessionManager
import com.example.marketpos.util.PosAnalyticsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BarWorkerUiState(
    val settings: AppSettings = AppSettings(),
    val currentUser: WorkerProfile? = null,
    val activeLocation: VenueLocation? = null,
    val activeLocationId: String = DefaultLocations.BAR_CENTER_ID,
    val products: List<Product> = emptyList(),
    val tables: List<BarTable> = emptyList(),
    val orders: List<PosOrder> = emptyList(),
    val transactions: List<SaleTransaction> = emptyList(),
    val activeBarTableId: String? = null,
    val selectedOrderId: String? = null,
    val searchQuery: String = "",
    val selectedCategory: ProductCategory? = null,
    val showOutOfStockOnly: Boolean = false,
    val stockWarningProduct: Product? = null,
    val tableLockFeedbackWorkerName: String? = null,
    val orderSavedNotice: Boolean = false,
    val nowMillis: Long = System.currentTimeMillis()
) {
    val venueCategories: List<ProductCategory>
        get() = ProductCategory.forVenue(settings.venueMode)

    val activeBarTable: BarTable?
        get() = activeBarTableId?.let { id -> tables.firstOrNull { it.id == id } }

    val selectedOrder: PosOrder?
        get() = selectedOrderId?.let { id -> orders.firstOrNull { it.id == id } }

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
}

class BarWorkerViewModel(
    private val sessionManager: PosSessionManager,
    catalogLocationRepository: CatalogLocationRepository,
    private val orderSalesRepository: OrderSalesRepository,
    private val printerCoordinator: PrinterCoordinator
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedCategory = MutableStateFlow<ProductCategory?>(null)
    private val showOutOfStockOnly = MutableStateFlow(false)
    private val activeBarTableId = MutableStateFlow<String?>(null)
    private val selectedOrderId = MutableStateFlow<String?>(null)
    private val stockWarningProduct = MutableStateFlow<Product?>(null)
    private val tableLockFeedbackWorkerName = MutableStateFlow<String?>(null)
    private val orderSavedNotice = MutableStateFlow(false)

    private val repoFlow = combine(
        catalogLocationRepository.locationsFlow,
        catalogLocationRepository.productsFlow,
        orderSalesRepository.tablesFlow,
        orderSalesRepository.ordersFlow,
        orderSalesRepository.transactionsFlow
    ) { locations, products, tables, orders, transactions ->
        BarRepoSlice(locations, products, tables, orders, transactions)
    }

    private val filterFlow = combine(
        searchQuery,
        selectedCategory,
        showOutOfStockOnly,
        stockWarningProduct
    ) { query, cat, outOnly, warning ->
        BarFilterSlice(query, cat, outOnly, warning)
    }

    private val interactionFlow = combine(
        activeBarTableId,
        selectedOrderId,
        tableLockFeedbackWorkerName,
        orderSavedNotice,
        sessionManager.nowMillisFlow
    ) { tableId, orderId, lockName, savedNotice, nowMs ->
        BarInteractionSlice(tableId, orderId, lockName, savedNotice, nowMs)
    }

    val uiState: StateFlow<BarWorkerUiState> = combine(
        sessionManager.effectiveSettingsFlow,
        sessionManager.currentUser,
        repoFlow,
        filterFlow,
        interactionFlow
    ) { settings, user, repo, filter, interaction ->
        val activeLocId = settings.selectedLocationId
        val activeLoc = repo.locations.firstOrNull { it.id == activeLocId }
            ?: repo.locations.firstOrNull { it.venueMode == settings.venueMode }
        BarWorkerUiState(
            settings = settings,
            currentUser = user,
            activeLocation = activeLoc,
            activeLocationId = activeLocId,
            products = repo.products.filter { it.locationId == activeLocId },
            tables = repo.tables.filter { it.locationId == activeLocId },
            orders = repo.orders.filter { it.locationId == activeLocId },
            transactions = repo.transactions.filter { it.locationId == activeLocId },
            activeBarTableId = interaction.activeTableId,
            selectedOrderId = interaction.selectedOrderId,
            searchQuery = filter.query,
            selectedCategory = filter.category,
            showOutOfStockOnly = filter.outOnly,
            stockWarningProduct = filter.warning,
            tableLockFeedbackWorkerName = interaction.lockFeedback,
            orderSavedNotice = interaction.savedNotice,
            nowMillis = interaction.nowMs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BarWorkerUiState()
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

    fun openOrLockTable(table: BarTable) {
        val worker = sessionManager.currentUser.value ?: return
        if (table.isLockedByOther(worker.id)) {
            tableLockFeedbackWorkerName.value = table.lockedByWorkerName ?: ""
            return
        }
        activeBarTableId.value = table.id
    }

    fun closeActiveBarTable() {
        activeBarTableId.value = null
    }

    fun keepActiveTableOrderPending() {
        activeBarTableId.value = null
        orderSavedNotice.value = true
    }

    fun dismissOrderSavedNotice() {
        orderSavedNotice.value = false
    }

    fun dismissTableLockFeedback() {
        tableLockFeedbackWorkerName.value = null
    }

    fun dismissStockWarning() {
        stockWarningProduct.value = null
    }

    fun unlockTableAfterCleanup(tableId: String) {
        val worker = sessionManager.currentUser.value
        val table = uiState.value.tables.firstOrNull { it.id == tableId } ?: return
        if (worker?.role != UserRole.ADMIN && table.isLockedByOther(worker?.id)) {
            tableLockFeedbackWorkerName.value = table.lockedByWorkerName ?: ""
            return
        }
        viewModelScope.launch {
            orderSalesRepository.unlockTable(tableId)
            if (activeBarTableId.value == tableId) {
                activeBarTableId.value = null
            }
        }
    }

    fun startNewOrderOnTable(tableId: String) {
        val worker = sessionManager.currentUser.value
        val table = uiState.value.tables.firstOrNull { it.id == tableId } ?: return
        if (table.isLockedByOther(worker?.id)) {
            tableLockFeedbackWorkerName.value = table.lockedByWorkerName ?: ""
            return
        }
        viewModelScope.launch {
            if (table.isLocked) {
                orderSalesRepository.startNewOrderOnLockedTable(tableId)
            }
            activeBarTableId.value = tableId
        }
    }

    fun addProductToActiveTable(product: Product) {
        val table = uiState.value.activeBarTable ?: return
        val worker = sessionManager.currentUser.value ?: return
        if (table.isLockedByOther(worker.id)) {
            tableLockFeedbackWorkerName.value = table.lockedByWorkerName ?: ""
            return
        }
        val latest = uiState.value.products.firstOrNull { it.id == product.id } ?: product
        if (!latest.isInStock) {
            stockWarningProduct.value = latest
            return
        }
        val orderedAcrossTables = uiState.value.tables.sumOf { t ->
            t.orderItems.filter { it.productId == latest.id }.sumOf { it.quantity }
        }
        if (orderedAcrossTables >= latest.stockQuantity) {
            stockWarningProduct.value = latest
            return
        }
        viewModelScope.launch {
            val added = orderSalesRepository.addProductToTable(table.id, worker, latest)
            if (!added) {
                val refreshed = uiState.value.tables.firstOrNull { it.id == table.id }
                tableLockFeedbackWorkerName.value = refreshed?.lockedByWorkerName ?: ""
                if (refreshed?.isLockedByOther(worker.id) == true && activeBarTableId.value == table.id) {
                    activeBarTableId.value = null
                }
            }
        }
    }

    fun decrementActiveTableItem(productId: String) {
        val tableId = activeBarTableId.value ?: return
        viewModelScope.launch {
            orderSalesRepository.decrementTableItem(tableId, productId)
        }
    }

    fun removeActiveTableItem(productId: String) {
        val tableId = activeBarTableId.value ?: return
        viewModelScope.launch {
            orderSalesRepository.removeTableItem(tableId, productId)
        }
    }

    fun clearActiveTableOrder() {
        val tableId = activeBarTableId.value ?: return
        viewModelScope.launch {
            orderSalesRepository.clearTableOrder(tableId)
        }
    }

    fun completeActiveTableSale(
        context: Context,
        paymentMethod: PaymentMethod,
        cashReceivedRsd: Double?,
        onComplete: (SaleTransaction) -> Unit
    ) {
        val worker = sessionManager.currentUser.value ?: sessionManager.workers.first()
        val table = uiState.value.activeBarTable ?: return
        if (table.orderItems.isEmpty() || table.isLockedByOther(worker.id)) return
        val settings = uiState.value.settings

        viewModelScope.launch {
            val tx = orderSalesRepository.completeTableSale(
                worker = worker,
                table = table,
                orderItems = table.orderItems,
                paymentMethod = paymentMethod,
                cashReceivedRsd = cashReceivedRsd
            )
            sessionManager.refreshNowTimestamp()
            printerCoordinator.printTransaction(context, tx, settings)
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
        val tableId = order.tableId ?: return
        val table = uiState.value.tables.firstOrNull { it.id == tableId } ?: return
        if (table.isLockedByOther(user?.id)) {
            tableLockFeedbackWorkerName.value = table.lockedByWorkerName ?: ""
            return
        }
        selectedOrderId.value = null
        activeBarTableId.value = table.id
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
        val order = uiState.value.orders.firstOrNull { it.id == orderId }
        viewModelScope.launch {
            orderSalesRepository.cancelOrder(orderId)
            if (order?.tableId != null && activeBarTableId.value == order.tableId) {
                activeBarTableId.value = null
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
            printerCoordinator.printTransaction(context, tx, settings)
            onComplete(tx)
        }
    }

    fun logout() {
        sessionManager.logout()
    }

    private data class BarRepoSlice(
        val locations: List<VenueLocation>,
        val products: List<Product>,
        val tables: List<BarTable>,
        val orders: List<PosOrder>,
        val transactions: List<SaleTransaction>
    )

    private data class BarFilterSlice(
        val query: String,
        val category: ProductCategory?,
        val outOnly: Boolean,
        val warning: Product?
    )

    private data class BarInteractionSlice(
        val activeTableId: String?,
        val selectedOrderId: String?,
        val lockFeedback: String?,
        val savedNotice: Boolean,
        val nowMs: Long
    )

    companion object {
        fun factory(container: PosAppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BarWorkerViewModel(
                        sessionManager = container.sessionManager,
                        catalogLocationRepository = container.catalogLocationRepository,
                        orderSalesRepository = container.orderSalesRepository,
                        printerCoordinator = container.printerCoordinator
                    ) as T
                }
            }
    }
}
