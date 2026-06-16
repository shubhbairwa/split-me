package com.shubh.splitme.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.data.entity.GroupWithMembers
import com.shubh.splitme.data.entity.Member
import com.shubh.splitme.ui.bill.BillEntryScreen
import com.shubh.splitme.ui.bill.BillViewModel
import com.shubh.splitme.ui.member.ContactSelectionScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupWithMembers: GroupWithMembers,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SplitMeApplication
    val billViewModel: BillViewModel = viewModel(factory = BillViewModel.Factory(app.billRepository))
    val groupViewModel: GroupViewModel = viewModel(factory = GroupViewModel.Factory(app.groupRepository, app.memberRepository))
    
    val bills by billViewModel.getBillsByGroup(groupWithMembers.group.id).collectAsState()
    var showBillEntry by remember { mutableStateOf(false) }
    var showSettleUp by remember { mutableStateOf(false) }
    var showAddMember by remember { mutableStateOf(false) }

    if (showSettleUp) {
        SettleUpScreen(
            groupId = groupWithMembers.group.id,
            onBack = { showSettleUp = false }
        )
    } else if (showAddMember) {
        ContactSelectionScreen(
            selectedMembers = groupWithMembers.members,
            onMemberToggle = { name, email, phone ->
                groupViewModel.viewModelScope.launch {
                    val allMembers = app.memberRepository.getAllMembersOnce()
                    val existingMember = allMembers.find { 
                        (it.phoneNumber != null && it.phoneNumber == phone) || 
                        (it.name == name && it.phoneNumber == null && phone == null)
                    }
                    
                    if (existingMember != null) {
                        val isInGroup = groupWithMembers.members.any { it.id == existingMember.id }
                        if (isInGroup) {
                            if (!existingMember.isMe) {
                                groupViewModel.removeMemberFromGroup(groupWithMembers.group.id, existingMember.id)
                            }
                        } else {
                            groupViewModel.addMemberToGroup(groupWithMembers.group.id, existingMember.id)
                        }
                    } else {
                        // Create new member and add to group
                        val newMemberId = app.memberRepository.insert(Member(name = name, email = email, phoneNumber = phone))
                        groupViewModel.addMemberToGroup(groupWithMembers.group.id, newMemberId)
                    }
                }
            },
            onBack = { showAddMember = false }
        )
    } else if (showBillEntry) {
        BillEntryScreen(
            groupId = groupWithMembers.group.id,
            groupName = groupWithMembers.group.name,
            members = groupWithMembers.members,
            onDismiss = { showBillEntry = false },
            onSave = { title, amount, category, payerId, shares ->
                billViewModel.addBill(groupWithMembers.group.id, title, amount, category, payerId, shares)
                showBillEntry = false
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(groupWithMembers.group.name) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAddMember = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Manage Contacts")
                        }
                        TextButton(onClick = { showSettleUp = true }) {
                            Text("Settle Up")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showBillEntry = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Bill")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                // Summary Card
                val totalSpent = bills.sumOf { it.bill.totalAmount }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Total Group Spending", style = MaterialTheme.typography.titleMedium)
                        Text("${"%.2f".format(totalSpent)}", style = MaterialTheme.typography.headlineLarge)
                    }
                }

                Text(
                    "Recent Bills",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleSmall
                )

                if (bills.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No bills yet.")
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        items(bills) { billWithShares ->
                            ListItem(
                                headlineContent = { Text(billWithShares.bill.title) },
                                supportingContent = { 
                                    val payerName = groupWithMembers.members.find { it.id == billWithShares.bill.payerId }?.name ?: "Unknown"
                                    Text("Paid by $payerName • ${billWithShares.bill.category}") 
                                },
                                trailingContent = { Text("${"%.2f".format(billWithShares.bill.totalAmount)}", style = MaterialTheme.typography.bodyLarge) }
                            )
                        }
                    }
                }
            }
        }
    }
}
