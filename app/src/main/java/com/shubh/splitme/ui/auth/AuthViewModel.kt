package com.shubh.splitme.ui.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.R
import com.shubh.splitme.data.Country
import com.shubh.splitme.data.PhoneNumbers
import com.shubh.splitme.domain.repository.AuthRepository
import com.shubh.splitme.domain.repository.PhoneAuthEvent
import com.shubh.splitme.domain.repository.PhoneAuthException
import com.shubh.splitme.domain.repository.PhoneAuthFailure
import com.shubh.splitme.domain.repository.SignInResult
import com.shubh.splitme.ui.common.UiText
import com.shubh.splitme.ui.profile.validateName
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Where the user is in sign-in. */
sealed interface AuthStep {
    data object EnterPhone : AuthStep

    data class EnterCode(
        val phoneE164: String,
        val verificationId: String,
        val resendToken: Any,
        val sentAtMillis: Long
    ) : AuthStep

    /** Signed in for the first time with this number; needs a name to finish sign-up. */
    data object EnterName : AuthStep
}

const val OTP_LENGTH = 6
const val RESEND_DELAY_SECONDS = 30

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<UiText>()
    val error: SharedFlow<UiText> = _error.asSharedFlow()

    /** Inline error under the phone or code field (string resource id), cleared as the user types. */
    private val _fieldError = MutableStateFlow<Int?>(null)
    val fieldError: StateFlow<Int?> = _fieldError.asStateFlow()

    private val _step = MutableStateFlow<AuthStep>(AuthStep.EnterPhone)
    val step: StateFlow<AuthStep> = _step.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(repository.isUserLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private var verificationJob: Job? = null

    init {
        // Signed in but sign-up never finished (e.g. the app was closed on the name step).
        if (_isLoggedIn.value) {
            viewModelScope.launch {
                if (runCatching { repository.hasProfile() }.getOrDefault(true).not()) {
                    _isLoggedIn.value = false
                    _step.value = AuthStep.EnterName
                }
            }
        }
    }

    fun clearFieldError() {
        _fieldError.value = null
    }

    /** Validates the number for [country] and sends the SMS code. */
    fun sendCode(activity: Activity, country: Country, nationalNumber: String) {
        if (nationalNumber.isBlank()) {
            _fieldError.value = R.string.auth_error_phone_required
            return
        }
        val e164 = PhoneNumbers.toE164(nationalNumber, country)
        if (e164 == null) {
            _fieldError.value = R.string.auth_error_phone_invalid
            return
        }
        requestCode(activity, e164, resendToken = null)
    }

    fun resendCode(activity: Activity) {
        val current = _step.value as? AuthStep.EnterCode ?: return
        requestCode(activity, current.phoneE164, current.resendToken)
    }

    private fun requestCode(activity: Activity, phoneE164: String, resendToken: Any?) {
        verificationJob?.cancel()
        _fieldError.value = null
        _isLoading.value = true
        verificationJob = viewModelScope.launch {
            repository.sendVerificationCode(activity, phoneE164, resendToken).collect { event ->
                when (event) {
                    is PhoneAuthEvent.CodeSent -> {
                        _isLoading.value = false
                        _step.value = AuthStep.EnterCode(phoneE164, event.verificationId, event.resendToken, System.currentTimeMillis())
                    }
                    is PhoneAuthEvent.SignedIn -> onSignedIn(event.result)
                    is PhoneAuthEvent.Failed -> {
                        _isLoading.value = false
                        showFailure(event.error)
                    }
                }
            }
            _isLoading.value = false
        }
    }

    fun verifyCode(code: String) {
        val current = _step.value as? AuthStep.EnterCode ?: return
        if (code.length != OTP_LENGTH || !code.all { it.isDigit() }) {
            _fieldError.value = R.string.auth_error_code_incomplete
            return
        }
        _fieldError.value = null
        _isLoading.value = true
        viewModelScope.launch {
            repository.verifyCode(current.verificationId, code)
                .onSuccess { onSignedIn(it) }
                .onFailure { showFailure(it as? PhoneAuthException ?: PhoneAuthException(PhoneAuthFailure.Other, it)) }
            _isLoading.value = false
        }
    }

    /** Back from the code step to fix the number. */
    fun changeNumber() {
        verificationJob?.cancel()
        _fieldError.value = null
        _step.value = AuthStep.EnterPhone
    }

    fun saveName(name: String) {
        val problem = validateName(name)
        if (problem != null) {
            _fieldError.value = problem
            return
        }
        _fieldError.value = null
        _isLoading.value = true
        viewModelScope.launch {
            repository.completeProfile(name.trim())
                .onSuccess { _isLoggedIn.value = true }
                .onFailure { _error.emit(UiText.failure(R.string.auth_error_save_name, it)) }
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            verificationJob?.cancel()
            repository.logout()
            _step.value = AuthStep.EnterPhone
            _isLoggedIn.value = false
        }
    }

    private fun onSignedIn(result: SignInResult) {
        verificationJob?.cancel()
        _isLoading.value = false
        if (result.needsProfile) {
            _step.value = AuthStep.EnterName
        } else {
            _isLoggedIn.value = true
        }
    }

    private suspend fun showFailure(error: PhoneAuthException) {
        when (error.reason) {
            // Problems with what the user typed go under the field...
            PhoneAuthFailure.InvalidNumber -> _fieldError.value = R.string.auth_error_phone_invalid
            PhoneAuthFailure.InvalidCode -> _fieldError.value = R.string.auth_error_code_wrong
            PhoneAuthFailure.CodeExpired -> _fieldError.value = R.string.auth_error_code_expired
            // ...everything else in a snackbar.
            PhoneAuthFailure.TooManyRequests -> _error.emit(UiText.of(R.string.auth_error_too_many))
            PhoneAuthFailure.QuotaExceeded -> _error.emit(UiText.of(R.string.auth_error_quota))
            PhoneAuthFailure.AppNotAuthorized -> _error.emit(UiText.of(R.string.auth_error_not_configured))
            PhoneAuthFailure.Network -> _error.emit(UiText.of(R.string.auth_error_network))
            PhoneAuthFailure.Other -> _error.emit(UiText.failure(R.string.auth_error_generic, error))
        }
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository) as T
        }
    }
}
