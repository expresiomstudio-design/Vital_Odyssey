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
    val deleteProgress: Float = 0f,
    val bodyType: com.moises.vitalodyssey.domain.model.BodyType? = null,
    val playerClassEnum: com.moises.vitalodyssey.domain.model.PlayerClass? = null,
    val requiresReauth: Boolean = false,
    val reauthError: String? = null,
    val developerMode: Boolean = false,
    val updateError: String? = null,
    val updateSuccess: String? = null
)

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val calculateStats: CalculatePlayerStatsUseCase,
    private val authRepository: AuthRepository,
    private val userPrefsManager: com.moises.vitalodyssey.data.local.UserPreferencesManager
) : ViewModel() {

    private val _isLoggedOut = MutableStateFlow(false)
    private val _showDeleteSuccess = MutableStateFlow(false)
    private val _deleteProgress = MutableStateFlow(0f)
    private val _isSaving = MutableStateFlow(false)
    private val _requiresReauth = MutableStateFlow(false)
    private val _reauthError = MutableStateFlow<String?>(null)
    private val _updateError = MutableStateFlow<String?>(null)
    private val _updateSuccess = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.getUserProfile(),
        userPrefsManager.developerModeFlow,
        combine(_isLoggedOut, _showDeleteSuccess, _deleteProgress, _isSaving) { a, b, c, d -> arrayOf(a, b, c, d) },
        combine(_requiresReauth, _reauthError, _updateError, _updateSuccess) { a, b, c, d -> arrayOf(a, b, c, d) }
    ) { profile, devMode, flags1, flags2 ->
        val loggedOut = flags1[0] as Boolean
        val success = flags1[1] as Boolean
        val progress = flags1[2] as Float
        val saving = flags1[3] as Boolean
        val reauth = flags2[0] as Boolean
        val reauthErr = flags2[1] as String?
        val updateErr = flags2[2] as String?
        val updateSucc = flags2[3] as String?

        if (profile == null && !success) {
            ProfileUiState(isLoggedOut = loggedOut, developerMode = devMode)
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
                deleteProgress = progress,
                bodyType = profile?.bodyType,
                playerClassEnum = profile?.playerClass,
                requiresReauth = reauth,
                reauthError = reauthErr,
                developerMode = devMode,
                updateError = updateErr,
                updateSuccess = updateSucc
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )

    fun logout() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // CRÍTICO: Subir TODOS los datos a la nube antes de cerrar sesión
                userRepository.performFullCloudSync()
            } catch (e: Exception) {
                // Si falla la sincronización (sin red, etc.), igual cerramos sesión
                // pero los datos podrían no haberse guardado
                android.util.Log.e("ProfileViewModel", "Sync before logout failed: ${e.message}")
            } finally {
                authRepository.logout()
                _isSaving.value = false
                _isLoggedOut.value = true
            }
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
            } catch (e: FirebaseAuthRecentLoginRequiredException) {
                _requiresReauth.value = true
                _reauthError.value = null
            } catch (e: Exception) {
                // Manejo de errores: Si falla por seguridad o sesión expirada, 
                // al menos forzamos el logout para que el usuario no se quede en un limbo
                _isLoggedOut.value = true
            }
        }
    }

    fun dismissReauthDialog() {
        _requiresReauth.value = false
        _reauthError.value = null
    }

    fun reauthenticateAndDelete(passwordOrToken: String, isGoogle: Boolean) {
        viewModelScope.launch {
            _reauthError.value = null
            val result = if (isGoogle) {
                authRepository.reauthenticateWithGoogle(passwordOrToken)
            } else {
                authRepository.reauthenticateWithEmail(passwordOrToken)
            }

            if (result.isSuccess) {
                _requiresReauth.value = false
                deleteAccount() // Volvemos a intentar el borrado
            } else {
                _reauthError.value = result.errorMessage ?: "Error al reautenticar"
            }
        }
    }

    fun setDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            userPrefsManager.setDeveloperMode(enabled)
        }
    }

    fun dismissUpdateMessages() {
        _updateError.value = null
        _updateSuccess.value = null
    }

    fun updateEmail(currentPassword: String, newEmail: String) {
        viewModelScope.launch {
            _isSaving.value = true
            _updateError.value = null
            _updateSuccess.value = null
            
            val reauthResult = authRepository.reauthenticateWithEmail(currentPassword)
            if (!reauthResult.isSuccess) {
                _updateError.value = "Contraseña actual incorrecta."
                _isSaving.value = false
                return@launch
            }
            
            val updateResult = authRepository.updateEmail(newEmail)
            if (updateResult.isSuccess) {
                _updateSuccess.value = "Correo electrónico actualizado con éxito."
                // Si el perfil guarda el email localmente, actualizarlo:
                val currentProfile = userRepository.getUserProfileOnce()
                if (currentProfile != null) {
                    userRepository.updateStats(currentProfile.copy(email = newEmail))
                }
            } else {
                _updateError.value = updateResult.errorMessage ?: "Error al actualizar el correo."
            }
            _isSaving.value = false
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _isSaving.value = true
            _updateError.value = null
            _updateSuccess.value = null
            
            val reauthResult = authRepository.reauthenticateWithEmail(currentPassword)
            if (!reauthResult.isSuccess) {
                _updateError.value = "Contraseña actual incorrecta."
                _isSaving.value = false
                return@launch
            }
            
            val updateResult = authRepository.updatePassword(newPassword)
            if (updateResult.isSuccess) {
                _updateSuccess.value = "Contraseña actualizada con éxito."
            } else {
                _updateError.value = updateResult.errorMessage ?: "Error al actualizar la contraseña."
            }
            _isSaving.value = false
        }
    }
}
