package com.shubh.splitme.ui.group

import com.shubh.splitme.ui.theme.tabScreenInsets
import com.shubh.splitme.ui.theme.glassCardColors
import com.shubh.splitme.ui.theme.glassBorder
import com.shubh.splitme.ui.theme.glassShadow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.domain.model.Group
import com.shubh.splitme.domain.model.GroupWithMembers
import com.shubh.splitme.ui.components.InitialsAvatar
import com.shubh.splitme.ui.theme.CardElevation
import com.shubh.splitme.ui.theme.NegativeRed
import com.shubh.splitme.ui.theme.PositiveGreen
import com.shubh.splitme.ui.theme.TextPrimary
import com.shubh.splitme.ui.theme.TextSecondary
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupListScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SplitMeApplication
    val groupViewModel: GroupViewModel = viewModel(factory = GroupViewModel.Factory(app.authRepository, app.groupRepository, app.billRepository))

    val groups by groupViewModel.groupsWithMembers.collectAsState()
    val summaries by groupViewModel.groupSummaries.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        groupViewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var showAddGroupDialog by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<Group?>(null) }
    var selectedGroupForDetail by remember { mutableStateOf<GroupWithMembers?>(null) }
    var openDetailOnAddMembers by remember { mutableStateOf(false) }

    if (selectedGroupForDetail != null) {
        GroupDetailScreen(
            groupWithMembers = selectedGroupForDetail!!,
            onBack = { selectedGroupForDetail = null },
            startWithAddMembers = openDetailOnAddMembers
        )
    } else {
        Scaffold(
        contentWindowInsets = tabScreenInsets(),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Groups", fontWeight = FontWeight.Bold, color = TextPrimary) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddGroupDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Add Group")
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(top = padding.calculateTopPadding())) {
                if (groups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No groups yet. Create one!", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = padding.calculateBottomPadding() + 12.dp)
                    ) {
                        items(groups, key = { it.group.id }) { groupWithMembers ->
                            GroupCard(
                                groupWithMembers = groupWithMembers,
                                summary = summaries[groupWithMembers.group.id],
                                onOpen = {
                                    openDetailOnAddMembers = false
                                    selectedGroupForDetail = groupWithMembers
                                },
                                onAddContacts = {
                                    openDetailOnAddMembers = true
                                    selectedGroupForDetail = groupWithMembers
                                },
                                onDelete = { groupToDelete = groupWithMembers.group }
                            )
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

            groupToDelete?.let { group ->
                AlertDialog(
                    onDismissRequest = { groupToDelete = null },
                    title = { Text("Delete \"${group.name}\"?") },
                    text = { Text("This removes the group for everyone. This can't be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            groupViewModel.deleteGroup(group.id)
                            groupToDelete = null
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { groupToDelete = null }) { Text("Cancel") }
                    }
                )
            }
        }
    }
}

@Composable
private fun GroupCard(
    groupWithMembers: GroupWithMembers,
    summary: GroupSummary?,
    onOpen: () -> Unit,
    onAddContacts: () -> Unit,
    onDelete: () -> Unit
) {
    val group = groupWithMembers.group
    val members = groupWithMembers.members
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.glassShadow(MaterialTheme.shapes.large).fillMaxWidth(),
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        colors = glassCardColors(),
        border = glassBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsAvatar(name = group.name, size = 48)
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    group.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Group,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (members.size == 1) "1 person" else "${members.size} people",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
                if (members.isNotEmpty()) {
                    Text(
                        members.joinToString { it.name },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            GroupAmount(summary)

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "More options", tint = TextPrimary)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Add contacts") },
                        leadingIcon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onAddContacts()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete group", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/** Total spent in the group, with the signed-in user's own balance under it. */
@Composable
private fun GroupAmount(summary: GroupSummary?) {
    val total = summary?.totalSpent ?: 0.0
    val mine = summary?.myBalance
    Column(horizontalAlignment = Alignment.End) {
        Text(
            "%.2f".format(total),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        val (label, color) = when {
            total <= 0.0 -> "no bills yet" to TextSecondary
            mine == null -> "total spent" to TextSecondary
            mine > 0.01 -> "you get %.2f".format(mine) to PositiveGreen
            mine < -0.01 -> "you owe %.2f".format(abs(mine)) to NegativeRed
            else -> "settled up" to TextSecondary
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.SemiBold)
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
