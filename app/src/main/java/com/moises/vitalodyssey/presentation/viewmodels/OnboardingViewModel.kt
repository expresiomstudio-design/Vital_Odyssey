package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.moises.vitalodyssey.domain.model.BodyType
import com.moises.vitalodyssey.domain.model.PlayerClass
import com.moises.vitalodyssey.domain.model.UserProfile
import com.moises.vitalodyssey.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val name: String = "",
    val selectedBodyType: BodyType? = null,
    val selectedPlayerClass: PlayerClass? = null,
    val isCompleted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class OnboardingViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        val currentUser = FirebaseAuth.getInstance().currentUser
        _uiState.update { it.copy(name = currentUser?.displayName ?: "") }
    }

    fun onNameChange(newName: String) {
        _uiState.update { it.copy(name = newName, errorMessage = null) }
    }

    fun selectBodyType(bodyType: BodyType) {
        _uiState.update { it.copy(selectedBodyType = bodyType, errorMessage = null) }
    }

    fun selectPlayerClass(playerClass: PlayerClass) {
        _uiState.update { it.copy(selectedPlayerClass = playerClass, errorMessage = null) }
    }

    fun triggerError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun completeOnboarding() {
        val currentState = _uiState.value
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""

        if (currentState.name.isBlank()) {
            triggerError("Debes escribir un nombre para tu héroe.")
            return
        }
        if (currentState.selectedBodyType == null) {
            triggerError("Debes seleccionar un tipo de cuerpo.")
            return
        }
        if (currentState.selectedPlayerClass == null) {
            triggerError("Debes elegir una clase para continuar.")
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val initialProfile = UserProfile(
            uid = uid,
            name = currentState.name,
            email = email,
            bodyType = currentState.selectedBodyType,
            playerClass = currentState.selectedPlayerClass,
            level = 1,
            currentXp = 0,
            currentHp = 1000,
            currentStamina = 100,
            presenceStreak = 0,
            highestStreak = 0,
            bossesDefeated = emptyList(),
            cutoffTime = "00:00",
            difficulty = "NORMAL",
            hasCompletedOnboarding = true
        )

        viewModelScope.launch {
            try {
                userRepository.updateStats(initialProfile)
                userRepository.syncUserToCloud()
                _uiState.update { it.copy(isCompleted = true, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error al guardar el perfil.") }
            }
        }
    }
}
