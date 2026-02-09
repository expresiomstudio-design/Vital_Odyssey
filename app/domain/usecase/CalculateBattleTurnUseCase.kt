package com.moises.vitalodyssey.domain.usecase

data class BattleResult(
    val damageDealtToBoss: Int,
    val damageReceivedFromBoss: Int,
    val xpEarned: Int
)

class CalculateBattleTurnUseCase {

    operator fun invoke(
        playerStats: PlayerStats,
        bossAttack: Int,
        completedHabitsCount: Int,
        totalHabitsCount: Int,
        isManualAttack: Boolean,
        healthBonusMultiplier: Float, // 1.0 (base), 1.2 (manual), 1.5 (auto)
        currentStreak: Int
    ): BattleResult {

        // 1. Calcular Daño Base del Jugador por Hábitos
        val completionRate = if (totalHabitsCount > 0) completedHabitsCount.toFloat() / totalHabitsCount else 0f
        val baseDamage = playerStats.baseAttack * completionRate

        // 2. Suma de Multiplicadores (Degradación Elegante)
        val presenceMultiplier = if (isManualAttack) 0.1f else 0f
        val streakMultiplier = (currentStreak * 0.05f).coerceAtMost(0.5f) // Máximo x1.5 (0.5 extra)

        // Los multiplicadores se suman a la base (1.0)
        val totalMultiplier = 1.0f + presenceMultiplier + streakMultiplier + (healthBonusMultiplier - 1.0f)

        val damageDealt = (baseDamage * totalMultiplier).toInt()

        // 3. Daño Recibido (El Jefe siempre ataca)
        val actualDefense = (playerStats.baseDefense * totalMultiplier).toInt()
        val damageReceived = (bossAttack - actualDefense).coerceAtLeast(0) // Nunca recibe daño negativo

        // 4. Experiencia Ganada (Distribución de Recompensas)
        var xpEarned = completedHabitsCount * 10
        if (isManualAttack) xpEarned += 20
        xpEarned += (currentStreak * 5)

        if (healthBonusMultiplier >= 1.5f) {
            xpEarned += 50
        } else if (healthBonusMultiplier > 1.0f) {
            xpEarned += 30
        }

        return BattleResult(
            damageDealtToBoss = damageDealt,
            damageReceivedFromBoss = damageReceived,
            xpEarned = xpEarned
        )
    }
}