package com.fourpit.marketpos.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object PosReportPdfHelpers {
    const val PAGE_WIDTH = 595
    const val PAGE_HEIGHT = 842
    const val MARGIN_X = 36f
    const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN_X

    fun titlePaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(0, 108, 76)
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    fun sectionPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(23, 29, 26)
        textSize = 13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    fun bodyPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(40, 48, 44)
        textSize = 10.5f
    }

    fun mutedPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(102, 114, 108)
        textSize = 9.5f
    }

    fun monoPaint(bold: Boolean = false): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(23, 29, 26)
        textSize = 10f
        typeface = Typeface.create(Typeface.MONOSPACE, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    fun dividerPaint(): Paint = Paint().apply {
        color = Color.rgb(210, 222, 216)
        strokeWidth = 1f
    }

    fun headerFillPaint(): Paint = Paint().apply {
        color = Color.rgb(233, 244, 238)
        style = Paint.Style.FILL
    }

    fun drawDivider(canvas: Canvas, y: Float) {
        canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, dividerPaint())
    }

    fun drawKeyValueRow(
        canvas: Canvas,
        y: Float,
        label: String,
        value: String,
        labelPaint: Paint = bodyPaint(),
        valuePaint: Paint = monoPaint(bold = true)
    ) {
        canvas.drawText(label, MARGIN_X, y, labelPaint)
        val valueWidth = valuePaint.measureText(value)
        canvas.drawText(value, PAGE_WIDTH - MARGIN_X - valueWidth, y, valuePaint)
    }

    fun formatTimestamp(millis: Long): String {
        val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US)
        return fmt.format(Date(millis))
    }

    fun truncate(text: String, maxChars: Int): String {
        return if (text.length <= maxChars) text else text.take(maxChars - 1) + "…"
    }
}
