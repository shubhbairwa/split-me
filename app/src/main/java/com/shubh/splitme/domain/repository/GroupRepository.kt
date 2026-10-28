package com.shubh.splitme.domain.repository

import com.shubh.splitme.domain.model.Group
import com.shubh.splitme.domain.model.GroupWithMembers
import com.shubh.splitme.domain.model.Member
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun getGroupsWithMembers(): Flow<List<GroupWithMembers>>
    fun getGroupWithMembersById(groupId: String): Flow<GroupWithMembers?>
    suspend fun createGroup(group: Group): String
    suspend fun addMemberToGroup(groupId: String, memberId: String)

    /**
     * Adds a person (from the phone's contacts or typed in by hand) to a group. Reuses an
     * existing member with the same phone or email, otherwise creates one. Returns that member.
     */
    suspend fun addContactToGroup(groupId: String, name: String, email: String?, phone: String?): Member
    suspend fun removeMemberFromGroup(groupId: String, memberId: String)
    suspend fun deleteGroup(groupId: String)
    suspend fun getGroupsWithMembersOnce(): List<GroupWithMembers>
}
