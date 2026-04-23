package com.moises.vitalodyssey.domain.model

data class AppUsageStat(
    val packageName: String,
    val appName: String,
    val timeUsedMinutes: Int,
    val lastUpdated: Long
)
