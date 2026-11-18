package com.example.marketpos.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.marketpos.model.OrderItemStatus
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.model.VenueMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PosDaoOrderLifecycleTest {

    private lateinit var database: PosDatabase
    private lateinit var catalogDao: CatalogLocationDao
    private lateinit var salesDao: SalesDao
    private lateinit var tableOrderDao: TableOrderDao

    private val espresso = ProductEntity(
        id = "p-esp",
        sku = "BAR-01",
        nameSr = "Espreso",
        nameEn = "Espresso",
        category = ProductCategory.COFFEE_TEA.name,
        priceRsd = 220.0,
        unitSr = "kom",
        unitEn = "pcs",
        stockQuantity = 20,
        lowStockThreshold = 5,
        isAvailable = true,
        venueMode = VenueMode.BAR_RESTAURANT.name
    )

    private val lemonade = ProductEntity(
        id = "p-lem",
        sku = "BAR-02",
        nameSr = "Limunada",
        nameEn = "Lemonade",
        category = ProductCategory.BAR_DRINKS.name,
        priceRsd = 310.0,
        unitSr = "kom",
        unitEn = "pcs",
        stockQuantity = 15,
        lowStockThreshold = 5,
        isAvailable = true,
        venueMode = VenueMode.BAR_RESTAURANT.name
    )

    private val table = BarTableEntity(
        id = "t1",
        name = "Sto 1",
        zone = "Sala",
        capacity = 4,
        sortOrder = 0
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        catalogDao = database.catalogLocationDao()
        salesDao = database.salesDao()
        tableOrderDao = database.tableOrderDao()
        runBlocking<Unit> {
            catalogDao.insertProducts(listOf(espresso, lemonade))
            tableOrderDao.upsertTable(table)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun tableOrder_startsPending_supportsSingleItemCancellation_andUnlocksWhenLastItemCancelled() = runBlocking {
        tableOrderDao.lockAndAddOrIncrementTableItem(
            tableId = "t1",
            workerId = "w1",
            workerName = "Marko",
            product = espresso,
            nowMillis = 1_000L
        )
        tableOrderDao.lockAndAddOrIncrementTableItem(
            tableId = "t1",
            workerId = "w1",
            workerName = "Marko",
            product = lemonade,
            nowMillis = 2_000L
        )

        var orders = tableOrderDao.observeOrdersWithItems().first().map { it.toDomain() }
        assertEquals(1, orders.size)
        val initialOrder = orders.first()
        assertEquals(OrderStatus.PENDING, initialOrder.status)
        assertEquals(2, initialOrder.activeItems.size)
        assertEquals("t1", initialOrder.tableId)

        val lockedTable = tableOrderDao.findTableById("t1")
        assertEquals("w1", lockedTable?.lockedByWorkerId)

        // Cancel single item (lemonade)
        val lemonadeRow = initialOrder.activeItems.first { it.productId == lemonade.id }
        tableOrderDao.cancelOrderItemByRowId(initialOrder.id, lemonadeRow.id, nowMillis = 3_000L)

        orders = tableOrderDao.observeOrdersWithItems().first().map { it.toDomain() }
        val afterSingleCancel = orders.first()
        assertEquals(OrderStatus.PENDING, afterSingleCancel.status)
        assertEquals(1, afterSingleCancel.activeItems.size)
        assertEquals(1, afterSingleCancel.cancelledItems.size)
        assertEquals(OrderItemStatus.CANCELLED, afterSingleCancel.cancelledItems.first().status)
        assertNotNull(tableOrderDao.findTableById("t1")?.lockedByWorkerId)

        // Cancel remaining active item (espresso) -> order becomes CANCELLED & table unlocks
        val espressoRow = afterSingleCancel.activeItems.first { it.productId == espresso.id }
        tableOrderDao.cancelOrderItemByRowId(afterSingleCancel.id, espressoRow.id, nowMillis = 4_000L)

        orders = tableOrderDao.observeOrdersWithItems().first().map { it.toDomain() }
        val afterLastCancel = orders.first()
        assertEquals(OrderStatus.CANCELLED, afterLastCancel.status)
        assertTrue(afterLastCancel.activeItems.isEmpty())
        assertEquals(2, afterLastCancel.cancelledItems.size)

        val unlockedTable = tableOrderDao.findTableById("t1")
        assertNull(unlockedTable?.lockedByWorkerId)
    }

    @Test
    fun completeOrderSale_marksOrderFinished_deductsStock_andCreatesBill() = runBlocking {
        val order = OrderEntity(
            id = "ord-market-1",
            orderNumber = "ORD-5001",
            venueMode = VenueMode.MARKET.name,
            workerId = "w2",
            workerName = "Ana",
            status = OrderStatus.PENDING.name,
            createdAtMillis = 10_000L,
            updatedAtMillis = 10_000L
        )
        val item = OrderItemEntity(
            orderId = order.id,
            productId = espresso.id,
            productNameSr = espresso.nameSr,
            productNameEn = espresso.nameEn,
            category = espresso.category,
            unitPriceRsd = espresso.priceRsd,
            quantity = 3,
            status = OrderItemStatus.ACTIVE.name
        )
        tableOrderDao.createOrderWithItems(order, listOf(item))

        val tx = SaleTransactionEntity(
            id = "tx-5001",
            receiptNumber = "MP-5001",
            workerId = "w2",
            workerName = "Ana",
            timestampMillis = 20_000L,
            subtotalRsd = 660.0,
            taxRsd = 132.0,
            totalRsd = 792.0,
            paymentMethod = PaymentMethod.CARD.name,
            cashReceivedRsd = null,
            changeRsd = null,
            venueMode = VenueMode.MARKET.name
        )
        val saleLine = SaleLineItemEntity(
            transactionId = tx.id,
            productId = espresso.id,
            productNameSr = espresso.nameSr,
            productNameEn = espresso.nameEn,
            category = espresso.category,
            unitPriceRsd = espresso.priceRsd,
            quantity = 3
        )

        salesDao.insertFullSale(tx, listOf(saleLine), deductStock = true)
        tableOrderDao.markOrderFinished(order.id, tx.id, completedAtMillis = 20_000L)

        val updatedOrder = tableOrderDao.observeOrdersWithItems().first().map { it.toDomain() }.first()
        assertEquals(OrderStatus.FINISHED, updatedOrder.status)
        assertFalse(updatedOrder.isEditable)
        assertEquals(tx.id, updatedOrder.transactionId)
        assertEquals(20_000L, updatedOrder.completedAtMillis)

        val transactions = salesDao.observeTransactionsWithItems().first().map { it.toDomain() }
        assertEquals(1, transactions.size)
        assertEquals("MP-5001", transactions.first().receiptNumber)

        val updatedProduct = catalogDao.observeProducts().first().first { it.id == espresso.id }
        assertEquals(17, updatedProduct.stockQuantity)
    }

    @Test
    fun cancelFullOrder_releasesTableLockAndMarksAllItemsCancelled() = runBlocking {
        tableOrderDao.lockAndAddOrIncrementTableItem(
            tableId = "t1",
            workerId = "w1",
            workerName = "Marko",
            product = espresso,
            nowMillis = 1_000L
        )
        val pendingOrder = tableOrderDao.observeOrdersWithItems().first().map { it.toDomain() }.first()
        assertEquals(OrderStatus.PENDING, pendingOrder.status)

        tableOrderDao.cancelFullOrder(pendingOrder.id, nowMillis = 5_000L)

        val cancelledOrder = tableOrderDao.observeOrdersWithItems().first().map { it.toDomain() }.first()
        assertEquals(OrderStatus.CANCELLED, cancelledOrder.status)
        assertTrue(cancelledOrder.activeItems.isEmpty())
        assertEquals(1, cancelledOrder.cancelledItems.size)
        assertNull(tableOrderDao.findTableById("t1")?.lockedByWorkerId)
    }
}
