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
import com.example.marketpos.model.LocationReportSummary
import com.example.marketpos.model.LocationStats
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PdfExportResult
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.Product
import com.example.marketpos.model.ReportPeriodType
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.session.PosSessionManager
import com.example.marketpos.util.DailyRevenuePoint
import com.example.marketpos.util.PdfExportHelper
import com.example.marketpos.util.PosAnalyticsCalculator
import com.example.marketpos.util.ProductSalesStat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AdminGlobalUiState(
    val settings: AppSettings = AppSettings(),
    val locations: List<VenueLocation> = emptyList(),
    val locationStats: List<LocationStats> = emptyList(),
    val allProducts: List<Product> = emptyList(),
    val allTransactions: List<SaleTransaction> = emptyList(),
    val allOrders: List<PosOrder> = emptyList(),
    val pdfExportResult: PdfExportResult? = null
) {
    val globalRevenueRsd: Double
        get() = allTransactions.sumOf { it.totalRsd }

    val globalCashRevenueRsd: Double
        get() = allTransactions.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalRsd }

    val globalCardRevenueRsd: Double
        get() = allTransactions.filter { it.paymentMethod == PaymentMethod.CARD }.sumOf { it.totalRsd }

    val globalAverageBasketRsd: Double
        get() = if (allTransactions.isEmpty()) 0.0 else globalRevenueRsd / allTransactions.size

    val globalCancelledOrdersCount: Int
        get() = allOrders.count { it.status == OrderStatus.CANCELLED }

    val globalTotalStockUnits: Int
        get() = allProducts.sumOf { it.stockQuantity }

    val globalTotalStockValueRsd: Double
        get() = allProducts.sumOf { it.priceRsd * it.stockQuantity }

    val globalLowOrOutStockProducts: List<Product>
        get() = PosAnalyticsCalculator.lowOrOutOfStockProducts(allProducts)

    val globalDailyRevenue: List<DailyRevenuePoint>
        get() = PosAnalyticsCalculator.computeDailyRevenue(allTransactions)

    val globalTopProducts: List<ProductSalesStat>
        get() = PosAnalyticsCalculator.computeTopProducts(allTransactions)
}

class AdminGlobalViewModel(
    private val sessionManager: PosSessionManager,
    private val catalogLocationRepository: CatalogLocationRepository,
    orderSalesRepository: OrderSalesRepository
) : ViewModel() {

    private val pdfExportResult = MutableStateFlow<PdfExportResult?>(null)

    private val dataFlow = combine(
        catalogLocationRepository.locationsFlow,
        catalogLocationRepository.productsFlow,
        orderSalesRepository.transactionsFlow,
        orderSalesRepository.ordersFlow,
        orderSalesRepository.tablesFlow
    ) { locations, products, transactions, orders, tables ->
        GlobalDataSlice(locations, products, transactions, orders, tables)
    }

    val uiState: StateFlow<AdminGlobalUiState> = combine(
        sessionManager.effectiveSettingsFlow,
        dataFlow,
        pdfExportResult
    ) { settings, data, pdfResult ->
        val stats = PosAnalyticsCalculator.computeLocationStats(
            locations = data.locations,
            products = data.products,
            transactions = data.transactions,
            orders = data.orders,
            tables = data.tables
        )
        AdminGlobalUiState(
            settings = settings,
            locations = data.locations,
            locationStats = stats,
            allProducts = data.products,
            allTransactions = data.transactions,
            allOrders = data.orders,
            pdfExportResult = pdfResult
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AdminGlobalUiState()
    )

    fun selectAdminLocation(location: VenueLocation) {
        sessionManager.selectAdminLocation(location)
    }

    fun adjustStock(productId: String, delta: Int) {
        viewModelScope.launch {
            catalogLocationRepository.adjustStock(productId, delta)
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
            workers = sessionManager.workers,
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

    private data class GlobalDataSlice(
        val locations: List<VenueLocation>,
        val products: List<Product>,
        val transactions: List<SaleTransaction>,
        val orders: List<PosOrder>,
        val tables: List<BarTable>
    )

    companion object {
        fun factory(container: PosAppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AdminGlobalViewModel(
                        sessionManager = container.sessionManager,
                        catalogLocationRepository = container.catalogLocationRepository,
                        orderSalesRepository = container.orderSalesRepository
                    ) as T
                }
            }
    }
}
