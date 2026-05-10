package com.moises.vitalodyssey.domain.usecase.health

import com.moises.vitalodyssey.data.local.UserPreferencesManager
import com.moises.vitalodyssey.domain.model.HealthStatsResult
import com.moises.vitalodyssey.domain.repository.HealthRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Recupera las estadísticas de salud del día anterior decidiendo de forma inteligente
 * entre la fuente **automática** (Health Connect) y la fuente **manual** (reporte del jugador).
 *
 * ## Lógica de decisión
 * | `healthConnectEnabled` | Condición extra              | Resultado                              |
 * |------------------------|------------------------------|----------------------------------------|
 * | `true`  (Automático)   | —                            | Datos de Health Connect, isReported=true  |
 * | `false` (Manual)       | Fecha del reporte == ayer    | Datos manuales guardados, isReported=true |
 * | `false` (Manual)       | Fecha del reporte ≠ ayer     | 0L / 0f, isReported=false              |
 */
class GetYesterdayHealthStatsUseCase @Inject constructor(
    private val healthRepository: HealthRepository,
    private val userPreferencesManager: UserPreferencesManager
) {

    suspend operator fun invoke(): HealthStatsResult {
        val isAutomatic = userPreferencesManager.healthConnectEnabledFlow.first()

        return if (isAutomatic) {
            // ── Modo Automático: leer de Health Connect ──────────────────────
            val steps = healthRepository.getStepsForYesterday()
            val sleepHours = healthRepository.getSleepHoursForYesterday()
            HealthStatsResult(
                steps = steps,
                sleepHours = sleepHours,
                isReported = true
            )
        } else {
            // ── Modo Manual: validar que el reporte sea de ayer ───────────────
            val yesterday = LocalDate.now().minusDays(1).toString() // "yyyy-MM-dd"
            val lastReportDate = userPreferencesManager.lastManualReportDateFlow.first()

            if (lastReportDate == yesterday) {
                val steps = userPreferencesManager.lastManualStepsFlow.first()
                val sleep = userPreferencesManager.lastManualSleepFlow.first()
                HealthStatsResult(
                    steps = steps,
                    sleepHours = sleep,
                    isReported = true
                )
            } else {
                // El jugador aún no ha enviado su reporte de hoy
                HealthStatsResult(
                    steps = 0L,
                    sleepHours = 0f,
                    isReported = false
                )
            }
        }
    }
}
