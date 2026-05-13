package com.moises.vitalodyssey.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BossDao {
    @Query("SELECT * FROM bosses WHERE id = 1 LIMIT 1")
    fun getCurrentBossFlow(): Flow<BossEntity?>

    @Query("SELECT * FROM bosses WHERE id = 1 LIMIT 1")
    suspend fun getCurrentBoss(): BossEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoss(boss: BossEntity)

    @Update
    suspend fun updateBoss(boss: BossEntity)
    
    @Query("DELETE FROM bosses")
    suspend fun clearBosses()
}
