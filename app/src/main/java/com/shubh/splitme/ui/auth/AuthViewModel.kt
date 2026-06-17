package com.shubh.splitme.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shubh.splitme.domain.repository.AuthRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    private val _isLoggedIn = MutableStateFlow(repository.isUserLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.login(email, password)
                .onSuccess { 
                    _isLoggedIn.value = true 
                }
                .onFailure { 
                    _error.emit(it.message ?: "Login failed")
                }
            _isLoading.value = false
        }
    }

    fun signup(email: String, password: String, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.signup(email, password, name)
                .onSuccess { 
                    _isLoggedIn.value = true 
                }
                .onFailure { 
                    _error.emit(it.message ?: "Signup failed")
                }
            _isLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _isLoggedIn.value = false
        }
    }

    class Factory(private val repository: AuthRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(repository) as T
        }
    }
}
