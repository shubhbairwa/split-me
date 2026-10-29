package com.shubh.splitme.domain.repository

import android.app.Activity
import com.shubh.splitme.domain.model.Member
import kotlinx.coroutines.flow.Flow

/** Why a phone sign-in step failed, so the screen can show a helpful, translated message. */
enum class PhoneAuthFailure {
    InvalidNumber,
    InvalidCode,
    CodeExpired,
    TooManyRequests,
    QuotaExceeded,
    /** The app isn't set up for phone sign-in in Firebase (Phone provider off or SHA fingerprints missing). */
    AppNotAuthorized,
    Network,
    Other
}

class PhoneAuthException(val reason: PhoneAuthFailure, cause: Throwable?) : Exception(cause?.message, cause)

/** What happens after asking for a verification SMS. */
sealed interface PhoneAuthEvent {
    /** The SMS was sent. [resendToken] is opaque; pass it back to resend the code. */
    data class CodeSent(val verificationId: String, val resendToken: Any) : PhoneAuthEvent

    /** Android verified the number by itself (auto-read SMS or instant verification); already signed in. */
    data class SignedIn(val result: SignInResult) : PhoneAuthEvent

    data class Failed(val error: PhoneAuthException) : PhoneAuthEvent
}

/** [needsProfile]: first sign-in for this number, so the user still has to enter their name. */
data class SignInResult(val uid: String, val phoneNumber: String, val needsProfile: Boolean)

interface AuthRepository {
    /** The signed-in user, or null. Only phone-number accounts count as signed in. */
    val currentUser: Flow<Member?>

    /** True when signed in with a phone number. Older email sessions are signed out by this call. */
    fun isUserLoggedIn(): Boolean

    /**
     * Sends a verification SMS to [phoneE164] ("+919876543210"). [activity] is needed by Firebase
     * for its app check (reCAPTCHA fallback). Pass [resendToken] from an earlier CodeSent to resend.
     * Emits CodeSent (or Failed). After CodeSent it may still emit SignedIn if Android reads the SMS
     * by itself; the flow completes after about a minute. Typing the code goes through [verifyCode].
     */
    fun sendVerificationCode(activity: Activity, phoneE164: String, resendToken: Any? = null): Flow<PhoneAuthEvent>

    /** Signs in with the 6-digit SMS code. Fails with a [PhoneAuthException]. */
    suspend fun verifyCode(verificationId: String, code: String): Result<SignInResult>

    /** Whether the signed-in user has finished sign-up (has a name saved). */
    suspend fun hasProfile(): Boolean

    /** Finishes sign-up: saves the user's name, linked to their verified phone number. */
    suspend fun completeProfile(name: String): Result<Unit>

    suspend fun logout()
}
