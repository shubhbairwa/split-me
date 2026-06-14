package com.shubh.splitme.data.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class GroupWithMembers(
    @Embedded val group: Group,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = GroupMemberCrossRef::class,
            parentColumn = "groupId",
            entityColumn = "memberId"
        )
    )
    val members: List<Member>
)
