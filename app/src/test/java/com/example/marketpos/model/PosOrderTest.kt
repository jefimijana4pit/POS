package com.example.marketpos.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PosOrderTest {

    @Test
    fun pendingOrder_calculatesActiveTotalsAndExcludesCancelledItems() {
        val activeEspresso = PosOrderItem(
            id = 1L,
            orderId = "ord-1",
            productId = "p-esp",
            productNameSr = "Espreso",
            productNameEn = "Espresso",
            category = ProductCategory.COFFEE_TEA,
            unitPriceRsd = 200.0,
            quantity = 2,
            status = OrderItemStatus.ACTIVE
        )
        val cancelledLemonade = PosOrderItem(
            id = 2L,
            orderId = "ord-1",
            productId = "p-lem",
            productNameSr = "Limunada",
            productNameEn = "Lemonade",
            category = ProductCategory.BAR_DRINKS,
            unitPriceRsd = 300.0,
            quantity = 1,
            status = OrderItemStatus.CANCELLED
        )

        val order = PosOrder(
            id = "ord-1",
            orderNumber = "ORD-1001",
            venueMode = VenueMode.BAR_RESTAURANT,
            tableId = "t1",
            tableName = "Sto 1",
            workerId = "w1",
            workerName = "Marko",
            status = OrderStatus.PENDING,
            createdAtMillis = 1_000L,
            updatedAtMillis = 2_000L,
            items = listOf(activeEspresso, cancelledLemonade)
        )

        assertTrue(order.isEditable)
        assertEquals(1, order.activeItems.size)
        assertEquals(1, order.cancelledItems.size)
        assertEquals(2, order.activeItemCount)
        assertEquals(1, order.cancelledItemCount)
        assertEquals(400.0, order.subtotalRsd, 0.001)
        assertEquals(80.0, order.taxRsd, 0.001)
        assertEquals(480.0, order.totalRsd, 0.001)
        assertEquals(0.0, cancelledLemonade.lineTotalRsd, 0.001)
        assertEquals(300.0, cancelledLemonade.grossLineTotalRsd, 0.001)
    }

    @Test
    fun finishedAndCancelledOrders_areNotEditable() {
        val baseItem = PosOrderItem(
            id = 1L,
            orderId = "ord-2",
            productId = "p-1",
            productNameSr = "Hleb",
            productNameEn = "Bread",
            category = ProductCategory.BAKERY,
            unitPriceRsd = 100.0,
            quantity = 1,
            status = OrderItemStatus.ACTIVE
        )

        val finished = PosOrder(
            id = "ord-2",
            orderNumber = "ORD-1002",
            venueMode = VenueMode.MARKET,
            workerId = "w1",
            workerName = "Ana",
            status = OrderStatus.FINISHED,
            createdAtMillis = 1_000L,
            updatedAtMillis = 2_000L,
            completedAtMillis = 2_000L,
            transactionId = "tx-1",
            items = listOf(baseItem)
        )
        val cancelled = finished.copy(
            id = "ord-3",
            status = OrderStatus.CANCELLED,
            completedAtMillis = null,
            transactionId = null
        )

        assertFalse(finished.isEditable)
        assertFalse(cancelled.isEditable)
    }
}
