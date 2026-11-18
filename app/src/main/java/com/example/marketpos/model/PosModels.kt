package com.example.marketpos.model

import com.example.marketpos.ui.theme.FontScaleOption
import com.example.marketpos.ui.theme.ThemeMode

enum class AppLanguage(val tag: String) {
    SERBIAN_LATIN("sr-Latn"),
    ENGLISH("en")
}

enum class VenueMode {
    MARKET,
    BAR_RESTAURANT
}

object DefaultLocations {
    const val MARKET_CENTRAL_ID = "loc_mkt_central"
    const val MARKET_NBG_ID = "loc_mkt_nbg"
    const val BAR_CENTER_ID = "loc_bar_center"
    const val BAR_RIVER_ID = "loc_bar_river"

    fun defaultIdFor(venueMode: VenueMode): String = when (venueMode) {
        VenueMode.MARKET -> MARKET_CENTRAL_ID
        VenueMode.BAR_RESTAURANT -> BAR_CENTER_ID
    }
}

data class VenueLocation(
    val id: String,
    val name: String,
    val address: String,
    val venueMode: VenueMode,
    val sortOrder: Int = 0
)

enum class UserRole {
    MARKET_WORKER,
    RESTAURANT_WORKER,
    ADMIN;

    val isWorker: Boolean
        get() = this != ADMIN

    fun assignedVenueMode(): VenueMode? = when (this) {
        MARKET_WORKER -> VenueMode.MARKET
        RESTAURANT_WORKER -> VenueMode.BAR_RESTAURANT
        ADMIN -> null
    }
}

enum class PrinterType {
    ANDROID_SYSTEM,
    BLUETOOTH_THERMAL
}

enum class PaymentMethod {
    CASH,
    CARD
}

enum class ProductCategory {
    // Market categories
    BAKERY,
    DAIRY,
    BEVERAGES,
    PRODUCE,
    SNACKS,
    HOUSEHOLD,
    // Bar & Restaurant categories
    COFFEE_TEA,
    BAR_DRINKS,
    MAIN_DISHES,
    DESSERTS;

    fun venueMode(): VenueMode = when (this) {
        BAKERY, DAIRY, BEVERAGES, PRODUCE, SNACKS, HOUSEHOLD -> VenueMode.MARKET
        COFFEE_TEA, BAR_DRINKS, MAIN_DISHES, DESSERTS -> VenueMode.BAR_RESTAURANT
    }

    companion object {
        fun forVenue(mode: VenueMode): List<ProductCategory> = entries.filter { it.venueMode() == mode }
    }
}

data class WorkerProfile(
    val id: String,
    val name: String,
    val role: UserRole,
    val pin: String,
    val shiftLabelSr: String,
    val shiftLabelEn: String,
    val locationId: String = DefaultLocations.defaultIdFor(
        role.assignedVenueMode() ?: VenueMode.MARKET
    )
)

data class Product(
    val id: String,
    val sku: String,
    val nameSr: String,
    val nameEn: String,
    val category: ProductCategory,
    val priceRsd: Double,
    val unitSr: String = "kom",
    val unitEn: String = "pcs",
    val stockQuantity: Int = 25,
    val lowStockThreshold: Int = 5,
    val isAvailable: Boolean = true,
    val venueMode: VenueMode = category.venueMode(),
    val locationId: String = DefaultLocations.defaultIdFor(venueMode)
) {
    val isInStock: Boolean
        get() = isAvailable && stockQuantity > 0

    val isLowStock: Boolean
        get() = isAvailable && stockQuantity in 1..lowStockThreshold

    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.SERBIAN_LATIN -> nameSr
        AppLanguage.ENGLISH -> nameEn
    }

    fun displayUnit(language: AppLanguage): String = when (language) {
        AppLanguage.SERBIAN_LATIN -> unitSr
        AppLanguage.ENGLISH -> unitEn
    }
}

data class CartItem(
    val product: Product,
    val quantity: Int
)

data class SaleLineItem(
    val productId: String,
    val productNameSr: String,
    val productNameEn: String,
    val category: ProductCategory,
    val unitPriceRsd: Double,
    val quantity: Int
) {
    val lineTotalRsd: Double get() = unitPriceRsd * quantity

    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.SERBIAN_LATIN -> productNameSr
        AppLanguage.ENGLISH -> productNameEn
    }
}

data class SaleTransaction(
    val id: String,
    val receiptNumber: String,
    val workerId: String,
    val workerName: String,
    val timestampMillis: Long,
    val items: List<SaleLineItem>,
    val subtotalRsd: Double,
    val taxRsd: Double,
    val totalRsd: Double,
    val paymentMethod: PaymentMethod,
    val cashReceivedRsd: Double?,
    val changeRsd: Double?,
    val venueMode: VenueMode = VenueMode.MARKET,
    val tableId: String? = null,
    val tableName: String? = null,
    val locationId: String = DefaultLocations.defaultIdFor(venueMode)
)

data class TableOrderItem(
    val rowId: Long = 0,
    val tableId: String,
    val productId: String,
    val productNameSr: String,
    val productNameEn: String,
    val category: ProductCategory,
    val unitPriceRsd: Double,
    val quantity: Int
) {
    val lineTotalRsd: Double get() = unitPriceRsd * quantity

    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.SERBIAN_LATIN -> productNameSr
        AppLanguage.ENGLISH -> productNameEn
    }
}

data class BarTable(
    val id: String,
    val name: String,
    val zone: String,
    val capacity: Int,
    val lockedByWorkerId: String? = null,
    val lockedByWorkerName: String? = null,
    val lockedAtMillis: Long? = null,
    val awaitingCleanup: Boolean = false,
    val paidAndPrintedAtMillis: Long? = null,
    val sortOrder: Int = 0,
    val orderItems: List<TableOrderItem> = emptyList(),
    val locationId: String = DefaultLocations.BAR_CENTER_ID
) {
    val isLocked: Boolean
        get() = lockedByWorkerId != null

    val itemCount: Int
        get() = orderItems.sumOf { it.quantity }

    val orderSubtotalRsd: Double
        get() = orderItems.sumOf { it.lineTotalRsd }

    val orderTaxRsd: Double
        get() = orderSubtotalRsd * 0.20

    val orderTotalRsd: Double
        get() = orderSubtotalRsd + orderTaxRsd

    fun isLockedBy(workerId: String?): Boolean = workerId != null && lockedByWorkerId == workerId

    fun isLockedByOther(workerId: String?): Boolean = lockedByWorkerId != null && lockedByWorkerId != workerId

    fun remainingCleanupMillis(nowMillis: Long): Long {
        val paidAt = paidAndPrintedAtMillis ?: return 0L
        if (!awaitingCleanup) return 0L
        return (TABLE_CLEANUP_AUTO_UNLOCK_MILLIS - (nowMillis - paidAt)).coerceAtLeast(0L)
    }

    fun availabilityOrderRank(currentWorkerId: String? = null): Int = when {
        !isLocked -> 0
        awaitingCleanup && isLockedBy(currentWorkerId) -> 1
        isLockedBy(currentWorkerId) -> 2
        awaitingCleanup -> 3
        else -> 4
    }

    companion object {
        const val TABLE_CLEANUP_AUTO_UNLOCK_MILLIS = 15L * 60L * 1000L
    }
}

enum class OrderStatus {
    PENDING,
    FINISHED,
    CANCELLED
}

enum class OrderItemStatus {
    ACTIVE,
    CANCELLED
}

data class PosOrderItem(
    val id: Long = 0,
    val orderId: String,
    val productId: String,
    val productNameSr: String,
    val productNameEn: String,
    val category: ProductCategory,
    val unitPriceRsd: Double,
    val quantity: Int,
    val status: OrderItemStatus = OrderItemStatus.ACTIVE
) {
    val isCancelled: Boolean
        get() = status == OrderItemStatus.CANCELLED

    val grossLineTotalRsd: Double
        get() = unitPriceRsd * quantity

    val lineTotalRsd: Double
        get() = if (isCancelled) 0.0 else unitPriceRsd * quantity

    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.SERBIAN_LATIN -> productNameSr
        AppLanguage.ENGLISH -> productNameEn
    }
}

data class PosOrder(
    val id: String,
    val orderNumber: String,
    val venueMode: VenueMode,
    val tableId: String? = null,
    val tableName: String? = null,
    val workerId: String,
    val workerName: String,
    val status: OrderStatus,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val completedAtMillis: Long? = null,
    val transactionId: String? = null,
    val items: List<PosOrderItem> = emptyList(),
    val locationId: String = DefaultLocations.defaultIdFor(venueMode)
) {
    val activeItems: List<PosOrderItem>
        get() = items.filter { !it.isCancelled }

    val cancelledItems: List<PosOrderItem>
        get() = items.filter { it.isCancelled }

    val activeItemCount: Int
        get() = activeItems.sumOf { it.quantity }

    val cancelledItemCount: Int
        get() = cancelledItems.sumOf { it.quantity }

    val subtotalRsd: Double
        get() = activeItems.sumOf { it.lineTotalRsd }

    val taxRsd: Double
        get() = subtotalRsd * 0.20

    val totalRsd: Double
        get() = subtotalRsd + taxRsd

    val isEditable: Boolean
        get() = status == OrderStatus.PENDING
}

fun List<BarTable>.sortedByAvailability(currentWorkerId: String? = null): List<BarTable> =
    sortedWith(
        compareBy<BarTable>(
            { it.availabilityOrderRank(currentWorkerId) },
            { it.sortOrder },
            { it.name.lowercase() }
        )
    )

data class AppSettings(
    val language: AppLanguage = AppLanguage.SERBIAN_LATIN,
    val venueMode: VenueMode = VenueMode.MARKET,
    val selectedLocationId: String = DefaultLocations.MARKET_CENTRAL_ID,
    val rsdPerEur: Double = 117.0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val fontScale: FontScaleOption = FontScaleOption.NORMAL,
    val printerType: PrinterType = PrinterType.ANDROID_SYSTEM,
    val selectedPrinterName: String? = null,
    val selectedPrinterAddress: String? = null
)

data class LocationStats(
    val location: VenueLocation,
    val revenueRsd: Double,
    val cashRevenueRsd: Double,
    val cardRevenueRsd: Double,
    val transactionCount: Int,
    val pendingOrdersCount: Int,
    val cancelledOrdersCount: Int,
    val productsCount: Int,
    val totalStockUnits: Int,
    val lowOrOutStockCount: Int,
    val stockValueRsd: Double,
    val freeTablesCount: Int,
    val totalTablesCount: Int
)

enum class ReportPeriodType {
    DAILY,
    MONTHLY,
    DATE_RANGE
}

data class WorkerProductSoldStat(
    val productId: String,
    val nameSr: String,
    val nameEn: String,
    val quantity: Int,
    val revenueRsd: Double
) {
    fun displayName(language: AppLanguage): String = when (language) {
        AppLanguage.SERBIAN_LATIN -> nameSr
        AppLanguage.ENGLISH -> nameEn
    }
}

data class WorkerReportRow(
    val workerId: String,
    val workerName: String,
    val finishedBillsCount: Int,
    val cancelledOrdersCount: Int,
    val cancelledItemsCount: Int,
    val tablesHandledCount: Int,
    val totalUnitsSold: Int,
    val revenueRsd: Double,
    val productsSold: List<WorkerProductSoldStat>
)

data class LocationReportSummary(
    val location: VenueLocation?,
    val isGlobal: Boolean,
    val periodType: ReportPeriodType,
    val startMillis: Long,
    val endMillis: Long,
    val generatedAtMillis: Long,
    val totalRevenueRsd: Double,
    val cashRevenueRsd: Double,
    val cardRevenueRsd: Double,
    val finishedBillsCount: Int,
    val cancelledOrdersCount: Int,
    val tablesHandledCount: Int,
    val totalUnitsSold: Int,
    val workerRows: List<WorkerReportRow>
)

sealed class PdfExportResult {
    data class Success(
        val fileName: String,
        val uriString: String,
        val shareUriString: String,
        val savedToDownloads: Boolean
    ) : PdfExportResult()

    data class Error(val message: String) : PdfExportResult()
}

data class PairedPrinterDevice(
    val name: String,
    val address: String,
    val isSimulated: Boolean = false
)

sealed class PrintResult {
    data class Success(val message: String, val escPosPreview: String? = null) : PrintResult()
    data class PermissionRequired(val permissions: List<String>) : PrintResult()
    data class Error(val message: String) : PrintResult()
}
