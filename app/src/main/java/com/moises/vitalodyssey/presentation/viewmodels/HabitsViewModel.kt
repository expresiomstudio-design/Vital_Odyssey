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
import com.moises.vitalodyssey.domain.model.*
import com.moises.vitalodyssey.domain.usecase.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

// Estado de la UI para la lista de hábitos
data class HabitsUiState(
    val habitsWithLogs: List<HabitWithLog> = emptyList(),
    val offensiveCount: Int = 0,
    val defensiveCount: Int = 0
)

data class HabitWithLog(
    val habit: Habit,
    val todayLog: HabitLog?
)

class HabitsViewModel(
    private val habitDao: HabitDao,
    private val calculateScoreUseCase: CalculateHabitScoreUseCase,
    private val evaluateStateUseCase: EvaluateHabitStateUseCase
) : ViewModel() {

    private val today = LocalDate.now().toString()

    val uiState: StateFlow<HabitsUiState> = habitDao.getAllHabits().flatMapLatest { habits ->
        val flows = habits.map { habit ->
            flow {
                val log = habitDao.getLogForDate(habit.id, today)
                emit(HabitWithLog(habit, log))
            }
        }
        if (flows.isEmpty()) flowOf(HabitsUiState())
        else combine(flows) { habitsWithLogs ->
            val list = habitsWithLogs.toList()
            HabitsUiState(
                habitsWithLogs = list,
                offensiveCount = list.count { it.habit.role == HabitRole.OFFENSIVE },
                defensiveCount = list.count { it.habit.role == HabitRole.DEFENSIVE }
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsUiState()
    )

    fun recordHabit(habit: Habit, state: HabitState, measuredValue: Float? = null) {
        viewModelScope.launch {
            val newScore = calculateScoreUseCase(habit.score, state)
            habitDao.updateHabit(habit.copy(score = newScore, isCompleted = state == HabitState.COMPLETED))
            
            val log = HabitLog(
                habitId = habit.id,
                date = today,
                state = state,
                measuredValue = measuredValue
            )
            habitDao.insertLog(log)
        }
    }

    fun recordMeasurableHabit(habit: Habit, value: Float) {
        val state = evaluateStateUseCase(habit.targetValue, value)
        recordHabit(habit, state, value)
    }

    // Función para eliminar un hábito
    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.deleteHabit(habit)
        }
    }
}