package com.moises.vitalodyssey.domain.usecase

data class BattleResult(
    val damageDealtToBoss: Int,
    val damageReceivedFromBoss: Int,
    val hpHealed: Int,
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
        healthBonusMultiplier: Float,
        currentStreak: Int
    ): BattleResult {

        val attackCompletionRate = if (offensiveHabitsTotal > 0) offensiveHabitsCompleted.toFloat() / offensiveHabitsTotal else 0f
        val baseDamage = playerStats.baseAttack * attackCompletionRate

        val presenceMultiplier = if (isManualAttack) 0.1f else 0f
        val streakMultiplier = (currentStreak * 0.05f).coerceAtMost(0.5f)
        val totalMultiplier = 1.0f + presenceMultiplier + streakMultiplier + (healthBonusMultiplier - 1.0f)

        val damageDealt = (baseDamage * totalMultiplier).toInt()

        val defenseCompletionRate = if (defensiveHabitsTotal > 0) defensiveHabitsCompleted.toFloat() / defensiveHabitsTotal else 0f
        val maxHealingPossible = playerStats.maxHp * 0.10f
        val hpHealed = (maxHealingPossible * defenseCompletionRate).toInt()

        val actualDefense = (playerStats.baseDefense * totalMultiplier).toInt()
        val damageReceived = (bossAttack - actualDefense).coerceAtLeast(0)

        val totalCompleted = offensiveHabitsCompleted + defensiveHabitsCompleted
        var xpEarned = totalCompleted * 10
        if (isManualAttack) xpEarned += 20
        xpEarned += (currentStreak * 5)

        if (healthBonusMultiplier >= 1.5f) {
            xpEarned += 50
        } else if (healthBonusMultiplier > 1.0f) {
            xpEarned += 30
        }

        xpEarned /= 2

        return BattleResult(
            damageDealtToBoss = damageDealt,
            damageReceivedFromBoss = damageReceived,
            hpHealed = hpHealed,
            xpEarned = xpEarned
        )
    }
}