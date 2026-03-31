package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.moises.vitalodyssey.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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

    private fun checkInitialStateOptimistic() {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                _startDestination.value = "login"
            } else {
                // Intentamos una lectura ultra rápida de Room (caché local)
                val profile = userRepository.getUserProfile().firstOrNull()
                
                when {
                    profile?.hasCompletedOnboarding == true -> {
                        _startDestination.value = "dashboard"
                    }
                    else -> {
                        // Si no hay perfil o no completó onboarding, va a onboarding
                        _startDestination.value = "onboarding"
                    }
                }
            }
            
            // Liberamos la UI de inmediato
            _isLoading.value = false
        }
    }
}
