package com.moises.vitalodyssey.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.moises.vitalodyssey.domain.model.BodyType
import com.moises.vitalodyssey.domain.model.PlayerClass
import com.moises.vitalodyssey.domain.model.UserProfile

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val uid: String,
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
    val hasCompletedOnboarding: Boolean
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
        hasCompletedOnboarding = hasCompletedOnboarding
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
            hasCompletedOnboarding = profile.hasCompletedOnboarding
        )
    }
}
