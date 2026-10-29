package com.shubh.splitme.ui.profile

import com.shubh.splitme.ui.common.rememberPhotoModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.shubh.splitme.data.PhoneNumbers
import androidx.compose.ui.res.stringResource
import com.shubh.splitme.R
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shubh.splitme.SplitMeApplication
import com.shubh.splitme.ui.components.colorForName
import com.shubh.splitme.ui.theme.AccentOrange
import com.shubh.splitme.ui.theme.AccentOrangeDeep
import com.shubh.splitme.ui.theme.PureWhite
import com.shubh.splitme.ui.theme.TextPrimary
import com.shubh.splitme.ui.theme.TextSecondary
import com.shubh.splitme.ui.theme.glass
import com.shubh.splitme.ui.theme.tabScreenInsets

private val FieldShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SplitMeApplication
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(app.authRepository, app.memberRepository, app.photoRepository)
    )

    val me by viewModel.me.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsState()
    // System photo picker: no storage permission needed.
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.uploadPhoto(context, uri)
    }

    var name by rememberSaveable { mutableStateOf("") }
    var dobMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var location by rememberSaveable { mutableStateOf("") }
    var loadedFor by rememberSaveable { mutableStateOf<String?>(null) }
    // Messages appear after the first Save tap, then update live as the user fixes each field.
    var showErrors by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.error.collect { snackbarHostState.showSnackbar(it.resolve(context)) } }
    LaunchedEffect(Unit) { viewModel.saved.collect { snackbarHostState.showSnackbar(it.resolve(context)) } }

    // Fill the form once per signed-in user, without overwriting what they're typing.
    LaunchedEffect(me) {
        val m = me ?: return@LaunchedEffect
        if (loadedFor != m.id) {
            name = m.name
            dobMillis = isoDateToMillis(m.dateOfBirth)
            location = m.location.orEmpty()
            loadedFor = m.id
        }
    }

    val errors = if (showErrors) validateProfile(name, dobMillis, location) else ProfileErrors()

    Scaffold(
        contentWindowInsets = tabScreenInsets(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profile_title), fontWeight = FontWeight.Bold, color = TextPrimary) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBackIos, contentDescription = stringResource(R.string.action_back), tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { confirmLogout = true }) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = stringResource(R.string.profile_log_out), tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = padding.calculateBottomPadding() + 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            ProfileAvatar(
                name = name.ifBlank { me?.name.orEmpty() },
                photoUrl = me?.photoUrl,
                uploading = isUploadingPhoto,
                onChoosePhoto = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onRemovePhoto = { viewModel.removePhoto() }
            )
            Spacer(modifier = Modifier.height(28.dp))

            FormField(label = stringResource(R.string.profile_field_full_name), error = errors.name) {
                ProfileTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.profile_hint_full_name),
                    isError = errors.name != null,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
                )
            }

            // The number the user signed in with; changing it would need a new SMS verification.
            FormField(label = stringResource(R.string.profile_field_mobile), error = null) {
                ProfileTextField(
                    value = PhoneNumbers.formatInternational(me?.phoneNumber),
                    onValueChange = {},
                    placeholder = "",
                    enabled = false,
                    trailingIcon = Icons.Outlined.Verified
                )
                Text(
                    stringResource(R.string.profile_mobile_verified),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            }

            FormField(label = stringResource(R.string.profile_field_dob), error = errors.dateOfBirth) {
                // Read-only field that opens the date picker when tapped.
                val interaction = remember { MutableInteractionSource() }
                LaunchedEffect(interaction) {
                    interaction.interactions.collect { if (it is PressInteraction.Release) showDatePicker = true }
                }
                ProfileTextField(
                    value = dobMillis?.let { formatDateForDisplay(it) }.orEmpty(),
                    onValueChange = {},
                    placeholder = stringResource(R.string.profile_hint_dob),
                    readOnly = true,
                    isError = errors.dateOfBirth != null,
                    trailingIcon = Icons.Outlined.CalendarMonth,
                    interactionSource = interaction
                )
            }

            FormField(label = stringResource(R.string.profile_field_location), error = errors.location) {
                ProfileTextField(
                    value = location,
                    onValueChange = { location = it },
                    placeholder = stringResource(R.string.profile_hint_location),
                    isError = errors.location != null,
                    trailingIcon = Icons.Outlined.LocationOn,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done)
                )
            }

            FormField(label = stringResource(R.string.profile_field_language), error = null) {
                LanguagePicker()
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    // Invalid fields show their message under the field; nothing is saved until all pass.
                    showErrors = true
                    viewModel.saveProfile(name, dobMillis, location)
                },
                enabled = !isSaving && me != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = FieldShape,
                // Dark charcoal in light mode, light in dark mode: high contrast like the design.
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.surface
                    )
                } else {
                    Text(stringResource(R.string.profile_submit), fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                }
            }
        }
    }

    if (showDatePicker) {
        DateOfBirthPicker(
            initialMillis = dobMillis,
            onDismiss = { showDatePicker = false },
            onPicked = {
                dobMillis = it
                showDatePicker = false
            }
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.profile_logout_title)) },
            text = { Text(stringResource(R.string.profile_logout_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) { Text(stringResource(R.string.profile_logout_confirm), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

/** Photo (or initials) in an orange ring, with a camera badge to choose or remove a picture. */
@Composable
private fun ProfileAvatar(
    name: String,
    photoUrl: String?,
    uploading: Boolean,
    onChoosePhoto: () -> Unit,
    onRemovePhoto: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val openPicker = { if (photoUrl == null) onChoosePhoto() else menuOpen = true }

    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(116.dp)
                .border(2.dp, Brush.linearGradient(listOf(AccentOrange, AccentOrangeDeep)), CircleShape)
                .padding(5.dp)
                .clip(CircleShape)
                .background(colorForName(name.ifBlank { "?" }))
                .clickable(enabled = !uploading, onClick = openPicker),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.trim().split(" ").filter { it.isNotEmpty() }.take(2)
                    .joinToString("") { it.take(1).uppercase() }.ifEmpty { "?" },
                color = PureWhite,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            )
            val photo = rememberPhotoModel(photoUrl)
            if (photo != null) {
                AsyncImage(
                    model = photo,
                    contentDescription = stringResource(R.string.profile_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (uploading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PureWhite, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                }
            }
        }

        Box {
            SmallFloatingActionButton(
                onClick = { if (!uploading) openPicker() },
                containerColor = AccentOrange,
                contentColor = PureWhite,
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Outlined.PhotoCamera,
                    contentDescription = stringResource(R.string.profile_change_photo),
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.profile_choose_photo)) },
                    leadingIcon = { Icon(Icons.Outlined.PhotoLibrary, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onChoosePhoto()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.profile_remove_photo), color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        menuOpen = false
                        onRemovePhoto()
                    }
                )
            }
        }
    }
}

/** Bold label above the field and a validation message under it, like the design. */
@Composable
private fun FormField(label: String, @StringRes error: Int?, field: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        field()
        if (error != null) {
            Text(
                stringResource(error, MIN_AGE_YEARS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    trailingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    interactionSource: MutableInteractionSource? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextSecondary) },
        singleLine = true,
        isError = isError,
        enabled = enabled,
        readOnly = readOnly,
        shape = FieldShape,
        keyboardOptions = keyboardOptions,
        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
        trailingIcon = trailingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        colors = profileFieldColors()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateOfBirthPicker(initialMillis: Long?, onDismiss: () -> Unit, onPicked: (Long) -> Unit) {
    val now = remember { System.currentTimeMillis() }
    val currentYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) }
    // With no date yet, open 25 years back rather than on today, so a birthday is a few taps away.
    val defaultMonth = remember {
        java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            add(java.util.Calendar.YEAR, -25)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        initialDisplayedMonthMillis = initialMillis ?: defaultMonth,
        yearRange = 1900..currentYear,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= now
            override fun isSelectableYear(year: Int) = year <= currentYear
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let(onPicked) },
                enabled = state.selectedDateMillis != null
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun profileFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = glass.surface,
    unfocusedContainerColor = glass.surface,
    disabledContainerColor = glass.surface,
    errorContainerColor = glass.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedBorderColor = AccentOrange,
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
    disabledTextColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    unfocusedTrailingIconColor = TextSecondary,
    focusedTrailingIconColor = AccentOrange
)

private val LanguageOptions = listOf(
    "" to R.string.language_system,
    "en" to R.string.language_english,
    "hi" to R.string.language_hindi,
    "es" to R.string.language_spanish
)

/**
 * App language. Uses AppCompat's per-app locales: on Android 13+ this is the same setting as
 * Settings > Apps > splitMe > Language; older versions store it in the app. The screen is
 * recreated in the new language straight away (typed-in form values are kept).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguagePicker() {
    val currentTag = remember {
        AppCompatDelegate.getApplicationLocales().get(0)?.language.orEmpty()
    }
    var expanded by remember { mutableStateOf(false) }
    val selected = LanguageOptions.firstOrNull { it.first == currentTag } ?: LanguageOptions.first()

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = stringResource(selected.second),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            shape = FieldShape,
            leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = profileFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LanguageOptions.forEach { (tag, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = {
                        expanded = false
                        if (tag != currentTag) {
                            AppCompatDelegate.setApplicationLocales(
                                if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                                else LocaleListCompat.forLanguageTags(tag)
                            )
                        }
                    }
                )
            }
        }
    }
}
