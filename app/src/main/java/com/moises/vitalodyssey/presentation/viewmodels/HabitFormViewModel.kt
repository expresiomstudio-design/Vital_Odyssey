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
    val targetValue: Float = 1f,
    val targetType: TargetType = TargetType.AT_LEAST,
    val frequencyType: String = "DAILY",
    val frequencyTarget: Int = 1,
    val startDate: String = "",
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false
)

class HabitFormViewModel(
    private val habitDao: HabitDao,
    private val userRepository: UserRepository
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
                    targetValue = it.targetValue,
                    targetType = it.targetType,
                    frequencyType = it.frequencyType,
                    frequencyTarget = it.frequencyTarget,
                    startDate = it.startDate,
                    isEditMode = true,
                    isLoading = false
                )
            }
        }
    }

    fun onNameChange(name: String) { uiState = uiState.copy(name = name) }
    fun onNoteChange(note: String) { uiState = uiState.copy(note = note) }
    fun onRoleChange(role: HabitRole) { uiState = uiState.copy(role = role) }
    fun onTypeChange(type: HabitType) { 
        if (!uiState.isEditMode) {
            uiState = uiState.copy(type = type) 
        }
    }
    fun onUnitChange(unit: String) { uiState = uiState.copy(unit = unit) }
    fun onTargetValueChange(value: Float) { uiState = uiState.copy(targetValue = value) }
    fun onFrequencyTypeChange(type: String) { uiState = uiState.copy(frequencyType = type) }
    fun onFrequencyTargetChange(target: Int) { uiState = uiState.copy(frequencyTarget = target) }

    fun saveHabit() {
        viewModelScope.launch {
            val habit = Habit(
                id = uiState.id,
                firestoreId = if (uiState.id == 0) UUID.randomUUID().toString() else "",
                name = uiState.name,
                note = uiState.note,
                role = uiState.role,
                type = uiState.type,
                unit = if (uiState.type == HabitType.MEASURABLE) uiState.unit else null,
                targetValue = uiState.targetValue,
                targetType = uiState.targetType,
                frequencyType = uiState.frequencyType,
                frequencyTarget = uiState.frequencyTarget,
                startDate = if (uiState.id == 0) LocalDate.now().toString() else uiState.startDate
            )

            if (uiState.isEditMode) {
                habitDao.updateHabit(habit)
            } else {
                habitDao.insertHabit(habit)
            }
            
            userRepository.syncHabitsToCloud()
            uiState = uiState.copy(isSaved = true)
        }
    }

    fun deleteHabit() {
        if (uiState.id == 0) return
        viewModelScope.launch {
            val habit = habitDao.getHabitById(uiState.id)
            habit?.let {
                habitDao.deleteHabit(it)
                userRepository.syncHabitsToCloud()
                uiState = uiState.copy(isSaved = true)
            }
        }
    }
}