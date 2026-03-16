package com.moises.vitalodyssey.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.moises.vitalodyssey.domain.model.AuthResult
import com.moises.vitalodyssey.domain.repository.AuthRepository
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth
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

    override fun logout() {
        firebaseAuth.signOut()
    }
}
