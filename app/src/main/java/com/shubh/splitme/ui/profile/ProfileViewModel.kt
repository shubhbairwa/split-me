package com.shubh.splitme.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.R
import com.shubh.splitme.domain.model.Member
import com.shubh.splitme.domain.repository.AuthRepository
import com.shubh.splitme.domain.repository.MemberRepository
import com.shubh.splitme.domain.repository.ProfilePhotoRepository
import com.shubh.splitme.ui.common.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val memberRepository: MemberRepository,
    private val photoRepository: ProfilePhotoRepository
) : ViewModel() {

    private val _error = MutableSharedFlow<UiText>()
    val error: SharedFlow<UiText> = _error.asSharedFlow()

    private val _saved = MutableSharedFlow<UiText>()
    /** Confirmations ("Profile saved", "Photo updated"). */
    val saved: SharedFlow<UiText> = _saved.asSharedFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto

    /**
     * Live profile from members/{uid}. The phone number always comes from the sign-in account
     * (it's verified), and the account details fill in if the document is missing.
     */
    val me: StateFlow<Member?> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user == null) flowOf(null)
            else memberRepository.observeMember(user.id)
                .map { doc -> (doc ?: user).copy(id = user.id, phoneNumber = user.phoneNumber) }
                .catch { e ->
                    _error.emit(UiText.failure(R.string.error_load_profile, e))
                    emit(user)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Validates and saves. Returns the validation result so the screen can show messages;
     * nothing is written unless every field is valid.
     */
    fun saveProfile(name: String, dobMillis: Long?, location: String): ProfileErrors {
        val errors = validateProfile(name, dobMillis, location)
        val current = me.value
        if (!errors.isValid || dobMillis == null) return errors
        if (current == null) {
            viewModelScope.launch { _error.emit(UiText.of(R.string.error_not_signed_in)) }
            return errors
        }

        viewModelScope.launch {
            _isSaving.value = true
            try {
                memberRepository.updateProfile(
                    id = current.id,
                    name = name.trim(),
                    dateOfBirth = millisToIsoDate(dobMillis),
                    location = location.trim().ifEmpty { null }
                )
                _saved.emit(UiText.of(R.string.profile_saved))
            } catch (e: Exception) {
                _error.emit(UiText.failure(R.string.error_save_profile, e))
            } finally {
                _isSaving.value = false
            }
        }
        return errors
    }

    /** Crops/resizes the picked image, uploads it and replaces the old picture. */
    fun uploadPhoto(context: Context, uri: Uri) {
        val current = me.value ?: return
        val appContext = context.applicationContext
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            try {
                val jpeg = withContext(Dispatchers.Default) { prepareProfilePhoto(appContext, uri) }
                val uploaded = photoRepository.upload(current.id, jpeg)
                memberRepository.updatePhoto(current.id, uploaded.url, uploaded.path)
                current.photoPath?.takeIf { it != uploaded.path }?.let { old -> runCatching { photoRepository.delete(old) } }
                _saved.emit(UiText.of(R.string.profile_photo_updated))
            } catch (e: Exception) {
                _error.emit(UiText.failure(R.string.error_upload_photo, e))
            } finally {
                _isUploadingPhoto.value = false
            }
        }
    }

    fun removePhoto() {
        val current = me.value ?: return
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            try {
                memberRepository.updatePhoto(current.id, null, null)
                current.photoPath?.let { runCatching { photoRepository.delete(it) } }
                _saved.emit(UiText.of(R.string.profile_photo_removed))
            } catch (e: Exception) {
                _error.emit(UiText.failure(R.string.error_upload_photo, e))
            } finally {
                _isUploadingPhoto.value = false
            }
        }
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val memberRepository: MemberRepository,
        private val photoRepository: ProfilePhotoRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(authRepository, memberRepository, photoRepository) as T
        }
    }
}
