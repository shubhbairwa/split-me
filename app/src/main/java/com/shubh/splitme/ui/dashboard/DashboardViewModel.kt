package com.shubh.splitme.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.domain.model.BillWithShares
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.domain.repository.AuthRepository
import com.shubh.splitme.domain.repository.BillRepository
import com.shubh.splitme.domain.repository.MemberRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val authRepository: AuthRepository,
    private val billRepository: BillRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    val state: StateFlow<DashboardState> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user != null) {
                combine(
                    billRepository.getAllBillsWithShares(),
                    memberRepository.getAllMembers()
                ) { bills, members ->
                    calculateDashboardState(bills, members, user.id)
                }.catch { e ->
                    _error.emit("Failed to load dashboard data: ${e.message}")
                }
            } else {
                flowOf(DashboardState())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardState()
        )

    private fun calculateDashboardState(
        bills: List<BillWithShares>,
        members: List<Member>,
        meId: String
    ): DashboardState {
        var totalOwedToMe = 0.0
        var totalIOwe = 0.0
        val balancesMap = mutableMapOf<String, Double>()

        bills.forEach { billWithShares ->
            val bill = billWithShares.bill
            val shares = billWithShares.shares

            if (bill.payerId == meId) {
                shares.forEach { share ->
                    if (share.memberId != meId) {
                        totalOwedToMe += share.amount
                        balancesMap[share.memberId] = (balancesMap[share.memberId] ?: 0.0) + share.amount
                    }
                }
            } else {
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
        private val authRepository: AuthRepository,
        private val billRepository: BillRepository,
        private val memberRepository: MemberRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(authRepository, billRepository, memberRepository) as T
        }
    }
}
