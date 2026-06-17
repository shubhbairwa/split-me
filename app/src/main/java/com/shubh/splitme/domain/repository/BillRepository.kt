package com.shubh.splitme.domain.repository

import com.shubh.splitme.domain.model.Bill
import com.shubh.splitme.domain.model.BillWithShares
import com.shubh.splitme.domain.model.ExpenseShare
import kotlinx.coroutines.flow.Flow

interface BillRepository {
    fun getBillsByGroup(groupId: String): Flow<List<BillWithShares>>
    fun getAllBillsWithShares(): Flow<List<BillWithShares>>
    suspend fun createBillWithShares(bill: Bill, shares: List<ExpenseShare>)
    fun getBillWithSharesById(billId: String): Flow<BillWithShares?>
    suspend fun deleteBill(billId: String)
}
