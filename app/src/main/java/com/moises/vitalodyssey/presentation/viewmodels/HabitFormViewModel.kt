package com.moises.vitalodyssey.presentation.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.data.local.HabitDao
import com.moises.vitalodyssey.domain.model.Habit
import com.moises.vitalodyssey.domain.model.HabitRole
import com.moises.vitalodyssey.domain.model.HabitType
import com.moises.vitalodyssey.domain.model.TargetType
import com.moises.vitalodyssey.domain.repository.UserRepository
import com.moises.vitalodyssey.domain.usecase.RecalculateHabitScoresUseCase
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class HabitFormUiState(
    val id: Int = 0,
    val name: String = "",
    val note: String = "",
    val role: HabitRole = HabitRole.OFFENSIVE,
    val type: HabitType = HabitType.BOOLEAN,
    val unit: String = "",
    val targetValueInput: String = "", // Usamos String para el input de la UI
    val targetType: TargetType = TargetType.AT_LEAST,
    val frequencyType: String = "DAILY",
    val frequencyTarget: Int = 1,
    val isCumulative: Boolean = false,
    val startDate: String = "",
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class HabitFormViewModel(
    private val habitDao: HabitDao,
    private val userRepository: UserRepository,
    private val recalculateHabitScoresUseCase: RecalculateHabitScoresUseCase
) : ViewModel() {

    var uiState by mutableStateOf(HabitFormUiState())
        private set

    fun loadHabit(habitId: Int) {
        if (habitId <= 0) return
        
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val habit = habitDao.getHabitById(habitId)
            habit?.let {
                uiState = uiState.copy(
                    id = it.id,
                    name = it.name,
                    note = it.note,
                    role = it.role,
                    type = it.type,
                    unit = it.unit ?: "",
                    targetValueInput = if (it.targetValue % 1 == 0f) it.targetValue.toInt().toString() else it.targetValue.toString(),
                    targetType = it.targetType,
                    frequencyType = it.frequencyType,
                    frequencyTarget = it.frequencyTarget,
                    isCumulative = it.isCumulative,
                    startDate = it.startDate,
                    isEditMode = true,
                    isLoading = false
                )
            }
        }
    }

    fun onNameChange(name: String) { uiState = uiState.copy(name = name, errorMessage = null) }
    fun onNoteChange(note: String) { uiState = uiState.copy(note = note) }
    fun onRoleChange(role: HabitRole) { uiState = uiState.copy(role = role) }
    fun onTypeChange(type: HabitType) { 
        if (!uiState.isEditMode) {
            uiState = uiState.copy(type = type) 
        }
    }
    fun onUnitChange(unit: String) { uiState = uiState.copy(unit = unit, errorMessage = null) }
    
    fun onTargetValueChange(input: String) {
        // Validación estricta
        val sanitized = input.replace(",", ".")
        val dotCount = sanitized.count { it == '.' }
        
        if (dotCount <= 1 && sanitized.all { it.isDigit() || it == '.' }) {
            uiState = uiState.copy(targetValueInput = sanitized, errorMessage = null)
        }
    }
    
    fun onFrequencyTypeChange(type: String) { uiState = uiState.copy(frequencyType = type) }
    fun onFrequencyTargetChange(target: Int) { uiState = uiState.copy(frequencyTarget = target) }
    fun onIsCumulativeChange(isCumulative: Boolean) { uiState = uiState.copy(isCumulative = isCumulative) }

    fun saveHabit() {
        if (uiState.name.isBlank()) {
            uiState = uiState.copy(errorMessage = "El nombre del hábito no puede estar vacío.")
            return
        }

        if (uiState.type == HabitType.MEASURABLE) {
            val target = uiState.targetValueInput.toFloatOrNull()
            if (target == null || target <= 0f) {
                uiState = uiState.copy(errorMessage = "La meta debe ser un número mayor a 0.")
                return
            }
            if (uiState.unit.isBlank()) {
                uiState = uiState.copy(errorMessage = "Debes especificar una unidad de medida.")
                return
            }
        }

        viewModelScope.launch {
            val targetValue = uiState.targetValueInput.toFloatOrNull() ?: 1f
            
            val habit = Habit(
                id = uiState.id,
                firestoreId = if (uiState.id == 0) UUID.randomUUID().toString() else "",
                name = uiState.name,
                note = uiState.note,
                role = uiState.role,
                type = uiState.type,
                unit = if (uiState.type == HabitType.MEASURABLE) uiState.unit else null,
                targetValue = targetValue,
                targetType = uiState.targetType,
                frequencyType = uiState.frequencyType,
                frequencyTarget = uiState.frequencyTarget,
                isCumulative = uiState.isCumulative,
                startDate = if (uiState.id == 0) LocalDate.now().toString() else uiState.startDate
            )

            val finalId = if (uiState.isEditMode) {
                habitDao.updateHabit(habit)
                habit.id
            } else {
                habitDao.insertHabit(habit).toInt()
            }
            
            // Forzar recálculo inicial o tras edición de meta
            recalculateHabitScoresUseCase(finalId)
            
            uiState = uiState.copy(isSaved = true)
            // Lanza la sincronización sin bloquear la actualización de la UI
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                userRepository.scheduleCloudSync()
            }
        }
    }

    fun deleteHabit() {
        if (uiState.id == 0) return
        viewModelScope.launch {
            val habit = habitDao.getHabitById(uiState.id)
            habit?.let {
                habitDao.deleteHabit(it)
                uiState = uiState.copy(isSaved = true)
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    userRepository.scheduleCloudSync()
                }
            }
        }
    }
}