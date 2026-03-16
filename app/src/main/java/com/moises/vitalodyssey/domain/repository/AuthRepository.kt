package com.moises.vitalodyssey.domain.repository

import com.moises.vitalodyssey.domain.model.AuthResult

interface AuthRepository {
    suspend fun loginWithEmail(email: String, pass: String): AuthResult
    suspend fun loginWithGoogle(idToken: String): AuthResult
    fun logout()
}
