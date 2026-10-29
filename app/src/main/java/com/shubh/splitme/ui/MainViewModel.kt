package com.shubh.splitme.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.shubh.splitme.domain.repository.AuthRepository
import com.shubh.splitme.domain.repository.GroupRepository
import com.shubh.splitme.domain.repository.MemberRepository

class MainViewModel(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {
    // Initialization logic if any
    
    class Factory(
        private val authRepository: AuthRepository,
        private val memberRepository: MemberRepository,
        private val groupRepository: GroupRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(authRepository, memberRepository, groupRepository) as T
        }
    }
}
