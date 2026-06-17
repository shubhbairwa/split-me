package com.shubh.splitme.domain.repository

import com.shubh.splitme.domain.model.Group
import com.shubh.splitme.domain.model.GroupWithMembers
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun getGroupsWithMembers(): Flow<List<GroupWithMembers>>
    fun getGroupWithMembersById(groupId: String): Flow<GroupWithMembers?>
    suspend fun createGroup(group: Group): String
    suspend fun addMemberToGroup(groupId: String, memberId: String)
    suspend fun deleteGroup(groupId: String)
    suspend fun getGroupsWithMembersOnce(): List<GroupWithMembers>
}
