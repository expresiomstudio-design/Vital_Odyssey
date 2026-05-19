package com.moises.vitalodyssey.domain.repository

import com.moises.vitalodyssey.domain.model.AuthResult

interface AuthRepository {
    suspend fun loginWithEmail(email: String, pass: String): AuthResult
    suspend fun registerWithEmail(email: String, pass: String): AuthResult
    suspend fun loginWithGoogle(idToken: String): AuthResult
    suspend fun reauthenticateWithEmail(password: String): AuthResult
    suspend fun reauthenticateWithGoogle(idToken: String): AuthResult
    suspend fun updateEmail(newEmail: String): AuthResult
    suspend fun updatePassword(newPassword: String): AuthResult
    suspend fun logout()
}
