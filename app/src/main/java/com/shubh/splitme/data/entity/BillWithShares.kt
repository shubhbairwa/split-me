package com.shubh.splitme.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class BillWithShares(
    @Embedded val bill: Bill,
    @Relation(
        parentColumn = "id",
        entityColumn = "billId"
    )
    val shares: List<ExpenseShare>
)
