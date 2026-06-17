package com.shubh.splitme.ui.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.data.ContactInfo
import com.shubh.splitme.data.fetchContacts
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.domain.repository.MemberRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MemberViewModel(private val repository: MemberRepository) : ViewModel() {

    private val _contacts = MutableStateFlow<List<ContactInfo>>(emptyList())
    val contacts: StateFlow<List<ContactInfo>> = _contacts

    val allMembers: StateFlow<List<Member>> = repository.getAllMembers().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadContacts(context: android.content.Context) {
        viewModelScope.launch {
            val contactList = withContext(Dispatchers.IO) {
                fetchContacts(context)
            }
            _contacts.value = contactList
        }
    }

    fun addMember(name: String, email: String?, phoneNumber: String? = null) {
        viewModelScope.launch {
            repository.saveMember(Member(name = name, email = email, phoneNumber = phoneNumber))
        }
    }

    class Factory(private val repository: MemberRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MemberViewModel(repository) as T
        }
    }
}
