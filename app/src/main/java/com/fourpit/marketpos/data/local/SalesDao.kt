package com.fourpit.marketpos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {

    @Transaction
    @Query("SELECT * FROM pos_orders WHERE locationId = :locationId ORDER BY createdAtMillis DESC")
    fun observeOrdersForLocation(locationId: Long): Flow<List<OrderWithItemsEntity>>

    @Transaction
    @Query("SELECT * FROM pos_orders WHERE locationId = :locationId AND status = 'PAID' ORDER BY closedAtMillis DESC")
    fun observePaidOrdersForLocation(locationId: Long): Flow<List<OrderWithItemsEntity>>

    @Transaction
    @Query("SELECT * FROM pos_orders ORDER BY createdAtMillis DESC")
    fun observeAllOrders(): Flow<List<OrderWithItemsEntity>>

    @Query("SELECT * FROM worker_logs WHERE locationId = :locationId ORDER BY timestampMillis DESC LIMIT 100")
    fun observeWorkerLogsForLocation(locationId: Long): Flow<List<WorkerLogEntity>>

    @Query("SELECT * FROM worker_logs ORDER BY timestampMillis DESC LIMIT 150")
    fun observeAllWorkerLogs(): Flow<List<WorkerLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkerLog(log: WorkerLogEntity): Long
}
