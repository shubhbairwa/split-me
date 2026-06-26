package com.shubh.splitme.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.domain.model.Bill
import com.shubh.splitme.domain.model.BillWithShares
import com.shubh.splitme.domain.model.ExpenseShare
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.domain.repository.BillRepository
import com.shubh.splitme.domain.repository.GroupRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class MemberBalance(
    val member: Member,
    val balance: Double // Positive means they are owed, negative means they owe
)

class SettleUpViewModel(
    private val billRepository: BillRepository,
    private val groupRepository: GroupRepository,
    private val groupId: String
) : ViewModel() {

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

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
            
            balanceMap[bill.payerId] = (balanceMap[bill.payerId] ?: 0.0) + bill.totalAmount
            
            shares.forEach { share ->
                balanceMap[share.memberId] = (balanceMap[share.memberId] ?: 0.0) - share.amount
            }
        }
        
        return members.map { member ->
            MemberBalance(member, balanceMap[member.id] ?: 0.0)
        }.sortedByDescending { it.balance }
    }

    fun settleUp(fromMemberId: String, toMemberId: String, amount: Double) {
        viewModelScope.launch {
            try {
                val settlementBill = Bill(
                    groupId = groupId,
                    title = "Settlement",
                    totalAmount = amount,
                    category = "SettleUp",
                    payerId = fromMemberId
                )
                val share = ExpenseShare(
                    billId = "",
                    memberId = toMemberId,
                    amount = amount
                )
                billRepository.createBillWithShares(settlementBill, listOf(share))
            } catch (e: Exception) {
                _error.emit("Failed to record settlement: ${e.message}")
            }
        }
    }

    class Factory(
        private val billRepository: BillRepository,
        private val groupRepository: GroupRepository,
        private val groupId: String
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SettleUpViewModel(billRepository, groupRepository, groupId) as T
        }
    }
}
