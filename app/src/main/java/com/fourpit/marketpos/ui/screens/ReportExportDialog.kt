package com.fourpit.marketpos.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fourpit.marketpos.i18n.LocalAppStrings
import com.fourpit.marketpos.model.PosOrder
import com.fourpit.marketpos.model.PosSettings
import com.fourpit.marketpos.model.ReportPeriod
import com.fourpit.marketpos.ui.screens.reports.ReportExportBottomBar
import com.fourpit.marketpos.ui.screens.reports.ReportPeriodControls
import com.fourpit.marketpos.ui.screens.reports.ReportPreviewCard
import com.fourpit.marketpos.util.PdfExportHelper
import com.fourpit.marketpos.util.PosReportPdfGenerator
import com.fourpit.marketpos.util.ReportAggregator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReportExportDialog(
    title: String,
    scopeLabel: String,
    orders: List<PosOrder>,
    locationNames: Map<Long, String>,
    settings: PosSettings,
    pdfGenerator: PosReportPdfGenerator,
    onDismiss: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.LAST_7_DAYS) }
    var isExporting by remember { mutableStateOf(false) }

    val aggregated = remember(orders, selectedPeriod, title, scopeLabel, locationNames) {
        ReportAggregator.aggregate(
            orders = orders,
            period = selectedPeriod,
            title = title,
            scopeLabel = scopeLabel,
            locationNames = locationNames
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.exportPdfReport, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.reportPeriod, style = MaterialTheme.typography.labelLarge)
                ReportPeriodControls(
                    selectedPeriod = selectedPeriod,
                    onSelectPeriod = { selectedPeriod = it }
                )
                ReportPreviewCard(
                    report = aggregated,
                    currency = settings.currency,
                    eurRate = settings.eurExchangeRate
                )
            }
        },
        confirmButton = {
            ReportExportBottomBar(
                isExporting = isExporting,
                onDismiss = onDismiss,
                onExportClick = {
                    if (isExporting) return@ReportExportBottomBar
                    isExporting = true
                    scope.launch {
                        val file = runCatching {
                            withContext(Dispatchers.IO) {
                                pdfGenerator.generateReportPdf(
                                    context = context,
                                    report = aggregated,
                                    businessName = settings.businessName,
                                    currency = settings.currency,
                                    eurRate = settings.eurExchangeRate
                                )
                            }
                        }.getOrNull()
                        isExporting = false
                        if (file != null) {
                            PdfExportHelper.sharePdfFile(context, file, strings.exportPdfReport)
                            onStatusMessage(strings.reportExported)
                            onDismiss()
                        } else {
                            onStatusMessage(strings.reportExportFailed)
                        }
                    }
                }
            )
        }
    )
}
