package com.shubh.splitme.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.data.entity.Bill
import com.shubh.splitme.data.entity.BillWithShares
import com.shubh.splitme.data.entity.ExpenseShare
import com.shubh.splitme.data.entity.Member
import com.shubh.splitme.data.repository.BillRepository
import com.shubh.splitme.data.repository.GroupRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MemberBalance(
    val member: Member,
    val balance: Double // Positive means they are owed, negative means they owe
)

class SettleUpViewModel(
    private val billRepository: BillRepository,
    private val groupRepository: GroupRepository,
    private val groupId: Long
) : ViewModel() {

    val memberBalances: StateFlow<List<MemberBalance>> = combine(
        billRepository.getBillsByGroup(groupId),
        groupRepository.getGroupWithMembersById(groupId)
    ) { bills, groupWithMembers ->
        if (groupWithMembers == null) return@combine emptyList()
        calculateBalances(bills, groupWithMembers.members)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun calculateBalances(bills: List<BillWithShares>, members: List<Member>): List<MemberBalance> {
        val balanceMap = members.associate { it.id to 0.0 }.toMutableMap()
        
        bills.forEach { billWithShares ->
            val bill = billWithShares.bill
            val shares = billWithShares.shares
            
            // Payer gets credit
            balanceMap[bill.payerId] = (balanceMap[bill.payerId] ?: 0.0) + bill.totalAmount
            
            // Each share is what someone owes
            shares.forEach { share ->
                balanceMap[share.memberId] = (balanceMap[share.memberId] ?: 0.0) - share.amount
            }
        }
        
        return members.map { member ->
            MemberBalance(member, balanceMap[member.id] ?: 0.0)
        }.sortedByDescending { it.balance }
    }

    fun settleUp(fromMemberId: Long, toMemberId: Long, amount: Double) {
        viewModelScope.launch {
            val settlementBill = Bill(
                groupId = groupId,
                title = "Settlement",
                totalAmount = amount,
                category = "SettleUp",
                payerId = fromMemberId
            )
            val share = ExpenseShare(
                billId = 0,
                memberId = toMemberId,
                amount = amount
            )
            billRepository.createBillWithShares(settlementBill, listOf(share))
        }
    }

    class Factory(
        private val billRepository: BillRepository,
        private val groupRepository: GroupRepository,
        private val groupId: Long
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettleUpViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return SettleUpViewModel(billRepository, groupRepository, groupId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
