package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.data.local.BossDao
import com.moises.vitalodyssey.data.local.BossEntity
import com.moises.vitalodyssey.data.local.Difficulty
import com.moises.vitalodyssey.domain.model.GameConstants

data class BossTemplate(
    val name: String,
    val imageAssetId: String,
    val difficulty: String,
    val catchPhrase: String,
    val description: String,
    val difficultyEnum: Difficulty
)

val BOSS_TEMPLATES = listOf(
    BossTemplate(
        name = "El Titán de la Procrastinación",
        imageAssetId = "boss_stone_golem",
        difficulty = "FÁCIL",
        catchPhrase = "No te preocupes por hoy, siempre habrá un mañana.",
        description = "Un monstruo lento pero inamovible que susurra constantemente que 'siempre hay un mañana'. Sus ataques son débiles al principio, pero si las tareas se acumulan, su daño crece exponencialmente.",
        difficultyEnum = Difficulty.EASY
    ),
    BossTemplate(
        name = "El Espíritu del Sueño Roto",
        imageAssetId = "boss_sleep_spirit",
        difficulty = "MEDIO",
        catchPhrase = "Sólo un video más, la noche es joven.",
        description = "Una criatura etérea que roba tus horas de descanso. Te tienta con pantallas brillantes y notificaciones infinitas a altas horas de la noche.",
        difficultyEnum = Difficulty.NORMAL
    ),
    BossTemplate(
        name = "El Mago de las Sombras",
        imageAssetId = "boss_shadow_mage",
        difficulty = "MEDIO",
        catchPhrase = "No eres lo suficientemente bueno para lograrlo.",
        description = "Un hechicero oscuro que se alimenta de tu síndrome del impostor. Susurra dudas en tu mente para paralizar tus proyectos.",
        difficultyEnum = Difficulty.NORMAL
    ),
    BossTemplate(
        name = "El Demonio del Burnout",
        imageAssetId = "boss_fire_demon",
        difficulty = "DIFÍCIL",
        catchPhrase = "Trabaja hasta arder, luego desaparece.",
        description = "Una entidad ardiente que te empuja a trabajar sin descanso hasta agotar tus fuerzas y consumir tu energía vital.",
        difficultyEnum = Difficulty.HARD
    ),
    BossTemplate(
        name = "La Emperatriz del Confort",
        imageAssetId = "boss_silk_embrace",
        difficulty = "DIFÍCIL",
        catchPhrase = "Quédate aquí, donde todo es seguro y cálido.",
        description = "Una araña majestuosa que te teje en una red de comodidades. Te impide salir de tu zona de confort con promesas de placer inmediato.",
        difficultyEnum = Difficulty.HARD
    ),
    BossTemplate(
        name = "El Rey del Espejo Distractor",
        imageAssetId = "boss_mirror_king",
        difficulty = "EXTREMO",
        catchPhrase = "Mira lo que pudiste ser, pero no te muevas.",
        description = "El soberano final. Refleja todas las distracciones del mundo a tu alrededor, creando un laberinto infinito de ilusiones.",
        difficultyEnum = Difficulty.HARD
    )
)

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
    private val calculateBossStats: CalculateBossStatsUseCase,
    private val bossDao: BossDao
) {
    suspend operator fun invoke(
        currentLevel: Int,
        currentXp: Int,
        currentHp: Int,
        battleResult: BattleResult,
        bossesDefeatedCount: Int
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

            if (bossDefeated) {
                // Seleccionar el siguiente jefe de forma cíclica
                val nextBossIndex = (bossesDefeatedCount + 1) % BOSS_TEMPLATES.size
                val template = BOSS_TEMPLATES[nextBossIndex]
                
                // Escalar las estadísticas del jefe según el nivel del jugador y el índice del jefe
                val scaleLevel = currentLevel + nextBossIndex
                val nextMaxHp = calculateBossStats.calculateBossHp(scaleLevel, template.difficultyEnum)
                val nextBaseAttack = calculateBossStats(scaleLevel, template.difficultyEnum)

                val nextBoss = BossEntity(
                    id = 1, // Reemplazamos siempre el id = 1 en la base de datos
                    name = template.name,
                    imageAssetId = template.imageAssetId,
                    difficulty = template.difficulty,
                    description = template.description,
                    catchPhrase = template.catchPhrase,
                    maxHp = nextMaxHp,
                    currentHp = nextMaxHp,
                    baseAttack = nextBaseAttack,
                    isDefeated = false,
                    lastUpdated = System.currentTimeMillis()
                )
                bossDao.insertBoss(nextBoss)
            } else {
                bossDao.updateBoss(
                    currentBoss.copy(
                        currentHp = newBossHp,
                        isDefeated = false,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
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