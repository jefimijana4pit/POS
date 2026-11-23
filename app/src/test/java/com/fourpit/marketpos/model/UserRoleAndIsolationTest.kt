package com.fourpit.marketpos.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRoleAndIsolationTest {

    @Test
    fun admin_canAccessAllLocationsAndVenues() {
        val admin = UserSession(UserRole.ADMIN, "Admin", locationId = null)
        assertTrue(admin.canAccessLocation(1L))
        assertTrue(admin.canAccessLocation(99L))
        assertTrue(admin.canAccessVenueType(VenueType.MARKET))
        assertTrue(admin.canAccessVenueType(VenueType.BAR))
    }

    @Test
    fun marketWorker_isIsolatedToAssignedMarketLocation() {
        val worker = UserSession(UserRole.MARKET_WORKER, "Ana", locationId = 1L)
        assertTrue(worker.canAccessLocation(1L))
        assertFalse(worker.canAccessLocation(2L))
        assertTrue(worker.canAccessVenueType(VenueType.MARKET))
        assertFalse(worker.canAccessVenueType(VenueType.BAR))
    }

    @Test
    fun barWorker_isIsolatedToAssignedBarLocation() {
        val worker = UserSession(UserRole.BAR_WORKER, "Nikola", locationId = 2L)
        assertTrue(worker.canAccessLocation(2L))
        assertFalse(worker.canAccessLocation(1L))
        assertTrue(worker.canAccessVenueType(VenueType.BAR))
        assertFalse(worker.canAccessVenueType(VenueType.MARKET))
    }
}
