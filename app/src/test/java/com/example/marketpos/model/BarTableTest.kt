package com.example.marketpos.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarTableTest {

    @Test
    fun lockOwnership_preventsOtherWorkersFromInteracting() {
        val table = BarTable(
            id = "t1",
            name = "Sto 1",
            zone = "Sala",
            capacity = 4,
            lockedByWorkerId = "w1",
            lockedByWorkerName = "Marko",
            lockedAtMillis = 1_000L
        )

        assertTrue(table.isLocked)
        assertTrue(table.isLockedBy("w1"))
        assertFalse(table.isLockedBy("w2"))
        assertTrue(table.isLockedByOther("w2"))
        assertFalse(table.isLockedByOther("w1"))
    }

    @Test
    fun cleanupTimer_countsDownFifteenMinutesAndExpires() {
        val paidAt = 10_000_000L
        val table = BarTable(
            id = "t2",
            name = "Sto 2",
            zone = "Bašta",
            capacity = 4,
            lockedByWorkerId = "w1",
            lockedByWorkerName = "Marko",
            lockedAtMillis = paidAt - 60_000L,
            awaitingCleanup = true,
            paidAndPrintedAtMillis = paidAt
        )

        assertEquals(
            BarTable.TABLE_CLEANUP_AUTO_UNLOCK_MILLIS,
            table.remainingCleanupMillis(paidAt)
        )
        assertEquals(
            60_000L,
            table.remainingCleanupMillis(paidAt + BarTable.TABLE_CLEANUP_AUTO_UNLOCK_MILLIS - 60_000L)
        )
        assertEquals(
            0L,
            table.remainingCleanupMillis(paidAt + BarTable.TABLE_CLEANUP_AUTO_UNLOCK_MILLIS + 1L)
        )
    }

    @Test
    fun productCategories_splitCleanlyBetweenMarketAndBar() {
        val marketCategories = ProductCategory.forVenue(VenueMode.MARKET)
        val barCategories = ProductCategory.forVenue(VenueMode.BAR_RESTAURANT)

        assertTrue(marketCategories.all { it.venueMode() == VenueMode.MARKET })
        assertTrue(barCategories.all { it.venueMode() == VenueMode.BAR_RESTAURANT })
        assertTrue(marketCategories.intersect(barCategories.toSet()).isEmpty())
    }

    @Test
    fun sortedByAvailability_keepsFreeTablesOnTop() {
        val otherBusy = BarTable(
            id = "t1",
            name = "Sto 1",
            zone = "Sala",
            capacity = 4,
            lockedByWorkerId = "w2",
            lockedByWorkerName = "Jelena",
            sortOrder = 1
        )
        val myCleanup = BarTable(
            id = "t2",
            name = "Sto 2",
            zone = "Sala",
            capacity = 4,
            lockedByWorkerId = "w1",
            lockedByWorkerName = "Marko",
            awaitingCleanup = true,
            paidAndPrintedAtMillis = 10_000L,
            sortOrder = 2
        )
        val freeLater = BarTable(
            id = "t3",
            name = "Sto 3",
            zone = "Bašta",
            capacity = 4,
            sortOrder = 3
        )
        val myActive = BarTable(
            id = "t4",
            name = "Sto 4",
            zone = "Bašta",
            capacity = 4,
            lockedByWorkerId = "w1",
            lockedByWorkerName = "Marko",
            sortOrder = 4
        )
        val freeFirst = BarTable(
            id = "t5",
            name = "Sto 5",
            zone = "Šank",
            capacity = 2,
            sortOrder = 0
        )

        val sorted = listOf(otherBusy, myCleanup, freeLater, myActive, freeFirst)
            .sortedByAvailability(currentWorkerId = "w1")

        assertEquals(
            listOf("t5", "t3", "t2", "t4", "t1"),
            sorted.map { it.id }
        )
    }
}
