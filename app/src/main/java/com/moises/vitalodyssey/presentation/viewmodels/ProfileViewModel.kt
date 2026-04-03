package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
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
    val isLoggedOut: Boolean = false,
    val showDeleteSuccess: Boolean = false,
    val deleteProgress: Float = 0f
)

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val calculateStats: CalculatePlayerStatsUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isLoggedOut = MutableStateFlow(false)
    private val _showDeleteSuccess = MutableStateFlow(false)
    private val _deleteProgress = MutableStateFlow(0f)

    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.getUserProfile(),
        _isLoggedOut,
        _showDeleteSuccess,
        _deleteProgress
    ) { profile, loggedOut, success, progress ->
        if (profile == null && !success) {
            ProfileUiState(isLoggedOut = loggedOut)
        } else {
            val stats = profile?.level?.let { calculateStats(it) }
            ProfileUiState(
                playerName = profile?.name ?: "Usuario",
                playerEmail = profile?.email ?: "",
                playerClass = profile?.playerClass?.name ?: "SIN CLASE",
                level = profile?.level ?: 1,
                currentHp = profile?.currentHp ?: 0,
                maxHp = stats?.maxHp ?: 0,
                attackStat = stats?.baseAttack ?: 0,
                defenseStat = stats?.baseDefense ?: 0,
                highestStreak = profile?.highestStreak ?: 0,
                bossesDefeatedCount = profile?.bossesDefeated?.size ?: 0,
                isLoggedOut = loggedOut,
                showDeleteSuccess = success,
                deleteProgress = progress
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
                // Iniciamos la fase de despedida
                _showDeleteSuccess.value = true
                
                // Simulación de barra de progreso/cuenta regresiva de 3 segundos
                val duration = 3000L
                val steps = 30
                val stepDuration = duration / steps
                
                for (i in 1..steps) {
                    kotlinx.coroutines.delay(stepDuration)
                    _deleteProgress.value = i.toFloat() / steps.toFloat()
                }
                
                // Al terminar el efecto, hacemos el logout real
                _isLoggedOut.value = true
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                // ...
            } catch (e: Exception) {
                // ...
            }
        }
    }
}
