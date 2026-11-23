package com.fourpit.marketpos.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import com.fourpit.marketpos.model.BarTable
import com.fourpit.marketpos.model.PosSettings
import com.fourpit.marketpos.model.Product
import com.fourpit.marketpos.model.VenueType
import com.fourpit.marketpos.ui.screens.admin.AdminDashboardTab
import com.fourpit.marketpos.ui.screens.admin.AdminProductsTab
import com.fourpit.marketpos.ui.screens.admin.AdminWorkerLogTab
import com.fourpit.marketpos.ui.screens.admin.BarTableEditorDialog
import com.fourpit.marketpos.ui.screens.admin.ProductEditorDialog
import com.fourpit.marketpos.util.PosReportPdfGenerator
import com.fourpit.marketpos.viewmodel.AdminBranchViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    locationId: Long,
    viewModel: AdminBranchViewModel,
    settings: PosSettings,
    pdfGenerator: PosReportPdfGenerator,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isBar = uiState.location?.venueType == VenueType.BAR
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showCreateProduct by remember { mutableStateOf(false) }
    var editingTable by remember { mutableStateOf<BarTable?>(null) }
    var showCreateTable by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val logTabIndex = if (isBar) 3 else 2

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.location?.name ?: strings.dashboard, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_branch_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                }
            )
        },
        floatingActionButton = {
            when {
                selectedTab == 1 -> {
                    ExtendedFloatingActionButton(
                        onClick = { showCreateProduct = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text(strings.addProduct) },
                        modifier = Modifier.testTag("add_product_fab")
                    )
                }
                isBar && selectedTab == 2 -> {
                    ExtendedFloatingActionButton(
                        onClick = { showCreateTable = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text(strings.addTable) },
                        modifier = Modifier.testTag("add_table_fab")
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = strings.dashboard) },
                    label = { Text(strings.dashboard) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = strings.products) },
                    label = { Text(strings.products) }
                )
                if (isBar) {
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.TableRestaurant, contentDescription = strings.tables) },
                        label = { Text(strings.tables) }
                    )
                }
                NavigationBarItem(
                    selected = selectedTab == logTabIndex,
                    onClick = { selectedTab = logTabIndex },
                    icon = { Icon(Icons.Default.History, contentDescription = strings.workerLog) },
                    label = { Text(strings.workerLog) }
                )
            }
        }
    ) { innerPadding ->
        when {
            selectedTab == 0 -> AdminDashboardTab(
                uiState = uiState,
                currency = settings.currency,
                eurRate = settings.eurExchangeRate,
                onExportPdfClick = { showExportDialog = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            selectedTab == 1 -> AdminProductsTab(
                products = uiState.products,
                currency = settings.currency,
                eurRate = settings.eurExchangeRate,
                onEditProduct = { editingProduct = it },
                onRestockProduct = { viewModel.restockProduct(it, 10) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            isBar && selectedTab == 2 -> AdminTablesTab(
                tables = uiState.tables,
                onEditTable = { editingTable = it },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            else -> AdminWorkerLogTab(
                logs = uiState.workerLogs,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }

    if (showCreateProduct) {
        ProductEditorDialog(
            locationId = locationId,
            initial = null,
            onDismiss = { showCreateProduct = false },
            onSave = {
                viewModel.saveProduct(it)
                showCreateProduct = false
            }
        )
    }

    editingProduct?.let { prod ->
        ProductEditorDialog(
            locationId = locationId,
            initial = prod,
            onDismiss = { editingProduct = null },
            onSave = {
                viewModel.saveProduct(it)
                editingProduct = null
            },
            onDelete = { id ->
                viewModel.deleteProduct(id)
                editingProduct = null
            }
        )
    }

    if (showCreateTable) {
        BarTableEditorDialog(
            initial = null,
            onDismiss = { showCreateTable = false },
            onSave = { label, seats, existingId ->
                viewModel.saveTable(label, seats, existingId)
                showCreateTable = false
            }
        )
    }

    editingTable?.let { tbl ->
        BarTableEditorDialog(
            initial = tbl,
            onDismiss = { editingTable = null },
            onSave = { label, seats, existingId ->
                viewModel.saveTable(label, seats, existingId)
                editingTable = null
            },
            onDelete = { id ->
                viewModel.deleteTable(id)
                editingTable = null
            }
        )
    }

    if (showExportDialog) {
        val locName = uiState.location?.name ?: "Branch #$locationId"
        ReportExportDialog(
            title = "Branch Sales Report",
            scopeLabel = locName,
            orders = uiState.orders,
            locationNames = mapOf(locationId to locName),
            settings = settings,
            pdfGenerator = pdfGenerator,
            onDismiss = { showExportDialog = false },
            onStatusMessage = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
        )
    }
}
