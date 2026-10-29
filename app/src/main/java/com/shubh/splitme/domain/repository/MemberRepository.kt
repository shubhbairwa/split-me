package com.shubh.splitme.domain.repository

import com.shubh.splitme.domain.model.Member
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    fun getAllMembers(): Flow<List<Member>>
    suspend fun getMemberById(id: String): Member?

    /** Live updates of one member (null if the document doesn't exist). */
    fun observeMember(id: String): Flow<Member?>

    suspend fun saveMember(member: Member): Member

    /**
     * Saves the signed-in user's editable profile fields to members/{id}. Merges, so other fields
     * (phone number, photo, ...) are kept, and creates the document if needed.
     */
    suspend fun updateProfile(id: String, name: String, dateOfBirth: String, location: String?)

    /** Sets or clears (null) the profile picture fields on members/{id}. */
    suspend fun updatePhoto(id: String, photoUrl: String?, photoPath: String?)
}
