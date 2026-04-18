package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.data.local.HabitDao
import com.moises.vitalodyssey.domain.model.*
import com.moises.vitalodyssey.domain.usecase.EvaluateHabitStateUseCase
import com.moises.vitalodyssey.domain.usecase.RecordHabitLogUseCase
import kotlinx.coroutines.Dispatchers
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
    private val evaluateStateUseCase: EvaluateHabitStateUseCase,
    private val recordHabitLogUseCase: RecordHabitLogUseCase
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
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsUiState()
    )

    fun saveLogForToday(habit: Habit, state: HabitState, value: Float?) {
        viewModelScope.launch {
            recordHabitLogUseCase(habit.id, today, state, value)
        }
    }

    // Función para eliminar un hábito
    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.deleteHabit(habit)
        }
    }
}