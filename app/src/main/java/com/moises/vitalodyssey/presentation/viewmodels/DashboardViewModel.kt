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
    val combatLog: String = "",
    val currentBoss: BossEntity? = null,
    val lastBattleResult: BattleResult? = null,
    val showBattleReport: Boolean = false,
    val bodyType: BodyType? = null,
    val playerClass: PlayerClass? = null,
    val playerName: String = "HÉROE",
    val developerMode: Boolean = false,
    val showNoOffensiveHabitsDialog: Boolean = false
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
    private val habitDao: HabitDao,
    private val userPrefsManager: com.moises.vitalodyssey.data.local.UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var currentLog = ""

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
            combine(
                userRepository.getUserProfile().filterNotNull(),
                userPrefsManager.developerModeFlow
            ) { profile, devMode ->
                profile to devMode
            }.collect { (profile, devMode) ->
                val stats = calculateStats(profile.level)
                val defMult = calculateDefenseMultiplierUseCase()
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
                        currentStamina = if (devMode) 9999 else profile.currentStamina,
                        presenceStreak = profile.presenceStreak,
                        attackStat = stats.baseAttack,
                        attackMultiplier = atkMult,
                        defenseStat = stats.baseDefense,
                        defenseMultiplier = defMult,
                        // combatLog se gestiona solo desde onAttackClicked — no sobreescribir aquí
                        bodyType = profile.bodyType,
                        playerClass = profile.playerClass,
                        playerName = profile.name.ifBlank { "HÉROE" },
                        developerMode = devMode
                    )
                }
            }
        }
    }

    fun onAttackClicked() {
        viewModelScope.launch {
            android.util.Log.d("CombatDebug", "=== onAttackClicked START ===")
            val currentState = _uiState.value
            val isDevMode = userPrefsManager.developerModeFlow.first()
            android.util.Log.d("CombatDebug", "devMode=$isDevMode stamina=${currentState.currentStamina}")
            
            // 1. Verificar hábitos ofensivos primero
            val habits = habitDao.getAllHabitsOnce()
            val today = java.time.LocalDate.now().toString()
            android.util.Log.d("CombatDebug", "Total habits from DB: ${habits.size}, today=$today")
            
            // Consultar los logs de hoy para cada hábito (fuente de verdad real de completado)
            val habitCompletionMap = mutableMapOf<Int, Boolean>()
            habits.forEach { h ->
                val todayLog = habitDao.getLogForDate(h.id, today)
                val isCompletedToday = todayLog != null && (
                    todayLog.state == com.moises.vitalodyssey.domain.model.HabitState.COMPLETED ||
                    todayLog.state == com.moises.vitalodyssey.domain.model.HabitState.COMPLETED_BY_PERIOD ||
                    todayLog.state == com.moises.vitalodyssey.domain.model.HabitState.CONTRIBUTED
                )
                habitCompletionMap[h.id] = isCompletedToday
                android.util.Log.d("CombatDebug", "  Habit[${h.id}]: name='${h.name}' role=${h.role} logState=${todayLog?.state} → completedToday=$isCompletedToday score=${h.score}")
            }
            
            val offTotal = habits.filter { it.role == com.moises.vitalodyssey.domain.model.HabitRole.OFFENSIVE }.size
            android.util.Log.d("CombatDebug", "Offensive habits count: $offTotal")
            
            if (offTotal == 0) {
                android.util.Log.d("CombatDebug", "BLOCKED: No offensive habits → showing Dialog popup")
                _uiState.update { it.copy(showNoOffensiveHabitsDialog = true) }
                return@launch
            }

            // 2. Verificar estamina
            if (!isDevMode && currentState.currentStamina < 33) {
                currentLog = "No tienes suficiente estamina (Foco Arcano) para atacar."
                android.util.Log.d("CombatDebug", "BLOCKED: Not enough stamina")
                _uiState.update { it.copy(combatLog = currentLog) }
                return@launch
            }

            // 3. Descontar Estamina inmediatamente
            val newStamina = if (isDevMode) 9999 else (currentState.currentStamina - 33).coerceAtLeast(0)

            // 4. Recopilar datos
            val user = userRepository.getUserProfile().firstOrNull() ?: run {
                android.util.Log.e("CombatDebug", "ABORT: user profile is null")
                return@launch
            }
            val boss = bossDao.getCurrentBoss() ?: run {
                android.util.Log.e("CombatDebug", "ABORT: boss is null")
                return@launch
            }
            val playerStats = calculateStats(user.level)
            android.util.Log.d("CombatDebug", "User: level=${user.level} hp=${user.currentHp} xp=${user.currentXp}")
            android.util.Log.d("CombatDebug", "Boss: name=${boss.name} hp=${boss.currentHp} atk=${boss.baseAttack}")
            android.util.Log.d("CombatDebug", "PlayerStats: baseAttack=${playerStats.baseAttack} baseDef=${playerStats.baseDefense} maxHp=${playerStats.maxHp}")
            
            // Estadísticas reales de combate usando HabitLog de hoy
            val offCompleted = habits.filter { it.role == com.moises.vitalodyssey.domain.model.HabitRole.OFFENSIVE && habitCompletionMap[it.id] == true }.size
            val defTotal = habits.filter { it.role == com.moises.vitalodyssey.domain.model.HabitRole.DEFENSIVE }.size
            val defCompleted = habits.filter { it.role == com.moises.vitalodyssey.domain.model.HabitRole.DEFENSIVE && habitCompletionMap[it.id] == true }.size
            val maxOffensiveScore = habits.filter { it.role == com.moises.vitalodyssey.domain.model.HabitRole.OFFENSIVE }.map { it.score }.maxOrNull()?.toInt() ?: 0
            android.util.Log.d("CombatDebug", "Combat stats: offTotal=$offTotal offCompleted=$offCompleted defTotal=$defTotal defCompleted=$defCompleted maxOffScore=$maxOffensiveScore streak=${user.presenceStreak}")

            // 5. Calcular Turno (Ataque Manual = true)
            val battleResult = calculateBattleTurn(
                playerStats = playerStats,
                bossAttack = boss.baseAttack,
                offensiveHabitsTotal = offTotal,
                offensiveHabitsCompleted = offCompleted,
                defensiveHabitsTotal = defTotal,
                defensiveHabitsCompleted = defCompleted,
                isManualAttack = true,
                maxOffensiveScore = maxOffensiveScore,
                presenceStreak = user.presenceStreak
            )
            android.util.Log.d("CombatDebug", "BattleResult: dmgToBoss=${battleResult.damageDealtToBoss} dmgFromBoss=${battleResult.damageReceivedFromBoss} healed=${battleResult.hpHealed} xp=${battleResult.xpEarned}")

            // 6. Procesar y guardar el resultado
            val updatedState = processBattleResult(
                currentLevel = user.level,
                currentXp = user.currentXp,
                currentHp = user.currentHp,
                battleResult = battleResult,
                bossesDefeatedCount = user.bossesDefeated.size
            )
            android.util.Log.d("CombatDebug", "ProcessResult: newLevel=${updatedState.newLevel} newHp=${updatedState.newHp} newXp=${updatedState.newXp} fainted=${updatedState.isFainted} levelUp=${updatedState.didLevelUp} bossDefeated=${updatedState.bossDefeated}")

            currentLog = if (updatedState.didLevelUp) {
                "¡NIVEL ${updatedState.newLevel} ALCANZADO! Tu voluntad se fortalece."
            } else if (updatedState.isFainted) {
                "Te has desmayado. El jefe recupera fuerzas. Mañana será otro día."
            } else {
                "El Jefe ataca (${battleResult.damageReceivedFromBoss} DMG). Te curas ${battleResult.hpHealed} HP."
            }

            // Actualizar rachas y contadores de jefes
            val newPresenceStreak = if (updatedState.isFainted) 0 else user.presenceStreak + 1
            val newHighestStreak = maxOf(user.highestStreak, newPresenceStreak)
            val newBossesDefeated = if (updatedState.bossDefeated) {
                user.bossesDefeated + boss.name
            } else {
                user.bossesDefeated
            }

            // 7. Actualizar el Usuario en BD con los nuevos valores de vida y XP
            userRepository.updateStats(
                user.copy(
                    level = updatedState.newLevel,
                    currentXp = updatedState.newXp,
                    currentHp = updatedState.newHp,
                    presenceStreak = newPresenceStreak,
                    highestStreak = newHighestStreak,
                    bossesDefeated = newBossesDefeated,
                    currentStamina = if (isDevMode) user.currentStamina else newStamina,
                    lastUpdated = System.currentTimeMillis()
                )
            )

            // 8. Actualizar UI y mostrar el reporte de batalla
            android.util.Log.d("CombatDebug", "Updating UI: showBattleReport=true combatLog=$currentLog")
            _uiState.update { it.copy(
                currentStamina = newStamina,
                lastBattleResult = battleResult,
                showBattleReport = true,
                combatLog = currentLog
            )}
            android.util.Log.d("CombatDebug", "=== onAttackClicked END ===")
        }
    }

    fun dismissBattleReport() {
        _uiState.update { it.copy(showBattleReport = false, lastBattleResult = null) }
    }

    fun dismissNoOffensiveHabitsDialog() {
        _uiState.update { it.copy(showNoOffensiveHabitsDialog = false) }
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
