package com.moises.vitalodyssey.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.moises.vitalodyssey.domain.model.Habit

@Database(entities = [Habit::class, UserEntity::class], version = 2, exportSchema = false)
@TypeConverters(HabitTypeConverters::class) // ¡Importante para que no falle al compilar!
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun userDao(): UserDao

}