package com.shubh.splitme.domain.model

data class GroupWithMembers(
    val group: Group,
    val members: List<Member>
)
