package com.shubh.splitme.data.dao

import androidx.room.*
import com.shubh.splitme.data.entity.Bill
import com.shubh.splitme.data.entity.BillWithShares
import com.shubh.splitme.data.entity.ExpenseShare
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseShares(shares: List<ExpenseShare>)

    @Transaction
    @Query("SELECT * FROM bills WHERE groupId = :groupId")
    fun getBillsByGroup(groupId: Long): Flow<List<BillWithShares>>

    @Transaction
    @Query("SELECT * FROM bills WHERE id = :billId")
    fun getBillWithSharesById(billId: Long): Flow<BillWithShares?>

    @Transaction
    @Query("SELECT * FROM bills")
    fun getAllBillsWithShares(): Flow<List<BillWithShares>>

    @Query("DELETE FROM bills WHERE id = :billId")
    suspend fun deleteBillById(billId: Long)
}
