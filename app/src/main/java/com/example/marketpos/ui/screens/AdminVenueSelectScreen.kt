package com.example.marketpos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.VenueMode
import com.example.marketpos.ui.components.PosTopAppBar
import com.example.marketpos.ui.screens.admin.GlobalOverviewCard
import com.example.marketpos.ui.screens.admin.LocationEditorDialog
import com.example.marketpos.ui.screens.admin.LocationGrid
import com.example.marketpos.ui.screens.admin.LocationSectionHeader
import com.example.marketpos.ui.theme.LocalSemanticColors
import com.example.marketpos.util.CurrencyFormatter
import com.example.marketpos.viewmodel.AdminVenueSelectUiState

@Composable
fun AdminVenueSelectScreen(
    uiState: AdminVenueSelectUiState,
    onOpenGlobalView: () -> Unit,
    onSelectLocation: (VenueLocation) -> Unit,
    onSaveLocation: (
        existingId: String?,
        name: String,
        address: String,
        venueMode: VenueMode,
        copyStarterData: Boolean
    ) -> Unit,
    onDeleteLocation: (String) -> Unit,
    onDismissLocationError: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalStrings.current
    val semanticColors = LocalSemanticColors.current
    val settings = uiState.settings
    val lang = settings.language
    val rate = settings.rsdPerEur

    val snackbarHostState = remember { SnackbarHostState() }
    var creatingVenueMode by remember { mutableStateOf<VenueMode?>(null) }
    var editingLocation by remember { mutableStateOf<VenueLocation?>(null) }
    var deletingLocation by remember { mutableStateOf<VenueLocation?>(null) }

    LaunchedEffect(uiState.locationErrorNotice) {
        if (uiState.locationErrorNotice) {
            snackbarHostState.showSnackbar(strings.cannotDeleteLocationMsg)
            onDismissLocationError()
        }
    }

    val marketStats = remember(uiState.locationStats) {
        uiState.locationStats.filter { it.location.venueMode == VenueMode.MARKET }
    }
    val barStats = remember(uiState.locationStats) {
        uiState.locationStats.filter { it.location.venueMode == VenueMode.BAR_RESTAURANT }
    }

    Scaffold(
        topBar = {
            PosTopAppBar(
                title = strings.adminPanelTitle,
                subtitle = uiState.currentUser?.name,
                currencyBadge = strings.activeCurrencyLabel,
                onOpenSettings = onOpenSettings,
                onLogout = onLogout
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            val isWide = maxWidth >= 760.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 920.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                GlobalOverviewCard(
                    revenueText = CurrencyFormatter.format(uiState.globalRevenueRsd, lang, rate),
                    locationsCount = uiState.locations.size,
                    totalSkus = uiState.allProductsCount,
                    lowStockCount = uiState.globalLowOrOutStockCount,
                    onClick = onOpenGlobalView
                )

                LocationSectionHeader(
                    title = "${strings.marketsSectionTitle} (${marketStats.size})",
                    addLabel = strings.addMarketBtn,
                    addTestTag = "admin_add_market_btn",
                    onAdd = { creatingVenueMode = VenueMode.MARKET }
                )

                LocationGrid(
                    statsList = marketStats,
                    isWide = isWide,
                    language = lang,
                    rsdPerEur = rate,
                    canDelete = marketStats.size > 1,
                    warningContainer = semanticColors.warningContainer,
                    onWarningContainer = semanticColors.onWarningContainer,
                    onOpen = onSelectLocation,
                    onEdit = { editingLocation = it },
                    onDelete = { deletingLocation = it }
                )

                LocationSectionHeader(
                    title = "${strings.barsSectionTitle} (${barStats.size})",
                    addLabel = strings.addBarBtn,
                    addTestTag = "admin_add_bar_btn",
                    onAdd = { creatingVenueMode = VenueMode.BAR_RESTAURANT }
                )

                LocationGrid(
                    statsList = barStats,
                    isWide = isWide,
                    language = lang,
                    rsdPerEur = rate,
                    canDelete = barStats.size > 1,
                    warningContainer = semanticColors.warningContainer,
                    onWarningContainer = semanticColors.onWarningContainer,
                    onOpen = onSelectLocation,
                    onEdit = { editingLocation = it },
                    onDelete = { deletingLocation = it }
                )
            }
        }
    }

    val dialogMode = editingLocation?.venueMode ?: creatingVenueMode
    if (dialogMode != null) {
        LocationEditorDialog(
            existing = editingLocation,
            venueMode = dialogMode,
            onDismiss = {
                creatingVenueMode = null
                editingLocation = null
            },
            onSave = { name, address, copyStarterData ->
                onSaveLocation(
                    editingLocation?.id,
                    name,
                    address,
                    dialogMode,
                    copyStarterData
                )
                creatingVenueMode = null
                editingLocation = null
            }
        )
    }

    deletingLocation?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingLocation = null },
            title = { Text(strings.deleteLocationTitle) },
            text = { Text(strings.deleteLocationConfirm(target.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteLocation(target.id)
                        deletingLocation = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingLocation = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}
