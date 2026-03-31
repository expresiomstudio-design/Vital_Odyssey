package com.moises.vitalodyssey.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Definición de los Enums que darán flexibilidad a tus hábitos
enum class HabitType { BOOLEAN, MEASURABLE }
enum class TargetType { AT_LEAST, AT_MOST }
enum class Frequency { DAILY, WEEKLY, MONTHLY }
enum class HabitRole { OFFENSIVE, DEFENSIVE }

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
    val frequency: Frequency = Frequency.DAILY,
    val isCompleted: Boolean = false,

    // RACHAS
    val currentStreak: Int = 0
)