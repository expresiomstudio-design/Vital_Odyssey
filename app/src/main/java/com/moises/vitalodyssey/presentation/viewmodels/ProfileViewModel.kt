package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.domain.repository.AuthRepository
import com.moises.vitalodyssey.domain.repository.UserRepository
import com.moises.vitalodyssey.domain.usecase.CalculatePlayerStatsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileUiState(
    val playerName: String = "",
    val playerEmail: String = "",
    val playerClass: String = "",
    val level: Int = 1,
    val currentHp: Int = 0,
    val maxHp: Int = 0,
    val attackStat: Int = 0,
    val defenseStat: Int = 0,
    val highestStreak: Int = 0,
    val bossesDefeatedCount: Int = 0,
    val isLoggedOut: Boolean = false
)

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val calculateStats: CalculatePlayerStatsUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoggedOut = MutableStateFlow(false)

    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.getUserProfile(),
        _isLoggedOut
    ) { profile, loggedOut ->
        if (profile == null) {
            ProfileUiState(isLoggedOut = loggedOut)
        } else {
            val stats = calculateStats(profile.level)
            ProfileUiState(
                playerName = profile.name,
                playerEmail = profile.email,
                playerClass = profile.playerClass?.name ?: "SIN CLASE",
                level = profile.level,
                currentHp = profile.currentHp,
                maxHp = stats.maxHp,
                attackStat = stats.baseAttack,
                defenseStat = stats.baseDefense,
                highestStreak = profile.highestStreak,
                bossesDefeatedCount = profile.bossesDefeated.size,
                isLoggedOut = loggedOut
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _isLoggedOut.value = true
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                userRepository.deleteUserAccount()
                authRepository.logout() // También cerramos sesión tras borrar
                _isLoggedOut.value = true
            } catch (e: Exception) {
                // En un caso real, podríamos exponer un error en el UI state
            }
        }
    }
}
