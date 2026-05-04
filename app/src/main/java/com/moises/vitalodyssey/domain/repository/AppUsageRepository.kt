package com.moises.vitalodyssey.domain.repository

import com.moises.vitalodyssey.domain.model.AppInfo

interface AppUsageRepository {
    suspend fun getUsageForPackage(packageName: String, startMillis: Long, endMillis: Long): Int
    suspend fun hasUsageStatsPermission(): Boolean
    suspend fun getInstalledApps(): List<AppInfo>
}
