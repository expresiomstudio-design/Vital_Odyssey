package com.moises.vitalodyssey.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.moises.vitalodyssey.domain.model.Habit
import com.moises.vitalodyssey.domain.model.HabitLog

@Database(entities = [Habit::class, HabitLog::class, UserEntity::class, AppRuleEntity::class], version = 6, exportSchema = false)
@TypeConverters(HabitTypeConverters::class) // ¡Importante para que no falle al compilar!
abstract class AppDatabase : RoomDatabase() {

    abstract fun habitDao(): HabitDao
    abstract fun userDao(): UserDao
    abstract fun appRuleDao(): AppRuleDao

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
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `app_rules_table` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `appName` TEXT NOT NULL,
                        `timeLimitMinutes` INTEGER NOT NULL,
                        `isBlockMode` INTEGER NOT NULL,
                        `startTime` TEXT,
                        `endTime` TEXT,
                        `activeDays` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
