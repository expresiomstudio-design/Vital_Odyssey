package com.moises.vitalodyssey.domain.model

object GameConstants {
    // --- EXPERIENCIA (XP) ---
    const val MAX_HABITS_XP = 50
    const val MANUAL_ATTACK_BASE_XP = 20
    const val OPTIMAL_HEALTH_XP_BONUS = 50
    
    // --- RACHAS Y MULTIPLICADORES ---
    const val MAX_OFFENSIVE_STREAK = 100
    const val MAX_PRESENCE_STREAK = 30
    
    const val MAX_OFFENSIVE_MULTIPLIER = 1.5f
    const val MAX_DEFENSE_MULTIPLIER = 1.5f
    const val MIN_DEFENSE_MULTIPLIER = 0.8f
    const val OPTIMAL_DEFENSE_THRESHOLD = 1.2f
    
    const val BASE_PRESENCE_BONUS = 0.1f // +0.1 al ataque
    
    // --- COMBATE Y SUPERVIVENCIA ---
    const val FAINT_HP_RATIO = 0.5f // Te desmayas al 50% de HP
    const val BOSS_HEAL_ON_FAINT_RATIO = 0.15f // El jefe se cura 15% si te desmayas
    const val MAX_DAILY_HEAL_RATIO = 0.10f // Te puedes curar máximo 10% de tu HP al día
}
