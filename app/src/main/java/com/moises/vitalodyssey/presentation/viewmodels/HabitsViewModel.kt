package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.data.local.HabitDao
import com.moises.vitalodyssey.domain.model.Habit
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.moises.vitalodyssey.domain.model.HabitRole
import com.moises.vitalodyssey.domain.model.HabitType

// Estado de la UI para la lista de hábitos
data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    val offensiveCount: Int = 0,
    val defensiveCount: Int = 0
)

class HabitsViewModel(
    private val habitDao: HabitDao
) : ViewModel() {

    // Observamos todos los hábitos y calculamos contadores para la UI
    val uiState: StateFlow<HabitsUiState> = habitDao.getAllHabits().map { habits ->
        HabitsUiState(
            habits = habits,
            offensiveCount = habits.count { it.role.name == "OFFENSIVE" },
            defensiveCount = habits.count { it.role.name == "DEFENSIVE" }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsUiState()
    )

    // Función para agregar un nuevo hábito
    fun addHabit(name: String, role: HabitRole, type: HabitType) {
        viewModelScope.launch {
            habitDao.insertHabit(
                Habit(
                    name = name,
                    role = role,
                    type = type
                )
            )
        }
    }

    // Función para marcar/desmarcar hábitos booleanos
    fun toggleHabitStatus(habitId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            habitDao.updateHabitStatus(habitId, isCompleted)
        }
    }

    // Función para actualizar el progreso de hábitos medibles
    fun updateHabitProgress(habit: Habit, newValue: Float) {
        viewModelScope.launch {
            val isNowCompleted = if (habit.targetType.name == "AT_LEAST") {
                newValue >= habit.targetValue
            } else {
                newValue <= habit.targetValue
            }
            habitDao.updateHabit(habit.copy(currentCount = newValue, isCompleted = isNowCompleted))
        }
    }

    // Función para eliminar un hábito
    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.deleteHabit(habit)
        }
    }
}