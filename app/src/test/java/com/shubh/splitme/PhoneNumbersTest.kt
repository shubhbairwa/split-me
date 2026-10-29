package com.shubh.splitme

import com.shubh.splitme.data.Country
import com.shubh.splitme.data.PhoneNumbers
import com.shubh.splitme.data.flagEmoji
import com.shubh.splitme.data.normalizePhone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class PhoneNumbersTest {
    private val india = Country("IN", 91)
    private val spain = Country("ES", 34)

    @Test
    fun indianNumbersInAnyFormatBecomeE164() {
        assertEquals("+919876543210", PhoneNumbers.toE164("98765 43210", india))
        assertEquals("+919876543210", PhoneNumbers.toE164("098765-43210", india))
        assertEquals("+919876543210", PhoneNumbers.toE164("+91 98765 43210", india))
    }

    @Test
    fun numberTypedWithItsOwnCountryCodeWins() {
        assertEquals("+34612345678", PhoneNumbers.toE164("+34 612 34 56 78", india))
    }

    @Test
    fun invalidNumbersAreRejected() {
        assertNull(PhoneNumbers.toE164("12345", india))
        assertNull(PhoneNumbers.toE164("", india))
        assertNull(PhoneNumbers.toE164("98765", spain))
    }

    @Test
    fun formatsForDisplay() {
        assertEquals("+91 98765 43210", PhoneNumbers.formatInternational("+919876543210"))
    }

    @Test
    fun countryListHasDialCodes() {
        val countries = PhoneNumbers.countries(Locale.ENGLISH)
        assertTrue(countries.size > 200)
        assertEquals(91, countries.first { it.iso == "IN" }.dialCode)
        assertEquals(1, countries.first { it.iso == "US" }.dialCode)
    }

    @Test
    fun placeholderHasNoCountryCodeOrTrunkZero() {
        assertEquals("81234 56789", PhoneNumbers.exampleNumber(india))
    }

    @Test
    fun flag() {
        assertEquals("🇮🇳", flagEmoji("IN"))
    }

    @Test
    fun e164AndLocalFormatsMatchTheSamePerson() {
        assertEquals(normalizePhone("+919876543210"), normalizePhone("098765 43210"))
    }
}
