package com.shubh.splitme.ui.member

import com.shubh.splitme.R
import com.shubh.splitme.ui.common.UiText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.data.ContactInfo
import com.shubh.splitme.data.fetchContacts
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.domain.repository.MemberRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MemberViewModel(private val repository: MemberRepository) : ViewModel() {

    private val _error = MutableSharedFlow<UiText>()
    val error: SharedFlow<UiText> = _error.asSharedFlow()

    private val _contacts = MutableStateFlow<List<ContactInfo>>(emptyList())
    val contacts: StateFlow<List<ContactInfo>> = _contacts

    private val _isLoadingContacts = MutableStateFlow(false)
    val isLoadingContacts: StateFlow<Boolean> = _isLoadingContacts

    val allMembers: StateFlow<List<Member>> = repository.getAllMembers()
        .catch { e -> _error.emit(UiText.failure(R.string.error_load_members, e)) }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadContacts(context: android.content.Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            _isLoadingContacts.value = true
            try {
                val contactList = withContext(Dispatchers.IO) {
                    fetchContacts(appContext)
                }
                _contacts.value = contactList
            } catch (e: Exception) {
                _error.emit(UiText.failure(R.string.error_load_contacts, e))
            } finally {
                _isLoadingContacts.value = false
            }
        }
    }

    fun addMember(name: String, email: String?, phoneNumber: String? = null) {
        if (name.isBlank()) {
            viewModelScope.launch { _error.emit(UiText.of(R.string.error_enter_member_name)) }
            return
        }

        viewModelScope.launch {
            try {
                repository.saveMember(Member(name = name, email = email, phoneNumber = phoneNumber))
            } catch (e: Exception) {
                _error.emit(UiText.failure(R.string.error_save_member, e))
            }
        }
    }

    class Factory(private val repository: MemberRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MemberViewModel(repository) as T
        }
    }
}
