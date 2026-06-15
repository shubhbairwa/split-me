package com.shubh.splitme.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val photoUri: String? = null,
    val isMe: Boolean = false
)
