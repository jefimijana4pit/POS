package com.fourpit.marketpos.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.fourpit.marketpos.model.BarTable
import com.fourpit.marketpos.model.OrderStatus
import com.fourpit.marketpos.model.PaymentMethod
import com.fourpit.marketpos.model.PosLocation
import com.fourpit.marketpos.model.PosOrder
import com.fourpit.marketpos.model.PosOrderItem
import com.fourpit.marketpos.model.Product
import com.fourpit.marketpos.model.UserRole
import com.fourpit.marketpos.model.VenueType
import com.fourpit.marketpos.model.WorkerActivityLog

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val address: String,
    val venueType: String,
    val isActive: Boolean = true
) {
    fun toModel(): PosLocation = PosLocation(
        id = id,
        name = name,
        address = address,
        venueType = runCatching { VenueType.valueOf(venueType) }.getOrDefault(VenueType.MARKET),
        isActive = isActive
    )
}

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("locationId")]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val locationId: Long,
    val name: String,
    val category: String,
    val sku: String,
    val priceRsd: Double,
    val stockQuantity: Int,
    val lowStockThreshold: Int = 10,
    val isActive: Boolean = true
) {
    fun toModel(): Product = Product(
        id = id,
        locationId = locationId,
        name = name,
        category = category,
        sku = sku,
        priceRsd = priceRsd,
        stockQuantity = stockQuantity,
        lowStockThreshold = lowStockThreshold,
        isActive = isActive
    )
}

@Entity(
    tableName = "bar_tables",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("locationId")]
)
data class BarTableEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val locationId: Long,
    val label: String,
    val seats: Int = 4,
    val activeOrderId: Long? = null
) {
    fun toModel(openTotalRsd: Double = 0.0, openItemCount: Int = 0): BarTable = BarTable(
        id = id,
        locationId = locationId,
        label = label,
        seats = seats,
        activeOrderId = activeOrderId,
        openTotalRsd = openTotalRsd,
        openItemCount = openItemCount
    )
}

@Entity(
    tableName = "pos_orders",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("locationId"), Index("tableId"), Index("status")]
)
data class PosOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val receiptNumber: String,
    val locationId: Long,
    val tableId: Long? = null,
    val tableLabel: String? = null,
    val workerName: String,
    val status: String,
    val paymentMethod: String? = null,
    val createdAtMillis: Long,
    val closedAtMillis: Long? = null,
    val totalRsd: Double,
    val taxAmountRsd: Double = 0.0,
    val receivedRsd: Double = 0.0,
    val changeRsd: Double = 0.0
)

@Entity(
    tableName = "pos_order_items",
    foreignKeys = [
        ForeignKey(
            entity = PosOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId"), Index("productId")]
)
data class PosOrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val category: String,
    val unitPriceRsd: Double,
    val quantity: Int
) {
    fun toModel(): PosOrderItem = PosOrderItem(
        id = id,
        orderId = orderId,
        productId = productId,
        productName = productName,
        category = category,
        unitPriceRsd = unitPriceRsd,
        quantity = quantity
    )
}

data class OrderWithItemsEntity(
    @Embedded val order: PosOrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<PosOrderItemEntity>
) {
    fun toModel(locationName: String = ""): PosOrder = PosOrder(
        id = order.id,
        receiptNumber = order.receiptNumber,
        locationId = order.locationId,
        locationName = locationName,
        tableId = order.tableId,
        tableLabel = order.tableLabel,
        workerName = order.workerName,
        status = runCatching { OrderStatus.valueOf(order.status) }.getOrDefault(OrderStatus.OPEN),
        paymentMethod = order.paymentMethod?.let { runCatching { PaymentMethod.valueOf(it) }.getOrNull() },
        createdAtMillis = order.createdAtMillis,
        closedAtMillis = order.closedAtMillis,
        totalRsd = order.totalRsd,
        taxAmountRsd = order.taxAmountRsd,
        receivedRsd = order.receivedRsd,
        changeRsd = order.changeRsd,
        items = items.map { it.toModel() }
    )
}

@Entity(
    tableName = "worker_logs",
    indices = [Index("locationId"), Index("timestampMillis")]
)
data class WorkerLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val locationId: Long,
    val workerName: String,
    val role: String,
    val action: String,
    val details: String,
    val timestampMillis: Long
) {
    fun toModel(): WorkerActivityLog = WorkerActivityLog(
        id = id,
        locationId = locationId,
        workerName = workerName,
        role = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.MARKET_WORKER),
        action = action,
        details = details,
        timestampMillis = timestampMillis
    )
}
