package com.example.marketpos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TableOrderDao {

    @Transaction
    @Query("SELECT * FROM bar_tables ORDER BY sortOrder ASC, name ASC")
    fun observeTablesWithItems(): Flow<List<BarTableWithItems>>

    @Query("SELECT * FROM bar_tables WHERE id = :tableId LIMIT 1")
    suspend fun findTableById(tableId: String): BarTableEntity?

    @Query("SELECT COUNT(*) FROM bar_tables")
    suspend fun countTables(): Int

    @Query("SELECT COUNT(*) FROM bar_tables WHERE locationId = :locationId")
    suspend fun countTablesByLocation(locationId: String): Int

    @Query("SELECT COUNT(*) FROM bar_tables WHERE locationId = :locationId AND lockedByWorkerId IS NOT NULL")
    suspend fun countLockedTablesByLocation(locationId: String): Int

    @Query("DELETE FROM bar_tables WHERE locationId = :locationId AND lockedByWorkerId IS NULL")
    suspend fun deleteUnlockedTablesByLocation(locationId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTable(table: BarTableEntity)

    @Transaction
    suspend fun saveTableIfUnlocked(table: BarTableEntity): Boolean {
        val existing = findTableById(table.id)
        if (existing != null && existing.lockedByWorkerId != null) {
            return false
        }
        upsertTable(table)
        return true
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTables(tables: List<BarTableEntity>)

    @Query("DELETE FROM bar_tables WHERE id = :tableId AND lockedByWorkerId IS NULL")
    suspend fun deleteTableById(tableId: String): Int

    @Query(
        """
        UPDATE bar_tables 
        SET lockedByWorkerId = :workerId, 
            lockedByWorkerName = :workerName, 
            lockedAtMillis = :nowMillis, 
            awaitingCleanup = 0, 
            paidAndPrintedAtMillis = NULL 
        WHERE id = :tableId AND lockedByWorkerId IS NULL
        """
    )
    suspend fun lockTableIfFree(
        tableId: String,
        workerId: String,
        workerName: String,
        nowMillis: Long
    ): Int

    @Query(
        """
        UPDATE bar_tables 
        SET lockedByWorkerId = NULL, 
            lockedByWorkerName = NULL, 
            lockedAtMillis = NULL, 
            awaitingCleanup = 0, 
            paidAndPrintedAtMillis = NULL 
        WHERE id = :tableId
        """
    )
    suspend fun clearTableLock(tableId: String)

    @Query(
        """
        UPDATE bar_tables 
        SET awaitingCleanup = 1, 
            paidAndPrintedAtMillis = :paidAtMillis 
        WHERE id = :tableId
        """
    )
    suspend fun markTableAwaitingCleanup(tableId: String, paidAtMillis: Long)

    @Query(
        """
        UPDATE bar_tables 
        SET awaitingCleanup = 0, 
            paidAndPrintedAtMillis = NULL 
        WHERE id = :tableId
        """
    )
    suspend fun resumeTableForNewOrder(tableId: String)

    @Query(
        """
        UPDATE bar_tables 
        SET lockedByWorkerId = NULL, 
            lockedByWorkerName = NULL, 
            lockedAtMillis = NULL, 
            awaitingCleanup = 0, 
            paidAndPrintedAtMillis = NULL 
        WHERE awaitingCleanup = 1 
          AND paidAndPrintedAtMillis IS NOT NULL 
          AND paidAndPrintedAtMillis <= :cutoffMillis
        """
    )
    suspend fun autoUnlockExpiredTables(cutoffMillis: Long): Int

    @Query("SELECT * FROM table_order_items WHERE tableId = :tableId AND productId = :productId LIMIT 1")
    suspend fun findTableOrderItem(tableId: String, productId: String): TableOrderItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTableOrderItem(item: TableOrderItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTableOrderItems(items: List<TableOrderItemEntity>)

    @Query("UPDATE table_order_items SET quantity = :quantity WHERE rowId = :rowId")
    suspend fun updateTableOrderItemQuantity(rowId: Long, quantity: Int)

    @Query("DELETE FROM table_order_items WHERE rowId = :rowId")
    suspend fun deleteTableOrderItemByRowId(rowId: Long)

    @Query("DELETE FROM table_order_items WHERE tableId = :tableId AND productId = :productId")
    suspend fun deleteTableOrderItem(tableId: String, productId: String)

    @Query("DELETE FROM table_order_items WHERE tableId = :tableId")
    suspend fun clearTableOrderItems(tableId: String)

    @Transaction
    @Query("SELECT * FROM orders ORDER BY updatedAtMillis DESC")
    fun observeOrdersWithItems(): Flow<List<OrderWithItems>>

    @Query("SELECT COUNT(*) FROM orders")
    suspend fun countOrders(): Int

    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun findOrderById(orderId: String): OrderEntity?

    @Query("SELECT * FROM orders WHERE tableId = :tableId AND status = 'PENDING' ORDER BY updatedAtMillis DESC LIMIT 1")
    suspend fun findPendingOrderForTable(tableId: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: OrderItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId AND productId = :productId AND status = 'ACTIVE' LIMIT 1")
    suspend fun findActiveOrderItem(orderId: String, productId: String): OrderItemEntity?

    @Query("SELECT * FROM order_items WHERE rowId = :rowId LIMIT 1")
    suspend fun findOrderItemByRowId(rowId: Long): OrderItemEntity?

    @Query("UPDATE order_items SET quantity = :quantity WHERE rowId = :rowId")
    suspend fun updateOrderItemQuantity(rowId: Long, quantity: Int)

    @Query("UPDATE order_items SET status = :status WHERE rowId = :rowId")
    suspend fun updateOrderItemStatus(rowId: Long, status: String)

    @Query("UPDATE order_items SET status = 'CANCELLED' WHERE orderId = :orderId AND status = 'ACTIVE'")
    suspend fun markActiveOrderItemsCancelled(orderId: String)

    @Query("SELECT COUNT(*) FROM order_items WHERE orderId = :orderId AND status = 'ACTIVE'")
    suspend fun countActiveOrderItems(orderId: String): Int

    @Query("UPDATE orders SET updatedAtMillis = :updatedAtMillis WHERE id = :orderId")
    suspend fun updateOrderUpdatedAt(orderId: String, updatedAtMillis: Long)

    @Query("UPDATE orders SET status = 'FINISHED', transactionId = :transactionId, completedAtMillis = :completedAtMillis, updatedAtMillis = :completedAtMillis WHERE id = :orderId")
    suspend fun markOrderFinished(orderId: String, transactionId: String, completedAtMillis: Long)

    @Query("UPDATE orders SET status = 'CANCELLED', updatedAtMillis = :updatedAtMillis WHERE id = :orderId")
    suspend fun markOrderCancelled(orderId: String, updatedAtMillis: Long)

    @Transaction
    suspend fun lockAndAddOrIncrementTableItem(
        tableId: String,
        workerId: String,
        workerName: String,
        nowMillis: Long,
        product: ProductEntity
    ): Boolean {
        val table = findTableById(tableId) ?: return false
        if (table.lockedByWorkerId == null) {
            val lockedRows = lockTableIfFree(tableId, workerId, workerName, nowMillis)
            if (lockedRows == 0) return false
        } else if (table.lockedByWorkerId != workerId) {
            return false
        }
        addOrIncrementTableItem(
            tableId = tableId,
            workerId = workerId,
            workerName = workerName,
            tableName = table.name,
            locationId = table.locationId,
            nowMillis = nowMillis,
            product = product
        )
        return true
    }

    @Transaction
    suspend fun addOrIncrementTableItem(
        tableId: String,
        workerId: String,
        workerName: String,
        tableName: String,
        locationId: String = "loc_bar_center",
        nowMillis: Long,
        product: ProductEntity
    ) {
        resumeTableForNewOrder(tableId)
        val existing = findTableOrderItem(tableId, product.id)
        if (existing != null) {
            updateTableOrderItemQuantity(existing.rowId, existing.quantity + 1)
        } else {
            insertTableOrderItem(
                TableOrderItemEntity(
                    tableId = tableId,
                    productId = product.id,
                    productNameSr = product.nameSr,
                    productNameEn = product.nameEn,
                    category = product.category,
                    unitPriceRsd = product.priceRsd,
                    quantity = 1
                )
            )
        }

        val pendingOrder = findPendingOrderForTable(tableId)
        val orderId = if (pendingOrder != null) {
            updateOrderUpdatedAt(pendingOrder.id, nowMillis)
            pendingOrder.id
        } else {
            val nextNum = countOrders() + 101
            val newId = "ord_${tableId}_$nowMillis"
            insertOrder(
                OrderEntity(
                    id = newId,
                    orderNumber = "BR-$nextNum",
                    venueMode = "BAR_RESTAURANT",
                    tableId = tableId,
                    tableName = tableName,
                    workerId = workerId,
                    workerName = workerName,
                    status = "PENDING",
                    createdAtMillis = nowMillis,
                    updatedAtMillis = nowMillis,
                    locationId = locationId
                )
            )
            newId
        }
        upsertActiveOrderItem(orderId, product)
    }

    @Transaction
    suspend fun decrementTableItem(tableId: String, productId: String, nowMillis: Long) {
        val existing = findTableOrderItem(tableId, productId) ?: return
        if (existing.quantity > 1) {
            updateTableOrderItemQuantity(existing.rowId, existing.quantity - 1)
        } else {
            deleteTableOrderItemByRowId(existing.rowId)
        }

        val pendingOrder = findPendingOrderForTable(tableId) ?: return
        val activeItem = findActiveOrderItem(pendingOrder.id, productId)
        if (activeItem != null) {
            if (activeItem.quantity > 1) {
                updateOrderItemQuantity(activeItem.rowId, activeItem.quantity - 1)
                updateOrderUpdatedAt(pendingOrder.id, nowMillis)
            } else {
                updateOrderItemStatus(activeItem.rowId, "CANCELLED")
                updateOrderUpdatedAt(pendingOrder.id, nowMillis)
                if (countActiveOrderItems(pendingOrder.id) == 0) {
                    markOrderCancelled(pendingOrder.id, nowMillis)
                    clearTableLock(tableId)
                }
            }
        }
    }

    @Transaction
    suspend fun cancelTableItem(tableId: String, productId: String, nowMillis: Long) {
        deleteTableOrderItem(tableId, productId)
        val pendingOrder = findPendingOrderForTable(tableId) ?: return
        val activeItem = findActiveOrderItem(pendingOrder.id, productId) ?: return
        updateOrderItemStatus(activeItem.rowId, "CANCELLED")
        updateOrderUpdatedAt(pendingOrder.id, nowMillis)
        if (countActiveOrderItems(pendingOrder.id) == 0) {
            markOrderCancelled(pendingOrder.id, nowMillis)
            clearTableLock(tableId)
        }
    }

    @Transaction
    suspend fun cancelPendingTableOrder(tableId: String, nowMillis: Long) {
        clearTableOrderItems(tableId)
        clearTableLock(tableId)
        val pendingOrder = findPendingOrderForTable(tableId) ?: return
        markActiveOrderItemsCancelled(pendingOrder.id)
        markOrderCancelled(pendingOrder.id, nowMillis)
    }

    @Transaction
    suspend fun unlockAndClearTable(tableId: String, nowMillis: Long = System.currentTimeMillis()) {
        clearTableOrderItems(tableId)
        clearTableLock(tableId)
        val pendingOrder = findPendingOrderForTable(tableId)
        if (pendingOrder != null) {
            markActiveOrderItemsCancelled(pendingOrder.id)
            markOrderCancelled(pendingOrder.id, nowMillis)
        }
    }

    @Transaction
    suspend fun upsertActiveOrderItem(orderId: String, product: ProductEntity) {
        val existing = findActiveOrderItem(orderId, product.id)
        if (existing != null) {
            updateOrderItemQuantity(existing.rowId, existing.quantity + 1)
        } else {
            insertOrderItem(
                OrderItemEntity(
                    orderId = orderId,
                    productId = product.id,
                    productNameSr = product.nameSr,
                    productNameEn = product.nameEn,
                    category = product.category,
                    unitPriceRsd = product.priceRsd,
                    quantity = 1,
                    status = "ACTIVE"
                )
            )
        }
    }

    @Transaction
    suspend fun createOrderWithItems(order: OrderEntity, items: List<OrderItemEntity>) {
        insertOrder(order)
        insertOrderItems(items)
    }

    @Transaction
    suspend fun addProductToPendingOrder(orderId: String, product: ProductEntity, nowMillis: Long): Boolean {
        val order = findOrderById(orderId) ?: return false
        if (order.status != "PENDING") return false
        upsertActiveOrderItem(orderId, product)
        updateOrderUpdatedAt(orderId, nowMillis)
        order.tableId?.let { tblId ->
            resumeTableForNewOrder(tblId)
            val existingTableItem = findTableOrderItem(tblId, product.id)
            if (existingTableItem != null) {
                updateTableOrderItemQuantity(existingTableItem.rowId, existingTableItem.quantity + 1)
            } else {
                insertTableOrderItem(
                    TableOrderItemEntity(
                        tableId = tblId,
                        productId = product.id,
                        productNameSr = product.nameSr,
                        productNameEn = product.nameEn,
                        category = product.category,
                        unitPriceRsd = product.priceRsd,
                        quantity = 1
                    )
                )
            }
        }
        return true
    }

    @Transaction
    suspend fun adjustOrderItemQuantity(orderId: String, rowId: Long, delta: Int, nowMillis: Long) {
        val order = findOrderById(orderId) ?: return
        if (order.status != "PENDING") return
        val item = findOrderItemByRowId(rowId) ?: return
        if (item.orderId != orderId || item.status != "ACTIVE") return
        val nextQty = item.quantity + delta
        if (nextQty >= 1) {
            updateOrderItemQuantity(rowId, nextQty)
            updateOrderUpdatedAt(orderId, nowMillis)
            order.tableId?.let { tblId ->
                val tblItem = findTableOrderItem(tblId, item.productId)
                if (tblItem != null) {
                    updateTableOrderItemQuantity(tblItem.rowId, nextQty)
                }
            }
        } else {
            cancelOrderItemByRowId(orderId, rowId, nowMillis)
        }
    }

    @Transaction
    suspend fun cancelOrderItemByRowId(orderId: String, rowId: Long, nowMillis: Long) {
        val order = findOrderById(orderId) ?: return
        if (order.status != "PENDING") return
        val item = findOrderItemByRowId(rowId) ?: return
        if (item.orderId != orderId || item.status != "ACTIVE") return
        updateOrderItemStatus(rowId, "CANCELLED")
        updateOrderUpdatedAt(orderId, nowMillis)
        order.tableId?.let { tblId ->
            deleteTableOrderItem(tblId, item.productId)
        }
        if (countActiveOrderItems(orderId) == 0) {
            markOrderCancelled(orderId, nowMillis)
            order.tableId?.let { tblId ->
                clearTableOrderItems(tblId)
                clearTableLock(tblId)
            }
        }
    }

    @Transaction
    suspend fun cancelFullOrder(orderId: String, nowMillis: Long) {
        val order = findOrderById(orderId) ?: return
        if (order.status != "PENDING") return
        markActiveOrderItemsCancelled(orderId)
        markOrderCancelled(orderId, nowMillis)
        order.tableId?.let { tblId ->
            clearTableOrderItems(tblId)
            clearTableLock(tblId)
        }
    }
}
