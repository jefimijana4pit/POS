package com.fourpit.marketpos.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PosOrderTest {

    @Test
    fun orderAndItems_computeTotalsAndCounts() {
        val item1 = PosOrderItem(1, 10, 1, "Espresso", "Kafa", 220.0, 2)
        val item2 = PosOrderItem(2, 10, 2, "Limunada", "Bezalkoholna", 280.0, 1)
        val order = PosOrder(
            id = 10,
            receiptNumber = "BAR-100",
            locationId = 2,
            workerName = "Jelena",
            status = OrderStatus.OPEN,
            createdAtMillis = 1000L,
            totalRsd = item1.lineTotalRsd + item2.lineTotalRsd,
            items = listOf(item1, item2)
        )

        assertEquals(440.0, item1.lineTotalRsd, 0.001)
        assertEquals(720.0, order.totalRsd, 0.001)
        assertEquals(3, order.itemCount)
    }
}
