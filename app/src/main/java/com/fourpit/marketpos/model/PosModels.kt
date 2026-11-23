package com.fourpit.marketpos.model

import com.fourpit.marketpos.ui.theme.FontScaleOption
import com.fourpit.marketpos.ui.theme.ThemeMode

enum class UserRole {
    ADMIN,
    MARKET_WORKER,
    BAR_WORKER
}

enum class VenueType {
    MARKET,
    BAR
}

enum class OrderStatus {
    OPEN,
    PAID,
    CANCELLED
}

enum class PaymentMethod {
    CASH,
    CARD
}

enum class TableStatus {
    AVAILABLE,
    OCCUPIED
}

enum class AppLanguage {
    SR_LATIN,
    EN
}

enum class CurrencyCode {
    RSD,
    EUR
}

enum class PrinterMode {
    ANDROID_SYSTEM,
    BLUETOOTH_THERMAL
}

enum class ReportPeriod {
    TODAY,
    LAST_7_DAYS,
    THIS_MONTH,
    ALL_TIME
}

data class PosLocation(
    val id: Long = 0L,
    val name: String,
    val address: String,
    val venueType: VenueType,
    val isActive: Boolean = true
)

data class Product(
    val id: Long = 0L,
    val locationId: Long,
    val name: String,
    val category: String,
    val sku: String,
    val priceRsd: Double,
    val stockQuantity: Int,
    val lowStockThreshold: Int = 10,
    val isActive: Boolean = true
) {
    val isLowStock: Boolean
        get() = stockQuantity <= lowStockThreshold
}

data class BarTable(
    val id: Long = 0L,
    val locationId: Long,
    val label: String,
    val seats: Int = 4,
    val activeOrderId: Long? = null,
    val openTotalRsd: Double = 0.0,
    val openItemCount: Int = 0
) {
    val status: TableStatus
        get() = if (activeOrderId != null) TableStatus.OCCUPIED else TableStatus.AVAILABLE
}

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val lineTotalRsd: Double
        get() = product.priceRsd * quantity
}

data class PosOrderItem(
    val id: Long = 0L,
    val orderId: Long = 0L,
    val productId: Long,
    val productName: String,
    val category: String,
    val unitPriceRsd: Double,
    val quantity: Int
) {
    val lineTotalRsd: Double
        get() = unitPriceRsd * quantity
}

data class PosOrder(
    val id: Long = 0L,
    val receiptNumber: String,
    val locationId: Long,
    val locationName: String = "",
    val tableId: Long? = null,
    val tableLabel: String? = null,
    val workerName: String,
    val status: OrderStatus,
    val paymentMethod: PaymentMethod? = null,
    val createdAtMillis: Long,
    val closedAtMillis: Long? = null,
    val totalRsd: Double,
    val taxAmountRsd: Double = 0.0,
    val receivedRsd: Double = 0.0,
    val changeRsd: Double = 0.0,
    val items: List<PosOrderItem> = emptyList()
) {
    val itemCount: Int
        get() = items.sumOf { it.quantity }
}

data class WorkerActivityLog(
    val id: Long = 0L,
    val locationId: Long,
    val workerName: String,
    val role: UserRole,
    val action: String,
    val details: String,
    val timestampMillis: Long
)

data class PosSettings(
    val language: AppLanguage = AppLanguage.SR_LATIN,
    val currency: CurrencyCode = CurrencyCode.RSD,
    val eurExchangeRate: Double = 117.2,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val fontScaleOption: FontScaleOption = FontScaleOption.NORMAL,
    val businessName: String = "MarketPOS Retail",
    val taxRatePercent: Double = 20.0,
    val printerMode: PrinterMode = PrinterMode.ANDROID_SYSTEM,
    val bluetoothPrinterName: String = "",
    val bluetoothPrinterAddress: String = ""
)

data class UserSession(
    val role: UserRole,
    val workerName: String,
    val locationId: Long?
) {
    fun canAccessLocation(targetLocationId: Long): Boolean {
        return role == UserRole.ADMIN || locationId == targetLocationId
    }

    fun canAccessVenueType(venueType: VenueType): Boolean {
        return when (role) {
            UserRole.ADMIN -> true
            UserRole.MARKET_WORKER -> venueType == VenueType.MARKET
            UserRole.BAR_WORKER -> venueType == VenueType.BAR
        }
    }
}
