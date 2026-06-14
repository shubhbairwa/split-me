package com.shubh.splitme.data.dao

import androidx.room.*
import com.shubh.splitme.data.entity.Group
import com.shubh.splitme.data.entity.GroupMemberCrossRef
import com.shubh.splitme.data.entity.GroupWithMembers
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMemberCrossRef(crossRef: GroupMemberCrossRef)

    @Transaction
    @Query("SELECT * FROM groups")
    fun getGroupsWithMembers(): Flow<List<GroupWithMembers>>

    @Transaction
    @Query("SELECT * FROM groups")
    suspend fun getGroupsWithMembersOnce(): List<GroupWithMembers>

    @Transaction
    @Query("SELECT * FROM groups WHERE id = :groupId")
    fun getGroupWithMembersById(groupId: Long): Flow<GroupWithMembers?>
    
    @Query("SELECT * FROM groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: Long): Group?

    @Delete
    suspend fun deleteGroup(group: Group)
}
