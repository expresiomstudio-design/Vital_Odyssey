package com.moises.vitalodyssey.domain.usecase.health

import com.moises.vitalodyssey.data.local.UserPreferencesManager
import kotlinx.coroutines.flow.first

/**
 * Calcula el multiplicador de defensa del escudo basándose en el cumplimiento
 * de las metas de salud del día anterior.
 *
 * ## Tabla de resultados
 *
 * ### Modo Automático (Health Connect)
 * | Condición                                  | Multiplicador |
 * |--------------------------------------------|---------------|
 * | stepPct < 30 % **ó** sleepPct < 30 %       | **0.8×**  (abandono extremo) |
 * | stepPct ≥ 100 % **y** sleepPct ≥ 100 %     | **1.5×**  (bono perfecto)    |
 * | stepPct ≥ 100 % **ó** sleepPct ≥ 100 %     | **1.2×**  (bono parcial)     |
 * | resto                                      | **1.0×**  (neutro)           |
 *
 * ### Modo Manual
 * | Condición               | Multiplicador |
 * |-------------------------|---------------|
 * | `isReported == true`    | **1.2×**  (premio por disciplina) |
 * | `isReported == false`   | **0.8×**  (penalización)          |
 */
open class CalculateDefenseMultiplierUseCase(
    private val getYesterdayHealthStatsUseCase: GetYesterdayHealthStatsUseCase,
    private val userPreferencesManager: UserPreferencesManager
) {

    open suspend operator fun invoke(): Float {
        val stats = getYesterdayHealthStatsUseCase()
        val isAutomatic = userPreferencesManager.healthConnectEnabledFlow.first()

        return if (isAutomatic) {
            val stepGoal = userPreferencesManager.stepGoalFlow.first()
            val sleepGoal = userPreferencesManager.sleepGoalFlow.first()

            val stepPct = if (stepGoal > 0) stats.steps.toFloat() / stepGoal else 0f
            val sleepPct = if (sleepGoal > 0f) stats.sleepHours / sleepGoal else 0f

            when {
                // Regla 1: Penalización estricta — abandono extremo en cualquier métrica
                stepPct < 0.3f || sleepPct < 0.3f -> 0.8f

                // Regla 2: Bono perfecto — cumplió ambas metas al 100 %
                stepPct >= 1.0f && sleepPct >= 1.0f -> 1.5f

                // Regla 3: Bono parcial — cumplió al menos una al 100 %
                stepPct >= 1.0f || sleepPct >= 1.0f -> 1.2f

                // Regla 4: Neutro — progreso entre 30 % y 99 % en ambas
                else -> 1.0f
            }
        } else {
            // Modo Manual: se premia la disciplina de reportar, se penaliza el olvido
            if (stats.isReported) 1.2f else 0.8f
        }
    }
}
