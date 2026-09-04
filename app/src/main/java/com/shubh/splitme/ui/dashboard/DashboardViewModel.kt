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
import java.util.Calendar

enum class TimeRange { DAY, WEEK, MONTH, YEAR }

data class ChartPoint(val label: String, val value: Double)

data class CategorySpend(val category: String, val amount: Double, val percent: Float)

data class DashboardState(
    val totalOwedToMe: Double = 0.0,
    val totalIOwe: Double = 0.0,
    val netBalance: Double = 0.0,
    val individualBalances: List<MemberSummary> = emptyList(),
    val spendByRange: Map<TimeRange, List<ChartPoint>> = emptyMap(),
    val categoryBreakdown: List<CategorySpend> = emptyList()
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
            individualBalances = individualBalances,
            spendByRange = TimeRange.entries.associateWith { range -> buildSpendSeries(bills, range) },
            categoryBreakdown = buildCategoryBreakdown(bills)
        )
    }

    private fun buildSpendSeries(bills: List<BillWithShares>, range: TimeRange): List<ChartPoint> {
        val now = Calendar.getInstance()

        fun bucketLabelsAndSums(bucketCount: Int, bucketOf: (Calendar) -> Int?, labelOf: (Int) -> String): List<ChartPoint> {
            val sums = DoubleArray(bucketCount)
            bills.forEach { billWithShares ->
                val cal = Calendar.getInstance().apply { timeInMillis = billWithShares.bill.date }
                val bucket = bucketOf(cal) ?: return@forEach
                if (bucket in 0 until bucketCount) {
                    sums[bucket] += billWithShares.bill.totalAmount
                }
            }
            return (0 until bucketCount).map { i -> ChartPoint(labelOf(i), sums[i]) }
        }

        return when (range) {
            TimeRange.DAY -> {
                val hourLabels = listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00")
                bucketLabelsAndSums(
                    bucketCount = 6,
                    bucketOf = { cal ->
                        if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                            cal.get(Calendar.HOUR_OF_DAY) / 4
                        } else null
                    },
                    labelOf = { hourLabels[it] }
                )
            }
            TimeRange.WEEK -> {
                val weekLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val startOfWeek = (now.clone() as Calendar).apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                }
                bucketLabelsAndSums(
                    bucketCount = 7,
                    bucketOf = { cal ->
                        val diffDays = ((cal.timeInMillis - startOfWeek.timeInMillis) / (24 * 60 * 60 * 1000L)).toInt()
                        if (diffDays in 0..6) diffDays else null
                    },
                    labelOf = { weekLabels[it] }
                )
            }
            TimeRange.MONTH -> {
                bucketLabelsAndSums(
                    bucketCount = 5,
                    bucketOf = { cal ->
                        if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)) {
                            ((cal.get(Calendar.DAY_OF_MONTH) - 1) / 7).coerceAtMost(4)
                        } else null
                    },
                    labelOf = { "W${it + 1}" }
                )
            }
            TimeRange.YEAR -> {
                val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                bucketLabelsAndSums(
                    bucketCount = 12,
                    bucketOf = { cal ->
                        if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) cal.get(Calendar.MONTH) else null
                    },
                    labelOf = { monthLabels[it] }
                )
            }
        }
    }

    private fun buildCategoryBreakdown(bills: List<BillWithShares>): List<CategorySpend> {
        val totals = mutableMapOf<String, Double>()
        bills.forEach { billWithShares ->
            val category = billWithShares.bill.category.ifBlank { "General" }
            totals[category] = (totals[category] ?: 0.0) + billWithShares.bill.totalAmount
        }
        val grandTotal = totals.values.sum()
        if (grandTotal <= 0.0) return emptyList()

        return totals.entries
            .sortedByDescending { it.value }
            .map { (category, amount) ->
                CategorySpend(category, amount, ((amount / grandTotal) * 100).toFloat())
            }
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
