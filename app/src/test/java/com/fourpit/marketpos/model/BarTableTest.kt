package com.fourpit.marketpos.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BarTableTest {

    @Test
    fun tableStatus_reflectsActiveOrderId() {
        val freeTable = BarTable(id = 1, locationId = 2, label = "Sto 1", seats = 4, activeOrderId = null)
        val occupiedTable = freeTable.copy(activeOrderId = 42L, openTotalRsd = 750.0, openItemCount = 3)

        assertEquals(TableStatus.AVAILABLE, freeTable.status)
        assertEquals(TableStatus.OCCUPIED, occupiedTable.status)
    }
}
