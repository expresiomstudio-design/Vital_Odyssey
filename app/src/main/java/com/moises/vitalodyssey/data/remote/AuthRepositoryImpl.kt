package com.moises.vitalodyssey.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.moises.vitalodyssey.domain.model.AuthResult
import com.moises.vitalodyssey.domain.repository.AuthRepository
import com.moises.vitalodyssey.domain.repository.UserRepository
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository
) : AuthRepository {

    override suspend fun loginWithEmail(email: String, pass: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            if (result.user != null) {
                AuthResult(isSuccess = true)
            } else {
                AuthResult(isSuccess = false, errorMessage = "Error al iniciar sesión")
            }
        } catch (e: Exception) {
            AuthResult(isSuccess = false, errorMessage = e.localizedMessage)
        }
    }

    override suspend fun registerWithEmail(email: String, pass: String): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            if (result.user != null) {
                AuthResult(isSuccess = true)
            } else {
                AuthResult(isSuccess = false, errorMessage = "Error al registrar usuario")
            }
        } catch (e: Exception) {
            AuthResult(isSuccess = false, errorMessage = e.localizedMessage)
        }
    }

    override suspend fun loginWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            if (result.user != null) {
                AuthResult(isSuccess = true)
            } else {
                AuthResult(isSuccess = false, errorMessage = "Error al iniciar sesión con Google")
            }
        } catch (e: Exception) {
            AuthResult(isSuccess = false, errorMessage = e.localizedMessage)
        }
    }

    override suspend fun reauthenticateWithEmail(password: String): AuthResult {
        return try {
            val user = firebaseAuth.currentUser
            val email = user?.email
            if (user != null && email != null) {
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential).await()
                AuthResult(isSuccess = true)
            } else {
                AuthResult(isSuccess = false, errorMessage = "Usuario no autenticado")
            }
        } catch (e: Exception) {
            AuthResult(isSuccess = false, errorMessage = e.localizedMessage)
        }
    }

    override suspend fun reauthenticateWithGoogle(idToken: String): AuthResult {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                user.reauthenticate(credential).await()
                AuthResult(isSuccess = true)
            } else {
                AuthResult(isSuccess = false, errorMessage = "Usuario no autenticado")
            }
        } catch (e: Exception) {
            AuthResult(isSuccess = false, errorMessage = e.localizedMessage)
        }
    }

    override suspend fun logout() {
        try {
            userRepository.performFullCloudSync()
        } catch (e: Exception) {
            e.printStackTrace()
            // Even if sync fails (e.g. no internet), we must logout
        } finally {
            firebaseAuth.signOut()
        }
    }
}
