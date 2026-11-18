package com.example.marketpos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.UserRole
import com.example.marketpos.ui.screens.login.HeroBrandPanel
import com.example.marketpos.ui.screens.login.LoginFormCard
import com.example.marketpos.viewmodel.LoginUiState

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onToggleLanguage: (AppLanguage) -> Unit,
    onLoginWithPin: (UserRole, String?, String) -> Boolean,
    onQuickDemoLogin: (UserRole) -> Unit
) {
    var selectedRole by rememberSaveable { mutableStateOf(UserRole.MARKET_WORKER) }
    val roleWorkers = remember(uiState.workers, selectedRole) {
        uiState.workers.filter { it.role == selectedRole }
    }
    var selectedWorkerId by rememberSaveable(uiState.workers, selectedRole) {
        mutableStateOf(roleWorkers.firstOrNull()?.id ?: "")
    }
    var pinInput by rememberSaveable { mutableStateOf("") }
    var pinError by rememberSaveable { mutableStateOf(false) }

    fun selectRole(role: UserRole) {
        selectedRole = role
        pinError = false
        if (role.isWorker) {
            val matching = uiState.workers.filter { it.role == role }
            if (matching.none { it.id == selectedWorkerId }) {
                selectedWorkerId = matching.firstOrNull()?.id ?: ""
            }
        }
    }

    fun attemptLogin() {
        val success = onLoginWithPin(
            selectedRole,
            if (selectedRole.isWorker) selectedWorkerId else null,
            pinInput
        )
        pinError = !success
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            val isWide = maxWidth >= 800.dp

            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeroBrandPanel(
                        language = uiState.language,
                        onToggleLanguage = onToggleLanguage,
                        modifier = Modifier.weight(1f)
                    )
                    LoginFormCard(
                        selectedRole = selectedRole,
                        onSelectRole = ::selectRole,
                        workers = roleWorkers,
                        locations = uiState.locations,
                        selectedWorkerId = selectedWorkerId,
                        onSelectWorker = { selectedWorkerId = it },
                        pinInput = pinInput,
                        onPinChange = {
                            if (it.length <= 6 && it.all(Char::isDigit)) {
                                pinInput = it
                                pinError = false
                            }
                        },
                        pinError = pinError,
                        onAttemptLogin = ::attemptLogin,
                        onQuickDemoLogin = onQuickDemoLogin,
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 520.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HeroBrandPanel(
                        language = uiState.language,
                        onToggleLanguage = onToggleLanguage,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                    )
                    LoginFormCard(
                        selectedRole = selectedRole,
                        onSelectRole = ::selectRole,
                        workers = roleWorkers,
                        locations = uiState.locations,
                        selectedWorkerId = selectedWorkerId,
                        onSelectWorker = { selectedWorkerId = it },
                        pinInput = pinInput,
                        onPinChange = {
                            if (it.length <= 6 && it.all(Char::isDigit)) {
                                pinInput = it
                                pinError = false
                            }
                        },
                        pinError = pinError,
                        onAttemptLogin = ::attemptLogin,
                        onQuickDemoLogin = onQuickDemoLogin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp)
                    )
                }
            }
        }
    }
}
