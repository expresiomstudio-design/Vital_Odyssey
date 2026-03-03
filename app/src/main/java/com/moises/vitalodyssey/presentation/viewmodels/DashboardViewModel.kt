package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.data.local.Difficulty
import com.moises.vitalodyssey.data.local.UserPreferencesManager
import com.moises.vitalodyssey.domain.usecase.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val level: Int = 1,
    val hpText: String = "1010 / 1010 HP",
    val visualHpPercent: Float = 1f,
    val xpText: String = "0 / 54 XP",
    val visualXpPercent: Float = 0f,
    val attackStat: Int = 101,
    val defenseStat: Int = 10,
    val combatLog: String = "La noche es oscura, pero tu voluntad es de hierro."
)

class DashboardViewModel(
    private val userPrefs: UserPreferencesManager,
    private val calculateStats: CalculatePlayerStatsUseCase,
    private val calculateBossStats: CalculateBossStatsUseCase,
    private val calculateBattleTurn: CalculateBattleTurnUseCase,
    private val processBattleResult: ProcessBattleResultUseCase
) : ViewModel() {

    // Variable temporal para el texto de la batalla
    private var currentLog = "La noche es oscura, pero tu voluntad es de hierro."

    val uiState: StateFlow<DashboardUiState> = userPrefs.userPrefsFlow.map { prefs ->
        val stats = calculateStats(prefs.level)

        val hpRange = (stats.maxHp - stats.faintHp).toFloat()
        val currentVisualHp = (prefs.currentHp - stats.faintHp).coerceAtLeast(0).toFloat()
        val hpPercent = if (hpRange > 0) currentVisualHp / hpRange else 0f

        val xpPercent = if (stats.xpForNextLevel > 0) {
            prefs.currentXp.toFloat() / stats.xpForNextLevel.toFloat()
        } else 0f

        DashboardUiState(
            level = prefs.level,
            hpText = "${prefs.currentHp} / ${stats.maxHp} HP",
            visualHpPercent = hpPercent,
            xpText = "${prefs.currentXp} / ${stats.xpForNextLevel} XP",
            visualXpPercent = xpPercent,
            attackStat = stats.baseAttack,
            defenseStat = stats.baseDefense,
            combatLog = currentLog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    // Función que llamará el botón de la UI para probar las matemáticas
    fun simulateAttack() {
        viewModelScope.launch {
            // Leemos el estado actual directamente
            val currentState = uiState.value
            val bossAttack = calculateBossStats(currentState.level, Difficulty.NORMAL)
            val playerStats = calculateStats(currentState.level)

            // Simulamos un jugador al 60% de cumplimiento
            val battleResult = calculateBattleTurn(
                playerStats = playerStats,
                bossAttack = bossAttack,
                offensiveHabitsTotal = 10,
                offensiveHabitsCompleted = 6,
                defensiveHabitsTotal = 2,
                defensiveHabitsCompleted = 1,
                isManualAttack = true,
                healthBonusMultiplier = 1.0f,
                currentStreak = 2
            )

            // Obtenemos cuánta vida tenía realmente en base de datos (hay que extraerlo de uiState)
            val currentHpInt = currentState.hpText.split(" / ")[0].toInt()
            val currentXpInt = currentState.xpText.split(" / ")[0].toInt()

            val newState = processBattleResult(
                currentLevel = currentState.level,
                currentXp = currentXpInt,
                currentHp = currentHpInt,
                battleResult = battleResult
            )

            // Actualizamos el texto del log
            currentLog = if (newState.didLevelUp) {
                "¡NIVEL ${newState.newLevel} ALCANZADO! Tu voluntad se fortalece."
            } else if (newState.isFainted) {
                "Te has desmayado. El jefe recupera fuerzas. Mañana será otro día."
            } else {
                "El Jefe ataca (${battleResult.damageReceivedFromBoss} DMG). Te curas ${battleResult.hpHealed} HP."
            }

            // Guardamos en DataStore (¡Esto actualiza la UI automáticamente!)
            userPrefs.updateLevelAndXp(newState.newLevel, newState.newXp)
            userPrefs.updateHp(newState.newHp)
        }
    }
}