package com.shubh.splitme.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.data.repository.GroupRepository
import com.shubh.splitme.data.repository.MemberRepository
import kotlinx.coroutines.launch

class MainViewModel(
    private val memberRepository: MemberRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {
    init {
        viewModelScope.launch {
            val me = memberRepository.getOrCreateMe()
            
            // Ensure "Me" is in all groups
            val groupsWithMembers = groupRepository.getGroupsWithMembersOnce()
            groupsWithMembers.forEach { groupWithMembers ->
                val hasMe = groupWithMembers.members.any { it.id == me.id }
                if (!hasMe) {
                    groupRepository.addMemberToGroup(groupWithMembers.group.id, me.id)
                }
            }
        }
    }

    class Factory(
        private val memberRepository: MemberRepository,
        private val groupRepository: GroupRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(memberRepository, groupRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
