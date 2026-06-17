package com.shubh.splitme.domain.repository

import com.shubh.splitme.domain.model.Member
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    fun getAllMembers(): Flow<List<Member>>
    suspend fun getMemberById(id: String): Member?
    suspend fun saveMember(member: Member)
}
