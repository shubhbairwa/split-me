package com.shubh.splitme.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.text.font.FontWeight
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.domain.model.Group
import com.shubh.splitme.domain.model.GroupWithMembers
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.ui.components.InitialsAvatar
import com.shubh.splitme.ui.member.MemberViewModel
import com.shubh.splitme.ui.theme.CardElevation
import com.shubh.splitme.ui.theme.TextPrimary
import com.shubh.splitme.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SplitMeApplication
    val groupViewModel: GroupViewModel = viewModel(factory = GroupViewModel.Factory(app.authRepository, app.groupRepository))
    val memberViewModel: MemberViewModel = viewModel(factory = MemberViewModel.Factory(app.memberRepository))
    
    val groups by groupViewModel.groupsWithMembers.collectAsState()
    val allMembers by memberViewModel.allMembers.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        groupViewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

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
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Groups", fontWeight = FontWeight.Bold, color = TextPrimary) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddGroupDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Group")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                if (groups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No groups yet. Create one!", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(groups) { groupWithMembers ->
                            val group = groupWithMembers.group
                            val members = groupWithMembers.members

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selectedGroupForDetail = groupWithMembers },
                                shape = MaterialTheme.shapes.large,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    InitialsAvatar(name = group.name, size = 48)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(group.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            members.joinToString { it.name }.ifBlank { "No members yet" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary,
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(onClick = { selectedGroupForMemberAdd = group }) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Member to Group", tint = TextSecondary)
                                    }
                                    IconButton(onClick = { groupViewModel.deleteGroup(group.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = TextSecondary)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
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
    onMemberSelected: (String) -> Unit
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
