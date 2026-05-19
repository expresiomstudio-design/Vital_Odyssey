package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.data.local.BossDao
import com.moises.vitalodyssey.data.local.BossEntity
import com.moises.vitalodyssey.data.local.Difficulty
import com.moises.vitalodyssey.data.local.HabitDao
import com.moises.vitalodyssey.domain.model.BodyType
import com.moises.vitalodyssey.domain.model.PlayerClass
import com.moises.vitalodyssey.domain.repository.UserRepository
import com.moises.vitalodyssey.domain.usecase.*
import com.moises.vitalodyssey.domain.usecase.apprules.CalculateFocoArcanoUseCase
import com.moises.vitalodyssey.domain.usecase.health.CalculateDefenseMultiplierUseCase
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
    val attackMultiplier: Float = 1.0f,
    val defenseStat: Int = 10,
    val defenseMultiplier: Float = 1.0f,
    val combatLog: String = "La noche es oscura, pero tu voluntad es de hierro.",
    val currentBoss: BossEntity? = null,
    val lastBattleResult: BattleResult? = null,
    val showBattleReport: Boolean = false,
    val bodyType: BodyType? = null,
    val playerClass: PlayerClass? = null,
    val playerName: String = "HÉROE"
)

class DashboardViewModel(
    private val userRepository: UserRepository,
    private val calculateStats: CalculatePlayerStatsUseCase,
    private val calculateBossStats: CalculateBossStatsUseCase,
    private val calculateBattleTurn: CalculateBattleTurnUseCase,
    private val processBattleResult: ProcessBattleResultUseCase,
    private val calculateFocoArcano: CalculateFocoArcanoUseCase,
    private val calculateDefenseMultiplierUseCase: CalculateDefenseMultiplierUseCase,
    private val checkAndSeedInitialBossUseCase: CheckAndSeedInitialBossUseCase,
    private val bossDao: BossDao,
    private val habitDao: HabitDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var currentLog = "La noche es oscura, pero tu voluntad es de hierro."

    init {
        viewModelScope.launch {
            // 1. Obtenemos el perfil del usuario (para saber su nivel)
            val user = userRepository.getUserProfile().firstOrNull()
            val playerLevel = user?.level ?: 1
            
            // 2. Ejecutamos el Seed (si ya hay jefe, esta función no hará nada)
            checkAndSeedInitialBossUseCase(playerLevel)

            // 3. Comenzamos a observar los datos del jefe para la UI
            bossDao.getCurrentBossFlow().collect { boss ->
                _uiState.update { it.copy(currentBoss = boss) }
            }
        }
        
        viewModelScope.launch {
            userRepository.getUserProfile().collect { profile ->
                if (profile == null) return@collect
                
                val stats = calculateStats(profile.level)
                val defMult = calculateDefenseMultiplierUseCase()
                
                // Mocking attackMultiplier until a UseCase is available
                val atkMult = 1.2f

                val hpRange = (stats.maxHp - stats.faintHp).toFloat()
                val currentVisualHp = (profile.currentHp - stats.faintHp).coerceAtLeast(0).toFloat()
                val hpPercent = if (hpRange > 0) currentVisualHp / hpRange else 0f

                val xpPercent = if (stats.xpForNextLevel > 0) {
                    profile.currentXp.toFloat() / stats.xpForNextLevel.toFloat()
                } else 0f

                _uiState.update { currentState ->
                    currentState.copy(
                        level = profile.level,
                        hpText = "${profile.currentHp} / ${stats.maxHp} HP",
                        visualHpPercent = hpPercent,
                        xpText = "${profile.currentXp} / ${stats.xpForNextLevel} XP",
                        visualXpPercent = xpPercent,
                        currentStamina = profile.currentStamina,
                        presenceStreak = profile.presenceStreak,
                        attackStat = stats.baseAttack,
                        attackMultiplier = atkMult,
                        defenseStat = stats.baseDefense,
                        defenseMultiplier = defMult,
                        combatLog = currentLog,
                        bodyType = profile.bodyType,
                        playerClass = profile.playerClass,
                        playerName = profile.name.ifBlank { "HÉROE" }
                    )
                }
            }
        }
    }

    fun onAttackClicked() {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState.currentStamina < 33) {
                currentLog = "No tienes suficiente estamina (Foco Arcano) para atacar."
                _uiState.update { it.copy(combatLog = currentLog) }
                return@launch
            }

            // 1. Descontar Estamina inmediatamente
            val newStamina = (currentState.currentStamina - 33).coerceAtLeast(0)

            // 2. Recopilar datos
            val user = userRepository.getUserProfile().firstOrNull() ?: return@launch
            val boss = bossDao.getCurrentBoss() ?: return@launch
            val playerStats = calculateStats(user.level)
            
            // TODO: Obtener completitud real de la BD. Por ahora simulamos 80% ofensivo y 50% defensivo
            val offTotal = 5; val offCompleted = 4
            val defTotal = 2; val defCompleted = 1

            // 3. Calcular Turno (Ataque Manual = true)
            val battleResult = calculateBattleTurn(
                playerStats = playerStats,
                bossAttack = boss.baseAttack,
                offensiveHabitsTotal = offTotal,
                offensiveHabitsCompleted = offCompleted,
                defensiveHabitsTotal = defTotal,
                defensiveHabitsCompleted = defCompleted,
                isManualAttack = true,
                maxOffensiveScore = 100, // TODO: Reemplazar por max score real
                presenceStreak = user.presenceStreak
            )

            // 4. Procesar y guardar el resultado
            val updatedState = processBattleResult(
                currentLevel = user.level,
                currentXp = user.currentXp,
                currentHp = user.currentHp,
                battleResult = battleResult
            )

            currentLog = if (updatedState.didLevelUp) {
                "¡NIVEL ${updatedState.newLevel} ALCANZADO! Tu voluntad se fortalece."
            } else if (updatedState.isFainted) {
                "Te has desmayado. El jefe recupera fuerzas. Mañana será otro día."
            } else {
                "El Jefe ataca (${battleResult.damageReceivedFromBoss} DMG). Te curas ${battleResult.hpHealed} HP."
            }

            // 5. Actualizar el Usuario en BD con los nuevos valores de vida y XP
            userRepository.updateStats(
                user.copy(
                    level = updatedState.newLevel,
                    currentXp = updatedState.newXp,
                    currentHp = updatedState.newHp,
                    presenceStreak = if (updatedState.isFainted) 0 else user.presenceStreak + 1,
                    currentStamina = newStamina
                )
            )

            // 6. Actualizar UI y mostrar el reporte de batalla
            _uiState.update { it.copy(
                currentStamina = newStamina,
                lastBattleResult = battleResult,
                showBattleReport = true,
                combatLog = currentLog
            )}
        }
    }

    fun dismissBattleReport() {
        _uiState.update { it.copy(showBattleReport = false, lastBattleResult = null) }
    }

    fun dailyReset() {
        viewModelScope.launch {
            val currentProfile = userRepository.getUserProfile().firstOrNull() ?: return@launch
            val appFocusPercentage = calculateFocoArcano()
            
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
