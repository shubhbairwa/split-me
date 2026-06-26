package com.shubh.splitme.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.domain.model.Group
import com.shubh.splitme.domain.model.GroupWithMembers
import com.shubh.splitme.domain.repository.AuthRepository
import com.shubh.splitme.domain.repository.GroupRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GroupViewModel(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    val groupsWithMembers: StateFlow<List<GroupWithMembers>> = groupRepository.getGroupsWithMembers()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createGroup(name: String, description: String?) {
        if (name.isBlank()) {
            viewModelScope.launch { _error.emit("Please enter a group name") }
            return
        }

        viewModelScope.launch {
            try {
                val groupId = groupRepository.createGroup(Group(name = name, description = description))
                authRepository.currentUser.first()?.let { me ->
                    groupRepository.addMemberToGroup(groupId, me.id)
                }
            } catch (e: Exception) {
                _error.emit("Failed to create group: ${e.message}")
            }
        }
    }

    fun addMemberToGroup(groupId: String, memberId: String) {
        viewModelScope.launch {
            try {
                groupRepository.addMemberToGroup(groupId, memberId)
            } catch (e: Exception) {
                _error.emit("Failed to add member: ${e.message}")
            }
        }
    }

    fun deleteGroup(groupId: String) {
        viewModelScope.launch {
            try {
                groupRepository.deleteGroup(groupId)
            } catch (e: Exception) {
                _error.emit("Failed to delete group: ${e.message}")
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val groupRepository: GroupRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return GroupViewModel(authRepository, groupRepository) as T
        }
    }
}
