package com.moises.vitalodyssey.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {

    @Query("SELECT * FROM app_rules_table")
    fun getAllRules(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules_table")
    suspend fun getAllRulesOnce(): List<AppRuleEntity>

    @Query("SELECT * FROM app_rules_table WHERE id = :id")
    suspend fun getRuleById(id: Int): AppRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AppRuleEntity): Long

    @Update
    suspend fun updateRule(rule: AppRuleEntity)

    @Delete
    suspend fun deleteRule(rule: AppRuleEntity)
}
