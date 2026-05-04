package com.moises.vitalodyssey.domain.usecase.apprules

import com.moises.vitalodyssey.data.local.AppRuleDao
import com.moises.vitalodyssey.domain.model.AppRuleWithUsage
import com.moises.vitalodyssey.domain.repository.AppUsageRepository
import com.moises.vitalodyssey.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class GetTrackedAppsUsageUseCase(
    private val appRuleDao: AppRuleDao,
    private val appUsageRepository: AppUsageRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Flow<List<AppRuleWithUsage>> {
        val profile = userRepository.getUserProfileOnce()
        val cutoffTime = profile?.cutoffTime ?: "00:00"
        val (logicalStart, _) = getLogicalDayBounds(cutoffTime)
        
        // Día de la semana lógico (1-Lunes, 7-Domingo)
        val currentDay = logicalStart.dayOfWeek.value

        return appRuleDao.getAllRules().map { entities ->
            val nowMillis = System.currentTimeMillis()

            entities.map { it.toDomain() }
                .map { rule ->
                    val isActiveToday = rule.isEnabled && rule.activeDays.contains(currentDay)
                    
                    val usageMinutes = if (isActiveToday) {
                        val (startMillis, endMillis) = if (!rule.isBlockMode) {
                            // Modo Diario: Desde el inicio lógico del día hasta el momento actual
                            val start = logicalStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            start to nowMillis
                        } else {
                            // Modo Bloque Horario: Lógica de cutoff y cruce de medianoche
                            val ruleStart = rule.startTime?.let { LocalTime.parse(it) } ?: LocalTime.MIDNIGHT
                            val ruleEnd = rule.endTime?.let { LocalTime.parse(it) } ?: LocalTime.MAX
                            val cutoff = LocalTime.parse(cutoffTime)

                            val blockStart = if (ruleStart.isBefore(cutoff)) {
                                logicalStart.toLocalDate().plusDays(1).atTime(ruleStart)
                            } else {
                                logicalStart.toLocalDate().atTime(ruleStart)
                            }

                            val blockEnd = if (ruleEnd.isBefore(cutoff) || ruleEnd.isBefore(ruleStart)) {
                                logicalStart.toLocalDate().plusDays(1).atTime(ruleEnd)
                            } else {
                                logicalStart.toLocalDate().atTime(ruleEnd)
                            }
                            
                            val startM = blockStart.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            val endM = blockEnd.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            
                            if (nowMillis < startM) {
                                0L to 0L
                            } else {
                                startM to minOf(nowMillis, endM)
                            }
                        }
                        
                        if (startMillis == 0L && endMillis == 0L) {
                            0
                        } else {
                            appUsageRepository.getUsageForPackage(rule.packageName, startMillis, endMillis)
                        }
                    } else {
                        0
                    }
                    
                    AppRuleWithUsage(
                        rule = rule,
                        usageMinutes = usageMinutes
                    )
                }
        }
    }

    private fun getLogicalDayBounds(cutoffTimeStr: String): Pair<LocalDateTime, LocalDateTime> {
        val now = LocalDateTime.now()
        val cutoff = LocalTime.parse(cutoffTimeStr)

        val logicalStart = if (now.toLocalTime().isBefore(cutoff)) {
            now.minusDays(1).toLocalDate().atTime(cutoff) // Aún es "ayer" lógicamente
        } else {
            now.toLocalDate().atTime(cutoff) // Es hoy
        }
        val logicalEnd = logicalStart.plusDays(1).minusNanos(1)
        return Pair(logicalStart, logicalEnd)
    }
}
