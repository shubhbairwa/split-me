package com.shubh.splitme.ui.member

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.data.entity.Member

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MemberListScreen() {
    val context = LocalContext.current
    val repository = (context.applicationContext as SplitMeApplication).memberRepository
    val viewModel: MemberViewModel = viewModel(factory = MemberViewModel.Factory(repository))
    
    val members by viewModel.allMembers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val contactsPermissionState = rememberPermissionState(
        android.Manifest.permission.READ_CONTACTS
    )

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let {
            val details = getContactDetails(context, it)
            if (details.first.isNotBlank()) {
                viewModel.addMember(details.first, details.second, details.third)
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Members") }) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = {
                        if (contactsPermissionState.status.isGranted) {
                            contactPickerLauncher.launch(null)
                        } else {
                            contactsPermissionState.launchPermissionRequest()
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.ContactPage, contentDescription = "Add from Contacts")
                }
                Spacer(modifier = Modifier.height(16.dp))
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Member")
                }
            }
        }
    ) { padding ->
        if (members.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No members yet. Add some!")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(members) { member ->
                    ListItem(
                        headlineContent = { Text(member.name) },
                        supportingContent = { 
                            Column {
                                member.email?.let { Text(it) }
                                member.phoneNumber?.let { Text(it) }
                            }
                        },
                        leadingContent = { Icon(Icons.Default.Person, contentDescription = null) },
                        trailingContent = {
                            if (!member.isMe) {
                                IconButton(onClick = { viewModel.deleteMember(member) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddMemberDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, email, phone ->
                    viewModel.addMember(name, email, phone)
                    showAddDialog = false
                }
            )
        }
    }
}

private fun getContactDetails(context: android.content.Context, contactUri: Uri): Triple<String, String?, String?> {
    var name = ""
    var email: String? = null
    var phone: String? = null
    
    context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            if (nameIndex != -1) name = cursor.getString(nameIndex)
            
            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            if (idIndex != -1) {
                val contactId = cursor.getString(idIndex)
                
                // Query phone
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    arrayOf(contactId),
                    null
                )?.use { pCursor ->
                    if (pCursor.moveToFirst()) {
                        val pIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (pIndex != -1) phone = pCursor.getString(pIndex)
                    }
                }

                // Query email
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    null,
                    ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                    arrayOf(contactId),
                    null
                )?.use { eCursor ->
                    if (eCursor.moveToFirst()) {
                        val eIndex = eCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
                        if (eIndex != -1) email = eCursor.getString(eIndex)
                    }
                }
            }
        }
    }
    return Triple(name, email, phone)
}

@Composable
fun AddMemberDialog(onDismiss: () -> Unit, onAdd: (String, String?, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Member") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onAdd(name, email.ifBlank { null }, phone.ifBlank { null }) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
