package com.fourpit.marketpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fourpit.marketpos.data.CatalogLocationRepository
import com.fourpit.marketpos.data.OrderSalesRepository
import com.fourpit.marketpos.data.SettingsDataStore
import com.fourpit.marketpos.model.AppLanguage
import com.fourpit.marketpos.model.PosLocation
import com.fourpit.marketpos.model.UserRole
import com.fourpit.marketpos.model.VenueType
import com.fourpit.marketpos.session.PosSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val selectedRole: UserRole = UserRole.MARKET_WORKER,
    val selectedLocationId: Long? = null,
    val workerName: String = "Ana Petrović",
    val pin: String = "0000",
    val locations: List<PosLocation> = emptyList(),
    val errorInvalidPin: Boolean = false
) {
    val eligibleLocations: List<PosLocation>
        get() = when (selectedRole) {
            UserRole.ADMIN -> locations
            UserRole.MARKET_WORKER -> locations.filter { it.venueType == VenueType.MARKET }
            UserRole.BAR_WORKER -> locations.filter { it.venueType == VenueType.BAR }
        }
}

class LoginViewModel(
    private val catalogRepo: CatalogLocationRepository,
    private val orderSalesRepo: OrderSalesRepository,
    private val sessionManager: PosSessionManager,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val inputState = MutableStateFlow(LoginUiState())

    val uiState: StateFlow<LoginUiState> = combine(
        inputState,
        catalogRepo.observeLocations()
    ) { state, locs ->
        val eligible = when (state.selectedRole) {
            UserRole.ADMIN -> locs
            UserRole.MARKET_WORKER -> locs.filter { it.venueType == VenueType.MARKET }
            UserRole.BAR_WORKER -> locs.filter { it.venueType == VenueType.BAR }
        }
        val resolvedLocationId = state.selectedLocationId
            ?.takeIf { id -> eligible.any { it.id == id } }
            ?: eligible.firstOrNull()?.id
        state.copy(
            locations = locs,
            selectedLocationId = resolvedLocationId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LoginUiState()
    )

    fun selectRole(role: UserRole) {
        inputState.update { current ->
            val defaultPin = if (role == UserRole.ADMIN) PosSessionManager.ADMIN_PIN else PosSessionManager.WORKER_PIN
            val defaultName = when (role) {
                UserRole.ADMIN -> "Milica Admin"
                UserRole.MARKET_WORKER -> "Ana Petrović"
                UserRole.BAR_WORKER -> "Jelena Kovač"
            }
            current.copy(
                selectedRole = role,
                workerName = defaultName,
                pin = defaultPin,
                errorInvalidPin = false
            )
        }
    }

    fun selectLocation(locationId: Long) {
        inputState.update { it.copy(selectedLocationId = locationId, errorInvalidPin = false) }
    }

    fun updateWorkerName(name: String) {
        inputState.update { it.copy(workerName = name, errorInvalidPin = false) }
    }

    fun updatePin(pin: String) {
        if (pin.length <= 6) {
            inputState.update { it.copy(pin = pin.filter { ch -> ch.isDigit() }, errorInvalidPin = false) }
        }
    }

    fun toggleLanguage(current: AppLanguage) {
        viewModelScope.launch {
            val next = if (current == AppLanguage.SR_LATIN) AppLanguage.EN else AppLanguage.SR_LATIN
            settingsDataStore.setLanguage(next)
        }
    }

    fun attemptLogin(onSuccess: (UserRole, Long?) -> Unit) {
        val state = uiState.value
        val targetLocId = if (state.selectedRole == UserRole.ADMIN) null else state.selectedLocationId
        val ok = sessionManager.authenticate(
            role = state.selectedRole,
            workerName = state.workerName,
            pin = state.pin,
            locationId = targetLocId
        )
        if (ok) {
            inputState.update { it.copy(errorInvalidPin = false) }
            targetLocId?.let { locId ->
                viewModelScope.launch {
                    orderSalesRepo.logWorkerAction(
                        locationId = locId,
                        workerName = state.workerName.ifBlank { state.selectedRole.name },
                        role = state.selectedRole,
                        action = "SIGN_IN",
                        details = "Opened POS terminal"
                    )
                }
            }
            onSuccess(state.selectedRole, targetLocId)
        } else {
            inputState.update { it.copy(errorInvalidPin = true) }
        }
    }

    class Factory(
        private val catalogRepo: CatalogLocationRepository,
        private val orderSalesRepo: OrderSalesRepository,
        private val sessionManager: PosSessionManager,
        private val settingsDataStore: SettingsDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LoginViewModel(catalogRepo, orderSalesRepo, sessionManager, settingsDataStore) as T
        }
    }
}
