package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.data.local.UserPreferencesManager
import com.moises.vitalodyssey.domain.repository.HealthRepository
import com.moises.vitalodyssey.domain.repository.UserRepository
import com.moises.vitalodyssey.domain.usecase.health.CalculateDefenseMultiplierUseCase
import com.moises.vitalodyssey.domain.usecase.health.GetYesterdayHealthStatsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HealthUiState(
    val isAutomatic: Boolean = false,
    val hasPermissions: Boolean = false,
    val steps: Long = 0L,
    val sleepHours: Float = 0f,
    val defenseMultiplier: Float = 1.0f,
    val showPermissionDialog: Boolean = false,
    val showManualDialog: Boolean = false,
    val hasConfiguredHealthGoals: Boolean = true,
    val showHealthGoalsDialog: Boolean = false,
    val currentStepGoal: Int = 8000,
    val currentSleepGoal: Float = 7.5f
)

class HealthViewModel(
    private val healthRepository: HealthRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val userRepository: UserRepository,
    private val getYesterdayHealthStats: GetYesterdayHealthStatsUseCase,
    private val calculateDefenseMultiplier: CalculateDefenseMultiplierUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        // Verificar permisos de Health Connect
        viewModelScope.launch {
            val granted = healthRepository.hasAllPermissions()
            _uiState.update { it.copy(hasPermissions = granted) }
        }

        // Observar el toggle Automático / Manual
        viewModelScope.launch {
            userPreferencesManager.healthConnectEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(isAutomatic = enabled) }
                loadHealthData()
            }
        }

        viewModelScope.launch {
            userPreferencesManager.hasConfiguredHealthGoalsFlow.collect { configured ->
                _uiState.update { it.copy(hasConfiguredHealthGoals = configured) }
            }
        }

        viewModelScope.launch {
            userPreferencesManager.stepGoalFlow.collect { steps ->
                _uiState.update { it.copy(currentStepGoal = steps) }
            }
        }

        viewModelScope.launch {
            userPreferencesManager.sleepGoalFlow.collect { sleep ->
                _uiState.update { it.copy(currentSleepGoal = sleep) }
            }
        }
    }

    /**
     * Carga los datos de salud de ayer y calcula el multiplicador de defensa.
     * Se invoca tras cada cambio de modo o al recibir un reporte manual.
     */
    private fun loadHealthData() {
        viewModelScope.launch {
            val stats = getYesterdayHealthStats()
            val multiplier = calculateDefenseMultiplier()
            _uiState.update {
                it.copy(
                    steps = stats.steps,
                    sleepHours = stats.sleepHours,
                    defenseMultiplier = multiplier
                )
            }
        }
    }

    // ── Acciones del usuario ──────────────────────────────────────────────────

    /**
     * Alterna entre modo Automático (Health Connect) y Manual.
     * Si el usuario intenta activar el modo automático sin permisos,
     * se muestra el diálogo de solicitud de permisos en lugar de activar.
     */
    fun onToggleAutomaticMode(enabled: Boolean) {
        if (enabled && !_uiState.value.hasPermissions) {
            _uiState.update { it.copy(showPermissionDialog = true) }
            return
        }
        viewModelScope.launch {
            userPreferencesManager.setHealthConnectEnabled(enabled)
            // El collect de healthConnectEnabledFlow se encargará de actualizar y recargar
        }
    }

    /**
     * Maneja el resultado de la solicitud de permisos de Health Connect.
     * Si se otorgaron, activa el modo automático; si no, permanece manual.
     */
    fun onPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                showPermissionDialog = false,
                hasPermissions = granted
            )
        }
        if (granted) {
            viewModelScope.launch {
                userPreferencesManager.setHealthConnectEnabled(true)
            }
        }
    }

    /** Muestra u oculta el diálogo de reporte manual. */
    fun toggleManualDialog(show: Boolean) {
        _uiState.update { it.copy(showManualDialog = show) }
    }

    /**
     * Envía un reporte manual con los pasos y horas de sueño del jugador.
     * La fecha se calcula automáticamente como "ayer" para ser consistente
     * con la lógica del [GetYesterdayHealthStatsUseCase].
     */
    fun submitManualReport(steps: Long, sleep: Float) {
        viewModelScope.launch {
            val yesterday = LocalDate.now().minusDays(1).toString()
            userPreferencesManager.setManualHealthReport(
                steps = steps,
                sleep = sleep,
                date = yesterday
            )
            _uiState.update { it.copy(showManualDialog = false) }
            loadHealthData()
        }
    }

    fun toggleHealthGoalsDialog(show: Boolean) {
        _uiState.update { it.copy(showHealthGoalsDialog = show) }
    }

    fun submitHealthGoals(steps: Int, sleep: Float) {
        viewModelScope.launch {
            // 1. Guardar en DataStore (local y reactivo)
            userPreferencesManager.updateHealthGoals(steps, sleep)

            // 2. Guardar en Room + sincronizar con Firestore
            val currentProfile = userRepository.getUserProfileOnce()
            if (currentProfile != null) {
                val updatedProfile = currentProfile.copy(
                    stepGoal = steps,
                    sleepGoal = sleep,
                    lastUpdated = System.currentTimeMillis()
                )
                userRepository.updateStats(updatedProfile)
            }

            _uiState.update { it.copy(showHealthGoalsDialog = false) }
            loadHealthData() // Recalculate multiplier with new goals
        }
    }
}
