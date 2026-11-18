package com.example.marketpos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.marketpos.data.CatalogLocationRepository
import com.example.marketpos.di.PosAppContainer
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.UserRole
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.WorkerProfile
import com.example.marketpos.session.PosSessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LoginUiState(
    val language: AppLanguage = AppLanguage.SERBIAN_LATIN,
    val workers: List<WorkerProfile> = emptyList(),
    val locations: List<VenueLocation> = emptyList()
)

class LoginViewModel(
    private val sessionManager: PosSessionManager,
    catalogLocationRepository: CatalogLocationRepository
) : ViewModel() {

    val uiState: StateFlow<LoginUiState> = combine(
        sessionManager.effectiveSettingsFlow,
        catalogLocationRepository.locationsFlow
    ) { settings, locations ->
        LoginUiState(
            language = settings.language,
            workers = sessionManager.workers,
            locations = locations
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LoginUiState(workers = sessionManager.workers)
    )

    fun updateLanguage(language: AppLanguage) {
        sessionManager.updateLanguage(language)
    }

    fun authenticate(role: UserRole, workerId: String?, pin: String): Boolean {
        return sessionManager.authenticate(role, workerId, pin)
    }

    fun quickDemoLogin(role: UserRole) {
        sessionManager.quickDemoLogin(role)
    }

    companion object {
        fun factory(container: PosAppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoginViewModel(
                        sessionManager = container.sessionManager,
                        catalogLocationRepository = container.catalogLocationRepository
                    ) as T
                }
            }
    }
}
