package com.shubh.splitme.ui.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.data.entity.Member
import com.shubh.splitme.data.repository.MemberRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemberViewModel(private val repository: MemberRepository) : ViewModel() {

    val allMembers: StateFlow<List<Member>> = repository.allMembers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addMember(name: String, email: String?, phoneNumber: String? = null) {
        viewModelScope.launch {
            repository.insert(Member(name = name, email = email, phoneNumber = phoneNumber))
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            repository.delete(member)
        }
    }

    class Factory(private val repository: MemberRepository) : ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MemberViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MemberViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
