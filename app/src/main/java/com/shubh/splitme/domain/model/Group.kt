package com.shubh.splitme.domain.model

data class Group(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val memberIds: List<String> = emptyList(),
    /** Uid of the user who created the group; empty for groups made before this was recorded. */
    val createdBy: String = "",
    /**
     * E.164 numbers of people added from contacts who don't have an account yet. When one signs
     * up, this is how they find (and are allowed to join) the group. See firestore.rules.
     */
    val invitedPhones: List<String> = emptyList()
)
