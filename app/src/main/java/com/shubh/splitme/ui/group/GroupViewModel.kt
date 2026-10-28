package com.shubh.splitme.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.data.normalizePhone
import com.shubh.splitme.domain.model.Group
import com.shubh.splitme.domain.model.GroupWithMembers
import com.shubh.splitme.domain.repository.AuthRepository
import com.shubh.splitme.domain.repository.BillRepository
import com.shubh.splitme.domain.repository.GroupRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Money summary for one group, shown on the group list. */
data class GroupSummary(
    /** Sum of the group's bills, not counting settle-up payments. */
    val totalSpent: Double,
    /** Signed-in user's balance: positive = they are owed, negative = they owe. Null if not a member. */
    val myBalance: Double?
)

class GroupViewModel(
    private val authRepository: AuthRepository,
    private val groupRepository: GroupRepository,
    private val billRepository: BillRepository
) : ViewModel() {

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    /** Short confirmations (e.g. "Added Alex to the group") for a snackbar. */
    private val _info = MutableSharedFlow<String>()
    val info: SharedFlow<String> = _info.asSharedFlow()

    // Contacts currently being saved, so a double tap doesn't create the same member twice.
    private val pendingContacts = mutableSetOf<String>()

    val groupsWithMembers: StateFlow<List<GroupWithMembers>> = groupRepository.getGroupsWithMembers()
        .catch { e -> _error.emit("Failed to load groups: ${e.message}") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Group id -> money summary. Uses the same balance rule as Settle Up (paid minus own share). */
    val groupSummaries: StateFlow<Map<String, GroupSummary>> = combine(
        billRepository.getAllBillsWithShares(),
        authRepository.currentUser,
        groupsWithMembers
    ) { bills, me, groups ->
        val billsByGroup = bills.filter { it.bill.groupId != null }.groupBy { it.bill.groupId!! }
        groups.associate { gwm ->
            val group = gwm.group
            val groupBills = billsByGroup[group.id].orEmpty()
            val spent = groupBills.filter { it.bill.category != "SettleUp" }.sumOf { it.bill.totalAmount }
            // Only members have a balance; for anyone else the list shows just the total.
            val myBalance = me?.takeIf { it.id in group.memberIds }?.let { user ->
                val paid = groupBills.filter { it.bill.payerId == user.id }.sumOf { it.bill.totalAmount }
                val owed = groupBills.sumOf { b -> b.shares.filter { it.memberId == user.id }.sumOf { it.amount } }
                paid - owed
            }
            group.id to GroupSummary(spent, myBalance)
        }
    }
        .catch { e -> _error.emit("Failed to load group totals: ${e.message}") }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
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

    fun addContactToGroup(groupId: String, name: String, email: String?, phone: String?) {
        if (name.isBlank()) {
            viewModelScope.launch { _error.emit("Please enter a name") }
            return
        }
        val key = "$groupId|${normalizePhone(phone) ?: email?.trim()?.lowercase() ?: name.trim().lowercase()}"
        if (!pendingContacts.add(key)) return

        viewModelScope.launch {
            try {
                val member = groupRepository.addContactToGroup(groupId, name, email, phone)
                _info.emit("Added ${member.name} to the group")
            } catch (e: Exception) {
                _error.emit("Failed to add $name: ${e.message}")
            } finally {
                pendingContacts.remove(key)
            }
        }
    }

    fun removeMemberFromGroup(groupId: String, memberId: String) {
        viewModelScope.launch {
            try {
                groupRepository.removeMemberFromGroup(groupId, memberId)
            } catch (e: Exception) {
                _error.emit("Failed to remove member: ${e.message}")
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
        private val groupRepository: GroupRepository,
        private val billRepository: BillRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return GroupViewModel(authRepository, groupRepository, billRepository) as T
        }
    }
}
