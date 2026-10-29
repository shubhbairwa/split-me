package com.shubh.splitme.data

import android.content.Context
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import java.util.Locale

/** A country for the phone number picker. [iso] is the two-letter region code, e.g. "IN". */
data class Country(val iso: String, val dialCode: Int) {
    val flag: String get() = flagEmoji(iso)

    /** Country name in the given language, e.g. "India", "भारत", "India". */
    fun displayName(locale: Locale): String = Locale("", iso).getDisplayCountry(locale)
}

/**
 * Phone numbers across the app are stored in E.164 form ("+919876543210"): it's what Firebase
 * phone sign-in uses, and it lets the same person be recognised however their number was typed.
 */
object PhoneNumbers {
    private val util: PhoneNumberUtil by lazy { PhoneNumberUtil.getInstance() }

    /** Every country libphonenumber knows, sorted by name in [locale]. */
    fun countries(locale: Locale): List<Country> =
        util.supportedRegions
            .map { Country(it, util.getCountryCodeForRegion(it)) }
            .sortedBy { it.displayName(locale).lowercase(locale) }

    fun country(iso: String): Country? =
        iso.uppercase(Locale.ROOT).takeIf { it in util.supportedRegions }
            ?.let { Country(it, util.getCountryCodeForRegion(it)) }

    /** The SIM's country, then the network's, then the phone's language region; India if none is known. */
    fun defaultCountry(context: Context): Country {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val candidates = listOf(
            tm?.simCountryIso,
            tm?.networkCountryIso,
            context.resources.configuration.locales[0].country
        )
        return candidates.firstNotNullOfOrNull { it?.takeIf { c -> c.length == 2 }?.let(::country) }
            ?: Country("IN", 91)
    }

    /**
     * The number in E.164 ("+919876543210") if it's a valid number for [country], else null.
     * Accepts spaces, dashes, a leading 0 and numbers typed with their own "+code".
     */
    fun toE164(number: String, country: Country): String? = toE164(number, country.iso)

    fun toE164(number: String?, defaultIso: String): String? {
        if (number.isNullOrBlank()) return null
        return try {
            val parsed = util.parse(number, defaultIso.uppercase(Locale.ROOT))
            if (util.isValidNumber(parsed)) util.format(parsed, PhoneNumberFormat.E164) else null
        } catch (e: NumberParseException) {
            null
        }
    }

    /** "+91 98765 43210" for showing a stored number; returns the input unchanged if it can't be parsed. */
    fun formatInternational(number: String?): String {
        if (number.isNullOrBlank()) return ""
        return try {
            util.format(util.parse(number, null), PhoneNumberFormat.INTERNATIONAL)
        } catch (e: NumberParseException) {
            number
        }
    }

    /**
     * An example mobile number for [country] as people type it after the country code, for the
     * input's placeholder: "81234 56789" for India (no leading 0, since the code is shown beside it).
     */
    fun exampleNumber(country: Country): String =
        util.getExampleNumberForType(country.iso, PhoneNumberUtil.PhoneNumberType.MOBILE)
            ?.let { util.format(it, PhoneNumberFormat.INTERNATIONAL).removePrefix("+${country.dialCode}").trim() }
            .orEmpty()
}

/** Flag emoji from a region code: "IN" -> 🇮🇳 (pairs of regional indicator symbols). */
fun flagEmoji(iso: String): String {
    val code = iso.uppercase(Locale.ROOT)
    if (code.length != 2 || !code.all { it in 'A'..'Z' }) return ""
    val first = Character.codePointAt(code, 0) - 'A'.code + 0x1F1E6
    val second = Character.codePointAt(code, 1) - 'A'.code + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}
