package com.moises.vitalodyssey.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.moises.vitalodyssey.domain.model.Habit
import com.moises.vitalodyssey.domain.model.HabitLog

@Database(entities = [Habit::class, HabitLog::class, UserEntity::class], version = 5, exportSchema = false)
@TypeConverters(HabitTypeConverters::class) // ¡Importante para que no falle al compilar!
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun userDao(): UserDao

    companion object {
        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits_table ADD COLUMN isCumulative INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habit_logs ADD COLUMN currentScore REAL NOT NULL DEFAULT 0.0")
            }
        }
    }
}