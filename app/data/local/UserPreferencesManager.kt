package com.moises.vitalodyssey.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Variable global privada para inicializar el DataStore de forma segura y única
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vital_odyssey_prefs")

// Data class que representa el estado actual de nuestro Guerrero
data class UserPrefs(
    val level: Int,
    val currentXp: Int,
    val currentHp: Int,
    val cutoffTime: String,
    val healthGoalSteps: Int,
    val healthGoalSleep: Float
)

class UserPreferencesManager(private val context: Context) {

    // Definición de las "llaves" exactas con las que guardaremos los datos
    private object PreferencesKeys {
        val LEVEL = intPreferencesKey("level")
        val CURRENT_XP = intPreferencesKey("current_xp")
        val CURRENT_HP = intPreferencesKey("current_hp")
        val CUTOFF_TIME = stringPreferencesKey("cutoff_time")
        val HEALTH_GOAL_STEPS = intPreferencesKey("health_goal_steps")
        val HEALTH_GOAL_SLEEP = floatPreferencesKey("health_goal_sleep")
    }

    // Un "Flow" es como un canal de agua: cada vez que cambie un dato,
    // la interfaz (UI) se actualizará automáticamente sin que tengamos que hacer nada.
    val userPrefsFlow: Flow<UserPrefs> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            // Aquí asignamos los valores base matemáticos que definimos en el GDD
            val level = preferences[PreferencesKeys.LEVEL] ?: 1
            val currentXp = preferences[PreferencesKeys.CURRENT_XP] ?: 0
            val currentHp = preferences[PreferencesKeys.CURRENT_HP] ?: 1000 // Vida base Nivel 1
            val cutoffTime = preferences[PreferencesKeys.CUTOFF_TIME] ?: "23:59"
            val healthGoalSteps = preferences[PreferencesKeys.HEALTH_GOAL_STEPS] ?: 8000
            val healthGoalSleep = preferences[PreferencesKeys.HEALTH_GOAL_SLEEP] ?: 7.5f

            UserPrefs(level, currentXp, currentHp, cutoffTime, healthGoalSteps, healthGoalSleep)
        }

    // Funciones para que el Motor de Combate actualice las estadísticas
    suspend fun updateLevelAndXp(level: Int, xp: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LEVEL] = level
            preferences[PreferencesKeys.CURRENT_XP] = xp
        }
    }

    suspend fun updateHp(hp: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_HP] = hp
        }
    }

    suspend fun updateCutoffTime(time: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUTOFF_TIME] = time
        }
    }

    suspend fun updateHealthGoals(steps: Int, sleep: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HEALTH_GOAL_STEPS] = steps
            preferences[PreferencesKeys.HEALTH_GOAL_SLEEP] = sleep
        }
    }
}