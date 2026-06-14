package com.shubh.splitme.data.dao

import androidx.room.*
import com.shubh.splitme.data.entity.Member
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: Member): Long

    @Update
    suspend fun update(member: Member)

    @Delete
    suspend fun delete(member: Member)

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getMemberById(id: Long): Member?

    @Query("SELECT * FROM members ORDER BY name ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM members")
    suspend fun getAllMembersOnce(): List<Member>
}
