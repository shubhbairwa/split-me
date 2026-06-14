package com.shubh.splitme.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(
            entity = Group::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Member::class,
            parentColumns = ["id"],
            childColumns = ["payerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long? = null,
    val title: String,
    val totalAmount: Double,
    val date: Long = System.currentTimeMillis(),
    val category: String,
    val payerId: Long
)
