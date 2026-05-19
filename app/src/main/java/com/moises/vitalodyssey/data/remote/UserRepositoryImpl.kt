package com.moises.vitalodyssey.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.moises.vitalodyssey.data.local.*
import com.moises.vitalodyssey.domain.model.UserProfile
import com.moises.vitalodyssey.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userPrefs: UserPreferencesManager,
    private val context: android.content.Context,
    private val databaseManager: DatabaseManager
) : UserRepository {

    // DAOs dinámicos: SIEMPRE apuntan a la base de datos del usuario actual
    private val userDao: UserDao get() = databaseManager.getDatabaseSync().userDao()
    private val habitDao: HabitDao get() = databaseManager.getDatabaseSync().habitDao()
    private val appRuleDao: AppRuleDao get() = databaseManager.getDatabaseSync().appRuleDao()
    private val bossDao: BossDao get() = databaseManager.getDatabaseSync().bossDao()

    private val currentUid: String?
        get() = auth.currentUser?.uid

    override fun getUserProfile(): Flow<UserProfile?> {
        val uid = currentUid ?: return kotlinx.coroutines.flow.flowOf(null)
        return userDao.getUser(uid).map { it?.toDomain() }
    }

    override suspend fun getUserProfileOnce(): UserProfile? = withContext(Dispatchers.IO) {
        currentUid?.let { uid ->
            userDao.getUserOnce(uid)?.toDomain()
        }
    }

    override suspend fun syncUserToCloud(): Unit = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        userDao.getUser(uid).firstOrNull()?.let { userEntity ->
            firestore.collection("users").document(uid)
                .set(userEntity, SetOptions.merge())
                .await()
        }
    }

    override suspend fun fetchUserFromCloud(): Unit = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        val snapshot = firestore.collection("users").document(uid).get().await()
        snapshot.toObject(UserEntity::class.java)?.let { userEntity ->
            userDao.insertOrUpdateUser(userEntity)
        }
    }

    override suspend fun updateStats(profile: UserProfile): Unit = withContext(Dispatchers.IO) {
        val userEntity = UserEntity.fromDomain(profile)
        userDao.insertOrUpdateUser(userEntity)
        scheduleCloudSync()
    }

    override suspend fun deleteUserAccount(): Unit = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        
        try {
            // 1. Eliminar de Firebase Auth PRIMERO (si esto falla por falta de re-auth, no borramos los datos aún)
            val user = auth.currentUser
            user?.delete()?.await()

            // 2. Eliminar Hábitos en Firestore (subcolección)
            val habitsSnapshot = firestore.collection("users").document(uid).collection("habits").get().await()
            habitsSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            // 3. Eliminar de Firestore (documento principal)
            firestore.collection("users").document(uid).delete().await()
            
            // 4. Limpiar todas las tablas de Room (sin borrar el archivo para no romper el InvalidationTracker)
            databaseManager.getDatabaseSync().clearAllTables()

            // 5. Limpiar DataStore
            userPrefs.clearAll()
            
            auth.signOut()
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw e 
        } catch (e: Exception) {
            // Re-lanzamos cualquier otra excepción para que el ViewModel la maneje
            throw e
        }
    }

    override suspend fun syncHabitsToCloud(): Unit = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        habitDao.getAllHabitsOnce().forEach { habit ->
            firestore.collection("users").document(uid)
                .collection("habits").document(habit.id.toString())
                .set(habit, SetOptions.merge())
                .await()
        }
    }

    override suspend fun fetchHabitsFromCloud(): Unit = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        val snapshot = firestore.collection("users").document(uid).collection("habits").get().await()
        android.util.Log.d("CloudSync", "fetchHabitsFromCloud: ${snapshot.documents.size} documents found")
        snapshot.documents.forEach { doc ->
            try {
                doc.toObject(com.moises.vitalodyssey.domain.model.Habit::class.java)?.let { habit ->
                    android.util.Log.d("CloudSync", "  Inserting habit: id=${habit.id}, name=${habit.name}")
                    habitDao.insertHabit(habit)
                }
            } catch (e: Exception) {
                android.util.Log.e("CloudSync", "Error parsing habit ${doc.id}: ${e.message}")
            }
        }
    }

    private suspend fun fetchRulesFromCloud(): Unit = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        val snapshot = firestore.collection("users").document(uid).collection("rules").get().await()
        android.util.Log.d("CloudSync", "fetchRulesFromCloud: ${snapshot.documents.size} documents found")
        snapshot.documents.forEach { doc ->
            try {
                doc.toObject(AppRuleEntity::class.java)?.let { rule ->
                    android.util.Log.d("CloudSync", "  Inserting rule: id=${rule.id}, name=${rule.appName}")
                    appRuleDao.insertRule(rule)
                }
            } catch (e: Exception) {
                android.util.Log.e("CloudSync", "Error parsing rule ${doc.id}: ${e.message}")
            }
        }
    }

    private suspend fun fetchBossesFromCloud(): Unit = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        val snapshot = firestore.collection("users").document(uid).collection("bosses").get().await()
        android.util.Log.d("CloudSync", "fetchBossesFromCloud: ${snapshot.documents.size} documents found")
        snapshot.documents.forEach { doc ->
            try {
                doc.toObject(BossEntity::class.java)?.let { boss ->
                    android.util.Log.d("CloudSync", "  Inserting boss: id=${boss.id}")
                    bossDao.insertBoss(boss)
                }
            } catch (e: Exception) {
                android.util.Log.e("CloudSync", "Error parsing boss ${doc.id}: ${e.message}")
            }
        }
    }

    private suspend fun fetchHabitLogsFromCloud(): Unit = withContext(Dispatchers.IO) {
        val uid = currentUid ?: run {
            android.util.Log.e("CloudSync", "fetchHabitLogsFromCloud: uid is null, skipping")
            return@withContext
        }
        android.util.Log.d("CloudSync", "fetchHabitLogsFromCloud: starting Firestore query...")
        try {
            val snapshot = firestore.collection("users").document(uid).collection("habit_logs").get().await()
            android.util.Log.d("CloudSync", "fetchHabitLogsFromCloud: ${snapshot.documents.size} documents found")
            var inserted = 0
            snapshot.documents.forEach { doc ->
                try {
                    doc.toObject(com.moises.vitalodyssey.domain.model.HabitLog::class.java)?.let { log ->
                        if (log.habitId != 0 && log.date.isNotEmpty()) {
                            habitDao.insertLog(log)
                            inserted++
                        } else {
                            android.util.Log.w("CloudSync", "Skipping log with empty fields: id=${log.id} habitId=${log.habitId} date='${log.date}'")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CloudSync", "Error parsing habit_log ${doc.id}: ${e.message}", e)
                }
            }
            android.util.Log.d("CloudSync", "fetchHabitLogsFromCloud: $inserted/${snapshot.documents.size} logs restored successfully")
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "fetchHabitLogsFromCloud: FAILED to fetch from Firestore", e)
        }
    }

    override suspend fun syncDatabasesOnLogin(): Unit = withContext(Dispatchers.IO) {
        val uid = currentUid
        android.util.Log.d("CloudSync", "=== syncDatabasesOnLogin START === uid=$uid")
        if (uid == null) {
            android.util.Log.e("CloudSync", "ABORT login sync: uid is null")
            return@withContext
        }

        try {
            // 1. Obtener UserEntity de la nube
            val cloudSnapshot = firestore.collection("users").document(uid).get().await()
            val cloudUser = cloudSnapshot.toObject(UserEntity::class.java)
            android.util.Log.d("CloudSync", "Cloud user: ${if (cloudUser != null) "FOUND (lastUpdated=${cloudUser.lastUpdated})" else "NULL"}")

            // 2. Obtener UserEntity local
            val localUser = userDao.getUserOnce(uid)
            android.util.Log.d("CloudSync", "Local user: ${if (localUser != null) "FOUND (lastUpdated=${localUser.lastUpdated})" else "NULL"}")

            // Si no hay datos en la nube, pero sí locales (muy raro, pero posible), subir
            if (cloudUser == null && localUser != null) {
                android.util.Log.d("CloudSync", "Case: local only → uploading to cloud")
                performFullCloudSync()
                return@withContext
            }

            // Si no hay datos locales, pero sí en la nube (ej. primera vez instalando o tras limpiar datos), descargar
            if (localUser == null && cloudUser != null) {
                android.util.Log.d("CloudSync", "Case: cloud only → downloading to local")
                fetchUserFromCloud()
                fetchHabitsFromCloud()
                fetchRulesFromCloud()
                fetchBossesFromCloud()
                fetchHabitLogsFromCloud()
                return@withContext
            }

            // Si ambos existen, comparar lastUpdated
            if (cloudUser != null && localUser != null) {
                if (cloudUser.lastUpdated > localUser.lastUpdated) {
                    android.util.Log.d("CloudSync", "Case: cloud is newer → downloading")
                    fetchUserFromCloud()
                    fetchHabitsFromCloud()
                    fetchRulesFromCloud()
                    fetchBossesFromCloud()
                    fetchHabitLogsFromCloud()
                } else if (localUser.lastUpdated > cloudUser.lastUpdated) {
                    android.util.Log.d("CloudSync", "Case: local is newer → uploading")
                    performFullCloudSync()
                } else {
                    android.util.Log.d("CloudSync", "Case: both same timestamp → no action")
                }
            }

            // Si ninguno existe
            if (cloudUser == null && localUser == null) {
                android.util.Log.d("CloudSync", "Case: neither exists → fresh user, no sync needed")
            }

            android.util.Log.d("CloudSync", "=== syncDatabasesOnLogin END ===")
        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "=== syncDatabasesOnLogin FAILED ===", e)
        }
    }

    override suspend fun performFullCloudSync(): Unit = withContext(Dispatchers.IO) {
        val uid = currentUid
        android.util.Log.d("CloudSync", "=== performFullCloudSync START === uid=$uid")
        if (uid == null) {
            android.util.Log.e("CloudSync", "ABORT: uid is null, user not authenticated")
            return@withContext
        }

        try {
            val userDoc = firestore.collection("users").document(uid)

            // ── PASO 1: Leer el estado local PRIMERO ──────────────────────────────
            val userEntity = userDao.getUserOnce(uid)
            val habits    = habitDao.getAllHabitsOnce()
            val rules     = appRuleDao.getAllRulesOnce()
            val boss      = bossDao.getCurrentBoss()
            val logs      = habitDao.getAllLogsOnce()

            android.util.Log.d("CloudSync", "Local state → user:${if (userEntity!=null) "FOUND" else "NULL"} habits:${habits.size} rules:${rules.size} boss:${if (boss!=null) "FOUND" else "NULL"} logs:${logs.size}")

            // ── PASO 2: Borrar subcolecciones obsoletas en la nube ────────────────
            suspend fun deleteCollection(collectionRef: com.google.firebase.firestore.CollectionReference) {
                val oldDocs = collectionRef.get().await()
                android.util.Log.d("CloudSync", "Deleting ${oldDocs.size()} old docs from ${collectionRef.path}")
                if (oldDocs.isEmpty) return
                oldDocs.documents.chunked(400).forEach { chunk ->
                    val deleteBatch = firestore.batch()
                    chunk.forEach { deleteBatch.delete(it.reference) }
                    deleteBatch.commit().await()
                }
            }

            deleteCollection(userDoc.collection("habits"))
            deleteCollection(userDoc.collection("rules"))
            deleteCollection(userDoc.collection("bosses"))
            deleteCollection(userDoc.collection("habit_logs"))

            // ── PASO 3: Escribir el estado actual ────────────────────────────────
            // Los logs pueden ser muchos, usar batches de 400 (margen seguro bajo el límite de 500)
            suspend fun writeBatch(items: List<Pair<com.google.firebase.firestore.DocumentReference, Any>>) {
                items.chunked(400).forEach { chunk ->
                    val batch = firestore.batch()
                    chunk.forEach { (ref, data) -> batch.set(ref, data) }
                    batch.commit().await()
                }
            }

            // User Profile
            userEntity?.let {
                val updatedUser = it.copy(lastUpdated = System.currentTimeMillis())
                val profileBatch = firestore.batch()
                profileBatch.set(userDoc, updatedUser, SetOptions.merge())
                profileBatch.commit().await()
                userDao.insertOrUpdateUser(updatedUser)
            }

            // Habits
            writeBatch(habits.map { habit ->
                userDoc.collection("habits").document(habit.id.toString()) to habit
            })

            // Rules
            writeBatch(rules.map { rule ->
                userDoc.collection("rules").document(rule.id.toString()) to (rule as Any)
            })

            // Boss
            boss?.let {
                val bossBatch = firestore.batch()
                bossBatch.set(userDoc.collection("bosses").document(it.id.toString()), it)
                bossBatch.commit().await()
            }

            // Habit Logs (el historial completo)
            writeBatch(logs.map { log ->
                userDoc.collection("habit_logs").document(log.id.toString()) to (log as Any)
            })

            val totalOps = habits.size + rules.size + (if (boss != null) 1 else 0) + logs.size + (if (userEntity != null) 1 else 0)
            android.util.Log.d("CloudSync", "Written $totalOps documents total (including ${logs.size} logs)")

            userPrefs.updateLastCloudSyncTime(System.currentTimeMillis())
            android.util.Log.d("CloudSync", "=== performFullCloudSync SUCCESS ===")

        } catch (e: Exception) {
            android.util.Log.e("CloudSync", "=== performFullCloudSync FAILED ===", e)
            throw e
        }
    }

    override suspend fun scheduleCloudSync(): Unit = withContext(Dispatchers.IO) {
        val lastSyncTime = userPrefs.lastCloudSyncTimeFlow.firstOrNull() ?: 0L
        val currentTime = System.currentTimeMillis()
        val timeSinceLastSync = currentTime - lastSyncTime
        
        val twelveHoursMillis = 12 * 60 * 60 * 1000L
        val minWaitMillis = 10 * 60 * 1000L // 10 minutes

        val delayMillis = if (timeSinceLastSync >= twelveHoursMillis) {
            minWaitMillis
        } else {
            maxOf(minWaitMillis, twelveHoursMillis - timeSinceLastSync)
        }

        val syncWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.moises.vitalodyssey.worker.SyncWorker>()
            .setInitialDelay(delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
            )
            .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "CloudSyncWork",
            androidx.work.ExistingWorkPolicy.REPLACE,
            syncWorkRequest
        )
    }
}
