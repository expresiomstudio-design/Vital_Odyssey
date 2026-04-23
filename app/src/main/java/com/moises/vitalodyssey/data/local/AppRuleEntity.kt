package com.moises.vitalodyssey.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.moises.vitalodyssey.domain.model.AppRule

@Entity(tableName = "app_rules_table")
data class AppRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val packageName: String,
    val appName: String,
    val timeLimitMinutes: Int,
    val isBlockMode: Boolean = false,
    val startTime: String? = null,
    val endTime: String? = null,
    val activeDays: List<Int>,
    val isEnabled: Boolean = true
) {
    fun toDomain(): AppRule {
        return AppRule(
            id = id,
            packageName = packageName,
            appName = appName,
            timeLimitMinutes = timeLimitMinutes,
            isBlockMode = isBlockMode,
            startTime = startTime,
            endTime = endTime,
            activeDays = activeDays,
            isEnabled = isEnabled
        )
    }

    companion object {
        fun fromDomain(rule: AppRule): AppRuleEntity {
            return AppRuleEntity(
                id = rule.id,
                packageName = rule.packageName,
                appName = rule.appName,
                timeLimitMinutes = rule.timeLimitMinutes,
                isBlockMode = rule.isBlockMode,
                startTime = rule.startTime,
                endTime = rule.endTime,
                activeDays = rule.activeDays,
                isEnabled = rule.isEnabled
            )
        }
    }
}
