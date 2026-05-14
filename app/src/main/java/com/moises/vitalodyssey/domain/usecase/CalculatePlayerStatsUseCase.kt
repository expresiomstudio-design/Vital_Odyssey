package com.moises.vitalodyssey.domain.usecase

import kotlin.math.pow

data class PlayerStats(
    val level: Int,
    val maxHp: Int,
    val faintHp: Int,
    val baseAttack: Int,
    val baseDefense: Int,
    val xpForNextLevel: Int
)

class CalculatePlayerStatsUseCase {

    operator fun invoke(level: Int): PlayerStats {
        val maxHp = 1000 + (10 * level)
        val baseAttack = (100 + 1.5 * level).toInt()
        val baseDefense = (10 + 0.5 * level).toInt()
        val xpReq = 50 + (4 * level)

        return PlayerStats(
            level = level,
            maxHp = maxHp,
            faintHp = (maxHp * com.moises.vitalodyssey.domain.model.GameConstants.FAINT_HP_RATIO).toInt(),
            baseAttack = baseAttack,
            baseDefense = baseDefense,
            xpForNextLevel = xpReq
        )
    }
}