package com.example.marketpos.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.LocationReportSummary
import com.example.marketpos.model.ReportPeriodType
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.ui.components.PosTopAppBar
import com.example.marketpos.ui.screens.admin.GlobalEarningsTab
import com.example.marketpos.ui.screens.admin.GlobalStockTab
import com.example.marketpos.viewmodel.AdminGlobalUiState

private enum class GlobalTab {
    EARNINGS,
    STOCK
}

@Composable
fun AdminGlobalScreen(
    uiState: AdminGlobalUiState,
    onOpenLocationAdmin: (VenueLocation) -> Unit,
    onAdjustStock: (String, Int) -> Unit,
    onBuildReportSummary: (
        locationId: String?,
        periodType: ReportPeriodType,
        startMillis: Long,
        endMillis: Long
    ) -> LocationReportSummary,
    onExportPdfReport: (LocationReportSummary) -> Unit,
    onDismissPdfResult: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalStrings.current
    var activeTab by rememberSaveable { mutableStateOf(GlobalTab.EARNINGS) }
    var showReportDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler { onBack() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp

        Scaffold(
            topBar = {
                PosTopAppBar(
                    title = strings.globalOverviewTitle,
                    subtitle = "${uiState.locations.size} • ${strings.adminPanelTitle}",
                    currencyBadge = strings.activeCurrencyLabel,
                    onOpenSettings = onOpenSettings,
                    onLogout = onLogout,
                    onBack = onBack,
                    extraActions = {
                        IconButton(
                            onClick = { showReportDialog = true },
                            modifier = Modifier.testTag("global_pdf_report_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PictureAsPdf,
                                contentDescription = strings.pdfReportTitle,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        GlobalTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            val label = when (tab) {
                                GlobalTab.EARNINGS -> strings.globalTabEarnings
                                GlobalTab.STOCK -> strings.globalTabStock
                            }
                            val icon = when (tab) {
                                GlobalTab.EARNINGS -> if (selected) Icons.Rounded.Analytics else Icons.Outlined.Analytics
                                GlobalTab.STOCK -> if (selected) Icons.Rounded.Inventory2 else Icons.Outlined.Inventory2
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag("global_nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        GlobalTab.entries.forEach { tab ->
                            val selected = activeTab == tab
                            val label = when (tab) {
                                GlobalTab.EARNINGS -> strings.globalTabEarnings
                                GlobalTab.STOCK -> strings.globalTabStock
                            }
                            val icon = when (tab) {
                                GlobalTab.EARNINGS -> if (selected) Icons.Rounded.Analytics else Icons.Outlined.Analytics
                                GlobalTab.STOCK -> if (selected) Icons.Rounded.Inventory2 else Icons.Outlined.Inventory2
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { activeTab = tab },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("global_rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                when (activeTab) {
                    GlobalTab.EARNINGS -> GlobalEarningsTab(
                        uiState = uiState,
                        onOpenLocationAdmin = onOpenLocationAdmin,
                        onOpenReportDialog = { showReportDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    GlobalTab.STOCK -> GlobalStockTab(
                        uiState = uiState,
                        onAdjustStock = onAdjustStock,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showReportDialog) {
        ReportExportDialog(
            settings = uiState.settings,
            locations = uiState.locations,
            allTransactions = uiState.allTransactions,
            allOrders = uiState.allOrders,
            pdfExportResult = uiState.pdfExportResult,
            initialLocationId = null,
            allowLocationSwitch = true,
            onDismiss = { showReportDialog = false },
            onBuildSummary = onBuildReportSummary,
            onExportPdf = onExportPdfReport,
            onDismissResult = onDismissPdfResult
        )
    }
}
