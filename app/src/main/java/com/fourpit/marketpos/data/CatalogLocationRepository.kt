package com.fourpit.marketpos.data

import com.fourpit.marketpos.data.local.BarTableEntity
import com.fourpit.marketpos.data.local.CatalogLocationDao
import com.fourpit.marketpos.data.local.LocationEntity
import com.fourpit.marketpos.data.local.ProductEntity
import com.fourpit.marketpos.data.local.TableOrderDao
import com.fourpit.marketpos.model.BarTable
import com.fourpit.marketpos.model.PosLocation
import com.fourpit.marketpos.model.Product
import com.fourpit.marketpos.model.VenueType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CatalogLocationRepository(
    private val catalogDao: CatalogLocationDao,
    private val tableOrderDao: TableOrderDao
) {

    fun observeLocations(): Flow<List<PosLocation>> =
        catalogDao.observeAllLocations().map { list -> list.map { it.toModel() } }

    fun observeLocation(locationId: Long): Flow<PosLocation?> =
        catalogDao.observeLocationById(locationId).map { it?.toModel() }

    suspend fun getLocation(locationId: Long): PosLocation? =
        catalogDao.getLocationById(locationId)?.toModel()

    suspend fun saveLocation(location: PosLocation): Long {
        val entity = LocationEntity(
            id = location.id,
            name = location.name.trim(),
            address = location.address.trim(),
            venueType = location.venueType.name,
            isActive = location.isActive
        )
        return if (location.id == 0L) {
            val newId = catalogDao.insertLocation(entity)
            if (location.venueType == VenueType.BAR) {
                tableOrderDao.insertTables(
                    listOf(
                        BarTableEntity(locationId = newId, label = "Sto 1", seats = 4),
                        BarTableEntity(locationId = newId, label = "Sto 2", seats = 4),
                        BarTableEntity(locationId = newId, label = "Sto 3", seats = 2)
                    )
                )
            }
            newId
        } else {
            catalogDao.updateLocation(entity)
            location.id
        }
    }

    suspend fun deleteLocation(locationId: Long) {
        catalogDao.deleteLocationById(locationId)
    }

    fun observeProductsForLocation(locationId: Long): Flow<List<Product>> =
        catalogDao.observeProductsForLocation(locationId).map { list -> list.map { it.toModel() } }

    fun observeAllProducts(): Flow<List<Product>> =
        catalogDao.observeAllProducts().map { list -> list.map { it.toModel() } }

    suspend fun saveProduct(product: Product): Long {
        val entity = ProductEntity(
            id = product.id,
            locationId = product.locationId,
            name = product.name.trim(),
            category = product.category.trim().ifBlank { "General" },
            sku = product.sku.trim().ifBlank { "SKU-${System.currentTimeMillis() % 10000}" },
            priceRsd = product.priceRsd.coerceAtLeast(0.0),
            stockQuantity = product.stockQuantity.coerceAtLeast(0),
            lowStockThreshold = product.lowStockThreshold.coerceAtLeast(0),
            isActive = product.isActive
        )
        return if (product.id == 0L) {
            catalogDao.insertProduct(entity)
        } else {
            catalogDao.updateProduct(entity)
            product.id
        }
    }

    suspend fun restockProduct(productId: Long, delta: Int) {
        if (delta > 0) {
            catalogDao.incrementStock(productId, delta)
        }
    }

    suspend fun deleteProduct(productId: Long) {
        catalogDao.deleteProductById(productId)
    }

    fun observeTablesForLocation(locationId: Long): Flow<List<BarTable>> {
        return combine(
            tableOrderDao.observeTablesForLocation(locationId),
            tableOrderDao.observeOpenOrdersForLocation(locationId)
        ) { tables, openOrders ->
            val ordersById = openOrders.associateBy { it.order.id }
            tables.map { entity ->
                val open = entity.activeOrderId?.let { ordersById[it] }
                entity.toModel(
                    openTotalRsd = open?.order?.totalRsd ?: 0.0,
                    openItemCount = open?.items?.sumOf { it.quantity } ?: 0
                )
            }
        }
    }

    suspend fun saveTable(table: BarTable): Long {
        val entity = BarTableEntity(
            id = table.id,
            locationId = table.locationId,
            label = table.label.trim(),
            seats = table.seats.coerceIn(1, 24),
            activeOrderId = table.activeOrderId
        )
        return if (table.id == 0L) {
            tableOrderDao.insertTable(entity)
        } else {
            tableOrderDao.updateTable(entity)
            table.id
        }
    }

    suspend fun deleteTable(tableId: Long) {
        tableOrderDao.deleteTableById(tableId)
    }
}
