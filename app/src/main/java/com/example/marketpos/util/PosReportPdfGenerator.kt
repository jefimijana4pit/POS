package com.example.marketpos.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import androidx.annotation.VisibleForTesting
import androidx.core.content.FileProvider
import com.example.marketpos.i18n.AppStrings
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.LocationReportSummary
import com.example.marketpos.model.PdfExportResult
import com.example.marketpos.model.ReportPeriodType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PosReportPdfGenerator {

    internal const val PAGE_WIDTH = 595
    internal const val PAGE_HEIGHT = 842
    private const val MARGIN_H = 30f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_H * 2
    private const val PAGE_TOP = 34f
    private const val PAGE_BOTTOM = PAGE_HEIGHT - 36f

    // Column widths sum to 535pt (= 595 - 60)
    private const val COL_WORKER_W = 105f
    private const val COL_BILLS_W = 68f
    private const val COL_TABLES_W = 46f
    private const val COL_PRODUCTS_W = 228f
    private const val COL_REVENUE_W = 88f

    fun buildFileName(summary: LocationReportSummary): String {
        val rawLoc = if (summary.isGlobal || summary.location == null) {
            "Global_All_Locations"
        } else {
            summary.location.name
        }
        val safeLoc = rawLoc
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "Report" }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateSuffix = when (summary.periodType) {
            ReportPeriodType.DAILY -> dateFormat.format(Date(summary.startMillis))
            ReportPeriodType.MONTHLY -> SimpleDateFormat("yyyy-MM", Locale.US).format(Date(summary.startMillis))
            ReportPeriodType.DATE_RANGE -> "${dateFormat.format(Date(summary.startMillis))}_to_${dateFormat.format(Date(summary.endMillis))}"
        }
        return "MarketPOS_${safeLoc}_${dateSuffix}.pdf"
    }

    fun buildPdfBytes(
        summary: LocationReportSummary,
        strings: AppStrings,
        settings: AppSettings
    ): ByteArray {
        return renderReport(
            backend = AndroidPdfRenderBackend(),
            summary = summary,
            strings = strings,
            settings = settings
        )
    }

    @VisibleForTesting
    internal fun renderReport(
        backend: PdfRenderBackend,
        summary: LocationReportSummary,
        strings: AppStrings,
        settings: AppSettings
    ): ByteArray {
        val lang = settings.language
        val rate = settings.rsdPerEur

        var pageNum = 1
        var canvas = backend.startPage(PAGE_WIDTH, PAGE_HEIGHT, pageNum)

        val headerBgPaint = Paint().apply {
            color = Color.rgb(0, 108, 76)
            style = Paint.Style.FILL
        }
        val softBoxPaint = Paint().apply {
            color = Color.rgb(239, 245, 240)
            style = Paint.Style.FILL
        }
        val tableHeaderPaint = Paint().apply {
            color = Color.rgb(220, 237, 228)
            style = Paint.Style.FILL
        }
        val altRowPaint = Paint().apply {
            color = Color.rgb(248, 251, 249)
            style = Paint.Style.FILL
        }
        val totalsBgPaint = Paint().apply {
            color = Color.rgb(230, 242, 236)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(185, 200, 192)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitleWhitePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(220, 248, 236)
            textSize = 9.5f
        }
        val sectionTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(23, 29, 26)
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 34, 31)
            textSize = 8.6f
        }
        val bodyBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(23, 29, 26)
            textSize = 8.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val mutedPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(75, 86, 80)
            textSize = 8.2f
        }
        val tableHeaderTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 56, 38)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Executive Header Banner
        var y = PAGE_TOP
        val headerRect = RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH, y + 84f)
        canvas.drawRoundRect(headerRect, 10f, 10f, headerBgPaint)

        val locName = if (summary.isGlobal || summary.location == null) {
            strings.globalOverviewTitle
        } else {
            summary.location.name
        }
        val locTypeAndAddress = if (summary.isGlobal || summary.location == null) {
            strings.globalReportSubtitle
        } else {
            "${strings.venueName(summary.location.venueMode)} • ${summary.location.address}"
        }
        val periodLabel = PosReportPdfHelpers.formatPeriodRangeLabel(summary, lang)
        val generatedLabel = "${strings.reportGeneratedAtLabel}: ${
            CurrencyFormatter.formatDateTime(summary.generatedAtMillis, lang)
        }"

        canvas.drawText("${strings.appName} — ${strings.pdfReportTitle}", MARGIN_H + 14f, y + 22f, titlePaint)
        canvas.drawText("$locName  |  $locTypeAndAddress", MARGIN_H + 14f, y + 40f, subtitleWhitePaint)
        canvas.drawText(
            "${strings.reportPeriodTypeName(summary.periodType)}: $periodLabel",
            MARGIN_H + 14f,
            y + 56f,
            subtitleWhitePaint
        )
        canvas.drawText(generatedLabel, MARGIN_H + 14f, y + 71f, subtitleWhitePaint)
        y += 96f

        // Summary KPI Card
        val kpiRect = RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH, y + 54f)
        canvas.drawRoundRect(kpiRect, 8f, 8f, softBoxPaint)
        canvas.drawRoundRect(kpiRect, 8f, 8f, borderPaint)

        val kpiW = CONTENT_WIDTH / 5
        val kpiPairs = listOf(
            strings.kpiRevenue to CurrencyFormatter.format(summary.totalRevenueRsd, lang, rate),
            strings.reportFinishedBillsLabel to summary.finishedBillsCount.toString(),
            strings.reportCancelledBillsLabel to summary.cancelledOrdersCount.toString(),
            strings.reportTablesHandledLabel to summary.tablesHandledCount.toString(),
            strings.reportUnitsSoldLabel to summary.totalUnitsSold.toString()
        )
        kpiPairs.forEachIndexed { idx, (label, value) ->
            val kx = MARGIN_H + idx * kpiW + 10f
            canvas.drawText(label, kx, y + 18f, mutedPaint)
            canvas.drawText(value, kx, y + 38f, bodyBoldPaint.apply { textSize = 10.5f })
        }
        bodyBoldPaint.textSize = 8.8f
        y += 66f

        val cashCardLine = "${strings.cashVsCardLabel}: ${
            CurrencyFormatter.format(summary.cashRevenueRsd, lang, rate)
        } / ${CurrencyFormatter.format(summary.cardRevenueRsd, lang, rate)}"
        canvas.drawText(cashCardLine, MARGIN_H, y + 8f, mutedPaint)
        y += 22f

        canvas.drawText(strings.reportWorkerStatsTitle, MARGIN_H, y + 10f, sectionTitlePaint)
        y += 18f

        fun drawTableHeader(targetCanvas: Canvas, topY: Float): Float {
            val h = 28f
            val rect = RectF(MARGIN_H, topY, MARGIN_H + CONTENT_WIDTH, topY + h)
            targetCanvas.drawRect(rect, tableHeaderPaint)
            targetCanvas.drawRect(rect, borderPaint)

            var x = MARGIN_H
            val colHeaders = listOf(
                COL_WORKER_W to strings.reportColWorker,
                COL_BILLS_W to strings.reportColBills,
                COL_TABLES_W to strings.reportColTables,
                COL_PRODUCTS_W to strings.reportColProducts,
                COL_REVENUE_W to strings.reportColRevenue
            )
            colHeaders.forEachIndexed { idx, (w, text) ->
                if (idx > 0) targetCanvas.drawLine(x, topY, x, topY + h, borderPaint)
                val layout = PosReportPdfHelpers.createStaticLayout(
                    text,
                    tableHeaderTextPaint,
                    (w - 8f).toInt().coerceAtLeast(10)
                )
                targetCanvas.save()
                targetCanvas.translate(x + 4f, topY + 5f)
                layout.draw(targetCanvas)
                targetCanvas.restore()
                x += w
            }
            return topY + h
        }

        fun startNewPage(): Float {
            PosReportPdfHelpers.drawPageFooter(canvas, pageNum, strings, MARGIN_H, PAGE_HEIGHT)
            backend.finishPage(pageNum)
            pageNum++
            canvas = backend.startPage(PAGE_WIDTH, PAGE_HEIGHT, pageNum)
            var newY = PAGE_TOP
            canvas.drawText(
                "$locName — ${strings.reportWorkerStatsTitle}",
                MARGIN_H,
                newY + 10f,
                sectionTitlePaint
            )
            newY += 18f
            return drawTableHeader(canvas, newY)
        }

        y = drawTableHeader(canvas, y)

        if (summary.workerRows.isEmpty()) {
            val emptyRect = RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH, y + 32f)
            canvas.drawRect(emptyRect, borderPaint)
            canvas.drawText(strings.reportNoWorkerData, MARGIN_H + 10f, y + 20f, mutedPaint)
        } else {
            summary.workerRows.forEachIndexed { index, row ->
                val workerLayout = PosReportPdfHelpers.createStaticLayout(row.workerName, bodyBoldPaint, (COL_WORKER_W - 8f).toInt())
                val billsText = buildString {
                    append("${strings.reportFinShort}: ${row.finishedBillsCount}\n")
                    append("${strings.reportCancShort}: ${row.cancelledOrdersCount}")
                    if (row.cancelledItemsCount > 0) {
                        append(" (${row.cancelledItemsCount} ${strings.reportItemsShort})")
                    }
                }
                val billsLayout = PosReportPdfHelpers.createStaticLayout(billsText, bodyPaint, (COL_BILLS_W - 8f).toInt())
                val tablesText = if (row.tablesHandledCount > 0) row.tablesHandledCount.toString() else "—"
                val tablesLayout = PosReportPdfHelpers.createStaticLayout(tablesText, bodyPaint, (COL_TABLES_W - 8f).toInt())
                val productsText = PosReportPdfHelpers.formatProductsColumn(row, lang, strings)
                val productsLayout = PosReportPdfHelpers.createStaticLayout(productsText, bodyPaint, (COL_PRODUCTS_W - 10f).toInt())
                val revText = CurrencyFormatter.format(row.revenueRsd, lang, rate)
                val revLayout = PosReportPdfHelpers.createStaticLayout(revText, bodyBoldPaint, (COL_REVENUE_W - 8f).toInt())

                val contentHeight = maxOf(
                    workerLayout.height,
                    billsLayout.height,
                    tablesLayout.height,
                    productsLayout.height,
                    revLayout.height
                )
                val rowHeight = (contentHeight + 12f).coerceAtLeast(28f)

                if (y + rowHeight > PAGE_BOTTOM - 28f) {
                    y = startNewPage()
                }

                val rowRect = RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH, y + rowHeight)
                if (index % 2 == 1) canvas.drawRect(rowRect, altRowPaint)
                canvas.drawRect(rowRect, borderPaint)

                val cells = listOf(
                    COL_WORKER_W to workerLayout,
                    COL_BILLS_W to billsLayout,
                    COL_TABLES_W to tablesLayout,
                    COL_PRODUCTS_W to productsLayout,
                    COL_REVENUE_W to revLayout
                )
                var x = MARGIN_H
                cells.forEachIndexed { cIdx, (w, layout) ->
                    if (cIdx > 0) canvas.drawLine(x, y, x, y + rowHeight, borderPaint)
                    canvas.save()
                    canvas.translate(x + 4f, y + 6f)
                    layout.draw(canvas)
                    canvas.restore()
                    x += w
                }
                y += rowHeight
            }

            val totalRowH = 30f
            if (y + totalRowH > PAGE_BOTTOM) {
                y = startNewPage()
            }
            val totalsRect = RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH, y + totalRowH)
            canvas.drawRect(totalsRect, totalsBgPaint)
            canvas.drawRect(totalsRect, borderPaint)

            val totalCells = listOf(
                COL_WORKER_W to strings.totalLabel,
                COL_BILLS_W to "${summary.finishedBillsCount} / ${summary.cancelledOrdersCount}",
                COL_TABLES_W to if (summary.tablesHandledCount > 0) summary.tablesHandledCount.toString() else "—",
                COL_PRODUCTS_W to "${summary.totalUnitsSold} ${strings.reportUnitsShort}",
                COL_REVENUE_W to CurrencyFormatter.format(summary.totalRevenueRsd, lang, rate)
            )
            var x = MARGIN_H
            totalCells.forEachIndexed { idx, (w, txt) ->
                if (idx > 0) canvas.drawLine(x, y, x, y + totalRowH, borderPaint)
                val layout = PosReportPdfHelpers.createStaticLayout(txt, bodyBoldPaint, (w - 8f).toInt())
                canvas.save()
                canvas.translate(x + 4f, y + 8f)
                layout.draw(canvas)
                canvas.restore()
                x += w
            }
        }

        PosReportPdfHelpers.drawPageFooter(canvas, pageNum, strings, MARGIN_H, PAGE_HEIGHT)
        backend.finishPage(pageNum)
        return backend.finishDocument()
    }

    fun exportReportToPdf(
        context: Context,
        summary: LocationReportSummary,
        strings: AppStrings,
        settings: AppSettings
    ): PdfExportResult {
        return runCatching {
            val pdfBytes = buildPdfBytes(summary, strings, settings)
            val fileName = buildFileName(summary)

            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val cacheFile = File(reportsDir, fileName)
            cacheFile.writeBytes(pdfBytes)
            val shareUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/MarketPOS")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val downloadsUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (downloadsUri != null) {
                resolver.openOutputStream(downloadsUri)?.use { out ->
                    out.write(pdfBytes)
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(downloadsUri, values, null, null)
                PdfExportResult.Success(
                    fileName = fileName,
                    uriString = downloadsUri.toString(),
                    shareUriString = shareUri.toString(),
                    savedToDownloads = true
                )
            } else {
                PdfExportResult.Success(
                    fileName = fileName,
                    uriString = shareUri.toString(),
                    shareUriString = shareUri.toString(),
                    savedToDownloads = false
                )
            }
        }.getOrElse { err ->
            PdfExportResult.Error(err.localizedMessage ?: "PDF export failed")
        }
    }
}
