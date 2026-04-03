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
    private val userDao: UserDao,
    private val habitDao: HabitDao,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userPrefs: UserPreferencesManager
) : UserRepository {

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
        
        currentUid?.let { uid ->
            firestore.collection("users").document(uid)
                .set(userEntity, SetOptions.merge())
                .await()
        }
    }

    override suspend fun deleteUserAccount(): Unit = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        
        try {
            // 1. Eliminar Hábitos en Firestore (subcolección)
            val habitsSnapshot = firestore.collection("users").document(uid).collection("habits").get().await()
            habitsSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }

            // 2. Eliminar de Firestore (documento principal)
            firestore.collection("users").document(uid).delete().await()
            
            // 3. Eliminar de Room
            userDao.deleteUser(uid)
            habitDao.deleteAllHabits()

            // 4. Limpiar DataStore
            userPrefs.clearAll()
            
            // 5. Eliminar de Firebase Auth
            val user = auth.currentUser
            user?.delete()?.await()
            auth.signOut()
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw e // Re-lanzamos para que el ViewModel pida re-autenticación
        } catch (e: Exception) {
            // Log o manejo de errores genérico
        }
    }

    override suspend fun syncHabitsToCloud(): Unit = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        habitDao.getAllHabits().firstOrNull()?.forEach { habit ->
            firestore.collection("users").document(uid)
                .collection("habits").document(habit.id.toString())
                .set(habit, SetOptions.merge())
                .await()
        }
    }

    override suspend fun fetchHabitsFromCloud(): Unit = withContext(Dispatchers.IO) {
        val uid = currentUid ?: return@withContext
        val snapshot = firestore.collection("users").document(uid).collection("habits").get().await()
        snapshot.toObjects(com.moises.vitalodyssey.domain.model.Habit::class.java).forEach { habit ->
            habitDao.insertHabit(habit)
        }
    }
}
