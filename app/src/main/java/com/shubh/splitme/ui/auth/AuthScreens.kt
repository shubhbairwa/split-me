package com.shubh.splitme.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shubh.splitme.R
import com.shubh.splitme.data.PhoneNumbers
import com.shubh.splitme.ui.components.PhoneNumberInput
import com.shubh.splitme.ui.theme.AccentOrange
import com.shubh.splitme.ui.theme.AccentOrangeDeep
import com.shubh.splitme.ui.theme.PureWhite
import com.shubh.splitme.ui.theme.TextPrimary
import com.shubh.splitme.ui.theme.TextSecondary
import com.shubh.splitme.ui.theme.glass
import kotlinx.coroutines.delay

/** Sign-in by mobile number: number -> SMS code -> (first time) name. */
@Composable
fun AuthFlowScreen(viewModel: AuthViewModel) {
    val context = LocalContext.current
    val step by viewModel.step.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val fieldError by viewModel.fieldError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.error.collect { snackbarHostState.showSnackbar(it.resolve(context)) }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            when (val s = step) {
                AuthStep.EnterPhone -> PhoneStep(viewModel, isLoading, fieldError)
                is AuthStep.EnterCode -> CodeStep(viewModel, s, isLoading, fieldError)
                AuthStep.EnterName -> NameStep(viewModel, isLoading, fieldError)
            }
        }
    }
}

@Composable
private fun PhoneStep(viewModel: AuthViewModel, isLoading: Boolean, fieldError: Int?) {
    val context = LocalContext.current
    var countryIso by rememberSaveable { mutableStateOf(PhoneNumbers.defaultCountry(context).iso) }
    val country = remember(countryIso) { PhoneNumbers.country(countryIso) ?: PhoneNumbers.defaultCountry(context) }
    var number by rememberSaveable { mutableStateOf("") }
    val send = { context.findActivity()?.let { viewModel.sendCode(it, country, number) } ?: Unit }

    Spacer(modifier = Modifier.height(72.dp))
    Text(stringResource(R.string.auth_welcome_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(8.dp))
    Text(stringResource(R.string.auth_phone_subtitle), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
    Spacer(modifier = Modifier.height(32.dp))

    Text(
        stringResource(R.string.auth_mobile_number),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
    PhoneNumberInput(
        country = country,
        onCountryChange = {
            countryIso = it.iso
            viewModel.clearFieldError()
        },
        number = number,
        onNumberChange = {
            number = it
            viewModel.clearFieldError()
        },
        error = fieldError,
        enabled = !isLoading,
        onImeAction = { send() }
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(stringResource(R.string.auth_sms_note), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    Spacer(modifier = Modifier.height(32.dp))
    PrimaryButton(text = stringResource(R.string.auth_send_code), loading = isLoading, onClick = { send() })
}

@Composable
private fun CodeStep(viewModel: AuthViewModel, step: AuthStep.EnterCode, isLoading: Boolean, fieldError: Int?) {
    val context = LocalContext.current
    var code by rememberSaveable(step.verificationId) { mutableStateOf("") }
    // Seconds until "Resend code" is allowed, counted from when this code was sent.
    var secondsLeft by remember(step.sentAtMillis) { mutableIntStateOf(RESEND_DELAY_SECONDS) }
    LaunchedEffect(step.sentAtMillis) {
        while (true) {
            val elapsed = ((System.currentTimeMillis() - step.sentAtMillis) / 1000).toInt()
            secondsLeft = (RESEND_DELAY_SECONDS - elapsed).coerceAtLeast(0)
            if (secondsLeft == 0) break
            delay(1000)
        }
    }
    BackHandler { viewModel.changeNumber() }

    Spacer(modifier = Modifier.height(16.dp))
    IconButton(onClick = { viewModel.changeNumber() }, modifier = Modifier.offset(x = (-12).dp)) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = TextPrimary)
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(stringResource(R.string.auth_code_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        stringResource(R.string.auth_code_subtitle, PhoneNumbers.formatInternational(step.phoneE164)),
        style = MaterialTheme.typography.bodyLarge,
        color = TextSecondary
    )
    TextButton(onClick = { viewModel.changeNumber() }, contentPadding = PaddingValues(0.dp)) {
        Text(stringResource(R.string.auth_change_number), color = AccentOrange, fontWeight = FontWeight.SemiBold)
    }
    Spacer(modifier = Modifier.height(24.dp))

    OtpInput(
        code = code,
        onCodeChange = {
            code = it
            viewModel.clearFieldError()
            if (it.length == OTP_LENGTH) viewModel.verifyCode(it)
        },
        isError = fieldError != null,
        enabled = !isLoading
    )
    if (fieldError != null) {
        Text(
            stringResource(fieldError),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    if (secondsLeft > 0) {
        Text(
            stringResource(R.string.auth_resend_in, "0:%02d".format(secondsLeft)),
            color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    } else {
        TextButton(
            onClick = { context.findActivity()?.let { viewModel.resendCode(it) } },
            enabled = !isLoading,
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(stringResource(R.string.auth_resend), color = AccentOrange, fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
    PrimaryButton(text = stringResource(R.string.auth_verify), loading = isLoading, onClick = { viewModel.verifyCode(code) })
}

@Composable
private fun NameStep(viewModel: AuthViewModel, isLoading: Boolean, fieldError: Int?) {
    var name by rememberSaveable { mutableStateOf("") }

    Spacer(modifier = Modifier.height(72.dp))
    Text(stringResource(R.string.auth_name_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(8.dp))
    Text(stringResource(R.string.auth_name_subtitle), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
    Spacer(modifier = Modifier.height(32.dp))
    OutlinedTextField(
        value = name,
        onValueChange = {
            name = it
            viewModel.clearFieldError()
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = fieldError != null,
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        label = { Text(stringResource(R.string.profile_field_full_name)) },
        supportingText = fieldError?.let { { Text(stringResource(it)) } },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { viewModel.saveName(name) }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = glass.surface,
            unfocusedContainerColor = glass.surface,
            errorContainerColor = glass.surface,
            focusedBorderColor = AccentOrange
        )
    )
    Spacer(modifier = Modifier.height(32.dp))
    PrimaryButton(text = stringResource(R.string.auth_continue), loading = isLoading, onClick = { viewModel.saveName(name) })
}

/** Six boxes backed by one text field, so the keyboard, paste and SMS autofill all work. */
@Composable
private fun OtpInput(code: String, onCodeChange: (String) -> Unit, isError: Boolean, enabled: Boolean) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        // The String overload keeps the keyboard's composing state between keystrokes. Building a new
        // TextFieldValue each time reset it, and some keyboards (Samsung) then dropped digits.
        value = code,
        onValueChange = { onCodeChange(it.filter(Char::isDigit).take(OTP_LENGTH)) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        modifier = Modifier.focusRequester(focusRequester),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(OTP_LENGTH) { i ->
                    val active = i == code.length && enabled
                    val borderColor = when {
                        isError -> MaterialTheme.colorScheme.error
                        active -> AccentOrange
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(0.85f)
                            .background(glass.surface, RoundedCornerShape(14.dp))
                            .border(if (active) 2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            code.getOrNull(i)?.toString().orEmpty(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun PrimaryButton(text: String, loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Brush.horizontalGradient(listOf(AccentOrange, AccentOrangeDeep)), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = PureWhite,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = PureWhite.copy(alpha = 0.8f)
        )
    ) {
        if (loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = PureWhite)
        else Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

/** Firebase phone sign-in needs the Activity; the Compose context may be wrapped (e.g. by AppCompat). */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
