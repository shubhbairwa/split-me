package com.shubh.splitme.data.repository

import com.shubh.splitme.data.dao.MemberDao
import com.shubh.splitme.data.entity.Member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MemberRepository(private val memberDao: MemberDao) {
    val allMembers: Flow<List<Member>> = memberDao.getAllMembers()

    suspend fun getOrCreateMe(): Member = withContext(Dispatchers.IO) {
        val members = memberDao.getAllMembersOnce()
        val me = members.find { it.isMe }
        if (me != null) return@withContext me
        
        // Fallback for old data where isMe was not set
        val oldMe = members.find { it.name.equals("Me", ignoreCase = true) }
        if (oldMe != null) {
            val updatedMe = oldMe.copy(isMe = true)
            memberDao.update(updatedMe)
            return@withContext updatedMe
        }

        val newMe = Member(name = "Me", isMe = true)
        val id = memberDao.insert(newMe)
        newMe.copy(id = id)
    }

    suspend fun insert(member: Member): Long = withContext(Dispatchers.IO) {
        memberDao.insert(member)
    }

    suspend fun update(member: Member) = withContext(Dispatchers.IO) {
        memberDao.update(member)
    }

    suspend fun delete(member: Member) = withContext(Dispatchers.IO) {
        memberDao.delete(member)
    }

    suspend fun getMemberById(id: Long) = withContext(Dispatchers.IO) {
        memberDao.getMemberById(id)
    }

    suspend fun getAllMembersOnce(): List<Member> = withContext(Dispatchers.IO) {
        memberDao.getAllMembersOnce()
    }
}
