package com.fourpit.marketpos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fourpit.marketpos.data.CatalogLocationRepository
import com.fourpit.marketpos.data.OrderSalesRepository
import com.fourpit.marketpos.model.OrderStatus
import com.fourpit.marketpos.model.PaymentMethod
import com.fourpit.marketpos.model.PosLocation
import com.fourpit.marketpos.model.Product
import com.fourpit.marketpos.model.VenueType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PosDaoOrderLifecycleTest {

    private lateinit var db: PosDatabase
    private lateinit var catalogRepo: CatalogLocationRepository
    private lateinit var salesRepo: OrderSalesRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        catalogRepo = CatalogLocationRepository(db.catalogLocationDao(), db.tableOrderDao())
        salesRepo = OrderSalesRepository(db.catalogLocationDao(), db.tableOrderDao(), db.salesDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun barTableOrder_openAddSettle_freesTableAndDecrementsStock() = runTest {
        val barId = catalogRepo.saveLocation(
            PosLocation(name = "Test Bar", address = "Main 1", venueType = VenueType.BAR)
        )
        val tables = catalogRepo.observeTablesForLocation(barId).first()
        val table = tables.first()
        val prodId = catalogRepo.saveProduct(
            Product(
                locationId = barId,
                name = "Espresso",
                category = "Kafa",
                sku = "ESP-1",
                priceRsd = 220.0,
                stockQuantity = 10
            )
        )
        val product = catalogRepo.observeProductsForLocation(barId).first().first { it.id == prodId }

        val orderId = salesRepo.openOrGetTableOrder(barId, table.id, "Jelena")
        salesRepo.addProductToOpenOrder(orderId, product, deltaQty = 2)

        val openOrder = salesRepo.getOrderById(orderId)
        assertEquals(OrderStatus.OPEN, openOrder?.status)
        assertEquals(440.0, openOrder?.totalRsd ?: 0.0, 0.001)

        salesRepo.settleOpenOrder(orderId, PaymentMethod.CARD, 440.0, 20.0, "Jelena")

        val settled = salesRepo.getOrderById(orderId)
        assertEquals(OrderStatus.PAID, settled?.status)
        val updatedTable = catalogRepo.observeTablesForLocation(barId).first().first { it.id == table.id }
        assertNull(updatedTable.activeOrderId)
        val updatedProduct = catalogRepo.observeProductsForLocation(barId).first().first { it.id == prodId }
        assertEquals(8, updatedProduct.stockQuantity)
    }
}
