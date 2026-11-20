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
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.annotation.VisibleForTesting
import androidx.core.content.FileProvider
import com.example.marketpos.i18n.AppStrings
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.LocationReportSummary
import com.example.marketpos.model.PdfExportResult
import com.example.marketpos.model.ReportPeriodType
import com.example.marketpos.model.WorkerProductSoldStat
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
    private const val COL_WORKER_W = 100f
    private const val COL_BILLS_W = 68f
    private const val COL_TABLES_W = 44f
    private const val COL_PRODUCTS_W = 237f
    private const val COL_REVENUE_W = 86f

    // Sub-column widths inside COL_PRODUCTS_W (sum = 237pt)
    private const val SUB_COL_ITEM_W = 133f
    private const val SUB_COL_QTY_W = 34f
    private const val SUB_COL_AMOUNT_W = 70f

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

        val subHeaderBgPaint = Paint().apply {
            color = Color.rgb(236, 244, 239)
            style = Paint.Style.FILL
        }
        val subBorderPaint = Paint().apply {
            color = Color.rgb(212, 222, 217)
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }
        val subHeaderTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(48, 66, 58)
            textSize = 7.4f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subItemPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 34, 31)
            textSize = 8.0f
        }
        val subQtyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(55, 68, 62)
            textSize = 8.0f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        data class PdfSubItemRow(
            val nameLayout: StaticLayout,
            val qtyLayout: StaticLayout,
            val amountLayout: StaticLayout,
            val height: Float
        )

        fun buildSubItemRows(items: List<WorkerProductSoldStat>): List<PdfSubItemRow> {
            return items.map { stat ->
                val nameLayout = PosReportPdfHelpers.createStaticLayout(
                    text = stat.displayName(lang),
                    paint = subItemPaint,
                    widthPx = (SUB_COL_ITEM_W - 8f).toInt()
                )
                val qtyLayout = PosReportPdfHelpers.createStaticLayout(
                    text = "${stat.quantity}×",
                    paint = subQtyPaint,
                    widthPx = (SUB_COL_QTY_W - 6f).toInt(),
                    alignment = Layout.Alignment.ALIGN_OPPOSITE
                )
                val amountLayout = PosReportPdfHelpers.createStaticLayout(
                    text = CurrencyFormatter.format(stat.revenueRsd, lang, rate),
                    paint = subItemPaint,
                    widthPx = (SUB_COL_AMOUNT_W - 8f).toInt(),
                    alignment = Layout.Alignment.ALIGN_OPPOSITE
                )
                val rowH = (maxOf(nameLayout.height, qtyLayout.height, amountLayout.height) + 6f)
                    .coerceAtLeast(14f)
                PdfSubItemRow(nameLayout, qtyLayout, amountLayout, rowH)
            }
        }

        val subHeaderH = 14f
        val subFooterH = 14f
        val maxSingleChunkHeight = (PAGE_BOTTOM - PAGE_TOP - 80f).coerceAtLeast(160f)

        if (summary.workerRows.isEmpty()) {
            val emptyRect = RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH, y + 32f)
            canvas.drawRect(emptyRect, borderPaint)
            canvas.drawText(strings.reportNoWorkerData, MARGIN_H + 10f, y + 20f, mutedPaint)
        } else {
            summary.workerRows.forEachIndexed { index, row ->
                val allSubRows = buildSubItemRows(row.productsSold)
                val chunks: List<List<PdfSubItemRow>> = if (allSubRows.isEmpty()) {
                    listOf(emptyList())
                } else {
                    val result = mutableListOf<List<PdfSubItemRow>>()
                    var current = mutableListOf<PdfSubItemRow>()
                    var accH = subHeaderH + subFooterH
                    allSubRows.forEach { itemRow ->
                        if (current.isNotEmpty() && accH + itemRow.height > maxSingleChunkHeight) {
                            result.add(current)
                            current = mutableListOf()
                            accH = subHeaderH + subFooterH
                        }
                        current.add(itemRow)
                        accH += itemRow.height
                    }
                    if (current.isNotEmpty()) result.add(current)
                    result
                }

                chunks.forEachIndexed { chunkIdx, subRowsChunk ->
                    val isFirstChunk = chunkIdx == 0
                    val isLastChunk = chunkIdx == chunks.lastIndex

                    val workerDisplayName = if (isFirstChunk) row.workerName else "${row.workerName} (cont.)"
                    val workerLayout = PosReportPdfHelpers.createStaticLayout(
                        workerDisplayName,
                        bodyBoldPaint,
                        (COL_WORKER_W - 8f).toInt()
                    )
                    val billsText = if (isFirstChunk) {
                        buildString {
                            append("${strings.reportFinShort}: ${row.finishedBillsCount}\n")
                            append("${strings.reportCancShort}: ${row.cancelledOrdersCount}")
                            if (row.cancelledItemsCount > 0) {
                                append(" (${row.cancelledItemsCount} ${strings.reportItemsShort})")
                            }
                        }
                    } else {
                        "—"
                    }
                    val billsLayout = PosReportPdfHelpers.createStaticLayout(billsText, bodyPaint, (COL_BILLS_W - 8f).toInt())
                    val tablesText = if (isFirstChunk && row.tablesHandledCount > 0) row.tablesHandledCount.toString() else "—"
                    val tablesLayout = PosReportPdfHelpers.createStaticLayout(tablesText, bodyPaint, (COL_TABLES_W - 8f).toInt())
                    val revText = if (isFirstChunk) CurrencyFormatter.format(row.revenueRsd, lang, rate) else "—"
                    val revLayout = PosReportPdfHelpers.createStaticLayout(
                        revText,
                        bodyBoldPaint,
                        (COL_REVENUE_W - 8f).toInt(),
                        alignment = Layout.Alignment.ALIGN_OPPOSITE
                    )

                    val productsContentH = if (subRowsChunk.isEmpty()) {
                        16f
                    } else {
                        subHeaderH + subRowsChunk.sumOf { it.height.toDouble() }.toFloat() +
                            if (isLastChunk) subFooterH else 0f
                    }

                    val standardColumnsH = maxOf(
                        workerLayout.height,
                        billsLayout.height,
                        tablesLayout.height,
                        revLayout.height
                    ) + 12f
                    val rowHeight = maxOf(standardColumnsH, productsContentH).coerceAtLeast(28f)

                    if (y + rowHeight > PAGE_BOTTOM - 28f) {
                        y = startNewPage()
                    }

                    val rowRect = RectF(MARGIN_H, y, MARGIN_H + CONTENT_WIDTH, y + rowHeight)
                    if (index % 2 == 1) canvas.drawRect(rowRect, altRowPaint)
                    canvas.drawRect(rowRect, borderPaint)

                    val colXWorker = MARGIN_H
                    val colXBills = colXWorker + COL_WORKER_W
                    val colXTables = colXBills + COL_BILLS_W
                    val colXProducts = colXTables + COL_TABLES_W
                    val colXRevenue = colXProducts + COL_PRODUCTS_W

                    // Vertical main table column borders
                    canvas.drawLine(colXBills, y, colXBills, y + rowHeight, borderPaint)
                    canvas.drawLine(colXTables, y, colXTables, y + rowHeight, borderPaint)
                    canvas.drawLine(colXProducts, y, colXProducts, y + rowHeight, borderPaint)
                    canvas.drawLine(colXRevenue, y, colXRevenue, y + rowHeight, borderPaint)

                    // Worker, Bills, Tables, Revenue cells
                    listOf(
                        colXWorker to workerLayout,
                        colXBills to billsLayout,
                        colXTables to tablesLayout,
                        colXRevenue to revLayout
                    ).forEach { (cellX, layout) ->
                        canvas.save()
                        canvas.translate(cellX + 4f, y + 6f)
                        layout.draw(canvas)
                        canvas.restore()
                    }

                    // Products tabular sub-table inside COL_PRODUCTS_W
                    if (subRowsChunk.isEmpty()) {
                        val emptyLayout = PosReportPdfHelpers.createStaticLayout("—", bodyPaint, (COL_PRODUCTS_W - 8f).toInt())
                        canvas.save()
                        canvas.translate(colXProducts + 4f, y + 6f)
                        emptyLayout.draw(canvas)
                        canvas.restore()
                    } else {
                        var subY = y
                        val subXQty = colXProducts + SUB_COL_ITEM_W
                        val subXAmount = subXQty + SUB_COL_QTY_W

                        // Mini header row
                        val subHeaderRect = RectF(colXProducts, subY, colXProducts + COL_PRODUCTS_W, subY + subHeaderH)
                        canvas.drawRect(subHeaderRect, subHeaderBgPaint)
                        canvas.drawLine(colXProducts, subY + subHeaderH, colXProducts + COL_PRODUCTS_W, subY + subHeaderH, subBorderPaint)

                        val hItem = PosReportPdfHelpers.createStaticLayout(strings.reportColItem, subHeaderTextPaint, (SUB_COL_ITEM_W - 8f).toInt())
                        val hQty = PosReportPdfHelpers.createStaticLayout(strings.reportColQty, subHeaderTextPaint, (SUB_COL_QTY_W - 6f).toInt(), Layout.Alignment.ALIGN_OPPOSITE)
                        val hAmt = PosReportPdfHelpers.createStaticLayout(strings.reportColRevenue, subHeaderTextPaint, (SUB_COL_AMOUNT_W - 8f).toInt(), Layout.Alignment.ALIGN_OPPOSITE)

                        canvas.save(); canvas.translate(colXProducts + 4f, subY + 3f); hItem.draw(canvas); canvas.restore()
                        canvas.save(); canvas.translate(subXQty + 3f, subY + 3f); hQty.draw(canvas); canvas.restore()
                        canvas.save(); canvas.translate(subXAmount + 4f, subY + 3f); hAmt.draw(canvas); canvas.restore()
                        subY += subHeaderH

                        // Individual item rows
                        subRowsChunk.forEachIndexed { rIdx, subRow ->
                            if (rIdx > 0) {
                                canvas.drawLine(colXProducts, subY, colXProducts + COL_PRODUCTS_W, subY, subBorderPaint)
                            }
                            canvas.save(); canvas.translate(colXProducts + 4f, subY + 3f); subRow.nameLayout.draw(canvas); canvas.restore()
                            canvas.save(); canvas.translate(subXQty + 3f, subY + 3f); subRow.qtyLayout.draw(canvas); canvas.restore()
                            canvas.save(); canvas.translate(subXAmount + 4f, subY + 3f); subRow.amountLayout.draw(canvas); canvas.restore()
                            subY += subRow.height
                        }

                        // Sub-table vertical dividers across header and item rows
                        canvas.drawLine(subXQty, y, subXQty, subY, subBorderPaint)
                        canvas.drawLine(subXAmount, y, subXAmount, subY, subBorderPaint)

                        // Units total footer strip on the final chunk for this worker
                        if (isLastChunk) {
                            val footerTop = (y + rowHeight - subFooterH).coerceAtLeast(subY)
                            val footerRect = RectF(colXProducts, footerTop, colXProducts + COL_PRODUCTS_W, y + rowHeight)
                            canvas.drawRect(footerRect, subHeaderBgPaint)
                            canvas.drawLine(colXProducts, footerTop, colXProducts + COL_PRODUCTS_W, footerTop, subBorderPaint)
                            val totalUnitsLayout = PosReportPdfHelpers.createStaticLayout(
                                "${strings.totalLabel}: ${row.totalUnitsSold} ${strings.reportUnitsShort}",
                                subHeaderTextPaint,
                                (COL_PRODUCTS_W - 8f).toInt()
                            )
                            canvas.save()
                            canvas.translate(colXProducts + 4f, footerTop + 3f)
                            totalUnitsLayout.draw(canvas)
                            canvas.restore()
                        }

                        // Re-stroke outer cell border cleanly over sub-fills
                        canvas.drawRect(rowRect, borderPaint)
                    }

                    y += rowHeight
                }
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
                val align = if (idx == totalCells.lastIndex) {
                    Layout.Alignment.ALIGN_OPPOSITE
                } else {
                    Layout.Alignment.ALIGN_NORMAL
                }
                val layout = PosReportPdfHelpers.createStaticLayout(txt, bodyBoldPaint, (w - 8f).toInt(), align)
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
