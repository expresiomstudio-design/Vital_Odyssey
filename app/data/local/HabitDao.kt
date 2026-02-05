package com.moises.vitalodyssey.data.local

import androidx.room.*
import com.moises.vitalodyssey.domain.model.Habit
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    // Obtener todos los hábitos en tiempo real
    @Query("SELECT * FROM habits_table")
    fun getAllHabits(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    // Atajo útil para marcar rápidamente como completado
    @Query("UPDATE habits_table SET isCompleted = :completed WHERE id = :habitId")
    suspend fun updateHabitStatus(habitId: Int, completed: Boolean)
}