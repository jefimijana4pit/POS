package com.example.marketpos.data

import com.example.marketpos.data.local.BarTableEntity
import com.example.marketpos.data.local.CatalogLocationDao
import com.example.marketpos.data.local.TableOrderDao
import com.example.marketpos.data.local.toDomain
import com.example.marketpos.data.local.toEntity
import com.example.marketpos.data.seed.CatalogSeedData
import com.example.marketpos.model.Product
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.VenueMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class CatalogLocationRepository(
    private val catalogLocationDao: CatalogLocationDao,
    private val tableOrderDao: TableOrderDao
) {
    val locationsFlow: Flow<List<VenueLocation>> = catalogLocationDao.observeLocations().map { list ->
        list.map { it.toDomain() }
    }

    val productsFlow: Flow<List<Product>> = catalogLocationDao.observeProducts().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun saveLocation(location: VenueLocation, copyStarterData: Boolean = true) {
        val isNew = catalogLocationDao.observeLocations().first().none { it.id == location.id }
        catalogLocationDao.upsertLocation(location.toEntity())
        if (isNew && copyStarterData) {
            val starterProducts = if (location.venueMode == VenueMode.MARKET) {
                CatalogSeedData.buildMarketSeedProducts(location.id, idPrefix = "${location.id}_")
            } else {
                CatalogSeedData.buildBarSeedProducts(location.id, idPrefix = "${location.id}_")
            }
            catalogLocationDao.insertProducts(starterProducts.map { it.toEntity() })
            if (location.venueMode == VenueMode.BAR_RESTAURANT) {
                val starterTables = listOf(
                    BarTableEntity(id = "${location.id}_t1", name = "Sto 1", zone = "Sala", capacity = 4, sortOrder = 1, locationId = location.id),
                    BarTableEntity(id = "${location.id}_t2", name = "Sto 2", zone = "Sala", capacity = 4, sortOrder = 2, locationId = location.id),
                    BarTableEntity(id = "${location.id}_t3", name = "Sto 3", zone = "Bašta", capacity = 4, sortOrder = 3, locationId = location.id),
                    BarTableEntity(id = "${location.id}_t4", name = "Šank 1", zone = "Šank", capacity = 2, sortOrder = 4, locationId = location.id)
                )
                tableOrderDao.insertTables(starterTables)
            }
        }
    }

    suspend fun deleteLocation(locationId: String): Boolean {
        val allLocations = catalogLocationDao.observeLocations().first().map { it.toDomain() }
        val target = allLocations.firstOrNull { it.id == locationId } ?: return false
        val sameTypeCount = allLocations.count { it.venueMode == target.venueMode }
        if (sameTypeCount <= 1) return false
        if (tableOrderDao.countLockedTablesByLocation(locationId) > 0) return false
        tableOrderDao.deleteUnlockedTablesByLocation(locationId)
        catalogLocationDao.deleteProductsByLocation(locationId)
        catalogLocationDao.deleteLocationById(locationId)
        return true
    }

    suspend fun saveProduct(product: Product) {
        catalogLocationDao.upsertProduct(product.toEntity())
    }

    suspend fun adjustStock(productId: String, delta: Int) {
        catalogLocationDao.adjustStock(productId, delta)
    }

    suspend fun deleteProduct(productId: String) {
        catalogLocationDao.deleteProductById(productId)
    }
}
