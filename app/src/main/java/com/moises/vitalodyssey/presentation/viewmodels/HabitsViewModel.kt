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
import com.moises.vitalodyssey.domain.usecase.EvaluateHabitStateUseCase
import com.moises.vitalodyssey.domain.usecase.RecalculateHabitScoresUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

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
    private val userRepository: com.moises.vitalodyssey.domain.repository.UserRepository,
    private val evaluateStateUseCase: EvaluateHabitStateUseCase,
    private val recalculateHabitScoresUseCase: RecalculateHabitScoresUseCase
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
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HabitsUiState()
    )

    fun recordHabit(habit: Habit, state: HabitState, measuredValue: Float? = null) {
        viewModelScope.launch {
            if (state == HabitState.UNRECORDED) {
                val existingLog = habitDao.getLogForDate(habit.id, today)
                existingLog?.let { habitDao.deleteLog(it) }
            } else {
                val log = HabitLog(
                    habitId = habit.id,
                    date = today,
                    state = state,
                    measuredValue = measuredValue
                )
                habitDao.insertLog(log)
            }
            
            // Recalcular todo el historial para asegurar consistencia
            recalculateHabitScoresUseCase(habit.id)
        }
    }

    fun recordMeasurableHabit(habit: Habit, value: Float) {
        viewModelScope.launch {
            var finalState = HabitState.CONTRIBUTED
            if (habit.isCumulative) {
                val startDayStr = userRepository.getUserProfileOnce()?.startOfWeek ?: "MONDAY"
                val startDay = DayOfWeek.valueOf(startDayStr)
                val date = LocalDate.now()
                val range = getPeriodRange(date, habit.frequencyType, startDay)

                val allLogs = habitDao.getLogsForHabit(habit.id).first()
                val currentTotal = allLogs.filter {
                    val logDate = LocalDate.parse(it.date)
                    it.date != today && !logDate.isBefore(range.first) && !logDate.isAfter(range.second)
                }.sumOf { it.measuredValue?.toDouble() ?: 0.0 }.toFloat()

                val total = currentTotal + value
                if (total >= habit.targetValue) {
                    finalState = HabitState.COMPLETED
                } else if (value > 0) {
                    finalState = HabitState.CONTRIBUTED
                } else {
                    finalState = HabitState.UNRECORDED
                }
            } else {
                finalState = if (value >= habit.targetValue) HabitState.COMPLETED else if (value > 0) HabitState.CONTRIBUTED else HabitState.UNRECORDED
            }
            recordHabit(habit, finalState, value)
        }
    }

    private fun getPeriodRange(
        date: LocalDate, 
        frequency: String,
        startDay: DayOfWeek
    ): Pair<LocalDate, LocalDate> {
        return when (frequency) {
            "WEEKLY" -> {
                val weekStart = date.with(TemporalAdjusters.previousOrSame(startDay))
                val weekEnd = weekStart.plusDays(6)
                Pair(weekStart, weekEnd)
            }
            "MONTHLY" -> Pair(
                date.with(TemporalAdjusters.firstDayOfMonth()),
                date.with(TemporalAdjusters.lastDayOfMonth())
            )
            else -> Pair(date, date)
        }
    }

    // Función para eliminar un hábito
    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitDao.deleteHabit(habit)
        }
    }
}