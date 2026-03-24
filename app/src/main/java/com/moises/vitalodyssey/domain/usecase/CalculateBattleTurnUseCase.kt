package com.moises.vitalodyssey.domain.usecase

data class BattleResult(
    val damageDealtToBoss: Int,
    val damageReceivedFromBoss: Int,
    val hpHealed: Int, // ¡Nueva variable para devolver cuánta vida recuperaste!
    val xpEarned: Int
)

class CalculateBattleTurnUseCase {

    operator fun invoke(
        playerStats: PlayerStats,
        bossAttack: Int,
        offensiveHabitsTotal: Int,
        offensiveHabitsCompleted: Int,
        defensiveHabitsTotal: Int,
        defensiveHabitsCompleted: Int,
        isManualAttack: Boolean,
        healthBonusMultiplier: Float, // 1.0 (base), 1.2 (manual), 1.5 (auto)
        currentStreak: Int
    ): BattleResult {

        // 1. Calcular Daño del Jugador (Solo Hábitos OFENSIVOS)
        val attackCompletionRate = if (offensiveHabitsTotal > 0) offensiveHabitsCompleted.toFloat() / offensiveHabitsTotal else 0f
        val baseDamage = playerStats.baseAttack * attackCompletionRate

        // Suma de Multiplicadores (Degradación Elegante)
        val presenceMultiplier = if (isManualAttack) 0.1f else 0f
        val streakMultiplier = (currentStreak * 0.05f).coerceAtMost(0.5f)
        val totalMultiplier = 1.0f + presenceMultiplier + streakMultiplier + (healthBonusMultiplier - 1.0f)

        val damageDealt = (baseDamage * totalMultiplier).toInt()

        // 2. Calcular Curación del Jugador (Solo Hábitos DEFENSIVOS)
        val defenseCompletionRate = if (defensiveHabitsTotal > 0) defensiveHabitsCompleted.toFloat() / defensiveHabitsTotal else 0f
        // La curación máxima es el 10% de la Vida Máxima actual
        val maxHealingPossible = playerStats.maxHp * 0.10f
        val hpHealed = (maxHealingPossible * defenseCompletionRate).toInt()

        // 3. Daño Recibido (El Jefe ataca, mitigado por tu Defensa Real)
        val actualDefense = (playerStats.baseDefense * totalMultiplier).toInt()
        val damageReceived = (bossAttack - actualDefense).coerceAtLeast(0)

        // 4. Experiencia Ganada (Cuenta el total de todos los hábitos)
        val totalCompleted = offensiveHabitsCompleted + defensiveHabitsCompleted
        var xpEarned = totalCompleted * 10
        if (isManualAttack) xpEarned += 20
        xpEarned += (currentStreak * 5)

        if (healthBonusMultiplier >= 1.5f) {
            xpEarned += 50
        } else if (healthBonusMultiplier > 1.0f) {
            xpEarned += 30
        }

        // Reducir a la mitad la XP obtenida por cada ataque (Balance de Estamina)
        xpEarned /= 2

        return BattleResult(
            damageDealtToBoss = damageDealt,
            damageReceivedFromBoss = damageReceived,
            hpHealed = hpHealed,
            xpEarned = xpEarned
        )
    }
}