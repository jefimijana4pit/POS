package com.fourpit.marketpos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogLocationDao {

    @Query("SELECT * FROM locations ORDER BY id ASC")
    fun observeAllLocations(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations ORDER BY id ASC")
    suspend fun getAllLocationsOnce(): List<LocationEntity>

    @Query("SELECT * FROM locations WHERE id = :locationId LIMIT 1")
    suspend fun getLocationById(locationId: Long): LocationEntity?

    @Query("SELECT * FROM locations WHERE id = :locationId LIMIT 1")
    fun observeLocationById(locationId: Long): Flow<LocationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: LocationEntity): Long

    @Update
    suspend fun updateLocation(location: LocationEntity)

    @Query("DELETE FROM locations WHERE id = :locationId")
    suspend fun deleteLocationById(locationId: Long)

    @Query("SELECT * FROM products WHERE locationId = :locationId AND isActive = 1 ORDER BY category ASC, name ASC")
    fun observeProductsForLocation(locationId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY locationId ASC, category ASC, name ASC")
    fun observeAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun getProductById(productId: Long): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProductById(productId: Long)

    @Query("UPDATE products SET stockQuantity = MAX(0, stockQuantity - :delta) WHERE id = :productId")
    suspend fun decrementStock(productId: Long, delta: Int)

    @Query("UPDATE products SET stockQuantity = stockQuantity + :delta WHERE id = :productId")
    suspend fun incrementStock(productId: Long, delta: Int)
}
