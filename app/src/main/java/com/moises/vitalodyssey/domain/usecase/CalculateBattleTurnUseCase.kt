package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.domain.model.GameConstants
import com.moises.vitalodyssey.domain.usecase.PlayerStats
import com.moises.vitalodyssey.domain.usecase.health.CalculateDefenseMultiplierUseCase

data class BattleResult(
    val damageDealtToBoss: Int,
    val damageReceivedFromBoss: Int,
    val hpHealed: Int,
    val xpEarned: Int
)

/** Abstracción mínima para poder inyectar fakes en tests sin heredar la clase concreta. */
fun interface DefenseMultiplierProvider {
    suspend fun getMultiplier(): Float
}

class CalculateBattleTurnUseCase(
    private val defenseProvider: DefenseMultiplierProvider
) {
    /** Convenience constructor para producción: envuelve el UseCase real. */
    constructor(useCase: CalculateDefenseMultiplierUseCase) : this(
        DefenseMultiplierProvider { useCase() }
    )

    suspend operator fun invoke(
        playerStats: PlayerStats,
        bossAttack: Int,
        offensiveHabitsTotal: Int,
        offensiveHabitsCompleted: Int,
        defensiveHabitsTotal: Int,
        defensiveHabitsCompleted: Int,
        isManualAttack: Boolean,
        maxOffensiveScore: Int,
        presenceStreak: Int
    ): BattleResult {
        // 1. Obtener el multiplicador de Defensa (Salud) BLINDADO
        val defenseMultiplier = try {
            defenseProvider.getMultiplier()
        } catch (e: Exception) {
            e.printStackTrace()
            1.0f
        }

        // 2. Calcular Multiplicadores de Daño
        val attackMultiplier = 1.0f + (maxOffensiveScore.coerceAtMost(GameConstants.MAX_OFFENSIVE_STREAK) / 200f)
        val presenceBonus = if (isManualAttack) {
            GameConstants.BASE_PRESENCE_BONUS + (presenceStreak.coerceAtMost(GameConstants.MAX_PRESENCE_STREAK) / 600f)
        } else {
            0f
        }
        val totalDamageMultiplier = attackMultiplier + (defenseMultiplier - 1.0f) + presenceBonus

        // 3. Calcular Daño al Jefe
        val attackCompletionRate = if (offensiveHabitsTotal > 0) offensiveHabitsCompleted.toFloat() / offensiveHabitsTotal else 0f
        val baseDamage = playerStats.baseAttack * attackCompletionRate
        val damageDealt = (baseDamage * totalDamageMultiplier).toInt()

        // 4. Calcular Daño Recibido
        val effectiveDefense = (playerStats.baseDefense * defenseMultiplier).toInt()
        val damageReceived = (bossAttack - effectiveDefense).coerceAtLeast(0)

        // 5. Calcular Curación
        val defenseCompletionRate = if (defensiveHabitsTotal > 0) defensiveHabitsCompleted.toFloat() / defensiveHabitsTotal else 0f
        val maxHealingPossible = playerStats.maxHp * GameConstants.MAX_DAILY_HEAL_RATIO
        val hpHealed = (maxHealingPossible * defenseCompletionRate).toInt()

        // 6. Calcular Experiencia
        val totalHabits = offensiveHabitsTotal + defensiveHabitsTotal
        val completedHabits = offensiveHabitsCompleted + defensiveHabitsCompleted
        val habitCompletionRate = if (totalHabits > 0) completedHabits.toFloat() / totalHabits else 0f

        var xpEarned = (habitCompletionRate * GameConstants.MAX_HABITS_XP).toInt()
        
        if (defenseMultiplier >= GameConstants.OPTIMAL_DEFENSE_THRESHOLD) {
            xpEarned += GameConstants.OPTIMAL_HEALTH_XP_BONUS
        }
        
        if (isManualAttack) {
            val presenceXpMultiplier = 1.0f + (presenceStreak.coerceAtMost(GameConstants.MAX_PRESENCE_STREAK) / 60f)
            xpEarned += (GameConstants.MANUAL_ATTACK_BASE_XP * presenceXpMultiplier).toInt()
        }

        return BattleResult(
            damageDealtToBoss = damageDealt,
            damageReceivedFromBoss = damageReceived,
            hpHealed = hpHealed,
            xpEarned = xpEarned
        )
    }
}