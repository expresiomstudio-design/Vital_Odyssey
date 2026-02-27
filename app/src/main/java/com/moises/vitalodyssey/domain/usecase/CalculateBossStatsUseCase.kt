package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.data.local.Difficulty

class CalculateBossStatsUseCase {

    // Devuelve el Daño Base Diario del Jefe
    operator fun invoke(level: Int, difficulty: Difficulty): Int {
        val l = level.toDouble()

        return when (difficulty) {
            Difficulty.EASY -> (20 + 1.2 * l).toInt()
            Difficulty.NORMAL -> (25 + 1.55 * l).toInt()
            Difficulty.HARD -> (25 + 1.2 * l + 0.01 * (l * l)).toInt() // Fórmula cuadrática
        }
    }
}