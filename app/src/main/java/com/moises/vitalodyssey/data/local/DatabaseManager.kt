package com.moises.vitalodyssey.data.local

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DatabaseManager(
    private val context: Context,
    private val auth: FirebaseAuth
) {
    private val databases = mutableMapOf<String, AppDatabase>()
    private val mutex = Mutex()

    suspend fun getDatabase(): AppDatabase {
        val uid = auth.currentUser?.uid ?: "anonymous"
        
        mutex.withLock {
            return getOrCreateDatabase(uid)
        }
    }

    // Para obtener DAOs temporalmente de forma sincrónica en inyección de dependencias.
    @Synchronized
    fun getDatabaseSync(): AppDatabase {
        val uid = auth.currentUser?.uid ?: "anonymous"
        return getOrCreateDatabase(uid)
    }

    private fun getOrCreateDatabase(uid: String): AppDatabase {
        var db = databases[uid]
        if (db == null || !db.isOpen) {
            val dbName = "vital_odyssey_db_$uid"
            db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                dbName
            )
                .addMigrations(
                    AppDatabase.MIGRATION_3_4,
                    AppDatabase.MIGRATION_4_5,
                    AppDatabase.MIGRATION_5_6,
                    AppDatabase.MIGRATION_7_8,
                    AppDatabase.MIGRATION_8_9
                )
                .fallbackToDestructiveMigration()
                .build()
            databases[uid] = db
        }
        return db
    }
    
    @Synchronized
    fun closeDatabase(uid: String) {
        databases[uid]?.close()
        databases.remove(uid)
    }
}
