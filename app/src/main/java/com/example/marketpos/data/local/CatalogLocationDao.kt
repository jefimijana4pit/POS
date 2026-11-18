package com.example.marketpos.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogLocationDao {

    @Query("SELECT * FROM venue_locations ORDER BY sortOrder ASC, name ASC")
    fun observeLocations(): Flow<List<VenueLocationEntity>>

    @Query("SELECT COUNT(*) FROM venue_locations")
    suspend fun countLocations(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocation(location: VenueLocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<VenueLocationEntity>)

    @Query("DELETE FROM venue_locations WHERE id = :locationId")
    suspend fun deleteLocationById(locationId: String)

    @Query("SELECT * FROM products ORDER BY nameSr ASC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Query("SELECT COUNT(*) FROM products WHERE venueMode = :venueMode")
    suspend fun countProductsByVenue(venueMode: String): Int

    @Query("SELECT COUNT(*) FROM products WHERE locationId = :locationId")
    suspend fun countProductsByLocation(locationId: String): Int

    @Query("DELETE FROM products WHERE locationId = :locationId")
    suspend fun deleteProductsByLocation(locationId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProductById(productId: String)

    @Query("UPDATE products SET stockQuantity = MAX(0, stockQuantity - :quantity) WHERE id = :productId")
    suspend fun decrementStock(productId: String, quantity: Int)

    @Query("UPDATE products SET stockQuantity = MAX(0, stockQuantity + :delta) WHERE id = :productId")
    suspend fun adjustStock(productId: String, delta: Int)
}
