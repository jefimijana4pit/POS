package com.fourpit.marketpos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.PosLocation
import com.fourpit.marketpos.model.PosSettings
import com.fourpit.marketpos.ui.components.MonoPriceText
import com.fourpit.marketpos.ui.screens.admin.LocationEditorDialog
import com.fourpit.marketpos.ui.screens.admin.VenueSummaryCard
import com.fourpit.marketpos.util.CurrencyFormatter
import com.fourpit.marketpos.viewmodel.AdminVenueSelectViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVenueSelectScreen(
    viewModel: AdminVenueSelectViewModel,
    settings: PosSettings,
    onOpenGlobal: () -> Unit,
    onOpenBranch: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingLocation by remember { mutableStateOf<PosLocation?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.adminVenuesTitle, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("admin_settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = strings.settings)
                    }
                    IconButton(onClick = onLogout, modifier = Modifier.testTag("admin_logout_button")) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = strings.logout)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.AddBusiness, contentDescription = null) },
                text = { Text(strings.addLocation) },
                modifier = Modifier.testTag("add_location_fab")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Card(
                    onClick = onOpenGlobal,
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_global_overview_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.Analytics,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = strings.globalOverview,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = strings.globalOverviewSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            MonoPriceText(
                                text = CurrencyFormatter.formatCompact(
                                    uiState.globalRevenueRsd,
                                    settings.currency,
                                    settings.eurExchangeRate
                                ),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = "${uiState.globalOrdersCount} ${strings.ordersCount.lowercase()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }

            items(uiState.venues, key = { it.location.id }) { venue ->
                VenueSummaryCard(
                    data = venue,
                    currency = settings.currency,
                    eurRate = settings.eurExchangeRate,
                    onOpen = { onOpenBranch(venue.location.id) },
                    onEdit = { editingLocation = venue.location }
                )
            }
        }
    }

    if (showCreateDialog) {
        LocationEditorDialog(
            initial = null,
            onDismiss = { showCreateDialog = false },
            onSave = {
                viewModel.saveLocation(it)
                showCreateDialog = false
            }
        )
    }

    editingLocation?.let { loc ->
        LocationEditorDialog(
            initial = loc,
            onDismiss = { editingLocation = null },
            onSave = {
                viewModel.saveLocation(it)
                editingLocation = null
            },
            onDelete = { id ->
                viewModel.deleteLocation(id)
                editingLocation = null
            }
        )
    }
}
