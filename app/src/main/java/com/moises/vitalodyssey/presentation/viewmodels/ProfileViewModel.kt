package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.data.local.UserPreferencesManager
import com.moises.vitalodyssey.domain.usecase.CalculatePlayerStatsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ProfileUiState(
    val level: Int = 1,
    val hpText: String = "1000/1000",
    val attackStat: Int = 100,
    val defenseStat: Int = 10,
    val maxHp: Int = 1000,
    val currentHp: Int = 1000,
    val playerName: String = "Moisés Sánchez",
    val playerClass: String = "MAGO DE GREMIO • CLASE DE HONOR"
)

class ProfileViewModel(
    private val userPrefs: UserPreferencesManager,
    private val calculateStats: CalculatePlayerStatsUseCase
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = userPrefs.userPrefsFlow.map { prefs ->
        val stats = calculateStats(prefs.level)
        
        ProfileUiState(
            level = prefs.level,
            hpText = "${prefs.currentHp}/${stats.maxHp}",
            attackStat = stats.baseAttack,
            defenseStat = stats.baseDefense,
            maxHp = stats.maxHp,
            currentHp = prefs.currentHp
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState()
    )
}
