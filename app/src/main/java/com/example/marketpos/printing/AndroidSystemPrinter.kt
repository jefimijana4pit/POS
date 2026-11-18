package com.example.marketpos.printing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import com.example.marketpos.i18n.stringsFor
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.PaymentMethod
import com.example.marketpos.model.PrintResult
import com.example.marketpos.model.SaleTransaction
import com.example.marketpos.util.CurrencyFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class AndroidSystemPrinter : ReceiptPrinter {

    override suspend fun printReceipt(
        context: Context,
        transaction: SaleTransaction,
        settings: AppSettings
    ): PrintResult = withContext(Dispatchers.Main) {
        val activity = context.findActivity()
            ?: return@withContext PrintResult.Error("PrintManager requires an active Activity context.")

        val strings = stringsFor(settings.language)
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return@withContext PrintResult.Error("Android PrintManager unavailable on this device.")

        val jobName = "MarketPOS_${transaction.receiptNumber}"
        val adapter = PosReceiptPrintDocumentAdapter(activity, transaction, settings)
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A6)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, adapter, attributes)
        PrintResult.Success(message = strings.printSuccessSystem)
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private class PosReceiptPrintDocumentAdapter(
    private val context: Context,
    private val transaction: SaleTransaction,
    private val settings: AppSettings
) : PrintDocumentAdapter() {

    private var pdfDocument: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {
        pdfDocument = PrintedPdfDocument(context, newAttributes)
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val info = PrintDocumentInfo.Builder("receipt_${transaction.receiptNumber}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(1)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        val doc = pdfDocument ?: run {
            callback.onWriteFailed("PDF document not initialized")
            return
        }

        val page = doc.startPage(0)
        if (cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            doc.close()
            pdfDocument = null
            return
        }

        drawReceiptPage(page.canvas)
        doc.finishPage(page)

        try {
            FileOutputStream(destination.fileDescriptor).use { out ->
                doc.writeTo(out)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (e: Exception) {
            callback.onWriteFailed(e.message ?: "Failed to write PDF receipt")
        } finally {
            doc.close()
            pdfDocument = null
        }
    }

    private fun drawReceiptPage(canvas: android.graphics.Canvas) {
        val strings = stringsFor(settings.language)
        val lang = settings.language
        val rate = settings.rsdPerEur

        val paint = Paint().apply {
            color = Color.BLACK
            typeface = Typeface.MONOSPACE
            textSize = 9.5f
            isAntiAlias = true
        }
        val boldPaint = Paint(paint).apply {
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 11f
        }

        val left = 24f
        val right = canvas.width - 24f
        var y = 32f
        val lineHeight = 14f

        fun drawCentered(text: String, p: Paint) {
            val w = p.measureText(text)
            canvas.drawText(text, ((canvas.width - w) / 2f).coerceAtLeast(left), y, p)
            y += lineHeight
        }

        fun drawKeyValue(label: String, value: String, p: Paint = paint) {
            canvas.drawText(label, left, y, p)
            val valWidth = p.measureText(value)
            canvas.drawText(value, (right - valWidth).coerceAtLeast(left), y, p)
            y += lineHeight
        }

        fun drawDivider() {
            paint.strokeWidth = 0.8f
            canvas.drawLine(left, y - 4f, right, y - 4f, paint)
            y += 8f
        }

        drawCentered(strings.storeNameHeader, boldPaint)
        drawCentered(strings.storeAddressHeader, paint)
        y += 6f
        drawDivider()

        drawKeyValue(strings.receiptNumberLabel, transaction.receiptNumber)
        drawKeyValue(
            strings.dateTimeLabel,
            CurrencyFormatter.formatDateTime(transaction.timestampMillis, lang)
        )
        drawKeyValue(strings.cashierLabel, transaction.workerName)
        drawDivider()

        transaction.items.forEach { item ->
            val name = item.displayName(lang).take(26)
            canvas.drawText(name, left, y, paint)
            y += lineHeight
            val qtyPrice = "${item.quantity} x ${CurrencyFormatter.format(item.unitPriceRsd, lang, rate)}"
            val lineTotal = CurrencyFormatter.format(item.lineTotalRsd, lang, rate)
            drawKeyValue("  $qtyPrice", lineTotal)
        }

        drawDivider()
        drawKeyValue(
            strings.subtotalLabel,
            CurrencyFormatter.format(transaction.subtotalRsd, lang, rate)
        )
        drawKeyValue(
            strings.pdvLabel,
            CurrencyFormatter.format(transaction.taxRsd, lang, rate)
        )
        drawKeyValue(
            strings.totalLabel,
            CurrencyFormatter.format(transaction.totalRsd, lang, rate),
            boldPaint
        )
        y += 4f
        drawKeyValue(
            strings.paymentUsedLabel,
            strings.paymentMethodName(transaction.paymentMethod)
        )
        if (transaction.paymentMethod == PaymentMethod.CASH && transaction.cashReceivedRsd != null) {
            drawKeyValue(
                strings.cashLabel,
                CurrencyFormatter.format(transaction.cashReceivedRsd, lang, rate)
            )
            drawKeyValue(
                strings.changeDueLabel,
                CurrencyFormatter.format(transaction.changeRsd ?: 0.0, lang, rate)
            )
        }

        y += 10f
        drawDivider()
        drawCentered(strings.thankYouFooter, boldPaint)
    }
}
