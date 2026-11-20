package com.example.marketpos.model

import com.example.marketpos.viewmodel.AdminBranchUiState
import com.example.marketpos.viewmodel.BarWorkerUiState
import com.example.marketpos.viewmodel.MarketWorkerUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRoleAndIsolationTest {

    private val marketWorker1 = WorkerProfile(
        id = "w_marko",
        name = "Marko Petrović",
        role = UserRole.MARKET_WORKER,
        pin = "1111",
        shiftLabelSr = "Prva smena",
        shiftLabelEn = "Morning shift"
    )

    private val marketWorker2 = WorkerProfile(
        id = "w_nikola",
        name = "Nikola Đorđević",
        role = UserRole.MARKET_WORKER,
        pin = "2222",
        shiftLabelSr = "Druga smena",
        shiftLabelEn = "Afternoon shift"
    )

    private val restaurantWorker1 = WorkerProfile(
        id = "w_jelena",
        name = "Jelena Jovanović",
        role = UserRole.RESTAURANT_WORKER,
        pin = "3333",
        shiftLabelSr = "Prva smena",
        shiftLabelEn = "Morning shift"
    )

    private val adminProfile = WorkerProfile(
        id = "admin_1",
        name = "Ana Kovačević (Admin)",
        role = UserRole.ADMIN,
        pin = "0000",
        shiftLabelSr = "Uprava",
        shiftLabelEn = "Management"
    )

    @Test
    fun `worker roles map directly to assigned venue modes and admin has no fixed venue`() {
        assertTrue(UserRole.MARKET_WORKER.isWorker)
        assertTrue(UserRole.RESTAURANT_WORKER.isWorker)
        assertFalse(UserRole.ADMIN.isWorker)

        assertEquals(VenueMode.MARKET, UserRole.MARKET_WORKER.assignedVenueMode())
        assertEquals(VenueMode.BAR_RESTAURANT, UserRole.RESTAURANT_WORKER.assignedVenueMode())
        assertNull(UserRole.ADMIN.assignedVenueMode())
    }

    @Test
    fun `venueWorkers filters workers matching current venue mode`() {
        val allWorkers = listOf(marketWorker1, marketWorker2, restaurantWorker1)

        val marketState = AdminBranchUiState(
            settings = AppSettings(venueMode = VenueMode.MARKET),
            workers = allWorkers
        )
        assertEquals(listOf("w_marko", "w_nikola"), marketState.venueWorkers.map { it.id })

        val restaurantState = AdminBranchUiState(
            settings = AppSettings(venueMode = VenueMode.BAR_RESTAURANT),
            workers = allWorkers
        )
        assertEquals(listOf("w_jelena"), restaurantState.venueWorkers.map { it.id })
    }

    @Test
    fun `worker sees only own orders and bills while admin sees all`() {
        val o1 = PosOrder(
            id = "o1",
            orderNumber = "ORD-1",
            venueMode = VenueMode.MARKET,
            workerId = marketWorker1.id,
            workerName = marketWorker1.name,
            status = OrderStatus.PENDING,
            createdAtMillis = 1000L,
            updatedAtMillis = 1000L
        )
        val o2 = PosOrder(
            id = "o2",
            orderNumber = "ORD-2",
            venueMode = VenueMode.MARKET,
            workerId = marketWorker2.id,
            workerName = marketWorker2.name,
            status = OrderStatus.PENDING,
            createdAtMillis = 2000L,
            updatedAtMillis = 2000L
        )
        val o3 = PosOrder(
            id = "o3",
            orderNumber = "ORD-3",
            venueMode = VenueMode.MARKET,
            workerId = marketWorker1.id,
            workerName = marketWorker1.name,
            status = OrderStatus.FINISHED,
            createdAtMillis = 3000L,
            updatedAtMillis = 3000L
        )

        val tx1 = SaleTransaction(
            id = "tx1",
            receiptNumber = "MP-1",
            workerId = marketWorker1.id,
            workerName = marketWorker1.name,
            timestampMillis = 1000L,
            items = emptyList(),
            subtotalRsd = 100.0,
            taxRsd = 20.0,
            totalRsd = 120.0,
            paymentMethod = PaymentMethod.CASH,
            cashReceivedRsd = 120.0,
            changeRsd = 0.0,
            venueMode = VenueMode.MARKET
        )
        val tx2 = tx1.copy(
            id = "tx2",
            receiptNumber = "MP-2",
            workerId = marketWorker2.id,
            workerName = marketWorker2.name
        )

        val barW1State = BarWorkerUiState(
            currentUser = marketWorker1,
            orders = listOf(o1, o2, o3),
            transactions = listOf(tx1, tx2)
        )
        assertEquals(listOf("o1", "o3"), barW1State.currentWorkerOrders.map { it.id })
        assertEquals(1, barW1State.pendingOrdersCount)
        assertEquals(listOf("tx1"), barW1State.currentWorkerTransactions.map { it.id })

        val barW2State = barW1State.copy(currentUser = marketWorker2)
        assertEquals(listOf("o2"), barW2State.currentWorkerOrders.map { it.id })
        assertEquals(1, barW2State.pendingOrdersCount)
        assertEquals(listOf("tx2"), barW2State.currentWorkerTransactions.map { it.id })

        val barAdminState = barW1State.copy(currentUser = adminProfile)
        assertEquals(listOf("o1", "o2", "o3"), barAdminState.currentWorkerOrders.map { it.id })
        assertEquals(2, barAdminState.pendingOrdersCount)
        assertEquals(listOf("tx1", "tx2"), barAdminState.currentWorkerTransactions.map { it.id })

        val marketW1State = MarketWorkerUiState(
            currentUser = marketWorker1,
            transactions = listOf(tx1, tx2)
        )
        assertEquals(listOf("tx1"), marketW1State.currentWorkerTransactions.map { it.id })
        assertEquals(
            listOf("tx2"),
            marketW1State.copy(currentUser = marketWorker2).currentWorkerTransactions.map { it.id }
        )
        assertEquals(
            listOf("tx1", "tx2"),
            marketW1State.copy(currentUser = adminProfile).currentWorkerTransactions.map { it.id }
        )
    }
}
