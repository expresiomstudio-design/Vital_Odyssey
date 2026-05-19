package com.moises.vitalodyssey.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.moises.vitalodyssey.domain.model.BodyType
import com.moises.vitalodyssey.domain.model.PlayerClass
import com.moises.vitalodyssey.domain.model.UserProfile

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val uid: String = "",
    val name: String = "",
    val email: String = "",
    val bodyType: BodyType? = null,
    val playerClass: PlayerClass? = null,
    val level: Int = 1,
    val currentXp: Int = 0,
    val currentHp: Int = 100,
    val currentStamina: Int = 100,
    val presenceStreak: Int = 0,
    val highestStreak: Int = 0,
    val bossesDefeated: List<String> = emptyList(),
    val cutoffTime: String = "00:00",
    val difficulty: String = "NORMAL",
    val hasCompletedOnboarding: Boolean = false,
    val startOfWeek: String = "MONDAY",
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toDomain() = UserProfile(
        uid = uid,
        name = name,
        email = email,
        bodyType = bodyType,
        playerClass = playerClass,
        level = level,
        currentXp = currentXp,
        currentHp = currentHp,
        currentStamina = currentStamina,
        presenceStreak = presenceStreak,
        highestStreak = highestStreak,
        bossesDefeated = bossesDefeated,
        cutoffTime = cutoffTime,
        difficulty = difficulty,
        hasCompletedOnboarding = hasCompletedOnboarding,
        startOfWeek = startOfWeek,
        lastUpdated = lastUpdated
    )

    companion object {
        fun fromDomain(profile: UserProfile) = UserEntity(
            uid = profile.uid,
            name = profile.name,
            email = profile.email,
            bodyType = profile.bodyType,
            playerClass = profile.playerClass,
            level = profile.level,
            currentXp = profile.currentXp,
            currentHp = profile.currentHp,
            currentStamina = profile.currentStamina,
            presenceStreak = profile.presenceStreak,
            highestStreak = profile.highestStreak,
            bossesDefeated = profile.bossesDefeated,
            cutoffTime = profile.cutoffTime,
            difficulty = profile.difficulty,
            hasCompletedOnboarding = profile.hasCompletedOnboarding,
            startOfWeek = profile.startOfWeek,
            lastUpdated = profile.lastUpdated
        )
    }
}
