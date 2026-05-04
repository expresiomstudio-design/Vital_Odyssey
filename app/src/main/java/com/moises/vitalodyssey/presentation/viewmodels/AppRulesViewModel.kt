package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.data.local.AppRuleDao
import com.moises.vitalodyssey.domain.model.AppRuleWithUsage
import com.moises.vitalodyssey.domain.usecase.apprules.GetTrackedAppsUsageUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.SaveAppRuleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.moises.vitalodyssey.domain.repository.AppUsageRepository

data class AppRulesUiState(
    val rules: List<AppRuleWithUsage> = emptyList(),
    val isLoading: Boolean = true,
    val hasPermission: Boolean = true
)

class AppRulesViewModel(
    private val getTrackedAppsUsageUseCase: GetTrackedAppsUsageUseCase,
    private val appRuleDao: AppRuleDao,
    private val saveAppRuleUseCase: SaveAppRuleUseCase,
    private val appUsageRepository: AppUsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppRulesUiState())
    val uiState: StateFlow<AppRulesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTrackedAppsUsageUseCase().collect { rulesWithUsage ->
                _uiState.update { it.copy(rules = rulesWithUsage, isLoading = false) }
            }
        }
        checkPermission()
    }

    fun checkPermission() {
        viewModelScope.launch {
            val hasPerm = appUsageRepository.hasUsageStatsPermission()
            _uiState.update { it.copy(hasPermission = hasPerm) }
        }
    }

    fun toggleRuleState(ruleId: Int, isEnabled: Boolean) {
        viewModelScope.launch {
            appRuleDao.getRuleById(ruleId)?.let { entity ->
                val updatedRule = entity.toDomain().copy(isEnabled = isEnabled)
                saveAppRuleUseCase(updatedRule)
            }
        }
    }
}
