package com.fourpit.marketpos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TableOrderDao {

    @Query("SELECT * FROM bar_tables WHERE locationId = :locationId ORDER BY id ASC")
    fun observeTablesForLocation(locationId: Long): Flow<List<BarTableEntity>>

    @Query("SELECT * FROM bar_tables WHERE id = :tableId LIMIT 1")
    suspend fun getTableById(tableId: Long): BarTableEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: BarTableEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTables(tables: List<BarTableEntity>)

    @Update
    suspend fun updateTable(table: BarTableEntity)

    @Query("DELETE FROM bar_tables WHERE id = :tableId")
    suspend fun deleteTableById(tableId: Long)

    @Query("UPDATE bar_tables SET activeOrderId = :orderId WHERE id = :tableId")
    suspend fun setTableActiveOrder(tableId: Long, orderId: Long?)

    @Transaction
    @Query("SELECT * FROM pos_orders WHERE locationId = :locationId AND status = 'OPEN' ORDER BY createdAtMillis DESC")
    fun observeOpenOrdersForLocation(locationId: Long): Flow<List<OrderWithItemsEntity>>

    @Transaction
    @Query("SELECT * FROM pos_orders WHERE id = :orderId LIMIT 1")
    fun observeOrderById(orderId: Long): Flow<OrderWithItemsEntity?>

    @Transaction
    @Query("SELECT * FROM pos_orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderById(orderId: Long): OrderWithItemsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: PosOrderEntity): Long

    @Update
    suspend fun updateOrder(order: PosOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: PosOrderItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<PosOrderItemEntity>)

    @Query("SELECT * FROM pos_order_items WHERE orderId = :orderId AND productId = :productId LIMIT 1")
    suspend fun findOrderItem(orderId: Long, productId: Long): PosOrderItemEntity?

    @Query("UPDATE pos_order_items SET quantity = :quantity WHERE id = :itemId")
    suspend fun updateOrderItemQuantity(itemId: Long, quantity: Int)

    @Query("DELETE FROM pos_order_items WHERE id = :itemId")
    suspend fun deleteOrderItemById(itemId: Long)

    @Query("SELECT * FROM pos_order_items WHERE orderId = :orderId")
    suspend fun getItemsForOrder(orderId: Long): List<PosOrderItemEntity>
}
