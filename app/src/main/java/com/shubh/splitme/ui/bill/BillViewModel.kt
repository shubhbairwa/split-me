package com.shubh.splitme.ui.bill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.domain.model.Bill
import com.shubh.splitme.domain.model.BillWithShares
import com.shubh.splitme.domain.model.ExpenseShare
import com.shubh.splitme.domain.repository.BillRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BillViewModel(private val repository: BillRepository) : ViewModel() {

    fun getBillsByGroup(groupId: String): StateFlow<List<BillWithShares>> {
        return repository.getBillsByGroup(groupId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addBill(
        groupId: String?,
        title: String,
        totalAmount: Double,
        category: String,
        payerId: String,
        shares: List<ExpenseShare>
    ) {
        viewModelScope.launch {
            val bill = Bill(
                groupId = groupId,
                title = title,
                totalAmount = totalAmount,
                category = category,
                payerId = payerId
            )
            repository.createBillWithShares(bill, shares)
        }
    }

    class Factory(private val repository: BillRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BillViewModel(repository) as T
        }
    }
}
