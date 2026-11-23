package com.fourpit.marketpos.data.seed

import com.fourpit.marketpos.data.local.BarTableEntity
import com.fourpit.marketpos.data.local.LocationEntity
import com.fourpit.marketpos.data.local.ProductEntity
import com.fourpit.marketpos.model.VenueType

object CatalogSeedData {

    fun defaultLocations(): List<LocationEntity> = listOf(
        LocationEntity(
            name = "Market Centar",
            address = "Knez Mihailova 14, Beograd",
            venueType = VenueType.MARKET.name,
            isActive = true
        ),
        LocationEntity(
            name = "Caffe Bar Terasa",
            address = "Zmaj Jovina 22, Novi Sad",
            venueType = VenueType.BAR.name,
            isActive = true
        )
    )

    fun marketProducts(locationId: Long): List<ProductEntity> = listOf(
        ProductEntity(locationId = locationId, name = "Hleb Sava 500g", category = "Pekara", sku = "MKT-001", priceRsd = 65.0, stockQuantity = 42, lowStockThreshold = 10),
        ProductEntity(locationId = locationId, name = "Kifla sa susamom", category = "Pekara", sku = "MKT-002", priceRsd = 55.0, stockQuantity = 28, lowStockThreshold = 8),
        ProductEntity(locationId = locationId, name = "Mleko Moja Kravica 1L", category = "Mlečni", sku = "MKT-003", priceRsd = 155.0, stockQuantity = 36, lowStockThreshold = 10),
        ProductEntity(locationId = locationId, name = "Jogurt 1L", category = "Mlečni", sku = "MKT-004", priceRsd = 165.0, stockQuantity = 24, lowStockThreshold = 8),
        ProductEntity(locationId = locationId, name = "Feta sir 250g", category = "Mlečni", sku = "MKT-005", priceRsd = 290.0, stockQuantity = 7, lowStockThreshold = 10),
        ProductEntity(locationId = locationId, name = "Knjaz Miloš 1.5L", category = "Pića", sku = "MKT-006", priceRsd = 85.0, stockQuantity = 60, lowStockThreshold = 15),
        ProductEntity(locationId = locationId, name = "Sok Pomorandža 1L", category = "Pića", sku = "MKT-007", priceRsd = 195.0, stockQuantity = 30, lowStockThreshold = 10),
        ProductEntity(locationId = locationId, name = "Domaća kafa 200g", category = "Kafa i čaj", sku = "MKT-008", priceRsd = 380.0, stockQuantity = 18, lowStockThreshold = 8),
        ProductEntity(locationId = locationId, name = "Plazma keks 150g", category = "Slatkiši", sku = "MKT-009", priceRsd = 175.0, stockQuantity = 45, lowStockThreshold = 12),
        ProductEntity(locationId = locationId, name = "Čokolada Mlečna 100g", category = "Slatkiši", sku = "MKT-010", priceRsd = 160.0, stockQuantity = 6, lowStockThreshold = 10),
        ProductEntity(locationId = locationId, name = "Jabuka Ajdared 1kg", category = "Voće i povrće", sku = "MKT-011", priceRsd = 130.0, stockQuantity = 25, lowStockThreshold = 8),
        ProductEntity(locationId = locationId, name = "Banane 1kg", category = "Voće i povrće", sku = "MKT-012", priceRsd = 210.0, stockQuantity = 19, lowStockThreshold = 8)
    )

    fun barProducts(locationId: Long): List<ProductEntity> = listOf(
        ProductEntity(locationId = locationId, name = "Espresso", category = "Kafa", sku = "BAR-001", priceRsd = 220.0, stockQuantity = 120, lowStockThreshold = 20),
        ProductEntity(locationId = locationId, name = "Cappuccino", category = "Kafa", sku = "BAR-002", priceRsd = 260.0, stockQuantity = 110, lowStockThreshold = 20),
        ProductEntity(locationId = locationId, name = "Caffe Latte", category = "Kafa", sku = "BAR-003", priceRsd = 290.0, stockQuantity = 95, lowStockThreshold = 15),
        ProductEntity(locationId = locationId, name = "Ceđena pomorandža", category = "Bezalkoholna", sku = "BAR-004", priceRsd = 340.0, stockQuantity = 35, lowStockThreshold = 10),
        ProductEntity(locationId = locationId, name = "Limunada sa nanom", category = "Bezalkoholna", sku = "BAR-005", priceRsd = 280.0, stockQuantity = 44, lowStockThreshold = 10),
        ProductEntity(locationId = locationId, name = "Kisela voda 0.25L", category = "Bezalkoholna", sku = "BAR-006", priceRsd = 190.0, stockQuantity = 65, lowStockThreshold = 15),
        ProductEntity(locationId = locationId, name = "Točeno pivo 0.5L", category = "Pivo", sku = "BAR-007", priceRsd = 360.0, stockQuantity = 50, lowStockThreshold = 12),
        ProductEntity(locationId = locationId, name = "Craft IPA 0.33L", category = "Pivo", sku = "BAR-008", priceRsd = 450.0, stockQuantity = 8, lowStockThreshold = 10),
        ProductEntity(locationId = locationId, name = "Vino belo 0.15L", category = "Vino", sku = "BAR-009", priceRsd = 420.0, stockQuantity = 32, lowStockThreshold = 8),
        ProductEntity(locationId = locationId, name = "Aperol Spritz", category = "Kokteli", sku = "BAR-010", priceRsd = 620.0, stockQuantity = 24, lowStockThreshold = 8),
        ProductEntity(locationId = locationId, name = "Tost sendvič", category = "Hrana", sku = "BAR-011", priceRsd = 480.0, stockQuantity = 16, lowStockThreshold = 6),
        ProductEntity(locationId = locationId, name = "Cheesecake", category = "Deserti", sku = "BAR-012", priceRsd = 440.0, stockQuantity = 5, lowStockThreshold = 6)
    )

    fun barTables(locationId: Long): List<BarTableEntity> = listOf(
        BarTableEntity(locationId = locationId, label = "Sto 1", seats = 2),
        BarTableEntity(locationId = locationId, label = "Sto 2", seats = 4),
        BarTableEntity(locationId = locationId, label = "Sto 3", seats = 4),
        BarTableEntity(locationId = locationId, label = "Sto 4", seats = 6),
        BarTableEntity(locationId = locationId, label = "Bašta 1", seats = 4),
        BarTableEntity(locationId = locationId, label = "Bašta 2", seats = 4),
        BarTableEntity(locationId = locationId, label = "Šank 1", seats = 2),
        BarTableEntity(locationId = locationId, label = "VIP Loža", seats = 8)
    )
}
