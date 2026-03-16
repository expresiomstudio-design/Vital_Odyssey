package com.moises.vitalodyssey.domain.model

data class AuthResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null
)
