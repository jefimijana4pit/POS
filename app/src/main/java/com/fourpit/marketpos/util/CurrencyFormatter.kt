package com.fourpit.marketpos.util

import com.fourpit.marketpos.model.CurrencyCode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    private val srSymbols = DecimalFormatSymbols(Locale.forLanguageTag("sr-Latn-RS")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val enSymbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    fun convertFromRsd(amountRsd: Double, currency: CurrencyCode, eurRate: Double): Double {
        return when (currency) {
            CurrencyCode.RSD -> amountRsd
            CurrencyCode.EUR -> {
                val safeRate = if (eurRate > 0.0) eurRate else 117.2
                amountRsd / safeRate
            }
        }
    }

    fun format(
        amountRsd: Double,
        currency: CurrencyCode = CurrencyCode.RSD,
        eurRate: Double = 117.2
    ): String {
        val converted = convertFromRsd(amountRsd, currency, eurRate)
        return when (currency) {
            CurrencyCode.RSD -> {
                val df = DecimalFormat("#,##0.00", srSymbols)
                "${df.format(converted)} RSD"
            }
            CurrencyCode.EUR -> {
                val df = DecimalFormat("#,##0.00", enSymbols)
                "€${df.format(converted)}"
            }
        }
    }

    fun formatCompact(
        amountRsd: Double,
        currency: CurrencyCode = CurrencyCode.RSD,
        eurRate: Double = 117.2
    ): String {
        val converted = convertFromRsd(amountRsd, currency, eurRate)
        return when (currency) {
            CurrencyCode.RSD -> {
                val df = DecimalFormat("#,##0", srSymbols)
                "${df.format(converted)} RSD"
            }
            CurrencyCode.EUR -> {
                val df = DecimalFormat("#,##0.##", enSymbols)
                "€${df.format(converted)}"
            }
        }
    }
}
