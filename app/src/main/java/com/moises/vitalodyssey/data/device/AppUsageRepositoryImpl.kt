package com.moises.vitalodyssey.data.device

import android.util.Log
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import com.moises.vitalodyssey.domain.model.AppInfo
import com.moises.vitalodyssey.domain.repository.AppUsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    override suspend fun getUsageForPackage(
        packageName: String,
        startMillis: Long,
        endMillis: Long
    ): Int = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(startMillis, endMillis)
        var totalTime = 0L
        var startTime = 0L
        val event = UsageEvents.Event()

        // 1. LOG DE INICIO DE BÚSQUEDA
        Log.d("FocoArcanoDebug", "--- BUSCANDO USO PARA: $packageName ---")
        Log.d("FocoArcanoDebug", "Rango: $startMillis -> $endMillis")

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName == packageName) {
                
                // 2. LOG DE EVENTOS ENCONTRADOS
                val eventType = when(event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> "RESUMED (Abierta)"
                    UsageEvents.Event.ACTIVITY_PAUSED -> "PAUSED (Pausada)"
                    UsageEvents.Event.ACTIVITY_STOPPED -> "STOPPED (Cerrada)"
                    else -> "OTRO (${event.eventType})"
                }
                Log.d("FocoArcanoDebug", "Evento: $eventType | Timestamp: ${event.timeStamp}")

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        startTime = event.timeStamp
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> {
                        if (startTime != 0L) {
                            val sessionTime = event.timeStamp - startTime
                            totalTime += sessionTime
                            Log.d("FocoArcanoDebug", "Sesión cerrada. Duración: ${sessionTime / 1000} segundos. Total acumulado: ${totalTime / 1000} seg")
                            startTime = 0L
                        }
                    }
                }
            }
        }

        // Corrección del tiempo presente
        if (startTime != 0L && endMillis > startTime) {
            val currentSessionTime = endMillis - startTime
            totalTime += currentSessionTime
            Log.d("FocoArcanoDebug", "App actualmente abierta. Sumando sesión en curso: ${currentSessionTime / 1000} segundos")
        }

        val minutesUsed = (totalTime / (1000 * 60)).toInt()
        
        // 3. LOG DE RESULTADO FINAL
        Log.d("FocoArcanoDebug", ">>> TOTAL FINAL para $packageName: $minutesUsed minutos <<<")
        Log.d("FocoArcanoDebug", "--------------------------------------")
        
        minutesUsed
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
