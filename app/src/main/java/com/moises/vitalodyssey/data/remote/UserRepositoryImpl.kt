package com.moises.vitalodyssey.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.moises.vitalodyssey.data.local.UserDao
import com.moises.vitalodyssey.data.local.UserEntity
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
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UserRepository {

    private val currentUid: String?
        get() = auth.currentUser?.uid

    override fun getUserProfile(): Flow<UserProfile?> {
        val uid = currentUid ?: return kotlinx.coroutines.flow.flowOf(null)
        return userDao.getUser(uid).map { it?.toDomain() }
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
        
        // 1. Eliminar de Firestore
        firestore.collection("users").document(uid).delete().await()
        
        // 2. Eliminar de Room
        userDao.deleteUser(uid)
        
        // 3. Eliminar de Firebase Auth
        auth.currentUser?.delete()?.await()
    }
}
