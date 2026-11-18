package com.example.marketpos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.marketpos.i18n.LocalStrings
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.LocationReportSummary
import com.example.marketpos.model.PdfExportResult
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.ReportPeriodType
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.ui.screens.reports.PickingReportDate
import com.example.marketpos.ui.screens.reports.ReportDatePickerModal
import com.example.marketpos.ui.screens.reports.ReportExportBottomBar
import com.example.marketpos.ui.screens.reports.ReportLocationSelectorRow
import com.example.marketpos.ui.screens.reports.ReportPeriodSelectorSection
import com.example.marketpos.ui.screens.reports.ReportSummaryHeaderCard
import com.example.marketpos.ui.screens.reports.WorkerReportRowCard
import com.example.marketpos.ui.screens.reports.shiftMonth
import com.example.marketpos.util.CurrencyFormatter
import com.example.marketpos.util.ReportAggregator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportExportDialog(
    settings: AppSettings,
    locations: List<VenueLocation>,
    allTransactions: List<SaleTransaction>,
    allOrders: List<PosOrder>,
    pdfExportResult: PdfExportResult?,
    initialLocationId: String?,
    allowLocationSwitch: Boolean = true,
    onDismiss: () -> Unit,
    onBuildSummary: (
        locationId: String?,
        periodType: ReportPeriodType,
        startMillis: Long,
        endMillis: Long
    ) -> LocationReportSummary,
    onExportPdf: (LocationReportSummary) -> Unit,
    onDismissResult: () -> Unit
) {
    val strings = LocalStrings.current
    val lang = settings.language
    val rate = settings.rsdPerEur

    var selectedLocationId by rememberSaveable(initialLocationId) { mutableStateOf(initialLocationId) }
    var periodType by rememberSaveable { mutableStateOf(ReportPeriodType.DAILY) }
    var anchorDayMillis by rememberSaveable {
        mutableLongStateOf(CurrencyFormatter.localDayStartMillis(System.currentTimeMillis()))
    }
    var rangeStartMillis by rememberSaveable {
        mutableLongStateOf(
            CurrencyFormatter.localDayStartMillis(System.currentTimeMillis() - 6L * 24L * 3600_000L)
        )
    }
    var rangeEndMillis by rememberSaveable {
        mutableLongStateOf(CurrencyFormatter.localDayStartMillis(System.currentTimeMillis()))
    }
    var pickingDate by rememberSaveable { mutableStateOf(PickingReportDate.NONE) }

    val (boundStart, boundEnd) = remember(periodType, anchorDayMillis, rangeStartMillis, rangeEndMillis) {
        ReportAggregator.computePeriodBounds(
            periodType = periodType,
            anchorMillis = anchorDayMillis,
            rangeStartMillis = rangeStartMillis,
            rangeEndMillis = rangeEndMillis
        )
    }

    val summary = remember(
        selectedLocationId,
        periodType,
        boundStart,
        boundEnd,
        allTransactions,
        allOrders,
        locations
    ) {
        onBuildSummary(selectedLocationId, periodType, boundStart, boundEnd)
    }

    Dialog(
        onDismissRequest = {
            onDismissResult()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 920.dp),
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = strings.pdfReportTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val locLabel = summary.location?.name ?: strings.reportAllLocationsOption
                                    Text(
                                        text = "$locLabel • ${strings.reportPeriodTypeName(periodType)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        onDismissResult()
                                        onDismiss()
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("report_dialog_close_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = strings.close
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        )
                    },
                    bottomBar = {
                        ReportExportBottomBar(
                            pdfExportResult = pdfExportResult,
                            summary = summary,
                            onExportPdf = onExportPdf
                        )
                    }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (allowLocationSwitch && locations.size > 1) {
                            item {
                                ReportLocationSelectorRow(
                                    locations = locations,
                                    selectedLocationId = selectedLocationId,
                                    onSelectLocation = { locId ->
                                        onDismissResult()
                                        selectedLocationId = locId
                                    }
                                )
                            }
                        }

                        item {
                            ReportPeriodSelectorSection(
                                periodType = periodType,
                                anchorDayMillis = anchorDayMillis,
                                rangeStartMillis = rangeStartMillis,
                                rangeEndMillis = rangeEndMillis,
                                language = lang,
                                onSelectPeriodType = { type ->
                                    onDismissResult()
                                    periodType = type
                                },
                                onShiftMonth = { delta ->
                                    onDismissResult()
                                    anchorDayMillis = shiftMonth(anchorDayMillis, delta)
                                },
                                onRequestDatePick = { pickingDate = it }
                            )
                        }

                        item {
                            ReportSummaryHeaderCard(
                                summary = summary,
                                language = lang,
                                rsdPerEur = rate
                            )
                        }

                        item {
                            Text(
                                text = strings.reportWorkerStatsTitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (summary.workerRows.isEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = strings.reportNoWorkerData,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(summary.workerRows, key = { it.workerId }) { row ->
                                WorkerReportRowCard(
                                    row = row,
                                    language = lang,
                                    rsdPerEur = rate
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (pickingDate != PickingReportDate.NONE) {
        val initialLocal = when (pickingDate) {
            PickingReportDate.SINGLE_DAY -> anchorDayMillis
            PickingReportDate.RANGE_START -> rangeStartMillis
            PickingReportDate.RANGE_END -> rangeEndMillis
            PickingReportDate.NONE -> System.currentTimeMillis()
        }
        ReportDatePickerModal(
            initialLocalMillis = initialLocal,
            onDismiss = { pickingDate = PickingReportDate.NONE },
            onDateSelected = { localDay ->
                onDismissResult()
                when (pickingDate) {
                    PickingReportDate.SINGLE_DAY -> anchorDayMillis = localDay
                    PickingReportDate.RANGE_START -> rangeStartMillis = localDay
                    PickingReportDate.RANGE_END -> rangeEndMillis = localDay
                    PickingReportDate.NONE -> Unit
                }
            }
        )
    }
}
