package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.data.local.BossEntity
import com.moises.vitalodyssey.data.local.BossDao
import com.moises.vitalodyssey.data.local.Difficulty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.random.Random

// --- FAKES PARA LA SIMULACIÓN ---
class FakeBossDao : BossDao {
    var current: BossEntity? = null
    override fun getCurrentBossFlow(): Flow<BossEntity?> = flowOf(current)
    override suspend fun getCurrentBoss(): BossEntity? = current
    override suspend fun insertBoss(boss: BossEntity) { current = boss }
    override suspend fun updateBoss(boss: BossEntity) { current = boss }
    override suspend fun clearBosses() { current = null }
}

// --- CLASE DE SIMULACIÓN ---
class CombatSimulationTest {

    @Test
    fun runMontecarloSimulations() = runBlocking {
        println("==================================================")
        println("INICIANDO SIMULACIONES DE 180 DÍAS (VITAL ODYSSEY)")
        println("==================================================\n")

        simulateProfile(
            profileName = "EL PALADÍN (100% Cumplidor)",
            habitCompletionRange = 0.9f..1.0f,
            defenseRange = 1.4f..1.5f,
            manualAttackChance = 1.0f // Ataca manualmente todos los días
        )

        simulateProfile(
            profileName = "EL AVENTURERO (Promedio)",
            habitCompletionRange = 0.5f..0.7f,
            defenseRange = 0.9f..1.2f,
            manualAttackChance = 0.7f // Se le olvida un par de días a la semana
        )

        simulateProfile(
            profileName = "EL ZOMBI (Vago / Inconsistente)",
            habitCompletionRange = 0.1f..0.2f,
            defenseRange = 0.8f..0.8f,
            manualAttackChance = 0.15f // Solo entra 1 vez a la semana
        )
    }

    private suspend fun simulateProfile(
        profileName: String,
        habitCompletionRange: ClosedFloatingPointRange<Float>,
        defenseRange: ClosedFloatingPointRange<Float>,
        manualAttackChance: Float
    ) {
        val calculatePlayerStats = CalculatePlayerStatsUseCase()
        val calculateBossStats = CalculateBossStatsUseCase()
        var mockedDefenseMultiplier = 1.0f
        val fakeDefenseProvider = DefenseMultiplierProvider { mockedDefenseMultiplier }
        val calculateBattleTurn = CalculateBattleTurnUseCase(fakeDefenseProvider)
        val fakeBossDao = FakeBossDao()
        val processBattleResult = ProcessBattleResultUseCase(calculatePlayerStats, fakeBossDao)

        // Estado Inicial del Jugador
        var level = 1
        var xp = 0
        var hp = calculatePlayerStats(level).maxHp
        var presenceStreak = 0
        var maxOffensiveScore = 0
        
        var totalFaints = 0
        var bossesDefeated = 0

        println(">>> PERFIL: $profileName")

        for (day in 1..180) {
            // 1. Spawnea jefe si no hay
            if (fakeBossDao.current == null || fakeBossDao.current!!.isDefeated) {
                val bossMaxHp = calculateBossStats.calculateBossHp(level, Difficulty.NORMAL)
                val bossAtk = calculateBossStats(level, Difficulty.NORMAL)
                fakeBossDao.insertBoss(
                    BossEntity(
                        name = "Archidemonio #${bossesDefeated + 1}",
                        imageAssetId = "dummy",
                        difficulty = "NORMAL",
                        maxHp = bossMaxHp,
                        currentHp = bossMaxHp,
                        baseAttack = bossAtk
                    )
                )
            }

            // 2. Generar datos del día basados en el perfil
            val isManualAttack = Random.nextFloat() <= manualAttackChance
            if (isManualAttack) presenceStreak++ else presenceStreak = 0

            val completionRate = if (habitCompletionRange.start >= habitCompletionRange.endInclusive) {
                habitCompletionRange.start
            } else {
                Random.nextDouble(habitCompletionRange.start.toDouble(), habitCompletionRange.endInclusive.toDouble()).toFloat()
            }
            val completedHabits = (10 * completionRate).toInt()
            
            val offCompleted = (5 * completionRate).toInt()
            val defCompleted = (5 * completionRate).toInt()
            
            maxOffensiveScore = (completionRate * 100).toInt() // Simulamos la racha del hábito
            
            mockedDefenseMultiplier = if (defenseRange.start >= defenseRange.endInclusive) {
                defenseRange.start
            } else {
                Random.nextDouble(defenseRange.start.toDouble(), defenseRange.endInclusive.toDouble()).toFloat()
            }

            // 3. Ejecutar Turno
            val boss = fakeBossDao.getCurrentBoss()!!
            val playerStats = calculatePlayerStats(level)
            
            val result = calculateBattleTurn(
                playerStats = playerStats,
                bossAttack = boss.baseAttack,
                offensiveHabitsTotal = 5,
                offensiveHabitsCompleted = offCompleted,
                defensiveHabitsTotal = 5,
                defensiveHabitsCompleted = defCompleted,
                isManualAttack = isManualAttack,
                maxOffensiveScore = maxOffensiveScore,
                presenceStreak = presenceStreak
            )

            val newState = processBattleResult(level, xp, hp, result)
            
            level = newState.newLevel
            xp = newState.newXp
            hp = newState.newHp
            
            if (newState.isFainted) {
                totalFaints++
                presenceStreak = 0
            }
            
            if (newState.bossDefeated) {
                bossesDefeated++
            }

            // Imprimir checkpoints
            if (day % 30 == 0) {
                println("Día $day -> Nivel: $level | Jefes Derrotados: $bossesDefeated | Desmayos: $totalFaints | Vida Actual: $hp/${calculatePlayerStats(level).maxHp}")
            }
        }
        println("--------------------------------------------------\n")
    }
}
