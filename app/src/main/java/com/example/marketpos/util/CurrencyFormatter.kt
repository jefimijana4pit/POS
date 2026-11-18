package com.example.marketpos.util

import com.example.marketpos.model.AppLanguage
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

object CurrencyFormatter {
    const val VAT_RATE = 0.20

    private val rsdSymbols = DecimalFormatSymbols(Locale.GERMANY).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private val eurSymbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ','
        decimalSeparator = '.'
    }

    fun convertFromRsd(amountRsd: Double, language: AppLanguage, rsdPerEur: Double): Double {
        return when (language) {
            AppLanguage.SERBIAN_LATIN -> amountRsd
            AppLanguage.ENGLISH -> {
                val safeRate = if (rsdPerEur > 0.0) rsdPerEur else 117.0
                amountRsd / safeRate
            }
        }
    }

    fun convertToRsd(inputAmount: Double, language: AppLanguage, rsdPerEur: Double): Double {
        return when (language) {
            AppLanguage.SERBIAN_LATIN -> inputAmount
            AppLanguage.ENGLISH -> {
                val safeRate = if (rsdPerEur > 0.0) rsdPerEur else 117.0
                inputAmount * safeRate
            }
        }
    }

    fun format(amountRsd: Double, language: AppLanguage, rsdPerEur: Double): String {
        val displayValue = convertFromRsd(amountRsd, language, rsdPerEur)
        return when (language) {
            AppLanguage.SERBIAN_LATIN -> {
                val df = DecimalFormat("#,##0.00", rsdSymbols)
                "${df.format(displayValue)} RSD"
            }
            AppLanguage.ENGLISH -> {
                val df = DecimalFormat("#,##0.00", eurSymbols)
                "€${df.format(displayValue)}"
            }
        }
    }

    fun formatCompact(amountRsd: Double, language: AppLanguage, rsdPerEur: Double): String {
        val displayValue = convertFromRsd(amountRsd, language, rsdPerEur)
        return when (language) {
            AppLanguage.SERBIAN_LATIN -> {
                if (displayValue >= 1000.0) {
                    val k = (displayValue / 1000.0 * 10).roundToLong() / 10.0
                    "${k}k"
                } else {
                    "${displayValue.roundToLong()}"
                }
            }
            AppLanguage.ENGLISH -> {
                if (displayValue >= 1000.0) {
                    val k = (displayValue / 1000.0 * 10).roundToLong() / 10.0
                    "€${k}k"
                } else {
                    "€${displayValue.roundToLong()}"
                }
            }
        }
    }

    fun formatEditableNumber(amountRsd: Double, language: AppLanguage, rsdPerEur: Double): String {
        val value = convertFromRsd(amountRsd, language, rsdPerEur)
        return String.format(Locale.US, "%.2f", value)
    }

    fun formatDateTime(timestampMillis: Long, language: AppLanguage): String {
        val pattern = when (language) {
            AppLanguage.SERBIAN_LATIN -> "dd.MM.yyyy. HH:mm"
            AppLanguage.ENGLISH -> "MMM dd, yyyy HH:mm"
        }
        val locale = when (language) {
            AppLanguage.SERBIAN_LATIN -> Locale.forLanguageTag("sr-Latn")
            AppLanguage.ENGLISH -> Locale.ENGLISH
        }
        return SimpleDateFormat(pattern, locale).format(Date(timestampMillis))
    }

    fun formatDayLabel(timestampMillis: Long, language: AppLanguage): String {
        val pattern = when (language) {
            AppLanguage.SERBIAN_LATIN -> "dd.MM"
            AppLanguage.ENGLISH -> "MM/dd"
        }
        val locale = when (language) {
            AppLanguage.SERBIAN_LATIN -> Locale.forLanguageTag("sr-Latn")
            AppLanguage.ENGLISH -> Locale.ENGLISH
        }
        return SimpleDateFormat(pattern, locale).format(Date(timestampMillis))
    }

    fun formatFullDate(timestampMillis: Long, language: AppLanguage): String {
        val pattern = when (language) {
            AppLanguage.SERBIAN_LATIN -> "EEEE, dd.MM.yyyy."
            AppLanguage.ENGLISH -> "EEE, MMM dd, yyyy"
        }
        val locale = when (language) {
            AppLanguage.SERBIAN_LATIN -> Locale.forLanguageTag("sr-Latn")
            AppLanguage.ENGLISH -> Locale.ENGLISH
        }
        return SimpleDateFormat(pattern, locale).format(Date(timestampMillis))
    }

    fun localDayStartMillis(timestampMillis: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestampMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun utcDatePickerMillisToLocalDayStart(utcMillis: Long): Long {
        val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        utcCal.timeInMillis = utcMillis
        val localCal = java.util.Calendar.getInstance()
        localCal.set(
            utcCal.get(java.util.Calendar.YEAR),
            utcCal.get(java.util.Calendar.MONTH),
            utcCal.get(java.util.Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
        localCal.set(java.util.Calendar.MILLISECOND, 0)
        return localCal.timeInMillis
    }

    fun localDayStartToUtcDatePickerMillis(localMillis: Long): Long {
        val localCal = java.util.Calendar.getInstance()
        localCal.timeInMillis = localMillis
        val utcCal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        utcCal.set(
            localCal.get(java.util.Calendar.YEAR),
            localCal.get(java.util.Calendar.MONTH),
            localCal.get(java.util.Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
        utcCal.set(java.util.Calendar.MILLISECOND, 0)
        return utcCal.timeInMillis
    }

    fun formatCountdown(remainingMillis: Long): String {
        val totalSeconds = (remainingMillis.coerceAtLeast(0L) + 999L) / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
