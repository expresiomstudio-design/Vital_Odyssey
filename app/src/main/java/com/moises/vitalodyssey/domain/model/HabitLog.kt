package com.moises.vitalodyssey.domain.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId"])]
)
data class HabitLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val habitId: Int = 0,         // Default necesario para Firestore
    val date: String = "",        // Default necesario para Firestore
    val state: HabitState = HabitState.UNRECORDED, // Default necesario para Firestore
    val measuredValue: Float? = null,
    val currentScore: Float = 0f
)