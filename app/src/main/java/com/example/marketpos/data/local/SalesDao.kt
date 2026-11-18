package com.example.marketpos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesDao {

    @Transaction
    @Query("SELECT * FROM sale_transactions ORDER BY timestampMillis DESC")
    fun observeTransactionsWithItems(): Flow<List<TransactionWithItems>>

    @Query("SELECT COUNT(*) FROM sale_transactions")
    suspend fun countTransactions(): Int

    @Query("SELECT COUNT(*) FROM sale_transactions WHERE venueMode = :venueMode")
    suspend fun countTransactionsByVenue(venueMode: String): Int

    @Query("SELECT COUNT(*) FROM sale_transactions WHERE locationId = :locationId")
    suspend fun countTransactionsByLocation(locationId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: SaleTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(items: List<SaleLineItemEntity>)

    @Query("UPDATE products SET stockQuantity = MAX(0, stockQuantity - :quantity) WHERE id = :productId")
    suspend fun decrementStock(productId: String, quantity: Int)

    @Transaction
    suspend fun insertFullSale(
        transaction: SaleTransactionEntity,
        items: List<SaleLineItemEntity>,
        deductStock: Boolean = true
    ) {
        insertTransaction(transaction)
        insertLineItems(items)
        if (deductStock) {
            items.forEach { line ->
                decrementStock(line.productId, line.quantity)
            }
        }
    }
}
