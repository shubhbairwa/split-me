package com.shubh.splitme.ui.member

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.data.ContactInfo
import com.shubh.splitme.domain.model.Member

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ContactSelectionScreen(
    selectedMembers: List<Member>,
    onMemberToggle: (name: String, email: String?, phone: String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as SplitMeApplication
    val viewModel: MemberViewModel = viewModel(factory = MemberViewModel.Factory(app.memberRepository))
    
    val contacts by viewModel.contacts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showManualAdd by remember { mutableStateOf(false) }

    val contactsPermissionState = rememberPermissionState(
        android.Manifest.permission.READ_CONTACTS
    )

    LaunchedEffect(contactsPermissionState.status.isGranted) {
        if (contactsPermissionState.status.isGranted) {
            viewModel.loadContacts(context)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Add Contacts") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    placeholder = { Text("Search contacts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showManualAdd = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Manually")
            }
        }
    ) { padding ->
        if (!contactsPermissionState.status.isGranted) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Button(onClick = { contactsPermissionState.launchPermissionRequest() }) {
                    Text("Grant Contacts Permission")
                }
            }
        } else {
            val filteredContacts = contacts.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                (it.phone?.contains(searchQuery) ?: false)
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredContacts) { contact ->
                    val isSelected = selectedMembers.any { 
                        (it.phoneNumber != null && it.phoneNumber == contact.phone) || 
                        (it.name == contact.name && it.phoneNumber == null && contact.phone == null)
                    }
                    
                    ContactSelectionCard(
                        contact = contact,
                        isSelected = isSelected,
                        onClick = { onMemberToggle(contact.name, contact.email, contact.phone) }
                    )
                }
            }
        }

        if (showManualAdd) {
            AddMemberDialog(
                onDismiss = { showManualAdd = false },
                onAdd = { name, email, phone ->
                    onMemberToggle(name, email, phone)
                    showManualAdd = false
                }
            )
        }
    }
}

@Composable
fun ContactSelectionCard(contact: ContactInfo, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatar(name = contact.name, photoUri = contact.photoUri)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                contact.phone?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Icon(
                if (isSelected) Icons.Default.RemoveCircle else Icons.Default.Add,
                contentDescription = if (isSelected) "Remove" else "Add",
                tint = if (isSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MemberAvatar(name: String, photoUri: String?, size: Int = 48) {
    val color = remember(name) {
        val hash = name.hashCode()
        Color(
            red = (hash and 0xFF0000 shr 16) % 200 + 50,
            green = (hash and 0x00FF00 shr 8) % 200 + 50,
            blue = (hash and 0x0000FF) % 200 + 50,
            alpha = 255
        )
    }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = Color.White,
                fontSize = (size / 2.5).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AddMemberDialog(onDismiss: () -> Unit, onAdd: (String, String?, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact Manually") },
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
