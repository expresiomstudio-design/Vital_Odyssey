package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.moises.vitalodyssey.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _startDestination = MutableStateFlow("login")
    val startDestination: StateFlow<String> = _startDestination.asStateFlow()

    init {
        checkInitialStateOptimistic()
    }

    suspend fun getDestinationAfterLogin(): String {
        return try {
            userRepository.fetchUserFromCloud()
            val profile = userRepository.getUserProfileOnce()
            if (profile?.hasCompletedOnboarding == true) "main_container" else "onboarding"
        } catch (e: Exception) {
            "onboarding"
        }
    }

    private fun checkInitialStateOptimistic() {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                _startDestination.value = "login"
            } else {
                // Sincronización robusta: intentamos traer datos de Firestore primero
                try {
                    userRepository.fetchUserFromCloud()
                } catch (e: Exception) {
                    // Si falla la red, confiaremos en lo que haya en Room (offline-first)
                }

                try {
                    // Ahora consultamos el perfil (que debería estar actualizado por fetchUserFromCloud)
                    val profile = userRepository.getUserProfileOnce()
                    
                    if (profile != null && profile.hasCompletedOnboarding) {
                        _startDestination.value = "main_container"
                    } else {
                        _startDestination.value = "onboarding"
                    }
                } catch (e: Exception) {
                    // Si Room falla (ej. error de migración), mandamos a onboarding o intentamos recuperarnos
                    _startDestination.value = "onboarding"
                }
            }
            
            // Solo pasamos a false cuando la decisión de navegación está tomada
            _isLoading.value = false
        }
    }
}
