package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.domain.model.AppInfo
import com.moises.vitalodyssey.domain.model.AppRule
import com.moises.vitalodyssey.domain.repository.AppUsageRepository
import com.moises.vitalodyssey.domain.repository.UserRepository
import com.moises.vitalodyssey.domain.usecase.apprules.DeleteAppRuleUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.GetAppRuleByIdUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.GetAppRulesUseCase
import com.moises.vitalodyssey.domain.usecase.apprules.SaveAppRuleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val isEnabled: Boolean = true,
    val cutoffTime: String = "00:00",
    val hasOtherRules: Boolean = false,
    val errorMessage: String? = null
)

class AppRuleFormViewModel(
    private val saveAppRuleUseCase: SaveAppRuleUseCase,
    private val deleteAppRuleUseCase: DeleteAppRuleUseCase,
    private val getAppRuleByIdUseCase: GetAppRuleByIdUseCase,
    private val appUsageRepository: AppUsageRepository,
    private val userRepository: UserRepository,
    private val getAppRulesUseCase: GetAppRulesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppRuleFormUiState())
    val uiState: StateFlow<AppRuleFormUiState> = _uiState.asStateFlow()

    init {
        loadInstalledApps()
        viewModelScope.launch {
            userRepository.getUserProfileOnce()?.cutoffTime?.let { time -> 
                _uiState.update { it.copy(cutoffTime = time) } 
            }
        }
    }

    fun loadRule(id: Int) {
        if (id == 0) return
        viewModelScope.launch {
            getAppRuleByIdUseCase(id)?.let { rule ->
                _uiState.update {
                    it.copy(
                        id = rule.id,
                        packageName = rule.packageName,
                        appName = rule.appName, // Fix bug: loading original name
                        ruleName = rule.appName,
                        timeLimitMinutes = rule.timeLimitMinutes,
                        isBlockMode = rule.isBlockMode,
                        startTime = rule.startTime ?: "09:00",
                        endTime = rule.endTime ?: "17:00",
                        activeDays = rule.activeDays,
                        isEnabled = rule.isEnabled
                    )
                }
                checkExistingRules(rule.packageName)
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
                ruleName = newRuleName,
                errorMessage = null
            )
        }
        checkExistingRules(app.packageName)
    }

    private fun checkExistingRules(pkgName: String) {
        viewModelScope.launch {
            getAppRulesUseCase().collect { rules ->
                val currentId = _uiState.value.id
                val hasOthers = rules.any { it.packageName == pkgName && it.id != currentId }
                _uiState.update { it.copy(hasOtherRules = hasOthers, errorMessage = null) }
            }
        }
    }

    fun onRuleNameChange(name: String) {
        _uiState.update { it.copy(ruleName = name, errorMessage = null) }
    }

    fun onTimeLimitChange(minutes: Int) {
        _uiState.update { it.copy(timeLimitMinutes = minutes, errorMessage = null) }
    }

    fun onBlockModeChange(isBlock: Boolean) {
        _uiState.update { it.copy(isBlockMode = isBlock, errorMessage = null) }
    }

    fun onStartTimeChange(time: String) {
        _uiState.update { it.copy(startTime = time, errorMessage = null) }
    }

    fun onEndTimeChange(time: String) {
        _uiState.update { it.copy(endTime = time, errorMessage = null) }
    }

    fun onDaysChanged(days: List<Int>) {
        _uiState.update { it.copy(activeDays = days, errorMessage = null) }
    }

    fun saveRule() {
        val state = _uiState.value
        
        if (state.packageName.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Debes seleccionar una aplicación.") }
            return
        }
        if (state.ruleName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Debes darle un nombre a la regla.") }
            return
        }
        if (!state.isBlockMode && state.timeLimitMinutes <= 0) {
            _uiState.update { it.copy(errorMessage = "El límite de tiempo debe ser mayor a 0.") }
            return
        }
        if (state.activeDays.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Debes seleccionar al menos un día activo.") }
            return
        }

        viewModelScope.launch {
            // Validador de duplicados e idénticos
            val currentRules = getAppRulesUseCase().first()
            val isDuplicate = currentRules.any { rule ->
                rule.id != state.id &&
                rule.packageName == state.packageName &&
                rule.isBlockMode == state.isBlockMode &&
                (!state.isBlockMode || (rule.startTime == state.startTime && rule.endTime == state.endTime))
            }

            if (isDuplicate) {
                _uiState.update { it.copy(errorMessage = "Ya existe una regla idéntica para esta aplicación.") }
                return@launch
            }

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
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                userRepository.scheduleCloudSync()
            }
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
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    userRepository.scheduleCloudSync()
                }
            }
        }
    }
}
