package com.shubh.splitme.data.repository

import com.shubh.splitme.data.dao.GroupDao
import com.shubh.splitme.data.entity.Group
import com.shubh.splitme.data.entity.GroupMemberCrossRef
import com.shubh.splitme.data.entity.GroupWithMembers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class GroupRepository(private val groupDao: GroupDao) {
    val groupsWithMembers: Flow<List<GroupWithMembers>> = groupDao.getGroupsWithMembers()

    suspend fun insertGroup(group: Group): Long = withContext(Dispatchers.IO) {
        groupDao.insertGroup(group)
    }

    suspend fun addMemberToGroup(groupId: Long, memberId: Long) = withContext(Dispatchers.IO) {
        groupDao.insertGroupMemberCrossRef(GroupMemberCrossRef(groupId, memberId))
    }

    fun getGroupWithMembersById(groupId: Long): Flow<GroupWithMembers?> = groupDao.getGroupWithMembersById(groupId)

    suspend fun removeMemberFromGroup(groupId: Long, memberId: Long) = withContext(Dispatchers.IO) {
        groupDao.deleteGroupMemberCrossRef(GroupMemberCrossRef(groupId, memberId))
    }

    suspend fun deleteGroup(group: Group) = withContext(Dispatchers.IO) {
        groupDao.deleteGroup(group)
    }

    suspend fun getGroupsWithMembersOnce(): List<GroupWithMembers> = withContext(Dispatchers.IO) {
        groupDao.getGroupsWithMembersOnce()
    }
}
