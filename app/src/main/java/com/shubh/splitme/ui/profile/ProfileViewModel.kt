package com.shubh.splitme.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.data.entity.Member
import com.shubh.splitme.data.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: MemberRepository) : ViewModel() {

    private val _me = MutableStateFlow<Member?>(null)
    val me: StateFlow<Member?> = _me.asStateFlow()

    init {
        loadMe()
    }

    private fun loadMe() {
        viewModelScope.launch {
            _me.value = repository.getOrCreateMe()
        }
    }

    fun updateProfile(name: String, email: String, phoneNumber: String, photoUri: String?) {
        viewModelScope.launch {
            val currentMe = _me.value ?: repository.getOrCreateMe()
            val updatedMe = currentMe.copy(
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                photoUri = photoUri
            )
            repository.update(updatedMe)
            _me.value = updatedMe
        }
    }

    class Factory(private val repository: MemberRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(repository) as T
        }
    }
}
