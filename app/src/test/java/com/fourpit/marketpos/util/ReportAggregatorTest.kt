package com.fourpit.marketpos.util

import com.fourpit.marketpos.model.OrderStatus
import com.fourpit.marketpos.model.PaymentMethod
import com.fourpit.marketpos.model.PosOrder
import com.fourpit.marketpos.model.PosOrderItem
import com.fourpit.marketpos.model.ReportPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportAggregatorTest {

    @Test
    fun aggregate_computesPaidRevenuePaymentSplitsAndIgnoresCancelled() {
        val now = System.currentTimeMillis()
        val orders = listOf(
            PosOrder(
                id = 1,
                receiptNumber = "MKT-1",
                locationId = 1,
                workerName = "Ana",
                status = OrderStatus.PAID,
                paymentMethod = PaymentMethod.CASH,
                createdAtMillis = now - 60_000L,
                closedAtMillis = now - 30_000L,
                totalRsd = 500.0,
                taxAmountRsd = 83.33,
                items = listOf(
                    PosOrderItem(1, 1, 10, "Hleb", "Pekara", 100.0, 2),
                    PosOrderItem(2, 1, 11, "Mleko", "Mlečni", 150.0, 2)
                )
            ),
            PosOrder(
                id = 2,
                receiptNumber = "MKT-2",
                locationId = 1,
                workerName = "Ana",
                status = OrderStatus.PAID,
                paymentMethod = PaymentMethod.CARD,
                createdAtMillis = now - 40_000L,
                closedAtMillis = now - 10_000L,
                totalRsd = 300.0,
                taxAmountRsd = 50.0,
                items = listOf(
                    PosOrderItem(3, 2, 12, "Kafa", "Kafa", 300.0, 1)
                )
            ),
            PosOrder(
                id = 3,
                receiptNumber = "MKT-3",
                locationId = 1,
                workerName = "Ana",
                status = OrderStatus.CANCELLED,
                createdAtMillis = now - 20_000L,
                closedAtMillis = now - 5_000L,
                totalRsd = 999.0
            )
        )

        val report = ReportAggregator.aggregate(
            orders = orders,
            period = ReportPeriod.TODAY,
            title = "Daily",
            scopeLabel = "Market Centar",
            nowMillis = now
        )

        assertEquals(800.0, report.totalRevenueRsd, 0.001)
        assertEquals(500.0, report.cashRevenueRsd, 0.001)
        assertEquals(300.0, report.cardRevenueRsd, 0.001)
        assertEquals(2, report.paidOrderCount)
        assertEquals(1, report.cancelledOrderCount)
        assertEquals(5, report.totalItemsSold)
        assertEquals(400.0, report.averageTicketRsd, 0.001)
    }
}
