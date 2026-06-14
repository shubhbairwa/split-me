package com.shubh.splitme.data.repository

import com.shubh.splitme.data.dao.BillDao
import com.shubh.splitme.data.entity.Bill
import com.shubh.splitme.data.entity.BillWithShares
import com.shubh.splitme.data.entity.ExpenseShare
import kotlinx.coroutines.flow.Flow

class BillRepository(private val billDao: BillDao) {
    fun getBillsByGroup(groupId: Long): Flow<List<BillWithShares>> = billDao.getBillsByGroup(groupId)

    fun getAllBillsWithShares(): Flow<List<BillWithShares>> = billDao.getAllBillsWithShares()
    
    suspend fun createBillWithShares(bill: Bill, shares: List<ExpenseShare>) {
        val billId = billDao.insertBill(bill)
        val sharesWithId = shares.map { it.copy(billId = billId) }
        billDao.insertExpenseShares(sharesWithId)
    }

    fun getBillWithSharesById(billId: Long): Flow<BillWithShares?> = billDao.getBillWithSharesById(billId)
    suspend fun deleteBill(billId: Long) = billDao.deleteBillById(billId)
}
