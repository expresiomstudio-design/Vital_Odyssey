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

    // Calcula y devuelve la Vida Máxima del Jefe (HP)
    fun calculateBossHp(level: Int, difficulty: Difficulty): Int {
        val l = level.toDouble()
        val baseHp = when (difficulty) {
            Difficulty.EASY -> (1000 + 50 * l).toInt()
            Difficulty.NORMAL -> (1500 + 75 * l).toInt()
            Difficulty.HARD -> (2000 + 100 * l + 0.5 * (l * l)).toInt()
        }
        // Duplicar el valor de HP máximo calculado actualmente según las reglas de Estamina
        return baseHp * 2
    }
}