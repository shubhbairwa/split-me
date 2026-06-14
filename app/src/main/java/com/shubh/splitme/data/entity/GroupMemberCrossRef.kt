package com.shubh.splitme.data.entity

import androidx.room.Entity

@Entity(tableName = "group_member_cross_ref", primaryKeys = ["groupId", "memberId"])
data class GroupMemberCrossRef(
    val groupId: Long,
    val memberId: Long
)
