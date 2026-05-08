package com.moises.vitalodyssey.domain.repository

interface HealthRepository {
    /** Verifica si la API de Health Connect está disponible en el dispositivo */
    fun isHealthConnectAvailable(): Boolean

    /** Verifica si el usuario ya nos ha otorgado los permisos necesarios */
    suspend fun hasAllPermissions(): Boolean

    /** Retorna la cantidad de pasos dados en el "Día Anterior" (Ayer 00:00 - 23:59) */
    suspend fun getStepsForYesterday(): Long

    /** Retorna las horas de sueño registradas en el "Día Anterior" (Ayer 00:00 - 23:59) */
    suspend fun getSleepHoursForYesterday(): Float
}
