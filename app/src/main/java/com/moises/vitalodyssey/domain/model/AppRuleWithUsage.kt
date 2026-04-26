package com.moises.vitalodyssey.domain.model

data class AppRuleWithUsage(
    val rule: AppRule,
    val usageMinutes: Int
)
