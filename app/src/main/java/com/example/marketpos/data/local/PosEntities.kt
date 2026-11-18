package com.example.marketpos.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.marketpos.model.BarTable
import com.example.marketpos.model.DefaultLocations
import com.example.marketpos.model.OrderItemStatus
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.PosOrderItem
import com.example.marketpos.model.Product
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.model.SaleLineItem
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.model.TableOrderItem
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.VenueMode

@Entity(tableName = "venue_locations")
data class VenueLocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val address: String,
    val venueMode: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val sku: String,
    val nameSr: String,
    val nameEn: String,
    val category: String,
    val priceRsd: Double,
    val unitSr: String,
    val unitEn: String,
    val stockQuantity: Int = 25,
    val lowStockThreshold: Int = 5,
    val isAvailable: Boolean,
    val venueMode: String = VenueMode.MARKET.name,
    val locationId: String = DefaultLocations.MARKET_CENTRAL_ID
)

@Entity(tableName = "sale_transactions")
data class SaleTransactionEntity(
    @PrimaryKey val id: String,
    val receiptNumber: String,
    val workerId: String,
    val workerName: String,
    val timestampMillis: Long,
    val subtotalRsd: Double,
    val taxRsd: Double,
    val totalRsd: Double,
    val paymentMethod: String,
    val cashReceivedRsd: Double?,
    val changeRsd: Double?,
    val venueMode: String = VenueMode.MARKET.name,
    val tableId: String? = null,
    val tableName: String? = null,
    val locationId: String = DefaultLocations.MARKET_CENTRAL_ID
)

@Entity(
    tableName = "sale_line_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleTransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("transactionId")]
)
data class SaleLineItemEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val transactionId: String,
    val productId: String,
    val productNameSr: String,
    val productNameEn: String,
    val category: String,
    val unitPriceRsd: Double,
    val quantity: Int
)

data class TransactionWithItems(
    @Embedded val transaction: SaleTransactionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val items: List<SaleLineItemEntity>
)

@Entity(tableName = "bar_tables")
data class BarTableEntity(
    @PrimaryKey val id: String,
    val name: String,
    val zone: String,
    val capacity: Int,
    val lockedByWorkerId: String? = null,
    val lockedByWorkerName: String? = null,
    val lockedAtMillis: Long? = null,
    val awaitingCleanup: Boolean = false,
    val paidAndPrintedAtMillis: Long? = null,
    val sortOrder: Int = 0,
    val locationId: String = DefaultLocations.BAR_CENTER_ID
)

@Entity(
    tableName = "table_order_items",
    foreignKeys = [
        ForeignKey(
            entity = BarTableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tableId")]
)
data class TableOrderItemEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val tableId: String,
    val productId: String,
    val productNameSr: String,
    val productNameEn: String,
    val category: String,
    val unitPriceRsd: Double,
    val quantity: Int
)

data class BarTableWithItems(
    @Embedded val table: BarTableEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "tableId"
    )
    val items: List<TableOrderItemEntity>
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val orderNumber: String,
    val venueMode: String,
    val tableId: String? = null,
    val tableName: String? = null,
    val workerId: String,
    val workerName: String,
    val status: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val completedAtMillis: Long? = null,
    val transactionId: String? = null,
    val locationId: String = DefaultLocations.MARKET_CENTRAL_ID
)

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("orderId")]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val orderId: String,
    val productId: String,
    val productNameSr: String,
    val productNameEn: String,
    val category: String,
    val unitPriceRsd: Double,
    val quantity: Int,
    val status: String = OrderItemStatus.ACTIVE.name
)

data class OrderWithItems(
    @Embedded val order: OrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<OrderItemEntity>
)

fun VenueLocationEntity.toDomain(): VenueLocation = VenueLocation(
    id = id,
    name = name,
    address = address,
    venueMode = runCatching { VenueMode.valueOf(venueMode) }.getOrDefault(VenueMode.MARKET),
    sortOrder = sortOrder
)

fun VenueLocation.toEntity(): VenueLocationEntity = VenueLocationEntity(
    id = id,
    name = name,
    address = address,
    venueMode = venueMode.name,
    sortOrder = sortOrder
)

fun ProductEntity.toDomain(): Product {
    val parsedCat = runCatching { ProductCategory.valueOf(category) }.getOrDefault(ProductCategory.BAKERY)
    val parsedVenue = runCatching { VenueMode.valueOf(venueMode) }.getOrDefault(parsedCat.venueMode())
    return Product(
        id = id,
        sku = sku,
        nameSr = nameSr,
        nameEn = nameEn,
        category = parsedCat,
        priceRsd = priceRsd,
        unitSr = unitSr,
        unitEn = unitEn,
        stockQuantity = stockQuantity,
        lowStockThreshold = lowStockThreshold,
        isAvailable = isAvailable,
        venueMode = parsedVenue,
        locationId = locationId.ifBlank { DefaultLocations.defaultIdFor(parsedVenue) }
    )
}

fun Product.toEntity(): ProductEntity = ProductEntity(
    id = id,
    sku = sku,
    nameSr = nameSr,
    nameEn = nameEn,
    category = category.name,
    priceRsd = priceRsd,
    unitSr = unitSr,
    unitEn = unitEn,
    stockQuantity = stockQuantity,
    lowStockThreshold = lowStockThreshold,
    isAvailable = isAvailable,
    venueMode = venueMode.name,
    locationId = locationId
)

fun TransactionWithItems.toDomain(): SaleTransaction {
    val parsedVenue = runCatching { VenueMode.valueOf(transaction.venueMode) }.getOrDefault(VenueMode.MARKET)
    return SaleTransaction(
        id = transaction.id,
        receiptNumber = transaction.receiptNumber,
        workerId = transaction.workerId,
        workerName = transaction.workerName,
        timestampMillis = transaction.timestampMillis,
        items = items.map { item ->
            SaleLineItem(
                productId = item.productId,
                productNameSr = item.productNameSr,
                productNameEn = item.productNameEn,
                category = runCatching { ProductCategory.valueOf(item.category) }.getOrDefault(ProductCategory.BAKERY),
                unitPriceRsd = item.unitPriceRsd,
                quantity = item.quantity
            )
        },
        subtotalRsd = transaction.subtotalRsd,
        taxRsd = transaction.taxRsd,
        totalRsd = transaction.totalRsd,
        paymentMethod = runCatching { PaymentMethod.valueOf(transaction.paymentMethod) }.getOrDefault(PaymentMethod.CASH),
        cashReceivedRsd = transaction.cashReceivedRsd,
        changeRsd = transaction.changeRsd,
        venueMode = parsedVenue,
        tableId = transaction.tableId,
        tableName = transaction.tableName,
        locationId = transaction.locationId.ifBlank { DefaultLocations.defaultIdFor(parsedVenue) }
    )
}

fun BarTableWithItems.toDomain(): BarTable = BarTable(
    id = table.id,
    name = table.name,
    zone = table.zone,
    capacity = table.capacity,
    lockedByWorkerId = table.lockedByWorkerId,
    lockedByWorkerName = table.lockedByWorkerName,
    lockedAtMillis = table.lockedAtMillis,
    awaitingCleanup = table.awaitingCleanup,
    paidAndPrintedAtMillis = table.paidAndPrintedAtMillis,
    sortOrder = table.sortOrder,
    locationId = table.locationId.ifBlank { DefaultLocations.BAR_CENTER_ID },
    orderItems = items.map { item ->
        TableOrderItem(
            rowId = item.rowId,
            tableId = item.tableId,
            productId = item.productId,
            productNameSr = item.productNameSr,
            productNameEn = item.productNameEn,
            category = runCatching { ProductCategory.valueOf(item.category) }.getOrDefault(ProductCategory.BAR_DRINKS),
            unitPriceRsd = item.unitPriceRsd,
            quantity = item.quantity
        )
    }
)

fun BarTable.toEntity(): BarTableEntity = BarTableEntity(
    id = id,
    name = name,
    zone = zone,
    capacity = capacity,
    lockedByWorkerId = lockedByWorkerId,
    lockedByWorkerName = lockedByWorkerName,
    lockedAtMillis = lockedAtMillis,
    awaitingCleanup = awaitingCleanup,
    paidAndPrintedAtMillis = paidAndPrintedAtMillis,
    sortOrder = sortOrder,
    locationId = locationId
)

fun OrderWithItems.toDomain(): PosOrder {
    val parsedVenue = runCatching { VenueMode.valueOf(order.venueMode) }.getOrDefault(VenueMode.MARKET)
    return PosOrder(
        id = order.id,
        orderNumber = order.orderNumber,
        venueMode = parsedVenue,
        tableId = order.tableId,
        tableName = order.tableName,
        workerId = order.workerId,
        workerName = order.workerName,
        status = runCatching { OrderStatus.valueOf(order.status) }.getOrDefault(OrderStatus.PENDING),
        createdAtMillis = order.createdAtMillis,
        updatedAtMillis = order.updatedAtMillis,
        completedAtMillis = order.completedAtMillis,
        transactionId = order.transactionId,
        locationId = order.locationId.ifBlank { DefaultLocations.defaultIdFor(parsedVenue) },
        items = items.map { item ->
            PosOrderItem(
                id = item.rowId,
                orderId = item.orderId,
                productId = item.productId,
                productNameSr = item.productNameSr,
                productNameEn = item.productNameEn,
                category = runCatching { ProductCategory.valueOf(item.category) }.getOrDefault(ProductCategory.BAKERY),
                unitPriceRsd = item.unitPriceRsd,
                quantity = item.quantity,
                status = runCatching { OrderItemStatus.valueOf(item.status) }.getOrDefault(OrderItemStatus.ACTIVE)
            )
        }
    )
}
