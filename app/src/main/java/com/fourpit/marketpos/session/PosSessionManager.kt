package com.fourpit.marketpos.session

import com.fourpit.marketpos.model.UserRole
import com.fourpit.marketpos.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PosSessionManager {
    private val _session = MutableStateFlow<UserSession?>(null)
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    companion object {
        const val ADMIN_PIN = "1234"
        const val WORKER_PIN = "0000"
    }

    fun authenticate(
        role: UserRole,
        workerName: String,
        pin: String,
        locationId: Long?
    ): Boolean {
        val expectedPin = if (role == UserRole.ADMIN) ADMIN_PIN else WORKER_PIN
        if (pin.trim() != expectedPin) return false
        if (role != UserRole.ADMIN && locationId == null) return false

        val cleanName = workerName.trim().ifBlank {
            when (role) {
                UserRole.ADMIN -> "Admin"
                UserRole.MARKET_WORKER -> "Kasir"
                UserRole.BAR_WORKER -> "Konobar"
            }
        }
        _session.value = UserSession(
            role = role,
            workerName = cleanName,
            locationId = if (role == UserRole.ADMIN) null else locationId
        )
        return true
    }

    fun logout() {
        _session.value = null
    }
}
