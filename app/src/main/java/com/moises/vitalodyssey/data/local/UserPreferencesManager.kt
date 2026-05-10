package com.moises.vitalodyssey.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.moises.vitalodyssey.domain.model.BodyType
import com.moises.vitalodyssey.domain.model.PlayerClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class Difficulty { EASY, NORMAL, HARD }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vital_odyssey_prefs")

data class UserPrefs(
    val level: Int,
    val currentXp: Int,
    val currentHp: Int,
    val cutoffTime: String,
    val healthGoalSteps: Int,
    val healthGoalSleep: Float,
    val difficulty: Difficulty,
    val currentStamina: Int,
    val presenceStreak: Int,
    val isLoggedIn: Boolean = false,
    val bodyType: String = "",
    val playerClass: String = "",
    val hasCompletedOnboarding: Boolean = false
)

class UserPreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val LEVEL = intPreferencesKey("level")
        val CURRENT_XP = intPreferencesKey("current_xp")
        val CURRENT_HP = intPreferencesKey("current_hp")
        val CUTOFF_TIME = stringPreferencesKey("cutoff_time")
        val HEALTH_GOAL_STEPS = intPreferencesKey("health_goal_steps")
        val HEALTH_GOAL_SLEEP = floatPreferencesKey("health_goal_sleep")
        val DIFFICULTY = stringPreferencesKey("difficulty")
        val CURRENT_STAMINA = intPreferencesKey("current_stamina")
        val PRESENCE_STREAK = intPreferencesKey("presence_streak")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val BODY_TYPE = stringPreferencesKey("body_type")
        val PLAYER_CLASS = stringPreferencesKey("player_class")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")

        // Health Connect & Manual Mode
        val HEALTH_CONNECT_ENABLED = booleanPreferencesKey("health_connect_enabled")
        val STEP_GOAL = intPreferencesKey("step_goal")
        val SLEEP_GOAL = floatPreferencesKey("sleep_goal")
        val LAST_MANUAL_STEPS = longPreferencesKey("last_manual_steps")
        val LAST_MANUAL_SLEEP = floatPreferencesKey("last_manual_sleep")
        val LAST_MANUAL_REPORT_DATE = stringPreferencesKey("last_manual_report_date")
    }

    val userPrefsFlow: Flow<UserPrefs> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val level = preferences[PreferencesKeys.LEVEL] ?: 1
            val currentXp = preferences[PreferencesKeys.CURRENT_XP] ?: 0
            val currentHp = preferences[PreferencesKeys.CURRENT_HP] ?: 1000
            val cutoffTime = preferences[PreferencesKeys.CUTOFF_TIME] ?: "23:59"
            val healthGoalSteps = preferences[PreferencesKeys.HEALTH_GOAL_STEPS] ?: 8000
            val healthGoalSleep = preferences[PreferencesKeys.HEALTH_GOAL_SLEEP] ?: 7.5f

            val difficultyStr = preferences[PreferencesKeys.DIFFICULTY] ?: Difficulty.NORMAL.name
            val difficulty = try {
                Difficulty.valueOf(difficultyStr)
            } catch (e: IllegalArgumentException) {
                Difficulty.NORMAL
            }
            
            val currentStamina = preferences[PreferencesKeys.CURRENT_STAMINA] ?: 100
            val presenceStreak = preferences[PreferencesKeys.PRESENCE_STREAK] ?: 0
            val isLoggedIn = preferences[PreferencesKeys.IS_LOGGED_IN] ?: false
            val bodyType = preferences[PreferencesKeys.BODY_TYPE] ?: ""
            val playerClass = preferences[PreferencesKeys.PLAYER_CLASS] ?: ""
            val hasCompletedOnboarding = preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false

            UserPrefs(
                level = level,
                currentXp = currentXp,
                currentHp = currentHp,
                cutoffTime = cutoffTime,
                healthGoalSteps = healthGoalSteps,
                healthGoalSleep = healthGoalSleep,
                difficulty = difficulty,
                currentStamina = currentStamina,
                presenceStreak = presenceStreak,
                isLoggedIn = isLoggedIn,
                bodyType = bodyType,
                playerClass = playerClass,
                hasCompletedOnboarding = hasCompletedOnboarding
            )
        }

    // ── Health Connect & Manual Mode Flows ──────────────────────────────────

    val healthConnectEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.HEALTH_CONNECT_ENABLED] ?: false }

    val stepGoalFlow: Flow<Int> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.STEP_GOAL] ?: 8000 }

    val sleepGoalFlow: Flow<Float> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.SLEEP_GOAL] ?: 7.5f }

    val lastManualStepsFlow: Flow<Long> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.LAST_MANUAL_STEPS] ?: 0L }

    val lastManualSleepFlow: Flow<Float> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.LAST_MANUAL_SLEEP] ?: 0f }

    val lastManualReportDateFlow: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferencesKeys.LAST_MANUAL_REPORT_DATE] ?: "" }

    // ── Suspend update functions ──────────────────────────────────────────────

    suspend fun setHealthConnectEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HEALTH_CONNECT_ENABLED] = enabled
        }
    }

    suspend fun setHealthGoals(steps: Int, sleep: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STEP_GOAL] = steps
            preferences[PreferencesKeys.SLEEP_GOAL] = sleep
        }
    }

    suspend fun setManualHealthReport(steps: Long, sleep: Float, date: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_MANUAL_STEPS] = steps
            preferences[PreferencesKeys.LAST_MANUAL_SLEEP] = sleep
            preferences[PreferencesKeys.LAST_MANUAL_REPORT_DATE] = date
        }
    }

    // ── Existing suspend functions ────────────────────────────────────────────

    suspend fun updateAuthStatus(isLoggedIn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_LOGGED_IN] = isLoggedIn
        }
    }

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

    suspend fun updateDifficulty(difficulty: Difficulty) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DIFFICULTY] = difficulty.name
        }
    }

    suspend fun updateStamina(stamina: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENT_STAMINA] = stamina.coerceIn(0, 100)
        }
    }

    suspend fun updatePresenceStreak(streak: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRESENCE_STREAK] = streak
        }
    }

    suspend fun completeOnboarding(bodyType: BodyType, playerClass: PlayerClass) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BODY_TYPE] = bodyType.name
            preferences[PreferencesKeys.PLAYER_CLASS] = playerClass.name
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = true
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
