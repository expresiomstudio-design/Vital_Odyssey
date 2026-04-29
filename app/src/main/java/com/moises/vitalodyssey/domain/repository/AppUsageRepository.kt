package com.moises.vitalodyssey.domain.repository

import com.moises.vitalodyssey.domain.model.AppInfo
import com.moises.vitalodyssey.domain.model.AppUsageStat

interface AppUsageRepository {
    suspend fun getDailyUsageStats(): List<AppUsageStat>
    suspend fun hasUsageStatsPermission(): Boolean
    suspend fun getInstalledApps(): List<AppInfo>
}
