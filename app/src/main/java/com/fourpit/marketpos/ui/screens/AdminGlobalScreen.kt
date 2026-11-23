package com.fourpit.marketpos.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.PosSettings
import com.fourpit.marketpos.ui.screens.admin.GlobalEarningsTab
import com.fourpit.marketpos.ui.screens.admin.GlobalStockTab
import com.fourpit.marketpos.util.PosReportPdfGenerator
import com.fourpit.marketpos.viewmodel.AdminGlobalViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGlobalScreen(
    viewModel: AdminGlobalViewModel,
    settings: PosSettings,
    pdfGenerator: PosReportPdfGenerator,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(strings.globalOverview, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_global_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = strings.globalEarnings) },
                    label = { Text(strings.globalEarnings) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = strings.globalStock) },
                    label = { Text(strings.globalStock) }
                )
            }
        }
    ) { innerPadding ->
        if (selectedTab == 0) {
            GlobalEarningsTab(
                uiState = uiState,
                currency = settings.currency,
                eurRate = settings.eurExchangeRate,
                onExportPdfClick = { showExportDialog = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            GlobalStockTab(
                products = uiState.allProducts,
                locations = uiState.locations,
                currency = settings.currency,
                eurRate = settings.eurExchangeRate,
                onRestock = { viewModel.restockProduct(it, 10) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }

    if (showExportDialog) {
        val locNames = remember(uiState.locations) { uiState.locations.associate { it.id to it.name } }
        ReportExportDialog(
            title = "Consolidated Global Report",
            scopeLabel = "All Branches (${uiState.locations.size})",
            orders = uiState.allOrders,
            locationNames = locNames,
            settings = settings,
            pdfGenerator = pdfGenerator,
            onDismiss = { showExportDialog = false },
            onStatusMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
        )
    }
}
