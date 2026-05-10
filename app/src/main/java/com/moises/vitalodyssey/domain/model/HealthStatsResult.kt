package com.moises.vitalodyssey.domain.model

/**
 * Resultado de la consulta de estadísticas de salud del día anterior.
 *
 * @param steps       Pasos dados ayer.
 * @param sleepHours  Horas de sueño registradas ayer.
 * @param isReported  `true` si hay datos reales para el día (automáticos o manuales
 *                    con fecha coincidente); `false` si el jugador aún no reportó.
 */
data class HealthStatsResult(
    val steps: Long,
    val sleepHours: Float,
    val isReported: Boolean
)
