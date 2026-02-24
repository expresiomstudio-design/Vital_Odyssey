package com.moises.vitalodyssey.domain.usecase

import kotlin.math.pow

// Un modelo puro de Kotlin para transportar los datos a la interfaz
data class PlayerStats(
    val level: Int,
    val maxHp: Int,
    val faintHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val xpForNextLevel: Int
)

class CalculatePlayerStatsUseCase {

    // El operador 'invoke' permite llamar a la clase como si fuera una función
    operator fun invoke(level: Int): PlayerStats {

        // Fórmulas matemáticas de progresión exponencial suave (GDD)
        val maxHp = 1000 + (10 * level)
        val baseAttack = (100 + 1.5 * level).toInt()
        val baseDefense = (10 + 0.5 * level).toInt()
        val xpReq = 50 + (4 * level)

        return PlayerStats(
            level = level,
            maxHp = maxHp,
            faintHp = maxHp / 2, // Límite exacto del 50%
            baseAttack = baseAttack,
            baseDefense = baseDefense,
            xpForNextLevel = xpReq
        )
    }
}