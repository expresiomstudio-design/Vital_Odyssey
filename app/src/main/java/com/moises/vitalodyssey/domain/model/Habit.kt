package com.moises.vitalodyssey.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Definición de los Enums que darán flexibilidad a tus hábitos
enum class HabitType { BOOLEAN, MEASURABLE }
enum class TargetType { AT_LEAST, AT_MOST }
enum class HabitRole { OFFENSIVE, DEFENSIVE }
enum class HabitState { UNRECORDED, COMPLETED, SKIPPED, MISSED, COMPLETED_BY_PERIOD }

@Entity(tableName = "habits_table")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val firestoreId: String = "",
    val name: String,
    val note: String = "",

    // ROL DEL HÁBITO (Ataque o Curación)
    val role: HabitRole = HabitRole.OFFENSIVE, // Por defecto ataca

    // TIPO DE HÁBITO
    val type: HabitType,

    // PROPIEDADES MEDIBLES (Solo se usan si type == MEASURABLE)
    val unit: String? = null,     // Ej: "Km", "Minutos", "Vasos"
    val targetValue: Float = 1f,  // Ej: 15 (minutos)
    val targetType: TargetType = TargetType.AT_LEAST,
    val currentCount: Float = 0f, // Progreso actual del día

    // FRECUENCIA Y ESTADO
    val frequencyType: String = "DAILY", // DAILY, WEEKLY, MONTHLY, INTERVAL
    val frequencyTarget: Int = 1,        // Ej: 2 veces, o cada 3 días
    val isCompleted: Boolean = false,

    // RACHAS Y PUNTUACIÓN
    val currentStreak: Int = 0,
    val score: Float = 0f,
    val startDate: String = "" // Fecha de creación (YYYY-MM-DD)
)