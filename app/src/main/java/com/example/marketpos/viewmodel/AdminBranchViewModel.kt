package com.example.marketpos.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.marketpos.data.CatalogLocationRepository
import com.example.marketpos.data.OrderSalesRepository
import com.example.marketpos.di.PosAppContainer
import com.example.marketpos.i18n.AppStrings
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.BarTable
import com.example.marketpos.model.DefaultLocations
import com.example.marketpos.model.LocationReportSummary
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PdfExportResult
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.Product
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.model.ReportPeriodType
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.WorkerProfile
import com.example.marketpos.session.PosSessionManager
import com.example.marketpos.util.CurrencyFormatter
import com.example.marketpos.util.DailyRevenuePoint
import com.example.marketpos.util.PdfExportHelper
import com.example.marketpos.util.PosAnalyticsCalculator
import com.example.marketpos.util.ProductSalesStat
import com.example.marketpos.util.WorkerSalesStat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class AdminBranchUiState(
    val settings: AppSettings = AppSettings(),
    val workers: List<WorkerProfile> = emptyList(),
    val locations: List<VenueLocation> = emptyList(),
    val activeLocationId: String = DefaultLocations.MARKET_CENTRAL_ID,
    val allProducts: List<Product> = emptyList(),
    val allTransactions: List<SaleTransaction> = emptyList(),
    val allOrders: List<PosOrder> = emptyList(),
    val products: List<Product> = emptyList(),
    val transactions: List<SaleTransaction> = emptyList(),
    val orders: List<PosOrder> = emptyList(),
    val tables: List<BarTable> = emptyList(),
    val selectedAdminWorkerFilterId: String? = null,
    val selectedDashboardDayMillis: Long? = null,
    val selectedOrderId: String? = null,
    val pdfExportResult: PdfExportResult? = null,
    val nowMillis: Long = System.currentTimeMillis()
) {
    val activeLocation: VenueLocation?
        get() = locations.firstOrNull { it.id == activeLocationId }
            ?: locations.firstOrNull { it.venueMode == settings.venueMode }

    val venueCategories: List<ProductCategory>
        get() = ProductCategory.forVenue(settings.venueMode)

    val venueWorkers: List<WorkerProfile>
        get() = PosAnalyticsCalculator.venueWorkers(workers, settings.venueMode, activeLocationId)

    val selectedOrder: PosOrder?
        get() = selectedOrderId?.let { id -> orders.firstOrNull { it.id == id } }

    val cancelledOrdersCount: Int
        get() = orders.count { it.status == OrderStatus.CANCELLED }

    val filteredAdminTransactions: List<SaleTransaction>
        get() = if (selectedAdminWorkerFilterId == null) {
            transactions
        } else {
            transactions.filter { it.workerId == selectedAdminWorkerFilterId }
        }

    val dashboardTransactions: List<SaleTransaction>
        get() = if (selectedDashboardDayMillis == null) {
            transactions
        } else {
            transactions.filter {
                CurrencyFormatter.localDayStartMillis(it.timestampMillis) == selectedDashboardDayMillis
            }
        }

    val totalRevenueRsd: Double
        get() = dashboardTransactions.sumOf { it.totalRsd }

    val averageBasketRsd: Double
        get() = if (dashboardTransactions.isEmpty()) 0.0 else totalRevenueRsd / dashboardTransactions.size

    val dashboardCashRevenueRsd: Double
        get() = dashboardTransactions
            .filter { it.paymentMethod == PaymentMethod.CASH }
            .sumOf { it.totalRsd }

    val dashboardCardRevenueRsd: Double
        get() = dashboardTransactions
            .filter { it.paymentMethod == PaymentMethod.CARD }
            .sumOf { it.totalRsd }

    val lowOrOutOfStockProducts: List<Product>
        get() = PosAnalyticsCalculator.lowOrOutOfStockProducts(products)

    val lowOrOutOfStockCount: Int
        get() = lowOrOutOfStockProducts.size

    val dailyRevenue: List<DailyRevenuePoint>
        get() = PosAnalyticsCalculator.computeDailyRevenue(transactions)

    val topProducts: List<ProductSalesStat>
        get() = PosAnalyticsCalculator.computeTopProducts(dashboardTransactions)

    val salesByWorker: List<WorkerSalesStat>
        get() = PosAnalyticsCalculator.computeSalesByWorker(dashboardTransactions)
}

class AdminBranchViewModel(
    private val sessionManager: PosSessionManager,
    private val catalogLocationRepository: CatalogLocationRepository,
    private val orderSalesRepository: OrderSalesRepository
) : ViewModel() {

    private val selectedAdminWorkerFilterId = MutableStateFlow<String?>(null)
    private val selectedDashboardDayMillis = MutableStateFlow<Long?>(null)
    private val selectedOrderId = MutableStateFlow<String?>(null)
    private val pdfExportResult = MutableStateFlow<PdfExportResult?>(null)

    private val repoFlow = combine(
        catalogLocationRepository.locationsFlow,
        catalogLocationRepository.productsFlow,
        orderSalesRepository.transactionsFlow,
        orderSalesRepository.ordersFlow,
        orderSalesRepository.tablesFlow
    ) { locations, products, transactions, orders, tables ->
        BranchRepoSlice(locations, products, transactions, orders, tables)
    }

    private val filterFlow = combine(
        selectedAdminWorkerFilterId,
        selectedDashboardDayMillis,
        selectedOrderId,
        pdfExportResult,
        sessionManager.nowMillisFlow
    ) { workerFilter, dayMillis, orderId, pdfRes, nowMs ->
        BranchFilterSlice(workerFilter, dayMillis, orderId, pdfRes, nowMs)
    }

    val uiState: StateFlow<AdminBranchUiState> = combine(
        sessionManager.effectiveSettingsFlow,
        repoFlow,
        filterFlow
    ) { settings, repo, filter ->
        val activeLocId = settings.selectedLocationId
        AdminBranchUiState(
            settings = settings,
            workers = sessionManager.workers,
            locations = repo.locations,
            activeLocationId = activeLocId,
            allProducts = repo.products,
            allTransactions = repo.transactions,
            allOrders = repo.orders,
            products = repo.products.filter { it.locationId == activeLocId },
            transactions = repo.transactions.filter { it.locationId == activeLocId },
            orders = repo.orders.filter { it.locationId == activeLocId },
            tables = repo.tables.filter { it.locationId == activeLocId },
            selectedAdminWorkerFilterId = filter.workerFilter,
            selectedDashboardDayMillis = filter.dayMillis,
            selectedOrderId = filter.orderId,
            pdfExportResult = filter.pdfRes,
            nowMillis = filter.nowMs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AdminBranchUiState(workers = sessionManager.workers)
    )

    fun selectAdminWorkerFilter(workerId: String?) {
        selectedAdminWorkerFilterId.value = workerId
    }

    fun selectDashboardDay(dayStartMillis: Long?, toggleIfSame: Boolean = true) {
        selectedDashboardDayMillis.value = if (
            toggleIfSame && dayStartMillis != null && selectedDashboardDayMillis.value == dayStartMillis
        ) {
            null
        } else {
            dayStartMillis
        }
    }

    fun openOrderDetails(orderId: String) {
        if (uiState.value.orders.any { it.id == orderId }) {
            selectedOrderId.value = orderId
        }
    }

    fun closeOrderDetails() {
        selectedOrderId.value = null
    }

    fun saveProduct(
        existingId: String?,
        sku: String,
        nameSr: String,
        nameEn: String,
        category: ProductCategory,
        priceRsd: Double,
        unitSr: String,
        unitEn: String,
        stockQuantity: Int,
        lowStockThreshold: Int,
        isAvailable: Boolean
    ) {
        viewModelScope.launch {
            val existing = existingId?.let { id -> uiState.value.allProducts.firstOrNull { it.id == id } }
            val product = Product(
                id = existingId ?: "p_${UUID.randomUUID().toString().take(8)}",
                sku = sku.trim(),
                nameSr = nameSr.trim(),
                nameEn = nameEn.trim(),
                category = category,
                priceRsd = priceRsd,
                unitSr = unitSr.trim().ifEmpty { "kom" },
                unitEn = unitEn.trim().ifEmpty { "pcs" },
                stockQuantity = stockQuantity.coerceAtLeast(0),
                lowStockThreshold = lowStockThreshold.coerceAtLeast(0),
                isAvailable = isAvailable,
                venueMode = category.venueMode(),
                locationId = existing?.locationId ?: uiState.value.activeLocationId
            )
            catalogLocationRepository.saveProduct(product)
        }
    }

    fun adjustStock(productId: String, delta: Int) {
        viewModelScope.launch {
            catalogLocationRepository.adjustStock(productId, delta)
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            catalogLocationRepository.deleteProduct(productId)
        }
    }

    fun saveTable(existingId: String?, name: String, zone: String, capacity: Int) {
        val existing = existingId?.let { id -> uiState.value.tables.firstOrNull { it.id == id } }
        if (existing?.isLocked == true) return
        viewModelScope.launch {
            val table = BarTable(
                id = existingId ?: "tbl_${UUID.randomUUID().toString().take(8)}",
                name = name.trim(),
                zone = zone.trim().ifEmpty { "Sala" },
                capacity = capacity.coerceIn(1, 24),
                sortOrder = existing?.sortOrder ?: (uiState.value.tables.size + 1),
                locationId = existing?.locationId ?: uiState.value.activeLocationId
            )
            orderSalesRepository.saveTable(table)
        }
    }

    fun deleteTable(tableId: String) {
        val target = uiState.value.tables.firstOrNull { it.id == tableId }
        if (target?.isLocked == true) return
        viewModelScope.launch {
            orderSalesRepository.deleteTable(tableId)
        }
    }

    fun forceUnlockTable(tableId: String) {
        viewModelScope.launch {
            orderSalesRepository.unlockTable(tableId)
        }
    }

    fun buildReportSummary(
        locationId: String?,
        periodType: ReportPeriodType,
        startMillis: Long,
        endMillis: Long
    ): LocationReportSummary {
        val state = uiState.value
        return PdfExportHelper.buildReportSummary(
            locationId = locationId,
            periodType = periodType,
            startMillis = startMillis,
            endMillis = endMillis,
            locations = state.locations,
            workers = state.workers,
            allTransactions = state.allTransactions,
            allOrders = state.allOrders
        )
    }

    fun exportPdfReport(context: Context, summary: LocationReportSummary, strings: AppStrings) {
        val settings = uiState.value.settings
        viewModelScope.launch {
            pdfExportResult.value = PdfExportHelper.exportToPdf(context, summary, strings, settings)
        }
    }

    fun dismissPdfExportResult() {
        pdfExportResult.value = null
    }

    fun logout() {
        sessionManager.logout()
    }

    private data class BranchRepoSlice(
        val locations: List<VenueLocation>,
        val products: List<Product>,
        val transactions: List<SaleTransaction>,
        val orders: List<PosOrder>,
        val tables: List<BarTable>
    )

    private data class BranchFilterSlice(
        val workerFilter: String?,
        val dayMillis: Long?,
        val orderId: String?,
        val pdfRes: PdfExportResult?,
        val nowMs: Long
    )

    companion object {
        fun factory(container: PosAppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AdminBranchViewModel(
                        sessionManager = container.sessionManager,
                        catalogLocationRepository = container.catalogLocationRepository,
                        orderSalesRepository = container.orderSalesRepository
                    ) as T
                }
            }
    }
}
