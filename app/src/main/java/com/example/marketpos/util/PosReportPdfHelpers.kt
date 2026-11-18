package com.example.marketpos.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.marketpos.i18n.AppStrings
import com.example.marketpos.model.AppLanguage
import com.example.marketpos.model.LocationReportSummary
import com.example.marketpos.model.ReportPeriodType
import com.example.marketpos.model.WorkerReportRow
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal interface PdfRenderBackend {
    fun startPage(width: Int, height: Int, pageNumber: Int): Canvas
    fun finishPage(pageNumber: Int)
    fun finishDocument(): ByteArray
}

internal class AndroidPdfRenderBackend : PdfRenderBackend {
    private val doc = PdfDocument()
    private var activePage: PdfDocument.Page? = null

    override fun startPage(width: Int, height: Int, pageNumber: Int): Canvas {
        val page = doc.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())
        activePage = page
        return page.canvas
    }

    override fun finishPage(pageNumber: Int) {
        activePage?.let { doc.finishPage(it) }
        activePage = null
    }

    override fun finishDocument(): ByteArray {
        val baos = ByteArrayOutputStream()
        doc.writeTo(baos)
        doc.close()
        return baos.toByteArray()
    }
}

internal object PosReportPdfHelpers {

    fun formatProductsColumn(
        row: WorkerReportRow,
        language: AppLanguage,
        strings: AppStrings
    ): String {
        if (row.productsSold.isEmpty()) return "—"
        val header = "${row.totalUnitsSold} ${strings.reportUnitsShort}:"
        val itemsList = row.productsSold.joinToString(", ") { stat ->
            "${stat.quantity}× ${stat.displayName(language)}"
        }
        return "$header $itemsList"
    }

    fun formatPeriodRangeLabel(
        summary: LocationReportSummary,
        language: AppLanguage
    ): String {
        return when (summary.periodType) {
            ReportPeriodType.DAILY -> CurrencyFormatter.formatFullDate(summary.startMillis, language)
            ReportPeriodType.MONTHLY -> {
                val pattern = if (language == AppLanguage.SERBIAN_LATIN) "LLLL yyyy" else "MMMM yyyy"
                val locale = if (language == AppLanguage.SERBIAN_LATIN) {
                    Locale.forLanguageTag("sr-Latn")
                } else {
                    Locale.ENGLISH
                }
                SimpleDateFormat(pattern, locale).format(Date(summary.startMillis))
                    .replaceFirstChar { it.uppercase(locale) }
            }
            ReportPeriodType.DATE_RANGE -> {
                val pattern = if (language == AppLanguage.SERBIAN_LATIN) "dd.MM.yyyy." else "MMM dd, yyyy"
                val locale = if (language == AppLanguage.SERBIAN_LATIN) {
                    Locale.forLanguageTag("sr-Latn")
                } else {
                    Locale.ENGLISH
                }
                val fmt = SimpleDateFormat(pattern, locale)
                "${fmt.format(Date(summary.startMillis))} – ${fmt.format(Date(summary.endMillis))}"
            }
        }
    }

    fun createStaticLayout(text: String, paint: TextPaint, widthPx: Int): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, widthPx.coerceAtLeast(10))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.5f, 1.0f)
            .setIncludePad(false)
            .build()
    }

    fun drawPageFooter(
        canvas: Canvas,
        pageNum: Int,
        strings: AppStrings,
        marginH: Float,
        pageHeight: Int
    ) {
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(110, 120, 114)
            textSize = 8f
        }
        canvas.drawText(
            "${strings.appName} • ${strings.reportPageLabel(pageNum)}",
            marginH,
            pageHeight - 16f,
            footerPaint
        )
    }
}
