package com.example.marketpos.util

import com.example.marketpos.model.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun formatRsd_usesSerbianSeparatorsAndSuffix() {
        val formatted = CurrencyFormatter.format(
            amountRsd = 1250.0,
            language = AppLanguage.SERBIAN_LATIN,
            rsdPerEur = 117.0
        )
        assertEquals("1.250,00 RSD", formatted)
    }

    @Test
    fun formatEur_convertsUsingExchangeRate() {
        val formatted = CurrencyFormatter.format(
            amountRsd = 1170.0,
            language = AppLanguage.ENGLISH,
            rsdPerEur = 117.0
        )
        assertEquals("€10.00", formatted)
    }

    @Test
    fun convertToRsd_roundTripsCleanly() {
        val rsd = CurrencyFormatter.convertToRsd(
            inputAmount = 10.0,
            language = AppLanguage.ENGLISH,
            rsdPerEur = 117.5
        )
        assertEquals(1175.0, rsd, 0.001)
    }

    @Test
    fun datePickerUtcAndLocalDayStart_roundTripsCleanly() {
        val nowLocalDay = CurrencyFormatter.localDayStartMillis(System.currentTimeMillis())
        val utcPickerMillis = CurrencyFormatter.localDayStartToUtcDatePickerMillis(nowLocalDay)
        val backToLocalDay = CurrencyFormatter.utcDatePickerMillisToLocalDayStart(utcPickerMillis)
        assertEquals(nowLocalDay, backToLocalDay)
    }

    @Test
    fun formatCountdown_formatsMinutesAndSecondsCorrectly() {
        assertEquals("15:00", CurrencyFormatter.formatCountdown(15 * 60 * 1000L))
        assertEquals("01:05", CurrencyFormatter.formatCountdown(65_000L))
        assertEquals("00:00", CurrencyFormatter.formatCountdown(-500L))
    }
}
