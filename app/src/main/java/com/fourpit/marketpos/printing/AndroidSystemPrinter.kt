package com.fourpit.marketpos.printing

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.fourpit.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class AndroidSystemPrinter : ReceiptPrinter {

    override suspend fun print(context: Context, payload: ReceiptPrintPayload): PrintResult {
        return withContext(Dispatchers.Main) {
            runCatching {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    ?: return@withContext PrintResult.Failure("Print service unavailable")
                val jobName = "${context.getString(R.string.receipt_print_job_name)} ${payload.order.receiptNumber}"
                val adapter = ReceiptPrintDocumentAdapter(payload)
                printManager.print(jobName, adapter, PrintAttributes.Builder().build())
                PrintResult.Success("Print job created")
            }.getOrElse { error ->
                PrintResult.Failure(error.message ?: "System print failed")
            }
        }
    }

    private class ReceiptPrintDocumentAdapter(
        private val payload: ReceiptPrintPayload
    ) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder("receipt_${payload.order.receiptNumber}.pdf")
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
            val lines = payload.buildPlainTextLines()
            val pageWidth = 240
            val pageHeight = (lines.size * 16 + 64).coerceAtLeast(320)
            val document = PdfDocument()
            val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 9.5f
                typeface = Typeface.MONOSPACE
            }

            var y = 28f
            lines.forEach { line ->
                page.canvas.drawText(line, 12f, y, paint)
                y += 14f
            }
            document.finishPage(page)

            try {
                FileOutputStream(destination.fileDescriptor).use { out ->
                    document.writeTo(out)
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                document.close()
            }
        }
    }
}
