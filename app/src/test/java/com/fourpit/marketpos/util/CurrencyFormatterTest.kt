package com.fourpit.marketpos.util

import com.fourpit.marketpos.model.CurrencyCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun formatRsd_includesSuffix() {
        val formatted = CurrencyFormatter.format(1250.5, CurrencyCode.RSD, 117.2)
        assertTrue(formatted.endsWith("RSD"))
        assertTrue(formatted.contains("1.250,50"))
    }

    @Test
    fun convertFromRsd_toEurUsesExchangeRate() {
        val eur = CurrencyFormatter.convertFromRsd(1172.0, CurrencyCode.EUR, 117.2)
        assertEquals(10.0, eur, 0.001)
    }

    @Test
    fun formatEur_usesEuroSymbol() {
        val formatted = CurrencyFormatter.format(1172.0, CurrencyCode.EUR, 117.2)
        assertEquals("€10.00", formatted)
    }
}
