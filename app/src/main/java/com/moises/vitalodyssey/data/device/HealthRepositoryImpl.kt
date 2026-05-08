package com.moises.vitalodyssey.data.device

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.moises.vitalodyssey.domain.repository.HealthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthRepositoryImpl(private val context: Context) : HealthRepository {

    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    override fun isHealthConnectAvailable(): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    override suspend fun hasAllPermissions(): Boolean = withContext(Dispatchers.IO) {
        if (!isHealthConnectAvailable()) return@withContext false
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        granted.containsAll(requiredPermissions)
    }

    override suspend fun getStepsForYesterday(): Long = withContext(Dispatchers.IO) {
        if (!hasAllPermissions()) return@withContext 0L

        val (start, end) = getYesterdayTimeRange()
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )

        try {
            val response = healthConnectClient.readRecords(request)
            response.records.sumOf { it.count }
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun getSleepHoursForYesterday(): Float = withContext(Dispatchers.IO) {
        if (!hasAllPermissions()) return@withContext 0f

        val (start, end) = getYesterdayTimeRange()
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )

        try {
            val response = healthConnectClient.readRecords(request)
            val totalMinutes = response.records.sumOf { record ->
                ChronoUnit.MINUTES.between(record.startTime, record.endTime)
            }
            totalMinutes / 60f
        } catch (e: Exception) {
            0f
        }
    }

    private fun getYesterdayTimeRange(): Pair<java.time.Instant, java.time.Instant> {
        val zoneId = ZoneId.systemDefault()
        val yesterday = LocalDate.now().minusDays(1)
        val startOfDay = yesterday.atStartOfDay(zoneId).toInstant()
        val endOfDay = yesterday.plusDays(1).atStartOfDay(zoneId).minusNanos(1).toInstant()
        return Pair(startOfDay, endOfDay)
    }
}
