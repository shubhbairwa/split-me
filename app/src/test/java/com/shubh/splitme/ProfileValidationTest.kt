package com.shubh.splitme

import com.shubh.splitme.ui.profile.ageInYears
import com.shubh.splitme.ui.profile.isoDateToMillis
import com.shubh.splitme.ui.profile.millisToIsoDate
import com.shubh.splitme.ui.profile.validateDateOfBirth
import com.shubh.splitme.ui.profile.validateLocation
import com.shubh.splitme.ui.profile.validateName
import com.shubh.splitme.ui.profile.validateProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileValidationTest {
    private val today = isoDateToMillis("2026-10-29")!!

    @Test
    fun name() {
        assertEquals(R.string.validation_name_required, validateName("  "))
        assertNotNull(validateName("A"))
        assertNotNull(validateName("Rafi123"))
        assertNull(validateName("MD Rafi Islam"))
        assertNull(validateName("Anne-Marie O'Neil"))
        assertNull(validateName("शुभम बैरवा"))
    }

    @Test
    fun dateOfBirth() {
        assertEquals(R.string.validation_dob_required, validateDateOfBirth(null, today))
        assertNotNull(validateDateOfBirth(isoDateToMillis("2027-01-01"), today))
        assertEquals(R.string.validation_dob_min_age, validateDateOfBirth(isoDateToMillis("2014-01-01"), today))
        assertNull(validateDateOfBirth(isoDateToMillis("2001-02-18"), today))
    }

    @Test
    fun ageCountsBirthdayNotYetReached() {
        assertEquals(12, ageInYears(isoDateToMillis("2013-10-30")!!, today))
        assertEquals(13, ageInYears(isoDateToMillis("2013-10-29")!!, today))
    }

    @Test
    fun isoDateRoundTrip() {
        assertEquals("2001-02-18", millisToIsoDate(isoDateToMillis("2001-02-18")!!))
    }

    @Test
    fun locationOptionalButLimited() {
        assertNull(validateLocation(""))
        assertNotNull(validateLocation("x".repeat(101)))
    }

    @Test
    fun wholeProfile() {
        assertTrue(validateProfile("MD Rafi Islam", isoDateToMillis("2001-02-18"), "Dhaka", today).isValid)
        assertFalse(validateProfile("", isoDateToMillis("2001-02-18"), "", today).isValid)
    }
}
