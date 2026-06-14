package com.shubh.splitme.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.data.entity.Group
import com.shubh.splitme.data.entity.GroupWithMembers
import com.shubh.splitme.data.repository.GroupRepository
import com.shubh.splitme.data.repository.MemberRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroupViewModel(
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    val groupsWithMembers: StateFlow<List<GroupWithMembers>> = groupRepository.groupsWithMembers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createGroup(name: String, description: String?) {
        viewModelScope.launch {
            val groupId = groupRepository.insertGroup(Group(name = name, description = description))
            val me = memberRepository.getOrCreateMe()
            groupRepository.addMemberToGroup(groupId, me.id)
        }
    }

    fun addMemberToGroup(groupId: Long, memberId: Long) {
        viewModelScope.launch {
            groupRepository.addMemberToGroup(groupId, memberId)
        }
    }

    fun deleteGroup(group: Group) {
        viewModelScope.launch {
            groupRepository.deleteGroup(group)
        }
    }

    class Factory(
        private val groupRepository: GroupRepository,
        private val memberRepository: MemberRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GroupViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return GroupViewModel(groupRepository, memberRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
