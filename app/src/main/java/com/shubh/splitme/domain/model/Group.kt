package com.shubh.splitme.domain.model

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val memberIds: List<String> = emptyList()
)
