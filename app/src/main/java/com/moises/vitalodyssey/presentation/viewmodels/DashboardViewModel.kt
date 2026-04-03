package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.data.local.Difficulty
import com.moises.vitalodyssey.domain.repository.UserRepository
import com.moises.vitalodyssey.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
    private val userRepository: UserRepository,
    private val calculateStats: CalculatePlayerStatsUseCase,
    private val calculateBossStats: CalculateBossStatsUseCase,
    private val calculateBattleTurn: CalculateBattleTurnUseCase,
    private val processBattleResult: ProcessBattleResultUseCase
) : ViewModel() {

    private var currentLog = "La noche es oscura, pero tu voluntad es de hierro."

    val uiState: StateFlow<DashboardUiState> = userRepository.getUserProfile().map { profile ->
        if (profile == null) return@map DashboardUiState()
        
        val stats = calculateStats(profile.level)

        val hpRange = (stats.maxHp - stats.faintHp).toFloat()
        val currentVisualHp = (profile.currentHp - stats.faintHp).coerceAtLeast(0).toFloat()
        val hpPercent = if (hpRange > 0) currentVisualHp / hpRange else 0f

        val xpPercent = if (stats.xpForNextLevel > 0) {
            profile.currentXp.toFloat() / stats.xpForNextLevel.toFloat()
        } else 0f

        DashboardUiState(
            level = profile.level,
            hpText = "${profile.currentHp} / ${stats.maxHp} HP",
            visualHpPercent = hpPercent,
            xpText = "${profile.currentXp} / ${stats.xpForNextLevel} XP",
            visualXpPercent = xpPercent,
            currentStamina = profile.currentStamina,
            presenceStreak = profile.presenceStreak,
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
            val currentProfile = userRepository.getUserProfile().firstOrNull() ?: return@launch

            if (currentProfile.currentStamina < 33) {
                currentLog = "No tienes suficiente estamina (Foco Arcano) para atacar."
                // Forzar actualización para que se vea el log si no hay cambio de datos
                userRepository.updateStats(currentProfile) 
                return@launch
            }

            val newStamina = currentProfile.currentStamina - 33
            val newStreak = currentProfile.presenceStreak + 1

            val bossAttack = calculateBossStats(currentProfile.level, Difficulty.NORMAL)
            val playerStats = calculateStats(currentProfile.level)

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

            val newState = processBattleResult(
                currentLevel = currentProfile.level,
                currentXp = currentProfile.currentXp,
                currentHp = currentProfile.currentHp,
                battleResult = battleResult
            )

            currentLog = if (newState.didLevelUp) {
                "¡NIVEL ${newState.newLevel} ALCANZADO! Tu voluntad se fortalece."
            } else if (newState.isFainted) {
                "Te has desmayado. El jefe recupera fuerzas. Mañana será otro día."
            } else {
                "El Jefe ataca (${battleResult.damageReceivedFromBoss} DMG). Te curas ${battleResult.hpHealed} HP."
            }

            val updatedProfile = currentProfile.copy(
                currentStamina = newStamina,
                currentHp = newState.newHp,
                currentXp = newState.newXp,
                level = newState.newLevel,
                presenceStreak = newStreak
            )
            
            userRepository.updateStats(updatedProfile)
        }
    }

    fun dailyReset(appFocusPercentage: Int) {
        viewModelScope.launch {
            val currentProfile = userRepository.getUserProfile().firstOrNull() ?: return@launch
            val streak = currentProfile.presenceStreak
            
            val baseStamina = 34
            val streakBonus = (streak * 3).coerceAtMost(33)
            val focusBonus = (appFocusPercentage * 0.33f).toInt().coerceAtMost(33)
            
            val newStamina = (baseStamina + streakBonus + focusBonus).coerceAtMost(100)
            
            val updatedProfile = currentProfile.copy(currentStamina = newStamina)
            userRepository.updateStats(updatedProfile)
        }
    }
}
