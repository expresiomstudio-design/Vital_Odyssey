package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.data.local.UserPreferencesManager
import com.moises.vitalodyssey.domain.model.BodyType
import com.moises.vitalodyssey.domain.model.PlayerClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val selectedBodyType: BodyType? = null,
    val selectedPlayerClass: PlayerClass? = null,
    val isCompleted: Boolean = false
)

class OnboardingViewModel(
    private val userPrefs: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun selectBodyType(bodyType: BodyType) {
        _uiState.update { it.copy(selectedBodyType = bodyType) }
    }

    fun selectPlayerClass(playerClass: PlayerClass) {
        _uiState.update { it.copy(selectedPlayerClass = playerClass) }
    }

    fun saveCharacter() {
        val currentState = _uiState.value
        val bodyType = currentState.selectedBodyType
        val playerClass = currentState.selectedPlayerClass

        if (bodyType != null && playerClass != null) {
            viewModelScope.launch {
                userPrefs.completeOnboarding(bodyType, playerClass)
                _uiState.update { it.copy(isCompleted = true) }
            }
        }
    }
}
