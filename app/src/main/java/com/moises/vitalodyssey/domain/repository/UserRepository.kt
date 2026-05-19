package com.moises.vitalodyssey.domain.repository

import com.moises.vitalodyssey.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun syncUserToCloud()
    suspend fun fetchUserFromCloud()
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun getUserProfileOnce(): UserProfile?
    suspend fun updateStats(profile: UserProfile)
    suspend fun deleteUserAccount()
    suspend fun syncHabitsToCloud()
    suspend fun fetchHabitsFromCloud()
    suspend fun syncDatabasesOnLogin()
    
    // Background Sync
    suspend fun scheduleCloudSync()
    suspend fun performFullCloudSync()
}
