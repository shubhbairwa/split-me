package com.shubh.splitme.ui.profile

import androidx.annotation.StringRes
import com.shubh.splitme.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Validation message per profile field, as a string resource id; null means the field is fine.
 * Show with stringResource(id, MIN_AGE_YEARS) (the age message takes the minimum age).
 */
data class ProfileErrors(
    @StringRes val name: Int? = null,
    @StringRes val dateOfBirth: Int? = null,
    @StringRes val location: Int? = null
) {
    val isValid: Boolean get() = name == null && dateOfBirth == null && location == null
}

const val MIN_AGE_YEARS = 13
private const val MAX_AGE_YEARS = 120
private val NAME_PATTERN = Regex("^[\\p{L}\\p{M} .'-]+$") // \p{M} allows vowel signs in Hindi and other Indic scripts

@StringRes
fun validateName(name: String): Int? {
    val n = name.trim()
    return when {
        n.isEmpty() -> R.string.validation_name_required
        n.length < 2 -> R.string.validation_name_short
        n.length > 50 -> R.string.validation_name_long
        !NAME_PATTERN.matches(n) -> R.string.validation_name_chars
        else -> null
    }
}

/** [dobMillis] is a UTC midnight timestamp, as returned by the Material date picker. */
@StringRes
fun validateDateOfBirth(dobMillis: Long?, todayMillis: Long = System.currentTimeMillis()): Int? {
    if (dobMillis == null) return R.string.validation_dob_required
    if (dobMillis > todayMillis) return R.string.validation_dob_future
    val age = ageInYears(dobMillis, todayMillis)
    return when {
        age < MIN_AGE_YEARS -> R.string.validation_dob_min_age
        age > MAX_AGE_YEARS -> R.string.validation_dob_invalid
        else -> null
    }
}

@StringRes
fun validateLocation(location: String): Int? =
    if (location.trim().length > 100) R.string.validation_location_long else null

/** The mobile number isn't edited here: it's the verified number the user signed in with. */
fun validateProfile(name: String, dobMillis: Long?, location: String, todayMillis: Long = System.currentTimeMillis()) =
    ProfileErrors(
        name = validateName(name),
        dateOfBirth = validateDateOfBirth(dobMillis, todayMillis),
        location = validateLocation(location)
    )

fun ageInYears(dobMillis: Long, todayMillis: Long): Int {
    val dob = utcCalendar(dobMillis)
    val today = utcCalendar(todayMillis)
    var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
    val birthdayNotYetThisYear = today.get(Calendar.MONTH) < dob.get(Calendar.MONTH) ||
        (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH))
    if (birthdayNotYetThisYear) age--
    return age
}

// Dates are stored in Firestore as "yyyy-MM-dd" so they read the same in every time zone.
private fun isoFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

fun millisToIsoDate(millis: Long): String = isoFormat().format(millis)

fun isoDateToMillis(iso: String?): Long? =
    iso?.takeIf { it.isNotBlank() }?.let { runCatching { isoFormat().parse(it)?.time }.getOrNull() }

/** "18 February, 2001", as in the design. */
fun formatDateForDisplay(millis: Long): String =
    SimpleDateFormat("d MMMM, yyyy", Locale.getDefault()).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(millis)

private fun utcCalendar(millis: Long) = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
