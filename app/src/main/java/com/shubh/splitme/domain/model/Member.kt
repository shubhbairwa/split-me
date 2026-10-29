package com.shubh.splitme.domain.model

data class Member(
    val id: String = "",
    val name: String = "",
    /** Older data only: the app no longer uses email accounts. */
    val email: String? = null,
    /** Mobile number, E.164 ("+919876543210") for accounts and newly added contacts. */
    val phoneNumber: String? = null,
    /** Date of birth as an ISO date, "yyyy-MM-dd". Only set for people with an account. */
    val dateOfBirth: String? = null,
    val location: String? = null,
    /** Profile picture download URL (Firebase Storage). */
    val photoUrl: String? = null,
    /** Storage path of the picture, so the old file can be deleted when it's replaced. */
    val photoPath: String? = null,
    /** True for people who signed up (document id = their auth uid); false for contacts others added. */
    val hasAccount: Boolean = false
)
