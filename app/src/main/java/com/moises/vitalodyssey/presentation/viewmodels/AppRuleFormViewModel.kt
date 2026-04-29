package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.domain.model.AppInfo
import com.moises.vitalodyssey.domain.model.AppRule
import com.moises.vitalodyssey.domain.repository.AppUsageRepository
import com.moises.vitalodyssey.domain.usecase.apprules.DeleteAppRuleUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.GetAppRuleByIdUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.SaveAppRuleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppRuleFormUiState(
    val id: Int = 0,
    val packageName: String = "",
    val appName: String = "", // Original app name
    val ruleName: String = "", // Editable rule name (maps to AppRule.appName)
    val timeLimitMinutes: Int = 60,
    val isBlockMode: Boolean = false,
    val startTime: String = "09:00",
    val endTime: String = "17:00",
    val activeDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val installedApps: List<AppInfo> = emptyList(),
    val isSaved: Boolean = false,
    val isEnabled: Boolean = true
)

class AppRuleFormViewModel(
    private val saveAppRuleUseCase: SaveAppRuleUseCase,
    private val deleteAppRuleUseCase: DeleteAppRuleUseCase,
    private val getAppRuleByIdUseCase: GetAppRuleByIdUseCase,
    private val appUsageRepository: AppUsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppRuleFormUiState())
    val uiState: StateFlow<AppRuleFormUiState> = _uiState.asStateFlow()

    init {
        loadInstalledApps()
    }

    fun loadRule(id: Int) {
        if (id == 0) return
        viewModelScope.launch {
            getAppRuleByIdUseCase(id)?.let { rule ->
                _uiState.update {
                    it.copy(
                        id = rule.id,
                        packageName = rule.packageName,
                        ruleName = rule.appName,
                        timeLimitMinutes = rule.timeLimitMinutes,
                        isBlockMode = rule.isBlockMode,
                        startTime = rule.startTime ?: "09:00",
                        endTime = rule.endTime ?: "17:00",
                        activeDays = rule.activeDays,
                        isEnabled = rule.isEnabled
                    )
                }
            }
        }
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = appUsageRepository.getInstalledApps()
            _uiState.update { it.copy(installedApps = apps) }
        }
    }

    fun onPackageSelected(app: AppInfo) {
        _uiState.update { state ->
            val newRuleName = if (state.ruleName.isEmpty() || state.ruleName == state.appName) {
                app.name
            } else {
                state.ruleName
            }
            state.copy(
                packageName = app.packageName,
                appName = app.name,
                ruleName = newRuleName
            )
        }
    }

    fun onRuleNameChange(name: String) {
        _uiState.update { it.copy(ruleName = name) }
    }

    fun onTimeLimitChange(minutes: Int) {
        _uiState.update { it.copy(timeLimitMinutes = minutes) }
    }

    fun onBlockModeChange(isBlock: Boolean) {
        _uiState.update { it.copy(isBlockMode = isBlock) }
    }

    fun onStartTimeChange(time: String) {
        _uiState.update { it.copy(startTime = time) }
    }

    fun onEndTimeChange(time: String) {
        _uiState.update { it.copy(endTime = time) }
    }

    fun onDaysChanged(days: List<Int>) {
        _uiState.update { it.copy(activeDays = days) }
    }

    fun saveRule() {
        viewModelScope.launch {
            val state = _uiState.value
            val rule = AppRule(
                id = state.id,
                packageName = state.packageName,
                appName = state.ruleName, // Usamos ruleName como el nombre legible
                timeLimitMinutes = state.timeLimitMinutes,
                isBlockMode = state.isBlockMode,
                startTime = if (state.isBlockMode) state.startTime else null,
                endTime = if (state.isBlockMode) state.endTime else null,
                activeDays = state.activeDays,
                isEnabled = state.isEnabled
            )
            saveAppRuleUseCase(rule)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun deleteRule() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.id != 0) {
                val rule = AppRule(
                    id = state.id,
                    packageName = state.packageName,
                    appName = state.ruleName,
                    timeLimitMinutes = state.timeLimitMinutes,
                    activeDays = state.activeDays
                )
                deleteAppRuleUseCase(rule)
                _uiState.update { it.copy(isSaved = true) }
            }
        }
    }
}
