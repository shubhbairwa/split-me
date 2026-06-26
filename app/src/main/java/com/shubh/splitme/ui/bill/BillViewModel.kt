package com.shubh.splitme.ui.bill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.domain.model.Bill
import com.shubh.splitme.domain.model.BillWithShares
import com.shubh.splitme.domain.model.ExpenseShare
import com.shubh.splitme.domain.repository.BillRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class BillViewModel(private val repository: BillRepository) : ViewModel() {

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

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
        if (title.isBlank()) {
            viewModelScope.launch { _error.emit("Please enter a bill title") }
            return
        }
        if (totalAmount <= 0) {
            viewModelScope.launch { _error.emit("Total amount must be greater than 0") }
            return
        }
        
        val totalShares = shares.sumOf { it.amount }
        if (kotlin.math.abs(totalAmount - totalShares) > 0.01) {
            viewModelScope.launch { _error.emit("Total shares (${"%.2f".format(totalShares)}) must equal total amount (${"%.2f".format(totalAmount)})") }
            return
        }

        viewModelScope.launch {
            try {
                repository.createBillWithShares(bill = Bill(
                    groupId = groupId,
                    title = title,
                    totalAmount = totalAmount,
                    category = category,
                    payerId = payerId
                ), shares = shares)
            } catch (e: Exception) {
                _error.emit("Failed to add bill: ${e.message ?: "Unknown error"}")
            }
        }
    }

    class Factory(private val repository: BillRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return BillViewModel(repository) as T
        }
    }
}
