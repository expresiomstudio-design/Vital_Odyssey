package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.data.local.BossDao
import com.moises.vitalodyssey.domain.model.GameConstants

data class UpdatedCombatState(
    val newLevel: Int,
    val newXp: Int,
    val newHp: Int,
    val didLevelUp: Boolean,
    val isFainted: Boolean,
    val bossDefeated: Boolean
)

class ProcessBattleResultUseCase(
    private val calculatePlayerStats: CalculatePlayerStatsUseCase,
    private val bossDao: BossDao
) {
    suspend operator fun invoke(
        currentLevel: Int,
        currentXp: Int,
        currentHp: Int,
        battleResult: BattleResult
    ): UpdatedCombatState {
        val currentStats = calculatePlayerStats(currentLevel)

        // 1. Calcular vida del jugador
        var tempHp = currentHp - battleResult.damageReceivedFromBoss + battleResult.hpHealed
        tempHp = tempHp.coerceIn(currentStats.faintHp, currentStats.maxHp)
        
        val isFainted = tempHp <= currentStats.faintHp

        // 2. Obtener al Jefe y aplicarle daño o curación
        val currentBoss = bossDao.getCurrentBoss()
        var bossDefeated = false

        if (currentBoss != null) {
            var newBossHp = currentBoss.currentHp
            
            if (isFainted) {
                // Castigo: El Jefe se cura 15%
                val bossHeal = (currentBoss.maxHp * GameConstants.BOSS_HEAL_ON_FAINT_RATIO).toInt()
                newBossHp = (newBossHp + bossHeal).coerceAtMost(currentBoss.maxHp)
            } else {
                // Ataque normal: El Jefe recibe daño
                newBossHp = (newBossHp - battleResult.damageDealtToBoss).coerceAtLeast(0)
            }

            bossDefeated = newBossHp <= 0

            bossDao.updateBoss(
                currentBoss.copy(
                    currentHp = newBossHp,
                    isDefeated = bossDefeated
                )
            )
        }

        // 3. Procesar Nivel y Experiencia
        var finalLevel = currentLevel
        var finalXp = currentXp + battleResult.xpEarned
        var didLevelUp = false

        if (!isFainted && finalXp >= currentStats.xpForNextLevel) {
            didLevelUp = true
            finalLevel++
            finalXp -= currentStats.xpForNextLevel
            val newStats = calculatePlayerStats(finalLevel)
            val levelUpHeal = (newStats.maxHp * 0.50f).toInt()
            tempHp = (tempHp + levelUpHeal).coerceAtMost(newStats.maxHp)
        }

        // 4. Segundo Aliento (Si se desmayó, mañana amanece full)
        if (isFainted) {
            tempHp = currentStats.maxHp
        }

        return UpdatedCombatState(
            newLevel = finalLevel,
            newXp = finalXp,
            newHp = tempHp,
            didLevelUp = didLevelUp,
            isFainted = isFainted,
            bossDefeated = bossDefeated
        )
    }
}