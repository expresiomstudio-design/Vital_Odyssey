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
import kotlinx.coroutines.flow.first

data class DashboardUiState(
    val level: Int = 1,
    val hpText: String = "1010 / 1010 HP",
    val visualHpPercent: Float = 1f,
    val xpText: String = "0 / 54 XP",
    val visualXpPercent: Float = 0f,
    val currentStamina: Int = 100,
    val presenceStreak: Int = 0,
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
            currentStamina = prefs.currentStamina,
            presenceStreak = prefs.presenceStreak,
            attackStat = stats.baseAttack,
            defenseStat = stats.baseDefense,
            combatLog = currentLog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun simulateAttack() {
        viewModelScope.launch {
            val currentState = uiState.value

            if (currentState.currentStamina < 33) {
                currentLog = "No tienes suficiente estamina (Foco Arcano) para atacar."
                userPrefs.updateStamina(currentState.currentStamina)
                return@launch
            }

            userPrefs.updateStamina(currentState.currentStamina - 33)
            userPrefs.updatePresenceStreak(currentState.presenceStreak + 1)

            val bossAttack = calculateBossStats(currentState.level, Difficulty.NORMAL)
            val playerStats = calculateStats(currentState.level)

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

            val currentHpInt = currentState.hpText.split(" / ")[0].toInt()
            val currentXpInt = currentState.xpText.split(" / ")[0].toInt()

            val newState = processBattleResult(
                currentLevel = currentState.level,
                currentXp = currentXpInt,
                currentHp = currentHpInt,
                battleResult = battleResult
            )

            currentLog = if (newState.didLevelUp) {
                "¡NIVEL ${newState.newLevel} ALCANZADO! Tu voluntad se fortalece."
            } else if (newState.isFainted) {
                "Te has desmayado. El jefe recupera fuerzas. Mañana será otro día."
            } else {
                "El Jefe ataca (${battleResult.damageReceivedFromBoss} DMG). Te curas ${battleResult.hpHealed} HP."
            }

            userPrefs.updateLevelAndXp(newState.newLevel, newState.newXp)
            userPrefs.updateHp(newState.newHp)
        }
    }

    fun dailyReset(appFocusPercentage: Int) {
        viewModelScope.launch {
            val currentState = userPrefs.userPrefsFlow.first()
            val streak = currentState.presenceStreak
            
            val baseStamina = 34
            val streakBonus = (streak * 3).coerceAtMost(33)
            val focusBonus = (appFocusPercentage * 0.33f).toInt().coerceAtMost(33)
            
            val newStamina = (baseStamina + streakBonus + focusBonus).coerceAtMost(100)
            
            userPrefs.updateStamina(newStamina)
        }
    }
}