package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.data.local.BossDao
import com.moises.vitalodyssey.data.local.BossEntity
import com.moises.vitalodyssey.data.local.Difficulty

class CheckAndSeedInitialBossUseCase(
    private val bossDao: BossDao,
    private val calculateBossStatsUseCase: CalculateBossStatsUseCase
) {
    suspend operator fun invoke(playerLevel: Int) {
        // Verificamos si ya hay un jefe en la base de datos
        val currentBoss = bossDao.getCurrentBoss()
        
        if (currentBoss == null) {
            // Calculamos sus stats iniciales basados en el nivel del jugador (Nivel 1 normalmente)
            val maxHp = calculateBossStatsUseCase.calculateBossHp(playerLevel, Difficulty.EASY)
            val baseAttack = calculateBossStatsUseCase(playerLevel, Difficulty.EASY)

            val initialBoss = BossEntity(
                id = 1,
                name = "El Titán de la Procrastinación",
                imageAssetId = "boss_stone_golem", // Nombre exacto del archivo drawable
                difficulty = "FÁCIL",
                description = "Un monstruo lento pero inamovible que susurra constantemente que 'siempre hay un mañana'. Sus ataques son débiles al principio, pero si las tareas se acumulan, su daño crece exponencialmente.",
                catchPhrase = "No te preocupes por hoy, siempre habrá un mañana.",
                maxHp = maxHp,
                currentHp = maxHp,
                baseAttack = baseAttack,
                isDefeated = false
            )
            
            // Insertamos al jefe en la base de datos
            bossDao.insertBoss(initialBoss)
        }
    }
}
