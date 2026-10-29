package com.shubh.splitme

import com.shubh.splitme.data.matchesContact
import com.shubh.splitme.data.normalizePhone
import com.shubh.splitme.domain.model.Member
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactMatchingTest {

    @Test
    fun normalizePhone_ignoresFormattingAndCountryCode() {
        assertEquals("9876543210", normalizePhone("+91 98765-43210"))
        assertEquals("9876543210", normalizePhone("098765 43210"))
        assertEquals("9876543210", normalizePhone("(987) 654-3210"))
        assertNull(normalizePhone("12"))
        assertNull(normalizePhone(null))
    }

    @Test
    fun matchesByPhoneInDifferentFormats() {
        val member = Member(id = "1", name = "Alex", phoneNumber = "+91 98765 43210")
        assertTrue(member.matchesContact("Alex K", null, "09876543210"))
    }

    @Test
    fun matchesByEmailIgnoringCase() {
        val member = Member(id = "1", name = "Alex", email = "Alex@Example.com")
        assertTrue(member.matchesContact("Alexander", "alex@example.com", "5551234567"))
    }

    @Test
    fun differentPeopleWithSameNameDoNotMatch() {
        val member = Member(id = "1", name = "Sam", phoneNumber = "1111111111")
        assertFalse(member.matchesContact("Sam", null, "2222222222"))
    }

    @Test
    fun nameOnlyFallbackWhenNoPhoneOrEmail() {
        val member = Member(id = "1", name = "Sam")
        assertTrue(member.matchesContact(" sam ", null, null))
        assertFalse(member.matchesContact("Sam", null, "2222222222"))
    }
}
