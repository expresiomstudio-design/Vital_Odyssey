package com.moises.vitalodyssey.data.local

import androidx.room.*
import com.moises.vitalodyssey.domain.model.Habit
import com.moises.vitalodyssey.domain.model.HabitLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    // --- MÉTODOS PARA HABIT ---
    @Query("SELECT * FROM habits_table")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits_table WHERE id = :id")
    suspend fun getHabitById(id: Int): Habit?

    @Query("SELECT * FROM habits_table WHERE id = :id")
    fun getHabitByIdFlow(id: Int): Flow<Habit?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Delete
    suspend fun deleteLog(log: HabitLog)

    @Query("UPDATE habits_table SET isCompleted = :completed WHERE id = :habitId")
    suspend fun updateHabitStatus(habitId: Int, completed: Boolean)

    @Query("DELETE FROM habits_table")
    suspend fun deleteAllHabits()

    // --- MÉTODOS PARA HABITLOG ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLog)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getLogForDate(habitId: Int, date: String): HabitLog?
}