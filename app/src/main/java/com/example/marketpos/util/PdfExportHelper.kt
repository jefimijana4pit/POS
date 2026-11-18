package com.example.marketpos.util

import android.content.Context
import com.example.marketpos.i18n.AppStrings
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.LocationReportSummary
import com.example.marketpos.model.PdfExportResult
import com.example.marketpos.model.PosOrder
import com.example.marketpos.model.ReportPeriodType
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.model.VenueLocation
import com.example.marketpos.model.WorkerProfile

object PdfExportHelper {

    fun buildReportSummary(
        locationId: String?,
        periodType: ReportPeriodType,
        startMillis: Long,
        endMillis: Long,
        locations: List<VenueLocation>,
        workers: List<WorkerProfile>,
        allTransactions: List<SaleTransaction>,
        allOrders: List<PosOrder>
    ): LocationReportSummary {
        val isGlobal = locationId == null
        val targetLoc = if (isGlobal) null else locations.firstOrNull { it.id == locationId }
        return ReportAggregator.buildLocationReportSummary(
            location = targetLoc,
            isGlobal = isGlobal,
            periodType = periodType,
            startMillis = startMillis,
            endMillis = endMillis,
            workers = workers,
            transactions = allTransactions,
            orders = allOrders
        )
    }

    fun exportToPdf(
        context: Context,
        summary: LocationReportSummary,
        strings: AppStrings,
        settings: AppSettings
    ): PdfExportResult {
        return PosReportPdfGenerator.exportReportToPdf(
            context = context,
            summary = summary,
            strings = strings,
            settings = settings
        )
    }
}
