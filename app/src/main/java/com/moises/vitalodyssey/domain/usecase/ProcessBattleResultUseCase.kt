package com.moises.vitalodyssey.domain.usecase

// Este modelo devuelve el "resumen" de lo que le pasó al jugador
data class UpdatedPlayerState(
    val newLevel: Int,
    val newXp: Int,
    val newHp: Int,
    val didLevelUp: Boolean,
    val isFainted: Boolean
)

class ProcessBattleResultUseCase(
    // Inyectamos el calculador de stats para poder saber la vida del nuevo nivel
    private val calculatePlayerStats: CalculatePlayerStatsUseCase
) {
    operator fun invoke(
        currentLevel: Int,
        currentXp: Int,
        currentHp: Int,
        battleResult: BattleResult
    ): UpdatedPlayerState {

        // 1. Obtener los topes del nivel actual
        val currentStats = calculatePlayerStats(currentLevel)

        // 2. Aplicar Daño del Jefe y Curación de Hábitos Defensivos
        var tempHp = currentHp - battleResult.damageReceivedFromBoss + battleResult.hpHealed

        // Aseguramos que no pase de la vida máxima ni baje del límite de desmayo (50%)
        tempHp = tempHp.coerceIn(currentStats.faintHp, currentStats.maxHp)

        // 3. Comprobar si llegó a la zona de Desmayo
        val isFainted = tempHp <= currentStats.faintHp

        // 4. Procesar Experiencia y Subida de Nivel
        var finalLevel = currentLevel
        var finalXp = currentXp + battleResult.xpEarned
        var didLevelUp = false

        // Si no se desmayó y alcanzó la XP requerida, sube de nivel
        if (!isFainted && finalXp >= currentStats.xpForNextLevel) {
            didLevelUp = true
            finalLevel++
            finalXp -= currentStats.xpForNextLevel // Conserva la XP sobrante

            // Obtenemos los stats del NUEVO nivel para calcular la curación del 50%
            val newStats = calculatePlayerStats(finalLevel)
            val levelUpHeal = (newStats.maxHp * 0.50f).toInt()

            tempHp += levelUpHeal

            // Aseguramos que la curación épica no supere la nueva vida máxima
            tempHp = tempHp.coerceAtMost(newStats.maxHp)
        }

        // 5. Penalización por Desmayo (GDD: La vida vuelve al 100% para el día siguiente)
        if (isFainted) {
            tempHp = currentStats.maxHp
        }

        return UpdatedPlayerState(
            newLevel = finalLevel,
            newXp = finalXp,
            newHp = tempHp,
            didLevelUp = didLevelUp,
            isFainted = isFainted
        )
    }
}