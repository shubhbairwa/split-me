package com.shubh.splitme.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.data.entity.Group
import com.shubh.splitme.data.entity.GroupWithMembers
import com.shubh.splitme.data.entity.Member
import com.shubh.splitme.ui.member.MemberViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SplitMeApplication
    val groupViewModel: GroupViewModel = viewModel(factory = GroupViewModel.Factory(app.groupRepository, app.memberRepository))
    val memberViewModel: MemberViewModel = viewModel(factory = MemberViewModel.Factory(app.memberRepository))
    
    val groups by groupViewModel.groupsWithMembers.collectAsState()
    val allMembers by memberViewModel.allMembers.collectAsState()
    
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var selectedGroupForMemberAdd by remember { mutableStateOf<Group?>(null) }
    var selectedGroupForDetail by remember { mutableStateOf<GroupWithMembers?>(null) }

    if (selectedGroupForDetail != null) {
        GroupDetailScreen(
            groupWithMembers = selectedGroupForDetail!!,
            onBack = { selectedGroupForDetail = null }
        )
    } else {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Groups") }) },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddGroupDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Group")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                if (groups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No groups yet. Create one!")
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(groups) { groupWithMembers ->
                            val group = groupWithMembers.group
                            val members = groupWithMembers.members
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                onClick = { selectedGroupForDetail = groupWithMembers },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column {
                                            Text(group.name, style = MaterialTheme.typography.headlineSmall)
                                            group.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                        }
                                        Row {
                                            IconButton(onClick = { selectedGroupForMemberAdd = group }) {
                                                Icon(Icons.Default.PersonAdd, contentDescription = "Add Member to Group")
                                            }
                                            IconButton(onClick = { groupViewModel.deleteGroup(group) }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete Group")
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Members: ${members.joinToString { it.name }.ifBlank { "None" }}")
                                }
                            }
                        }
                    }
                }
            }

            if (showAddGroupDialog) {
                AddGroupDialog(
                    onDismiss = { showAddGroupDialog = false },
                    onAdd = { name, desc ->
                        groupViewModel.createGroup(name, desc)
                        showAddGroupDialog = false
                    }
                )
            }

            if (selectedGroupForMemberAdd != null) {
                AddMemberToGroupDialog(
                    group = selectedGroupForMemberAdd!!,
                    availableMembers = allMembers,
                    onDismiss = { selectedGroupForMemberAdd = null },
                    onMemberSelected = { memberId ->
                        groupViewModel.addMemberToGroup(selectedGroupForMemberAdd!!.id, memberId)
                        selectedGroupForMemberAdd = null
                    }
                )
            }
        }
    }
}

@Composable
fun AddGroupDialog(onDismiss: () -> Unit, onAdd: (String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onAdd(name, desc.ifBlank { null }) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberToGroupDialog(
    group: Group,
    availableMembers: List<Member>,
    onDismiss: () -> Unit,
    onMemberSelected: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member to ${group.name}") },
        text = {
            if (availableMembers.isEmpty()) {
                Text("No members available. Add members first!")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(availableMembers) { member ->
                        ListItem(
                            headlineContent = { Text(member.name) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingContent = {
                                TextButton(onClick = { onMemberSelected(member.id) }) {
                                    Text("Add")
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
