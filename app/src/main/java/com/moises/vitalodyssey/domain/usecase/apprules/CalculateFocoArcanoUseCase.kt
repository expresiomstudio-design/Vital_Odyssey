package com.moises.vitalodyssey.domain.usecase.apprules

import com.moises.vitalodyssey.domain.model.AppRuleWithUsage
import kotlinx.coroutines.flow.first

/**
 * Use Case to calculate the "Foco Arcano" (Arcane Focus) percentage.
 * Represents the player's success rate in complying with their digital temptation rules.
 */
class CalculateFocoArcanoUseCase(
    private val getTrackedAppsUsageUseCase: GetTrackedAppsUsageUseCase
) {
    /**
     * Calculates the success percentage based on current app usage rules.
     * 
     * @return An integer between 0 and 100 representing the percentage of complied rules.
     */
    suspend operator fun invoke(): Int {
        val trackedAppsUsage = getTrackedAppsUsageUseCase().first()

        // If there are no rules defined or tracking, we consider the player fully focused.
        if (trackedAppsUsage.isEmpty()) {
            return 100
        }

        val totalRules = trackedAppsUsage.size
        val compliedRules = trackedAppsUsage.count { it.usageMinutes <= it.rule.timeLimitMinutes }

        // Calculate the percentage: (complied / total) * 100
        return (compliedRules.toFloat() / totalRules.toFloat() * 100).toInt()
    }
}
