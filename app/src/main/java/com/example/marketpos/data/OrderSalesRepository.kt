package com.example.marketpos.data

import androidx.room.withTransaction
import com.example.marketpos.data.local.OrderEntity
import com.example.marketpos.data.local.OrderItemEntity
import com.example.marketpos.data.local.PosDatabase
import com.example.marketpos.data.local.SaleLineItemEntity
import com.example.marketpos.data.local.SaleTransactionEntity
import com.example.marketpos.data.local.SalesDao
import com.example.marketpos.data.local.TableOrderDao
import com.example.marketpos.data.local.toDomain
import com.example.marketpos.data.local.toEntity
import com.example.marketpos.model.BarTable
import com.example.marketpos.model.CartItem
import com.example.marketpos.model.DefaultLocations
import com.example.marketpos.model.OrderItemStatus
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.Product
import com.example.marketpos.model.SaleLineItem
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.model.TableOrderItem
import com.example.marketpos.model.VenueMode
import com.example.marketpos.model.WorkerProfile
import com.example.marketpos.util.CurrencyFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class OrderSalesRepository(
    private val database: PosDatabase,
    private val salesDao: SalesDao,
    private val tableOrderDao: TableOrderDao
) {
    val transactionsFlow: Flow<List<SaleTransaction>> = salesDao.observeTransactionsWithItems().map { list ->
        list.map { it.toDomain() }
    }

    val tablesFlow: Flow<List<BarTable>> = tableOrderDao.observeTablesWithItems().map { list ->
        list.map { it.toDomain() }
    }

    val ordersFlow: Flow<List<PosOrder>> = tableOrderDao.observeOrdersWithItems().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun saveTable(table: BarTable): Boolean =
        tableOrderDao.saveTableIfUnlocked(table.toEntity())

    suspend fun deleteTable(tableId: String): Boolean =
        tableOrderDao.deleteTableById(tableId) > 0

    suspend fun lockTableIfFree(tableId: String, worker: WorkerProfile): Boolean {
        val updatedRows = tableOrderDao.lockTableIfFree(
            tableId = tableId,
            workerId = worker.id,
            workerName = worker.name,
            nowMillis = System.currentTimeMillis()
        )
        return updatedRows > 0
    }

    suspend fun unlockTable(tableId: String) {
        tableOrderDao.unlockAndClearTable(tableId)
    }

    suspend fun startNewOrderOnLockedTable(tableId: String) {
        tableOrderDao.resumeTableForNewOrder(tableId)
    }

    suspend fun addProductToTable(tableId: String, worker: WorkerProfile, product: Product): Boolean =
        tableOrderDao.lockAndAddOrIncrementTableItem(
            tableId = tableId,
            workerId = worker.id,
            workerName = worker.name,
            nowMillis = System.currentTimeMillis(),
            product = product.toEntity()
        )

    suspend fun decrementTableItem(tableId: String, productId: String) {
        tableOrderDao.decrementTableItem(tableId, productId, System.currentTimeMillis())
    }

    suspend fun removeTableItem(tableId: String, productId: String) {
        tableOrderDao.cancelTableItem(tableId, productId, System.currentTimeMillis())
    }

    suspend fun clearTableOrder(tableId: String) {
        tableOrderDao.cancelPendingTableOrder(tableId, System.currentTimeMillis())
    }

    suspend fun autoUnlockExpiredTables(nowMillis: Long) {
        val cutoff = nowMillis - BarTable.TABLE_CLEANUP_AUTO_UNLOCK_MILLIS
        tableOrderDao.autoUnlockExpiredTables(cutoff)
    }

    suspend fun createPendingOrder(
        worker: WorkerProfile,
        venueMode: VenueMode,
        locationId: String,
        cartItems: List<CartItem>
    ): String {
        val now = System.currentTimeMillis()
        val orderId = "ord_${UUID.randomUUID().toString().take(8)}"
        val prefix = if (venueMode == VenueMode.BAR_RESTAURANT) "BR" else "MP"
        val nextNum = tableOrderDao.countOrders() + 101
        val order = OrderEntity(
            id = orderId,
            orderNumber = "$prefix-$nextNum",
            venueMode = venueMode.name,
            tableId = null,
            tableName = null,
            workerId = worker.id,
            workerName = worker.name,
            status = OrderStatus.PENDING.name,
            createdAtMillis = now,
            updatedAtMillis = now,
            locationId = locationId
        )
        val items = cartItems.map { item ->
            OrderItemEntity(
                orderId = orderId,
                productId = item.product.id,
                productNameSr = item.product.nameSr,
                productNameEn = item.product.nameEn,
                category = item.product.category.name,
                unitPriceRsd = item.product.priceRsd,
                quantity = item.quantity,
                status = OrderItemStatus.ACTIVE.name
            )
        }
        tableOrderDao.createOrderWithItems(order, items)
        return orderId
    }

    suspend fun addProductToPendingOrder(orderId: String, product: Product): Boolean =
        tableOrderDao.addProductToPendingOrder(
            orderId = orderId,
            product = product.toEntity(),
            nowMillis = System.currentTimeMillis()
        )

    suspend fun adjustOrderItemQuantity(orderId: String, itemRowId: Long, delta: Int) {
        tableOrderDao.adjustOrderItemQuantity(
            orderId = orderId,
            rowId = itemRowId,
            delta = delta,
            nowMillis = System.currentTimeMillis()
        )
    }

    suspend fun cancelOrderItem(orderId: String, itemRowId: Long) {
        tableOrderDao.cancelOrderItemByRowId(
            orderId = orderId,
            rowId = itemRowId,
            nowMillis = System.currentTimeMillis()
        )
    }

    suspend fun cancelOrder(orderId: String) {
        tableOrderDao.cancelFullOrder(
            orderId = orderId,
            nowMillis = System.currentTimeMillis()
        )
    }

    suspend fun recordSale(
        worker: WorkerProfile,
        cartItems: List<CartItem>,
        paymentMethod: PaymentMethod,
        cashReceivedRsd: Double?,
        venueMode: VenueMode = VenueMode.MARKET,
        locationId: String = DefaultLocations.defaultIdFor(venueMode)
    ): SaleTransaction {
        val subtotalRsd = cartItems.sumOf { it.product.priceRsd * it.quantity }
        val taxRsd = subtotalRsd * CurrencyFormatter.VAT_RATE
        val totalRsd = subtotalRsd + taxRsd
        val changeRsd = if (paymentMethod == PaymentMethod.CASH && cashReceivedRsd != null) {
            (cashReceivedRsd - totalRsd).coerceAtLeast(0.0)
        } else {
            null
        }

        val txId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val existingCount = salesDao.countTransactions() + 1001
        val prefix = if (venueMode == VenueMode.BAR_RESTAURANT) "BR" else "MP"
        val receiptNumber = "$prefix-$existingCount"

        val txEntity = SaleTransactionEntity(
            id = txId,
            receiptNumber = receiptNumber,
            workerId = worker.id,
            workerName = worker.name,
            timestampMillis = now,
            subtotalRsd = subtotalRsd,
            taxRsd = taxRsd,
            totalRsd = totalRsd,
            paymentMethod = paymentMethod.name,
            cashReceivedRsd = cashReceivedRsd,
            changeRsd = changeRsd,
            venueMode = venueMode.name,
            tableId = null,
            tableName = null,
            locationId = locationId
        )

        val lineEntities = cartItems.map { item ->
            SaleLineItemEntity(
                transactionId = txId,
                productId = item.product.id,
                productNameSr = item.product.nameSr,
                productNameEn = item.product.nameEn,
                category = item.product.category.name,
                unitPriceRsd = item.product.priceRsd,
                quantity = item.quantity
            )
        }

        val orderId = "ord_${UUID.randomUUID().toString().take(8)}"
        val orderNum = "$prefix-${tableOrderDao.countOrders() + 101}"
        val orderEntity = OrderEntity(
            id = orderId,
            orderNumber = orderNum,
            venueMode = venueMode.name,
            tableId = null,
            tableName = null,
            workerId = worker.id,
            workerName = worker.name,
            status = OrderStatus.FINISHED.name,
            createdAtMillis = now,
            updatedAtMillis = now,
            completedAtMillis = now,
            transactionId = txId,
            locationId = locationId
        )
        val orderItemEntities = cartItems.map { item ->
            OrderItemEntity(
                orderId = orderId,
                productId = item.product.id,
                productNameSr = item.product.nameSr,
                productNameEn = item.product.nameEn,
                category = item.product.category.name,
                unitPriceRsd = item.product.priceRsd,
                quantity = item.quantity,
                status = OrderItemStatus.ACTIVE.name
            )
        }

        database.withTransaction {
            salesDao.insertFullSale(txEntity, lineEntities, deductStock = true)
            tableOrderDao.insertOrder(orderEntity)
            tableOrderDao.insertOrderItems(orderItemEntities)
        }

        return SaleTransaction(
            id = txId,
            receiptNumber = receiptNumber,
            workerId = worker.id,
            workerName = worker.name,
            timestampMillis = now,
            items = cartItems.map { item ->
                SaleLineItem(
                    productId = item.product.id,
                    productNameSr = item.product.nameSr,
                    productNameEn = item.product.nameEn,
                    category = item.product.category,
                    unitPriceRsd = item.product.priceRsd,
                    quantity = item.quantity
                )
            },
            subtotalRsd = subtotalRsd,
            taxRsd = taxRsd,
            totalRsd = totalRsd,
            paymentMethod = paymentMethod,
            cashReceivedRsd = cashReceivedRsd,
            changeRsd = changeRsd,
            venueMode = venueMode,
            locationId = locationId
        )
    }

    suspend fun completeOrderSale(
        worker: WorkerProfile,
        order: PosOrder,
        paymentMethod: PaymentMethod,
        cashReceivedRsd: Double?
    ): SaleTransaction {
        val activeItems = order.activeItems
        val subtotalRsd = activeItems.sumOf { it.lineTotalRsd }
        val taxRsd = subtotalRsd * CurrencyFormatter.VAT_RATE
        val totalRsd = subtotalRsd + taxRsd
        val changeRsd = if (paymentMethod == PaymentMethod.CASH && cashReceivedRsd != null) {
            (cashReceivedRsd - totalRsd).coerceAtLeast(0.0)
        } else {
            null
        }

        val txId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val existingCount = salesDao.countTransactions() + 1001
        val prefix = if (order.venueMode == VenueMode.BAR_RESTAURANT) "BR" else "MP"
        val receiptNumber = "$prefix-$existingCount"

        val txEntity = SaleTransactionEntity(
            id = txId,
            receiptNumber = receiptNumber,
            workerId = worker.id,
            workerName = worker.name,
            timestampMillis = now,
            subtotalRsd = subtotalRsd,
            taxRsd = taxRsd,
            totalRsd = totalRsd,
            paymentMethod = paymentMethod.name,
            cashReceivedRsd = cashReceivedRsd,
            changeRsd = changeRsd,
            venueMode = order.venueMode.name,
            tableId = order.tableId,
            tableName = order.tableName,
            locationId = order.locationId
        )

        val lineEntities = activeItems.map { item ->
            SaleLineItemEntity(
                transactionId = txId,
                productId = item.productId,
                productNameSr = item.productNameSr,
                productNameEn = item.productNameEn,
                category = item.category.name,
                unitPriceRsd = item.unitPriceRsd,
                quantity = item.quantity
            )
        }

        database.withTransaction {
            salesDao.insertFullSale(txEntity, lineEntities, deductStock = true)
            tableOrderDao.markOrderFinished(order.id, txId, now)
            order.tableId?.let { tblId ->
                tableOrderDao.clearTableOrderItems(tblId)
                tableOrderDao.markTableAwaitingCleanup(tblId, now)
            }
        }

        return SaleTransaction(
            id = txId,
            receiptNumber = receiptNumber,
            workerId = worker.id,
            workerName = worker.name,
            timestampMillis = now,
            items = activeItems.map { item ->
                SaleLineItem(
                    productId = item.productId,
                    productNameSr = item.productNameSr,
                    productNameEn = item.productNameEn,
                    category = item.category,
                    unitPriceRsd = item.unitPriceRsd,
                    quantity = item.quantity
                )
            },
            subtotalRsd = subtotalRsd,
            taxRsd = taxRsd,
            totalRsd = totalRsd,
            paymentMethod = paymentMethod,
            cashReceivedRsd = cashReceivedRsd,
            changeRsd = changeRsd,
            venueMode = order.venueMode,
            tableId = order.tableId,
            tableName = order.tableName,
            locationId = order.locationId
        )
    }

    suspend fun completeTableSale(
        worker: WorkerProfile,
        table: BarTable,
        orderItems: List<TableOrderItem>,
        paymentMethod: PaymentMethod,
        cashReceivedRsd: Double?
    ): SaleTransaction {
        val subtotalRsd = orderItems.sumOf { it.unitPriceRsd * it.quantity }
        val taxRsd = subtotalRsd * CurrencyFormatter.VAT_RATE
        val totalRsd = subtotalRsd + taxRsd
        val changeRsd = if (paymentMethod == PaymentMethod.CASH && cashReceivedRsd != null) {
            (cashReceivedRsd - totalRsd).coerceAtLeast(0.0)
        } else {
            null
        }

        val txId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val existingCount = salesDao.countTransactions() + 1001
        val receiptNumber = "BR-$existingCount"

        val txEntity = SaleTransactionEntity(
            id = txId,
            receiptNumber = receiptNumber,
            workerId = worker.id,
            workerName = worker.name,
            timestampMillis = now,
            subtotalRsd = subtotalRsd,
            taxRsd = taxRsd,
            totalRsd = totalRsd,
            paymentMethod = paymentMethod.name,
            cashReceivedRsd = cashReceivedRsd,
            changeRsd = changeRsd,
            venueMode = VenueMode.BAR_RESTAURANT.name,
            tableId = table.id,
            tableName = table.name,
            locationId = table.locationId
        )

        val lineEntities = orderItems.map { item ->
            SaleLineItemEntity(
                transactionId = txId,
                productId = item.productId,
                productNameSr = item.productNameSr,
                productNameEn = item.productNameEn,
                category = item.category.name,
                unitPriceRsd = item.unitPriceRsd,
                quantity = item.quantity
            )
        }

        database.withTransaction {
            salesDao.insertFullSale(txEntity, lineEntities, deductStock = true)
            tableOrderDao.clearTableOrderItems(table.id)
            tableOrderDao.markTableAwaitingCleanup(table.id, now)
            val pendingOrder = tableOrderDao.findPendingOrderForTable(table.id)
            if (pendingOrder != null) {
                tableOrderDao.markOrderFinished(pendingOrder.id, txId, now)
            }
        }

        return SaleTransaction(
            id = txId,
            receiptNumber = receiptNumber,
            workerId = worker.id,
            workerName = worker.name,
            timestampMillis = now,
            items = orderItems.map { item ->
                SaleLineItem(
                    productId = item.productId,
                    productNameSr = item.productNameSr,
                    productNameEn = item.productNameEn,
                    category = item.category,
                    unitPriceRsd = item.unitPriceRsd,
                    quantity = item.quantity
                )
            },
            subtotalRsd = subtotalRsd,
            taxRsd = taxRsd,
            totalRsd = totalRsd,
            paymentMethod = paymentMethod,
            cashReceivedRsd = cashReceivedRsd,
            changeRsd = changeRsd,
            venueMode = VenueMode.BAR_RESTAURANT,
            tableId = table.id,
            tableName = table.name,
            locationId = table.locationId
        )
    }
}
