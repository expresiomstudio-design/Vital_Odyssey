package com.moises.vitalodyssey.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bosses")
data class BossEntity(
    @PrimaryKey(autoGenerate = false) val id: Int = 1, // Solo habrá 1 jefe activo a la vez
    val name: String,
    val imageAssetId: String, // Para saber qué imagen de los 6 jefes cargar
    val difficulty: String,
    val maxHp: Int,
    val currentHp: Int,
    val baseAttack: Int,
    val isDefeated: Boolean = false
)
