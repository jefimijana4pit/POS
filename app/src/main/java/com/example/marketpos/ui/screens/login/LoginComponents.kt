package com.example.marketpos.ui.screens.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.UserRole
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.WorkerProfile
import com.example.marketpos.ui.theme.JetBrainsMonoFamily

@Composable
internal fun HeroBrandPanel(
    language: AppLanguage,
    onToggleLanguage: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val nextLang = if (language == AppLanguage.SERBIAN_LATIN) {
        AppLanguage.ENGLISH
    } else {
        AppLanguage.SERBIAN_LATIN
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_pos_hero),
                    contentDescription = strings.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f),
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)
                                )
                            )
                        )
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    TextButton(
                        onClick = { onToggleLanguage(nextLang) },
                        modifier = Modifier.testTag("login_language_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = strings.languageLabel,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.SERBIAN_LATIN) "SR • RSD" else "EN • EUR",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFamily)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.appName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                        Text(
                            text = strings.storeNameHeader,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.85f)
                        )
                    }
                }
            }
            Text(
                text = strings.loginSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun LoginFormCard(
    selectedRole: UserRole,
    onSelectRole: (UserRole) -> Unit,
    workers: List<WorkerProfile>,
    locations: List<VenueLocation>,
    selectedWorkerId: String,
    onSelectWorker: (String) -> Unit,
    pinInput: String,
    onPinChange: (String) -> Unit,
    pinError: Boolean,
    onAttemptLogin: () -> Unit,
    onQuickDemoLogin: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = strings.selectRole,
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LoginRoleCard(
                    label = strings.marketWorkerRole,
                    icon = Icons.Rounded.Storefront,
                    selected = selectedRole == UserRole.MARKET_WORKER,
                    onClick = { onSelectRole(UserRole.MARKET_WORKER) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("role_market_worker_tab")
                )
                LoginRoleCard(
                    label = strings.restaurantWorkerRole,
                    icon = Icons.Rounded.Restaurant,
                    selected = selectedRole == UserRole.RESTAURANT_WORKER,
                    onClick = { onSelectRole(UserRole.RESTAURANT_WORKER) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("role_restaurant_worker_tab")
                )
                LoginRoleCard(
                    label = strings.adminRole,
                    icon = Icons.Rounded.AdminPanelSettings,
                    selected = selectedRole == UserRole.ADMIN,
                    onClick = { onSelectRole(UserRole.ADMIN) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("role_admin_tab")
                )
            }

            AnimatedVisibility(visible = selectedRole.isWorker) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = strings.selectWorker,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        workers.forEach { worker ->
                            val locName = locations.firstOrNull { it.id == worker.locationId }?.name
                            val chipLabel = if (locName != null) {
                                "${worker.name} • $locName"
                            } else {
                                worker.name
                            }
                            FilterChip(
                                selected = worker.id == selectedWorkerId,
                                onClick = { onSelectWorker(worker.id) },
                                label = { Text(chipLabel) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Badge,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = pinInput,
                onValueChange = onPinChange,
                label = { Text(strings.enterPin) },
                placeholder = {
                    Text(
                        if (selectedRole.isWorker) strings.pinHintWorker else strings.pinHintAdmin
                    )
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Lock, contentDescription = null)
                },
                supportingText = {
                    Text(
                        text = when {
                            pinError -> strings.invalidPinError
                            selectedRole.isWorker -> strings.pinHintWorker
                            else -> strings.pinHintAdmin
                        }
                    )
                },
                isError = pinError,
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onAttemptLogin() }),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    letterSpacing = 4.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pin_input_field")
            )

            Button(
                onClick = onAttemptLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_submit_btn"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = strings.signInButton,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.quickDemoTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onQuickDemoLogin(UserRole.MARKET_WORKER) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("demo_market_worker_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = strings.demoMarketWorkerBtn,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FilledTonalButton(
                        onClick = { onQuickDemoLogin(UserRole.RESTAURANT_WORKER) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("demo_restaurant_worker_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = strings.demoRestaurantWorkerBtn,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                FilledTonalButton(
                    onClick = { onQuickDemoLogin(UserRole.ADMIN) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("demo_admin_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = strings.demoAdminBtn,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginRoleCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = fg,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
