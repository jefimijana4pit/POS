package com.fourpit.marketpos.util

import android.content.Context
import android.graphics.pdf.PdfDocument
import com.fourpit.marketpos.model.CurrencyCode
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosReportPdfGenerator {

    fun generateReportPdf(
        context: Context,
        report: AggregatedReport,
        businessName: String,
        currency: CurrencyCode,
        eurRate: Double
    ): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(report.generatedAtMillis))
        val outFile = File(reportsDir, "MarketPOS_Report_$stamp.pdf")

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(
            PosReportPdfHelpers.PAGE_WIDTH,
            PosReportPdfHelpers.PAGE_HEIGHT,
            1
        ).create()

        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = 44f
        var pageNum = 1

        fun ensureSpace(neededHeight: Float) {
            if (y + neededHeight > PosReportPdfHelpers.PAGE_HEIGHT - 44f) {
                document.finishPage(page)
                pageNum += 1
                val nextInfo = PdfDocument.PageInfo.Builder(
                    PosReportPdfHelpers.PAGE_WIDTH,
                    PosReportPdfHelpers.PAGE_HEIGHT,
                    pageNum
                ).create()
                page = document.startPage(nextInfo)
                canvas = page.canvas
                y = 44f
            }
        }

        // Header banner
        canvas.drawRect(
            PosReportPdfHelpers.MARGIN_X,
            y - 24f,
            PosReportPdfHelpers.PAGE_WIDTH - PosReportPdfHelpers.MARGIN_X,
            y + 38f,
            PosReportPdfHelpers.headerFillPaint()
        )
        canvas.drawText(
            PosReportPdfHelpers.truncate(businessName.ifBlank { "MarketPOS" }, 40),
            PosReportPdfHelpers.MARGIN_X + 12f,
            y,
            PosReportPdfHelpers.titlePaint()
        )
        y += 18f
        canvas.drawText(
            "${report.title} · ${report.scopeLabel}",
            PosReportPdfHelpers.MARGIN_X + 12f,
            y,
            PosReportPdfHelpers.bodyPaint()
        )
        y += 14f
        canvas.drawText(
            "Generated: ${PosReportPdfHelpers.formatTimestamp(report.generatedAtMillis)}",
            PosReportPdfHelpers.MARGIN_X + 12f,
            y,
            PosReportPdfHelpers.mutedPaint()
        )
        y += 28f

        // Summary KPIs
        canvas.drawText("Financial Summary", PosReportPdfHelpers.MARGIN_X, y, PosReportPdfHelpers.sectionPaint())
        y += 10f
        PosReportPdfHelpers.drawDivider(canvas, y)
        y += 16f

        val summaryRows = listOf(
            "Total Revenue" to CurrencyFormatter.format(report.totalRevenueRsd, currency, eurRate),
            "Included VAT / Tax" to CurrencyFormatter.format(report.totalTaxRsd, currency, eurRate),
            "Cash Payments" to CurrencyFormatter.format(report.cashRevenueRsd, currency, eurRate),
            "Card Payments" to CurrencyFormatter.format(report.cardRevenueRsd, currency, eurRate),
            "Completed Orders" to report.paidOrderCount.toString(),
            "Voided Orders" to report.cancelledOrderCount.toString(),
            "Items Sold" to report.totalItemsSold.toString(),
            "Average Ticket" to CurrencyFormatter.format(report.averageTicketRsd, currency, eurRate)
        )
        summaryRows.forEach { (k, v) ->
            ensureSpace(18f)
            PosReportPdfHelpers.drawKeyValueRow(canvas, y, k, v)
            y += 16f
        }

        // Branch breakdown (when multi-branch)
        if (report.branchBreakdown.size > 1) {
            y += 12f
            ensureSpace(36f)
            canvas.drawText("Branch Breakdown", PosReportPdfHelpers.MARGIN_X, y, PosReportPdfHelpers.sectionPaint())
            y += 10f
            PosReportPdfHelpers.drawDivider(canvas, y)
            y += 16f
            report.branchBreakdown.forEach { b ->
                ensureSpace(18f)
                PosReportPdfHelpers.drawKeyValueRow(
                    canvas,
                    y,
                    "${PosReportPdfHelpers.truncate(b.locationName, 32)} (${b.orderCount} orders)",
                    CurrencyFormatter.format(b.revenueRsd, currency, eurRate)
                )
                y += 16f
            }
        }

        // Category breakdown
        if (report.categoryBreakdown.isNotEmpty()) {
            y += 12f
            ensureSpace(36f)
            canvas.drawText("Category Performance", PosReportPdfHelpers.MARGIN_X, y, PosReportPdfHelpers.sectionPaint())
            y += 10f
            PosReportPdfHelpers.drawDivider(canvas, y)
            y += 16f
            report.categoryBreakdown.forEach { cat ->
                ensureSpace(18f)
                val pct = (cat.share * 100).toInt()
                PosReportPdfHelpers.drawKeyValueRow(
                    canvas,
                    y,
                    "${PosReportPdfHelpers.truncate(cat.category, 28)} (${cat.quantitySold} pcs · $pct%)",
                    CurrencyFormatter.format(cat.revenueRsd, currency, eurRate)
                )
                y += 16f
            }
        }

        // Top products
        if (report.topProducts.isNotEmpty()) {
            y += 12f
            ensureSpace(36f)
            canvas.drawText("Top Selling Products", PosReportPdfHelpers.MARGIN_X, y, PosReportPdfHelpers.sectionPaint())
            y += 10f
            PosReportPdfHelpers.drawDivider(canvas, y)
            y += 16f
            report.topProducts.forEach { prod ->
                ensureSpace(18f)
                PosReportPdfHelpers.drawKeyValueRow(
                    canvas,
                    y,
                    "${PosReportPdfHelpers.truncate(prod.productName, 32)} x${prod.quantitySold}",
                    CurrencyFormatter.format(prod.revenueRsd, currency, eurRate)
                )
                y += 16f
            }
        }

        // Recent orders list
        if (report.orders.isNotEmpty()) {
            y += 12f
            ensureSpace(36f)
            canvas.drawText("Receipts Ledger (${report.orders.size})", PosReportPdfHelpers.MARGIN_X, y, PosReportPdfHelpers.sectionPaint())
            y += 10f
            PosReportPdfHelpers.drawDivider(canvas, y)
            y += 16f
            report.orders.take(40).forEach { order ->
                ensureSpace(16f)
                val ts = PosReportPdfHelpers.formatTimestamp(order.closedAtMillis ?: order.createdAtMillis)
                val method = order.paymentMethod?.name ?: "CASH"
                PosReportPdfHelpers.drawKeyValueRow(
                    canvas,
                    y,
                    "${order.receiptNumber} · $ts · ${PosReportPdfHelpers.truncate(order.workerName, 14)} ($method)",
                    CurrencyFormatter.format(order.totalRsd, currency, eurRate),
                    labelPaint = PosReportPdfHelpers.mutedPaint(),
                    valuePaint = PosReportPdfHelpers.monoPaint(bold = false)
                )
                y += 15f
            }
        }

        document.finishPage(page)
        FileOutputStream(outFile).use { stream ->
            document.writeTo(stream)
        }
        document.close()
        return outFile
    }
}
