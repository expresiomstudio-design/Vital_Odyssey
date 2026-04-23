package com.moises.vitalodyssey.domain.model

data class AppRule(
    val id: Int = 0,
    val packageName: String,
    val appName: String,
    val timeLimitMinutes: Int,
    val isBlockMode: Boolean = false,
    val startTime: String? = null,
    val endTime: String? = null,
    val activeDays: List<Int>,
    val isEnabled: Boolean = true
)
