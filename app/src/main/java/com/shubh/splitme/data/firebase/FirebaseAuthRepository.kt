package com.shubh.splitme.data.firebase

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import com.shubh.splitme.data.normalizePhone
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.domain.repository.AuthRepository
import com.shubh.splitme.domain.repository.PhoneAuthEvent
import com.shubh.splitme.domain.repository.PhoneAuthException
import com.shubh.splitme.domain.repository.PhoneAuthFailure
import com.shubh.splitme.domain.repository.SignInResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class FirebaseAuthRepository : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val members = firestore.collection("members")

    override val currentUser: Flow<Member?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            val phone = user?.phoneNumber
            if (user == null || phone.isNullOrBlank()) {
                trySend(null)
            } else {
                trySend(Member(id = user.uid, name = user.displayName.orEmpty(), phoneNumber = phone, hasAccount = true))
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun isUserLoggedIn(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.phoneNumber.isNullOrBlank()) {
            // Signed in from the email-based version of the app: the app is phone-only now.
            auth.signOut()
            return false
        }
        return true
    }

    override fun sendVerificationCode(activity: Activity, phoneE164: String, resendToken: Any?): Flow<PhoneAuthEvent> = callbackFlow {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                trySend(PhoneAuthEvent.CodeSent(verificationId, token))
                // Stay open: Android may still read the SMS by itself and sign in.
            }

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                launch {
                    val event = signIn(credential).fold(
                        onSuccess = { PhoneAuthEvent.SignedIn(it) },
                        onFailure = { PhoneAuthEvent.Failed(it as? PhoneAuthException ?: PhoneAuthException(PhoneAuthFailure.Other, it)) }
                    )
                    trySend(event)
                    close()
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                trySend(PhoneAuthEvent.Failed(mapError(e)))
                close()
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                close() // The user can still type the code; verifyCode() doesn't need this flow.
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneE164)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .apply { (resendToken as? PhoneAuthProvider.ForceResendingToken)?.let { setForceResendingToken(it) } }
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        awaitClose { }
    }

    override suspend fun verifyCode(verificationId: String, code: String): Result<SignInResult> =
        signIn(PhoneAuthProvider.getCredential(verificationId, code))

    private suspend fun signIn(credential: PhoneAuthCredential): Result<SignInResult> = try {
        val user = auth.signInWithCredential(credential).await().user
            ?: throw PhoneAuthException(PhoneAuthFailure.Other, null)
        val phone = user.phoneNumber.orEmpty()
        val needsProfile = members.document(user.uid).get().await().getString("name").isNullOrBlank()
        if (!needsProfile) claimContactEntries(user.uid, phone)
        Result.success(SignInResult(user.uid, phone, needsProfile))
    } catch (e: PhoneAuthException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(mapError(e))
    }

    override suspend fun hasProfile(): Boolean {
        val uid = auth.currentUser?.uid ?: return false
        return !members.document(uid).get().await().getString("name").isNullOrBlank()
    }

    override suspend fun completeProfile(name: String): Result<Unit> = try {
        val user = auth.currentUser ?: error("Not signed in")
        val phone = user.phoneNumber.orEmpty()
        members.document(user.uid).set(
            mapOf("id" to user.uid, "name" to name, "phoneNumber" to phone, "hasAccount" to true),
            SetOptions.merge()
        ).await()
        user.updateProfile(userProfileChangeRequest { displayName = name }).await()
        claimContactEntries(user.uid, phone)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun logout() {
        auth.signOut()
    }

    /**
     * Friends may have added this person from their phone contacts before they had an account,
     * which created a separate "contact" member with the same number and put the number in the
     * group's invitedPhones. Join those groups, move the contact's bills and shares over to the real
     * account, so balances show up for them.
     *
     * Order matters for the security rules: the user can only read a group's bills once they are a
     * member, so the group is joined first. Best effort: a failure here never blocks sign-in.
     */
    private suspend fun claimContactEntries(uid: String, phone: String) {
        val key = normalizePhone(phone) ?: return
        try {
            val duplicateIds = members.get().await().toObjects(Member::class.java).filter {
                it.id.isNotBlank() && it.id != uid && !it.hasAccount &&
                    // Firebase Auth uids are 28 characters; contact members use 20-character Firestore ids.
                    // Never take over another account's document.
                    it.id.length != FIREBASE_UID_LENGTH &&
                    normalizePhone(it.phoneNumber) == key
            }.map { it.id }.toSet()

            val groups = firestore.collection("groups").whereArrayContains("invitedPhones", phone).get().await()
            for (groupDoc in groups.documents) {
                val oldIds = (groupDoc.get("memberIds") as? List<*>).orEmpty().filterIsInstance<String>()
                    .filter { it in duplicateIds }
                val join = mutableMapOf<String, Any>(
                    "memberIds" to FieldValue.arrayUnion(uid),
                    "invitedPhones" to FieldValue.arrayRemove(phone)
                )
                groupDoc.reference.update(join).await()
                if (oldIds.isEmpty()) continue
                groupDoc.reference.update("memberIds", FieldValue.arrayRemove(*oldIds.toTypedArray())).await()

                val writes = mutableListOf<(WriteBatch) -> Unit>()
                val bills = firestore.collection("bills").whereEqualTo("groupId", groupDoc.id).get().await()
                for (bill in bills.documents) {
                    if (bill.getString("payerId") in oldIds) writes += { b -> b.update(bill.reference, "payerId", uid) }
                    val shares = firestore.collection("shares").whereEqualTo("billId", bill.id).get().await()
                    shares.documents.filter { it.getString("memberId") in oldIds }
                        .forEach { share -> writes += { b -> b.update(share.reference, "memberId", uid) } }
                }
                // Firestore batches hold up to 500 writes.
                writes.chunked(450).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { it(batch) }
                    batch.commit().await()
                }
            }
            // The contact entries themselves are kept: groups made before invites were recorded can't
            // be found here (the rules hide them), and deleting the entry would drop the person from
            // those groups. Adding a contact prefers the real account over a leftover entry.
        } catch (_: Exception) {
            // Ignored on purpose; see above.
        }
    }

    private fun mapError(e: Throwable): PhoneAuthException {
        val code = (e as? FirebaseAuthException)?.errorCode
        val reason = when {
            e is FirebaseNetworkException -> PhoneAuthFailure.Network
            e is FirebaseTooManyRequestsException -> PhoneAuthFailure.TooManyRequests
            code == "ERROR_INVALID_PHONE_NUMBER" || code == "ERROR_MISSING_PHONE_NUMBER" -> PhoneAuthFailure.InvalidNumber
            code == "ERROR_INVALID_VERIFICATION_CODE" || code == "ERROR_MISSING_VERIFICATION_CODE" -> PhoneAuthFailure.InvalidCode
            code == "ERROR_SESSION_EXPIRED" || code == "ERROR_CODE_EXPIRED" -> PhoneAuthFailure.CodeExpired
            code == "ERROR_QUOTA_EXCEEDED" -> PhoneAuthFailure.QuotaExceeded
            code in APP_SETUP_ERRORS || e.message.orEmpty().contains("not authorized", ignoreCase = true) -> PhoneAuthFailure.AppNotAuthorized
            e is FirebaseAuthInvalidCredentialsException -> PhoneAuthFailure.InvalidCode
            else -> PhoneAuthFailure.Other
        }
        return PhoneAuthException(reason, e)
    }

    private companion object {
        const val FIREBASE_UID_LENGTH = 28
        val APP_SETUP_ERRORS = setOf(
            "ERROR_APP_NOT_AUTHORIZED",
            "ERROR_OPERATION_NOT_ALLOWED",
            "ERROR_INVALID_APP_CREDENTIAL",
            "ERROR_MISSING_CLIENT_IDENTIFIER",
            "ERROR_MISSING_APP_CREDENTIAL"
        )
    }
}
