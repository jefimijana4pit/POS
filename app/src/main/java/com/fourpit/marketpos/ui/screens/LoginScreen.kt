package com.fourpit.marketpos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.AppLanguage
import com.fourpit.marketpos.model.UserRole
import com.fourpit.marketpos.ui.screens.login.LoginHeroBanner
import com.fourpit.marketpos.ui.screens.login.RoleSelectorRow
import com.fourpit.marketpos.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    currentLanguage: AppLanguage,
    onOpenSettings: () -> Unit,
    onLoginSuccess: (UserRole, Long?) -> Unit
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var locationDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.appName, fontWeight = FontWeight.Bold) },
                actions = {
                    OutlinedButton(
                        onClick = { viewModel.toggleLanguage(currentLanguage) },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("login_language_button")
                    ) {
                        Icon(Icons.Default.Language, contentDescription = strings.switchLanguage)
                        Spacer(Modifier.padding(horizontal = 3.dp))
                        Text(if (currentLanguage == AppLanguage.SR_LATIN) "SR" else "EN")
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("login_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = strings.settings)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LoginHeroBanner()

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = strings.loginTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = strings.selectRole,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        RoleSelectorRow(
                            selectedRole = uiState.selectedRole,
                            onSelectRole = viewModel::selectRole
                        )

                        if (uiState.selectedRole != UserRole.ADMIN) {
                            val eligible = uiState.eligibleLocations
                            val selectedLoc = eligible.firstOrNull { it.id == uiState.selectedLocationId }
                            ExposedDropdownMenuBox(
                                expanded = locationDropdownExpanded,
                                onExpandedChange = { locationDropdownExpanded = !locationDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedLoc?.name ?: "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(strings.selectLocation) },
                                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationDropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("location_selector_field")
                                )
                                ExposedDropdownMenu(
                                    expanded = locationDropdownExpanded,
                                    onDismissRequest = { locationDropdownExpanded = false }
                                ) {
                                    eligible.forEach { loc ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(loc.name, fontWeight = FontWeight.SemiBold)
                                                    Text(loc.address, style = MaterialTheme.typography.bodySmall)
                                                }
                                            },
                                            onClick = {
                                                viewModel.selectLocation(loc.id)
                                                locationDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = uiState.workerName,
                            onValueChange = viewModel::updateWorkerName,
                            label = { Text(strings.workerNameLabel) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("worker_name_input")
                        )

                        OutlinedTextField(
                            value = uiState.pin,
                            onValueChange = viewModel::updatePin,
                            label = { Text(strings.pinLabel) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = uiState.errorInvalidPin,
                            supportingText = {
                                Text(
                                    text = if (uiState.errorInvalidPin) strings.invalidPin else strings.defaultPinHint
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pin_input")
                        )

                        Spacer(Modifier.height(4.dp))

                        Button(
                            onClick = { viewModel.attemptLogin(onLoginSuccess) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("login_submit_button")
                        ) {
                            Text(strings.signIn, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
