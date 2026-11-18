package com.example.marketpos.printing

import android.content.Context
import com.example.marketpos.model.AppSettings
import com.example.marketpos.model.PrintResult
import com.example.marketpos.model.SaleTransaction

interface ReceiptPrinter {
    suspend fun printReceipt(
        context: Context,
        transaction: SaleTransaction,
        settings: AppSettings
    ): PrintResult
}
