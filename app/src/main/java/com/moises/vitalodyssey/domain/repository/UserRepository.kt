package com.moises.vitalodyssey.domain.repository

import com.moises.vitalodyssey.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun syncUserToCloud()
    suspend fun fetchUserFromCloud()
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun updateStats(profile: UserProfile)
    suspend fun deleteUserAccount()
}
