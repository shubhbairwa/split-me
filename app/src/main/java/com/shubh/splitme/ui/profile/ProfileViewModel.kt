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

    val me: StateFlow<Member?> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user != null) {
                flow<Member?> { emit(memberRepository.getMemberById(user.id)) }
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateProfile(name: String, email: String) {
        viewModelScope.launch {
            me.value?.let { currentMe ->
                val updatedMe = currentMe.copy(name = name, email = email)
                memberRepository.saveMember(updatedMe)
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
