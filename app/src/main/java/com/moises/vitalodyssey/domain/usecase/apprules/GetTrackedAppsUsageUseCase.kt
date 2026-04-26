package com.moises.vitalodyssey.domain.usecase.apprules

import com.moises.vitalodyssey.data.local.AppRuleDao
import com.moises.vitalodyssey.domain.model.AppRuleWithUsage
import com.moises.vitalodyssey.domain.repository.AppUsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

class GetTrackedAppsUsageUseCase(
    private val appRuleDao: AppRuleDao,
    private val appUsageRepository: AppUsageRepository
) {
    operator fun invoke(): Flow<List<AppRuleWithUsage>> {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val currentDay = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1

        return appRuleDao.getAllRules().map { entities ->
            val stats = appUsageRepository.getDailyUsageStats().associateBy { it.packageName }
            
            entities.map { it.toDomain() }
                .filter { rule -> 
                    rule.isEnabled && rule.activeDays.contains(currentDay)
                }
                .map { rule ->
                    AppRuleWithUsage(
                        rule = rule,
                        usageMinutes = stats[rule.packageName]?.timeUsedMinutes ?: 0
                    )
                }
        }
    }
}
