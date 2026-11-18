package com.example.marketpos.util

import com.example.marketpos.model.DefaultLocations
import com.example.marketpos.model.OrderItemStatus
import com.example.marketpos.model.OrderStatus
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.PosOrderItem
import com.example.marketpos.model.ProductCategory
import com.example.marketpos.model.ReportPeriodType
import com.example.marketpos.model.SaleLineItem
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.model.UserRole
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.VenueMode
import com.example.marketpos.model.WorkerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ReportAggregatorTest {

    private val barCenter = VenueLocation(
        id = DefaultLocations.BAR_CENTER_ID,
        name = "Kafe Bar Korzo",
        address = "Knez Mihailova 24, Beograd",
        venueMode = VenueMode.BAR_RESTAURANT,
        sortOrder = 1
    )

    private val marketCenter = VenueLocation(
        id = DefaultLocations.MARKET_CENTRAL_ID,
        name = "Market Centar",
        address = "Bulevar Kralja Aleksandra 142, Beograd",
        venueMode = VenueMode.MARKET,
        sortOrder = 0
    )

    private val workers = listOf(
        WorkerProfile("w_jelena", "Jelena K.", UserRole.RESTAURANT_WORKER, "1234", "Smena 1", "Shift 1", DefaultLocations.BAR_CENTER_ID),
        WorkerProfile("w_milica", "Milica S.", UserRole.RESTAURANT_WORKER, "1234", "Smena 2", "Shift 2", DefaultLocations.BAR_CENTER_ID),
        WorkerProfile("w_marko", "Marko P.", UserRole.MARKET_WORKER, "1234", "Smena 1", "Shift 1", DefaultLocations.MARKET_CENTRAL_ID)
    )

    @Test
    fun computePeriodBounds_coversFullMonthAndNormalizesReversedRange() {
        val cal = Calendar.getInstance().apply {
            set(2025, Calendar.MARCH, 15, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val (monthStart, monthEnd) = ReportAggregator.computePeriodBounds(
            periodType = ReportPeriodType.MONTHLY,
            anchorMillis = cal.timeInMillis
        )
        val startCal = Calendar.getInstance().apply { timeInMillis = monthStart }
        val endCal = Calendar.getInstance().apply { timeInMillis = monthEnd }
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(31, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))

        val day10 = Calendar.getInstance().apply { set(2025, Calendar.MARCH, 10, 12, 0, 0) }.timeInMillis
        val day20 = Calendar.getInstance().apply { set(2025, Calendar.MARCH, 20, 9, 0, 0) }.timeInMillis
        val (rangeStart, rangeEnd) = ReportAggregator.computePeriodBounds(
            periodType = ReportPeriodType.DATE_RANGE,
            anchorMillis = day10,
            rangeStartMillis = day20,
            rangeEndMillis = day10
        )
        assertTrue(rangeStart < rangeEnd)
        val rStartCal = Calendar.getInstance().apply { timeInMillis = rangeStart }
        val rEndCal = Calendar.getInstance().apply { timeInMillis = rangeEnd }
        assertEquals(10, rStartCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(20, rEndCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun buildLocationReportSummary_computesPerWorkerBillsCanceledTablesAndProducts() {
        val now = System.currentTimeMillis()
        val (dayStart, dayEnd) = ReportAggregator.computePeriodBounds(ReportPeriodType.DAILY, now)
        val insideTs = dayStart + 10_000L
        val outsideTs = dayStart - 86_400_000L

        val transactions = listOf(
            SaleTransaction(
                id = "tx_1",
                receiptNumber = "MP-101",
                workerId = "w_jelena",
                workerName = "Jelena K.",
                timestampMillis = insideTs,
                items = listOf(
                    SaleLineItem("p1", "Espreso", "Espresso", ProductCategory.COFFEE_TEA, 220.0, 2),
                    SaleLineItem("p2", "Limunada", "Lemonade", ProductCategory.BAR_DRINKS, 310.0, 1)
                ),
                subtotalRsd = 750.0,
                taxRsd = 150.0,
                totalRsd = 900.0,
                paymentMethod = PaymentMethod.CASH,
                cashReceivedRsd = 1000.0,
                changeRsd = 100.0,
                venueMode = VenueMode.BAR_RESTAURANT,
                tableId = "tbl_1",
                tableName = "Sto 1",
                locationId = DefaultLocations.BAR_CENTER_ID
            ),
            SaleTransaction(
                id = "tx_2",
                receiptNumber = "MP-102",
                workerId = "w_jelena",
                workerName = "Jelena K.",
                timestampMillis = insideTs + 5_000L,
                items = listOf(
                    SaleLineItem("p1", "Espreso", "Espresso", ProductCategory.COFFEE_TEA, 220.0, 3)
                ),
                subtotalRsd = 660.0,
                taxRsd = 132.0,
                totalRsd = 792.0,
                paymentMethod = PaymentMethod.CARD,
                cashReceivedRsd = null,
                changeRsd = null,
                venueMode = VenueMode.BAR_RESTAURANT,
                tableId = "tbl_2",
                tableName = "Sto 2",
                locationId = DefaultLocations.BAR_CENTER_ID
            ),
            // Different location (must be excluded from barCenter report)
            SaleTransaction(
                id = "tx_3",
                receiptNumber = "MP-103",
                workerId = "w_marko",
                workerName = "Marko P.",
                timestampMillis = insideTs,
                items = listOf(
                    SaleLineItem("pm1", "Hleb", "Bread", ProductCategory.BAKERY, 75.0, 4)
                ),
                subtotalRsd = 300.0,
                taxRsd = 60.0,
                totalRsd = 360.0,
                paymentMethod = PaymentMethod.CASH,
                cashReceivedRsd = 500.0,
                changeRsd = 140.0,
                venueMode = VenueMode.MARKET,
                locationId = DefaultLocations.MARKET_CENTRAL_ID
            ),
            // Outside period (must be excluded)
            SaleTransaction(
                id = "tx_old",
                receiptNumber = "MP-099",
                workerId = "w_milica",
                workerName = "Milica S.",
                timestampMillis = outsideTs,
                items = listOf(
                    SaleLineItem("p1", "Espreso", "Espresso", ProductCategory.COFFEE_TEA, 220.0, 1)
                ),
                subtotalRsd = 220.0,
                taxRsd = 44.0,
                totalRsd = 264.0,
                paymentMethod = PaymentMethod.CASH,
                cashReceivedRsd = 300.0,
                changeRsd = 36.0,
                venueMode = VenueMode.BAR_RESTAURANT,
                tableId = "tbl_3",
                tableName = "Sto 3",
                locationId = DefaultLocations.BAR_CENTER_ID
            )
        )

        val orders = listOf(
            PosOrder(
                id = "ord_canc",
                orderNumber = "BR-201",
                venueMode = VenueMode.BAR_RESTAURANT,
                tableId = "tbl_4",
                tableName = "Sto 4",
                workerId = "w_milica",
                workerName = "Milica S.",
                status = OrderStatus.CANCELLED,
                createdAtMillis = insideTs,
                updatedAtMillis = insideTs + 1_000L,
                completedAtMillis = null,
                items = listOf(
                    PosOrderItem(1, "ord_canc", "p2", "Limunada", "Lemonade", ProductCategory.BAR_DRINKS, 310.0, 2, OrderItemStatus.CANCELLED)
                ),
                locationId = DefaultLocations.BAR_CENTER_ID
            )
        )

        val barSummary = ReportAggregator.buildLocationReportSummary(
            location = barCenter,
            isGlobal = false,
            periodType = ReportPeriodType.DAILY,
            startMillis = dayStart,
            endMillis = dayEnd,
            workers = workers,
            transactions = transactions,
            orders = orders
        )

        assertEquals(2, barSummary.finishedBillsCount)
        assertEquals(1, barSummary.cancelledOrdersCount)
        assertEquals(3, barSummary.tablesHandledCount) // tbl_1, tbl_2, tbl_4
        assertEquals(6, barSummary.totalUnitsSold)
        assertEquals(1692.0, barSummary.totalRevenueRsd, 0.01)
        assertEquals(900.0, barSummary.cashRevenueRsd, 0.01)
        assertEquals(792.0, barSummary.cardRevenueRsd, 0.01)

        val jelenaRow = barSummary.workerRows.first { it.workerId == "w_jelena" }
        assertEquals(2, jelenaRow.finishedBillsCount)
        assertEquals(0, jelenaRow.cancelledOrdersCount)
        assertEquals(2, jelenaRow.tablesHandledCount)
        assertEquals(6, jelenaRow.totalUnitsSold)
        assertEquals("p1", jelenaRow.productsSold.first().productId)
        assertEquals(5, jelenaRow.productsSold.first().quantity)

        val milicaRow = barSummary.workerRows.first { it.workerId == "w_milica" }
        assertEquals(0, milicaRow.finishedBillsCount)
        assertEquals(1, milicaRow.cancelledOrdersCount)
        assertEquals(2, milicaRow.cancelledItemsCount)
        assertEquals(1, milicaRow.tablesHandledCount)

        // Global summary includes Marko's Market sale too
        val globalSummary = ReportAggregator.buildLocationReportSummary(
            location = null,
            isGlobal = true,
            periodType = ReportPeriodType.DAILY,
            startMillis = dayStart,
            endMillis = dayEnd,
            workers = workers,
            transactions = transactions,
            orders = orders
        )
        assertEquals(3, globalSummary.finishedBillsCount)
        assertEquals(10, globalSummary.totalUnitsSold)
        assertEquals(2052.0, globalSummary.totalRevenueRsd, 0.01)
    }
}
