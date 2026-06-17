package com.shubh.splitme.domain.model

data class Bill(
    val id: String = "",
    val groupId: String? = null,
    val title: String = "",
    val totalAmount: Double = 0.0,
    val date: Long = System.currentTimeMillis(),
    val category: String = "",
    val payerId: String = ""
)

data class ExpenseShare(
    val id: String = "",
    val billId: String = "",
    val memberId: String = "",
    val amount: Double = 0.0
)

data class BillWithShares(
    val bill: Bill,
    val shares: List<ExpenseShare>
)
