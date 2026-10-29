package com.shubh.splitme.ui.member

import com.shubh.splitme.ui.common.rememberPhotoModel
import androidx.compose.ui.res.stringResource
import com.shubh.splitme.R
import com.shubh.splitme.ui.theme.tabScreenInsets
import com.shubh.splitme.ui.theme.glassCardColors
import com.shubh.splitme.ui.theme.glassBorder
import com.shubh.splitme.ui.theme.glassShadow
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.google.accompanist.permissions.shouldShowRationale
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.data.ContactInfo
import androidx.annotation.StringRes
import com.shubh.splitme.ui.profile.validateName
import com.shubh.splitme.ui.components.PhoneNumberInput
import com.shubh.splitme.data.PhoneNumbers
import com.shubh.splitme.data.matchesContact
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.ui.theme.CardElevation
import com.shubh.splitme.ui.theme.TextPrimary
import com.shubh.splitme.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ContactSelectionScreen(
    selectedMembers: List<Member>,
    onAddContact: (name: String, email: String?, phone: String?) -> Unit,
    onRemoveMember: (Member) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val context = LocalContext.current
    val app = context.applicationContext as SplitMeApplication
    val viewModel: MemberViewModel = viewModel(factory = MemberViewModel.Factory(app.memberRepository))

    val contacts by viewModel.contacts.collectAsState()
    val defaultIso = remember { PhoneNumbers.defaultCountry(context).iso }
    val isLoadingContacts by viewModel.isLoadingContacts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showManualAdd by remember { mutableStateOf(false) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }

    val contactsPermissionState = rememberPermissionState(
        android.Manifest.permission.READ_CONTACTS
    ) { permissionRequested = true }
    val isGranted = contactsPermissionState.status.isGranted
    // After a denial without "rationale", Android won't show the dialog again: send the user to Settings.
    val permanentlyDenied = !isGranted && permissionRequested &&
        !contactsPermissionState.status.shouldShowRationale

    BackHandler(onBack = onBack)

    LaunchedEffect(isGranted) {
        if (isGranted) viewModel.loadContacts(context)
    }

    LaunchedEffect(Unit) {
        viewModel.error.collect { snackbarHostState.showSnackbar(it.resolve(context)) }
    }

    Scaffold(
        contentWindowInsets = tabScreenInsets(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.contacts_title), color = TextPrimary) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = TextPrimary)
                        }
                    }
                )
                if (isGranted) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text(stringResource(R.string.contacts_search)) },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        singleLine = true,
                        shape = CircleShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showManualAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.contacts_add_manually))
            }
        }
    ) { padding ->
        val filteredContacts = remember(contacts, searchQuery) {
            val q = searchQuery.trim()
            if (q.isEmpty()) contacts
            else contacts.filter {
                it.name.contains(q, ignoreCase = true) ||
                    (it.phone?.contains(q) ?: false) ||
                    (it.email?.contains(q, ignoreCase = true) ?: false)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = padding.calculateBottomPadding() + 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Who is already in the group, including people added by hand or not in the phone's contacts.
            item(key = "members_header") {
                SectionHeader(stringResource(R.string.contacts_in_group, selectedMembers.size))
            }
            if (selectedMembers.isEmpty()) {
                item(key = "members_empty") {
                    Text(stringResource(R.string.group_no_members), color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(selectedMembers, key = { "member_${it.id}" }) { member ->
                    GroupMemberRow(member = member, onRemove = { onRemoveMember(member) })
                }
            }

            item(key = "contacts_header") {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(stringResource(R.string.contacts_phone_contacts))
            }

            when {
                !isGranted -> item(key = "permission") {
                    PermissionPrompt(
                        permanentlyDenied = permanentlyDenied,
                        onRequest = {
                            permissionRequested = true
                            contactsPermissionState.launchPermissionRequest()
                        },
                        onOpenSettings = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    )
                }
                isLoadingContacts && contacts.isEmpty() -> item(key = "loading") {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                filteredContacts.isEmpty() -> item(key = "contacts_empty") {
                    Text(
                        if (contacts.isEmpty()) stringResource(R.string.contacts_none_on_phone)
                        else stringResource(R.string.contacts_no_match, searchQuery),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> items(filteredContacts, key = { "contact_${it.id}" }) { contact ->
                    val matchedMember = selectedMembers.find {
                        it.matchesContact(contact.name, contact.email, contact.phone)
                    }

                    ContactSelectionCard(
                        contact = contact,
                        isSelected = matchedMember != null,
                        onClick = {
                            if (matchedMember != null) onRemoveMember(matchedMember)
                            // Store numbers as E.164 so the person is recognised when they sign up.
                            else onAddContact(contact.name, contact.email, PhoneNumbers.toE164(contact.phone, defaultIso) ?: contact.phone)
                        }
                    )
                }
            }
        }

        if (showManualAdd) {
            AddMemberDialog(
                onDismiss = { showManualAdd = false },
                onAdd = { name, email, phone ->
                    onAddContact(name, email, phone)
                    showManualAdd = false
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun GroupMemberRow(member: Member, onRemove: () -> Unit) {
    Card(
        modifier = Modifier.glassShadow(MaterialTheme.shapes.medium).fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = glassCardColors(),
        border = glassBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            MemberAvatar(name = member.name, photoUri = member.photoUrl, size = 40)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                (member.phoneNumber?.let { PhoneNumbers.formatInternational(it) } ?: member.email)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Outlined.RemoveCircleOutline,
                    contentDescription = stringResource(R.string.contacts_remove_person, member.name),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun PermissionPrompt(permanentlyDenied: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (permanentlyDenied) stringResource(R.string.contacts_permission_off)
            else stringResource(R.string.contacts_permission_ask),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = if (permanentlyDenied) onOpenSettings else onRequest) {
            Text(if (permanentlyDenied) stringResource(R.string.contacts_open_settings) else stringResource(R.string.contacts_allow_access))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(R.string.contacts_add_by_hand_hint),
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun ContactSelectionCard(contact: ContactInfo, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.glassShadow(MaterialTheme.shapes.medium)
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = glassCardColors(selected = isSelected),
        border = glassBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = CardElevation)
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
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                contact.phone?.let {
                    Text(text = it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            
            Icon(
                if (isSelected) Icons.Outlined.RemoveCircleOutline else Icons.Outlined.Add,
                contentDescription = if (isSelected) stringResource(R.string.action_remove) else stringResource(R.string.action_add),
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
        val photo = rememberPhotoModel(photoUri)
        if (photo != null) {
            AsyncImage(
                model = photo,
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

/** Add someone who isn't in the phone's contacts: name + mobile number (with country). */
@Composable
fun AddMemberDialog(onDismiss: () -> Unit, onAdd: (String, String?, String?) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var countryIso by remember { mutableStateOf(PhoneNumbers.defaultCountry(context).iso) }
    val country = remember(countryIso) { PhoneNumbers.country(countryIso) ?: PhoneNumbers.defaultCountry(context) }
    var phone by remember { mutableStateOf("") }
    @StringRes var nameError by remember { mutableStateOf<Int?>(null) }
    @StringRes var phoneError by remember { mutableStateOf<Int?>(null) }

    val submit = {
        val e164 = PhoneNumbers.toE164(phone, country)
        nameError = validateName(name)
        phoneError = when {
            phone.isBlank() -> R.string.auth_error_phone_required
            e164 == null -> R.string.auth_error_phone_invalid
            else -> null
        }
        if (nameError == null && phoneError == null) onAdd(name.trim(), null, e164)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.contacts_manual_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text(stringResource(R.string.field_name)) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(stringResource(it)) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                PhoneNumberInput(
                    country = country,
                    onCountryChange = {
                        countryIso = it.iso
                        phoneError = null
                    },
                    number = phone,
                    onNumberChange = {
                        phone = it
                        phoneError = null
                    },
                    error = phoneError,
                    onImeAction = { submit() }
                )
            }
        },
        confirmButton = {
            Button(onClick = { submit() }) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
