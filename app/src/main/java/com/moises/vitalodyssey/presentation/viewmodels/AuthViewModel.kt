package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.data.local.UserPreferencesManager
import com.moises.vitalodyssey.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false,
    val isLoginMode: Boolean = true
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: com.moises.vitalodyssey.domain.repository.UserRepository,
    private val userPrefs: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun toggleAuthMode() {
        _uiState.update { it.copy(isLoginMode = !it.isLoginMode, errorMessage = null) }
    }

    fun loginWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.loginWithEmail(email, pass)
            handleAuthResult(result)
        }
    }

    fun registerWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.registerWithEmail(email, pass)
            handleAuthResult(result)
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            android.util.Log.d("AuthDebug", "Iniciando login en Firebase...")
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.loginWithGoogle(idToken)
            android.util.Log.d("AuthDebug", "Resultado de Firebase: ${result.isSuccess}")
            handleAuthResult(result)
        }
    }

    fun onLoginNavigated() {
        _uiState.update { it.copy(loginSuccess = false) }
    }

    private suspend fun handleAuthResult(result: com.moises.vitalodyssey.domain.model.AuthResult) {
        if (result.isSuccess) {
            userRepository.syncDatabasesOnLogin()
            userPrefs.updateAuthStatus(true)
            _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
        } else {
            _uiState.update { it.copy(isLoading = false, errorMessage = result.errorMessage) }
        }
    }
}
