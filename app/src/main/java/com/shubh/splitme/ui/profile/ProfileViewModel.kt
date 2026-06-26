package com.shubh.splitme.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.domain.repository.AuthRepository
import com.shubh.splitme.domain.repository.MemberRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    val me: StateFlow<Member?> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user != null) {
                flow<Member?> { 
                    try {
                        emit(memberRepository.getMemberById(user.id)) 
                    } catch (e: Exception) {
                        _error.emit("Failed to load profile: ${e.message}")
                        emit(null)
                    }
                }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateProfile(name: String, email: String) {
        if (name.isBlank()) {
            viewModelScope.launch { _error.emit("Name cannot be empty") }
            return
        }

        viewModelScope.launch {
            try {
                me.value?.let { currentMe ->
                    val updatedMe = currentMe.copy(name = name, email = email)
                    memberRepository.saveMember(updatedMe)
                }
            } catch (e: Exception) {
                _error.emit("Failed to update profile: ${e.message}")
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val memberRepository: MemberRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(authRepository, memberRepository) as T
        }
    }
}
