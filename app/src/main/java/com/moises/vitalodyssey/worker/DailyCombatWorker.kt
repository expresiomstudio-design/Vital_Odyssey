package com.moises.vitalodyssey.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moises.vitalodyssey.data.local.BossDao
import com.moises.vitalodyssey.data.local.HabitDao
import com.moises.vitalodyssey.domain.repository.UserRepository
import com.moises.vitalodyssey.domain.usecase.CalculateBattleTurnUseCase
import com.moises.vitalodyssey.domain.usecase.CalculatePlayerStatsUseCase
import com.moises.vitalodyssey.domain.usecase.ProcessBattleResultUseCase
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
    
    private val userRepository: UserRepository by inject()
    private val bossDao: BossDao by inject()
    private val habitDao: HabitDao by inject()

    override suspend fun doWork(): Result {
        try {
            // TODO: Consultar si el usuario YA atacó manualmente hoy. Si ya lo hizo, retornar Result.success()
            
            // 1. Recopilar datos
            val user = userRepository.getUserProfile().firstOrNull() ?: return Result.failure()
            val boss = bossDao.getCurrentBoss() ?: return Result.success()
            val playerStats = calculateStats(user.level)

            // TODO: Obtener completitud real de la BD. Por ahora simulamos
            val offTotal = 5; val offCompleted = 4
            val defTotal = 2; val defCompleted = 1

            // 2. Calcular Turno Automático (isManualAttack = false)
            val battleResult = calculateBattleTurn(
                playerStats = playerStats,
                bossAttack = boss.baseAttack,
                offensiveHabitsTotal = offTotal,
                offensiveHabitsCompleted = offCompleted,
                defensiveHabitsTotal = defTotal,
                defensiveHabitsCompleted = defCompleted,
                isManualAttack = false, // ¡Sin bono de presencia!
                maxOffensiveScore = 100, // TODO: Reemplazar por max score real
                presenceStreak = user.presenceStreak
            )
            
            // 3. Procesar y guardar en BD
            val updatedState = processBattleResult(
                currentLevel = user.level,
                currentXp = user.currentXp,
                currentHp = user.currentHp,
                battleResult = battleResult
            )
            
            // 4. Guardar nuevo estado en BD y perder la racha si no hubo combate manual (ajustable según reglas)
            userRepository.updateStats(
                user.copy(
                    level = updatedState.newLevel,
                    currentXp = updatedState.newXp,
                    currentHp = updatedState.newHp,
                    // TODO: Si el worker corre y no hubo ataque manual, probablemente se deba perder la racha de presencia.
                    presenceStreak = if (updatedState.isFainted) 0 else 0 
                )
            )

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
