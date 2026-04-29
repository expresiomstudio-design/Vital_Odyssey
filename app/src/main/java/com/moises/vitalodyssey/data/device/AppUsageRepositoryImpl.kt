package com.moises.vitalodyssey.data.device

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.moises.vitalodyssey.domain.model.AppInfo
import com.moises.vitalodyssey.domain.model.AppUsageStat
import com.moises.vitalodyssey.domain.repository.AppUsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class AppUsageRepositoryImpl(private val context: Context) : AppUsageRepository {

    override suspend fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override suspend fun getDailyUsageStats(): List<AppUsageStat> = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val packageManager = context.packageManager

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        stats.filter { it.totalTimeInForeground > 0 }
            .map { usageStats ->
                val packageName = usageStats.packageName
                val appName = try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    packageName
                }

                AppUsageStat(
                    packageName = packageName,
                    appName = appName,
                    timeUsedMinutes = (usageStats.totalTimeInForeground / (1000 * 60)).toInt(),
                    lastUpdated = usageStats.lastTimeStamp
                )
            }
    }

    override suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        apps.filter { packageManager.getLaunchIntentForPackage(it.packageName) != null }
            .map { app ->
                AppInfo(
                    packageName = app.packageName,
                    name = packageManager.getApplicationLabel(app).toString(),
                    icon = packageManager.getApplicationIcon(app)
                )
            }
            .sortedBy { it.name }
    }
}
