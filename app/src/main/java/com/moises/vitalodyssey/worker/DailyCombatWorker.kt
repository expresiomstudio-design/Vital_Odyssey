package com.moises.vitalodyssey.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moises.vitalodyssey.data.local.BossDao
import com.moises.vitalodyssey.data.local.HabitDao
import com.moises.vitalodyssey.domain.repository.UserRepository
import com.moises.vitalodyssey.domain.model.HabitRole
import com.moises.vitalodyssey.domain.usecase.CalculateBattleTurnUseCase
import com.moises.vitalodyssey.domain.usecase.CalculatePlayerStatsUseCase
import com.moises.vitalodyssey.domain.usecase.ProcessBattleResultUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.CalculateFocoArcanoUseCase
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DailyCombatWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    // Inyectamos los casos de uso y repositorios necesarios
    private val calculateBattleTurn: CalculateBattleTurnUseCase by inject()
    private val processBattleResult: ProcessBattleResultUseCase by inject()
    private val calculateStats: CalculatePlayerStatsUseCase by inject()
    private val calculateFocoArcano: CalculateFocoArcanoUseCase by inject()
    
    private val userRepository: UserRepository by inject()
    private val bossDao: BossDao by inject()
    private val habitDao: HabitDao by inject()

    override suspend fun doWork(): Result {
        try {
            // 1. Recopilar datos
            val user = userRepository.getUserProfile().firstOrNull() ?: return Result.failure()
            val boss = bossDao.getCurrentBoss() ?: return Result.success()
            val playerStats = calculateStats(user.level)

            // Obtener hábitos de la base de datos local para estadísticas reales de combate
            val habits = habitDao.getAllHabitsOnce()
            val today = java.time.LocalDate.now().toString()
            
            // Consultar HabitLog de hoy para cada hábito (misma fuente de verdad que la UI)
            val completionMap = mutableMapOf<Int, Boolean>()
            habits.forEach { h ->
                val log = habitDao.getLogForDate(h.id, today)
                completionMap[h.id] = log != null && (
                    log.state == com.moises.vitalodyssey.domain.model.HabitState.COMPLETED ||
                    log.state == com.moises.vitalodyssey.domain.model.HabitState.COMPLETED_BY_PERIOD ||
                    log.state == com.moises.vitalodyssey.domain.model.HabitState.CONTRIBUTED
                )
            }
            
            val offTotal = habits.filter { it.role == HabitRole.OFFENSIVE }.size
            val offCompleted = habits.filter { it.role == HabitRole.OFFENSIVE && completionMap[it.id] == true }.size
            val defTotal = habits.filter { it.role == HabitRole.DEFENSIVE }.size
            val defCompleted = habits.filter { it.role == HabitRole.DEFENSIVE && completionMap[it.id] == true }.size
            val maxOffensiveScore = habits.filter { it.role == HabitRole.OFFENSIVE }.map { it.score }.maxOrNull()?.toInt() ?: 0

            // 2. Calcular Turno Automático (isManualAttack = false)
            val battleResult = calculateBattleTurn(
                playerStats = playerStats,
                bossAttack = boss.baseAttack,
                offensiveHabitsTotal = offTotal,
                offensiveHabitsCompleted = offCompleted,
                defensiveHabitsTotal = defTotal,
                defensiveHabitsCompleted = defCompleted,
                isManualAttack = false, // ¡Sin bono de presencia!
                maxOffensiveScore = maxOffensiveScore,
                presenceStreak = user.presenceStreak
            )
            
            // 3. Procesar y guardar en BD el resultado del combate
            val updatedState = processBattleResult(
                currentLevel = user.level,
                currentXp = user.currentXp,
                currentHp = user.currentHp,
                battleResult = battleResult,
                bossesDefeatedCount = user.bossesDefeated.size
            )

            // 4. Calcular restauración diaria de estamina y actualizar rachas/estadísticas
            val appFocusPercentage = calculateFocoArcano()
            val baseStamina = 34
            val streakBonus = (user.presenceStreak * 3).coerceAtMost(33)
            val focusBonus = (appFocusPercentage * 0.33f).toInt().coerceAtMost(33)
            val newStamina = (baseStamina + streakBonus + focusBonus).coerceAtMost(100)

            // Al finalizar el día lógico, si no atacó manualmente hoy, la racha vuelve a 0
            val newPresenceStreak = 0
            val newHighestStreak = maxOf(user.highestStreak, user.presenceStreak)
            val newBossesDefeated = if (updatedState.bossDefeated) {
                user.bossesDefeated + boss.name
            } else {
                user.bossesDefeated
            }
            
            // 5. Guardar nuevo estado en BD
            userRepository.updateStats(
                user.copy(
                    level = updatedState.newLevel,
                    currentXp = updatedState.newXp,
                    currentHp = updatedState.newHp,
                    presenceStreak = newPresenceStreak,
                    highestStreak = newHighestStreak,
                    bossesDefeated = newBossesDefeated,
                    currentStamina = newStamina,
                    lastUpdated = System.currentTimeMillis()
                )
            )

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
