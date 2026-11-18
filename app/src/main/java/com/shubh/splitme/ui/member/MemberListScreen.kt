package com.shubh.splitme.ui.member

import androidx.compose.ui.res.stringResource
import com.shubh.splitme.R
import com.shubh.splitme.data.PhoneNumbers
import com.shubh.splitme.ui.theme.glassCardColors
import com.shubh.splitme.ui.theme.glassBorder
import com.shubh.splitme.ui.theme.glassShadow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.ui.components.InitialsAvatar
import com.shubh.splitme.ui.preview.PreviewData
import com.shubh.splitme.ui.preview.PreviewScreen
import com.shubh.splitme.ui.preview.ScreenPreviews
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.ui.theme.CardElevation
import com.shubh.splitme.ui.theme.TextPrimary
import com.shubh.splitme.ui.theme.TextSecondary
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberListScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as SplitMeApplication
    val viewModel: MemberViewModel = viewModel(factory = MemberViewModel.Factory(app.memberRepository))
    
    val members by viewModel.allMembers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message.resolve(context))
        }
    }

    MemberListContent(
        members = members,
        snackbarHostState = snackbarHostState,
        onAddMember = { name, phone -> viewModel.addMember(name, email = null, phoneNumber = phone) }
    )
}

/** The member list UI without the ViewModel, so it can be shown in previews. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberListContent(
    members: List<Member>,
    snackbarHostState: SnackbarHostState,
    onAddMember: (name: String, phone: String?) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.members_title), color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.members_add))
            }
        }
    ) { padding ->
        if (members.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.members_empty), color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(members) { member ->
                    Card(
                        modifier = Modifier.glassShadow(MaterialTheme.shapes.medium).fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = glassCardColors(),
                        border = glassBorder(),
                        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InitialsAvatar(name = member.name, size = 40, photoUrl = member.photoUrl)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(member.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                member.phoneNumber?.let { Text(PhoneNumbers.formatInternational(it), style = MaterialTheme.typography.bodyMedium, color = TextSecondary) }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddMemberDialog(
                onDismiss = { showAddDialog = false },
                // Same name + mobile number dialog as the Add Contacts screen.
                onAdd = { name, _, phone ->
                    onAddMember(name, phone)
                    showAddDialog = false
                }
            )
        }
    }
}

@ScreenPreviews
@Composable
private fun MemberListPreview() {
    PreviewScreen {
        MemberListContent(members = PreviewData.members, snackbarHostState = remember { SnackbarHostState() }, onAddMember = { _, _ -> })
    }
}

@ScreenPreviews
@Composable
private fun MemberListEmptyPreview() {
    PreviewScreen {
        MemberListContent(members = emptyList(), snackbarHostState = remember { SnackbarHostState() }, onAddMember = { _, _ -> })
    }
}
