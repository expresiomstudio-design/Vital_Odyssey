package com.moises.vitalodyssey.domain.model

data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val bodyType: BodyType?,
    val playerClass: PlayerClass?,
    val level: Int,
    val currentXp: Int,
    val currentHp: Int,
    val currentStamina: Int,
    val presenceStreak: Int,
    val highestStreak: Int,
    val bossesDefeated: List<String>,
    val cutoffTime: String,
    val difficulty: String,
    val hasCompletedOnboarding: Boolean,
    val startOfWeek: String = "MONDAY"
)
