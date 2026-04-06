package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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
    val currentXp: Int = 0,
    val xpForNextLevel: Int = 0,
    val presenceStreak: Int = 0,
    val highestStreak: Int = 0,
    val bossesDefeatedCount: Int = 0,
    val startOfWeek: String = "MONDAY",
    val cutoffTime: String = "00:00",
    val loginProvider: String = "password", // "google.com" o "password"
    val isSaving: Boolean = false,
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
    private val _isSaving = MutableStateFlow(false)

    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.getUserProfile(),
        _isLoggedOut,
        _showDeleteSuccess,
        _deleteProgress,
        _isSaving
    ) { profile, loggedOut, success, progress, saving ->
        if (profile == null && !success) {
            ProfileUiState(isLoggedOut = loggedOut)
        } else {
            val stats = profile?.level?.let { calculateStats(it) }
            val provider = FirebaseAuth.getInstance().currentUser?.providerData?.lastOrNull()?.providerId ?: "password"

            ProfileUiState(
                playerName = profile?.name ?: "Usuario",
                playerEmail = profile?.email ?: "",
                playerClass = profile?.playerClass?.name ?: "SIN CLASE",
                level = profile?.level ?: 1,
                currentHp = profile?.currentHp ?: 0,
                maxHp = stats?.maxHp ?: 0,
                attackStat = stats?.baseAttack ?: 0,
                defenseStat = stats?.baseDefense ?: 0,
                currentXp = profile?.currentXp ?: 0,
                xpForNextLevel = stats?.xpForNextLevel ?: 0,
                presenceStreak = profile?.presenceStreak ?: 0,
                highestStreak = profile?.highestStreak ?: 0,
                bossesDefeatedCount = profile?.bossesDefeated?.size ?: 0,
                startOfWeek = profile?.startOfWeek ?: "MONDAY",
                cutoffTime = profile?.cutoffTime ?: "00:00",
                loginProvider = provider,
                isSaving = saving,
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

    fun updateName(newName: String) {
        viewModelScope.launch {
            _isSaving.value = true
            val currentProfile = userRepository.getUserProfileOnce()
            if (currentProfile != null) {
                userRepository.updateStats(currentProfile.copy(name = newName))
            }
            _isSaving.value = false
        }
    }

    fun updateStartOfWeek(day: String) {
        viewModelScope.launch {
            val currentProfile = userRepository.getUserProfileOnce()
            if (currentProfile != null) {
                userRepository.updateStats(currentProfile.copy(startOfWeek = day))
            }
        }
    }

    fun updateCutoffTime(time: String) {
        viewModelScope.launch {
            val currentProfile = userRepository.getUserProfileOnce()
            if (currentProfile != null) {
                userRepository.updateStats(currentProfile.copy(cutoffTime = time))
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                userRepository.deleteUserAccount()
                
                // Si llegamos aquí, se borró con éxito
                _showDeleteSuccess.value = true
                val duration = 3000L
                val steps = 30
                val stepDuration = duration / steps
                for (i in 1..steps) {
                    kotlinx.coroutines.delay(stepDuration)
                    _deleteProgress.value = i.toFloat() / steps.toFloat()
                }
                _isLoggedOut.value = true
            } catch (e: Exception) {
                // Manejo de errores: Si falla por seguridad o sesión expirada, 
                // al menos forzamos el logout para que el usuario no se quede en un limbo
                _isLoggedOut.value = true
            }
        }
    }
}
