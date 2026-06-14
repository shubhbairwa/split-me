package com.shubh.splitme.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.data.entity.BillWithShares
import com.shubh.splitme.data.entity.Member
import com.shubh.splitme.data.repository.BillRepository
import com.shubh.splitme.data.repository.MemberRepository
import kotlinx.coroutines.flow.*

data class DashboardState(
    val totalOwedToMe: Double = 0.0,
    val totalIOwe: Double = 0.0,
    val netBalance: Double = 0.0,
    val individualBalances: List<MemberSummary> = emptyList()
)

data class MemberSummary(
    val memberName: String,
    val balance: Double // Positive: they owe me, Negative: I owe them
)

class DashboardViewModel(
    private val billRepository: BillRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    val state: StateFlow<DashboardState> = combine(
        billRepository.getAllBillsWithShares(),
        memberRepository.allMembers
    ) { bills, members ->
        val me = memberRepository.getOrCreateMe()
        calculateDashboardState(bills, members, me.id)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardState()
        )

    private fun calculateDashboardState(
        bills: List<BillWithShares>,
        members: List<Member>,
        meId: Long
    ): DashboardState {
        var totalOwedToMe = 0.0
        var totalIOwe = 0.0
        val balancesMap = mutableMapOf<Long, Double>()

        bills.forEach { billWithShares ->
            val bill = billWithShares.bill
            val shares = billWithShares.shares

            if (bill.payerId == meId) {
                // I paid. Others owe me their share.
                shares.forEach { share ->
                    if (share.memberId != meId) {
                        totalOwedToMe += share.amount
                        balancesMap[share.memberId] = (balancesMap[share.memberId] ?: 0.0) + share.amount
                    }
                }
            } else {
                // Someone else paid. I might owe them.
                shares.forEach { share ->
                    if (share.memberId == meId) {
                        totalIOwe += share.amount
                        balancesMap[bill.payerId] = (balancesMap[bill.payerId] ?: 0.0) - share.amount
                    }
                }
            }
        }

        val individualBalances = members.filter { it.id != meId }
            .map { member ->
                MemberSummary(member.name, balancesMap[member.id] ?: 0.0)
            }
            .filter { kotlin.math.abs(it.balance) > 0.01 }
            .sortedByDescending { it.balance }

        return DashboardState(
            totalOwedToMe = totalOwedToMe,
            totalIOwe = totalIOwe,
            netBalance = totalOwedToMe - totalIOwe,
            individualBalances = individualBalances
        )
    }

    class Factory(
        private val billRepository: BillRepository,
        private val memberRepository: MemberRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DashboardViewModel(billRepository, memberRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
