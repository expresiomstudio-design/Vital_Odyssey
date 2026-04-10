package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.data.local.HabitDao
import com.moises.vitalodyssey.domain.model.Habit
import com.moises.vitalodyssey.domain.model.HabitLog
import com.moises.vitalodyssey.domain.model.HabitState
import com.moises.vitalodyssey.domain.usecase.CalculateHabitScoreUseCase
import com.moises.vitalodyssey.domain.usecase.EvaluateStrictStateUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class HabitTrackingUiState(
    val habit: Habit? = null,
    val logs: List<HabitLog> = emptyList(),
    val scoreHistory: List<ScorePoint> = emptyList(),
    val currentWeekStart: LocalDate = LocalDate.now(),
    val currentStreak: Int = 0,
    val isLoading: Boolean = false
)

data class ScorePoint(
    val date: LocalDate,
    val score: Float
)

class HabitTrackingViewModel(
    private val habitId: Int,
    private val habitDao: HabitDao,
    private val calculateScoreUseCase: CalculateHabitScoreUseCase,
    private val evaluateStrictStateUseCase: EvaluateStrictStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitTrackingUiState())
    val uiState: StateFlow<HabitTrackingUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val habitFlow = flow { emit(habitDao.getHabitById(habitId)) }
            val logsFlow = habitDao.getLogsForHabit(habitId)

            combine(habitFlow, logsFlow) { habit, logs ->
                val history = calculateScoreHistory(habit, logs)
                val streak = calculateCurrentStreak(logs)
                
                _uiState.update { 
                    it.copy(
                        habit = habit,
                        logs = logs,
                        scoreHistory = history,
                        currentStreak = streak,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    private fun calculateCurrentStreak(logs: List<HabitLog>): Int {
        val today = LocalDate.now()
        val logsByDate = logs.associateBy { it.date }
        
        var streak = 0
        var checkDate = today
        
        // Loop backwards to check for a continuous streak, treating gaps as UNRECORDED (which becomes MISSED if in the past)
        while (true) {
            val dateStr = checkDate.toString()
            val log = logsByDate[dateStr]
            val originalState = log?.state ?: HabitState.UNRECORDED
            val strictState = evaluateStrictStateUseCase(originalState, dateStr)
            
            when (strictState) {
                HabitState.COMPLETED, HabitState.COMPLETED_BY_PERIOD -> {
                    streak++
                }
                HabitState.MISSED -> {
                    return streak
                }
                HabitState.SKIPPED -> {
                    // SKIPPED doesn't break nor increase the streak
                }
                HabitState.UNRECORDED -> {
                    // This only happens for Today if not yet recorded
                }
            }
            
            checkDate = checkDate.minusDays(1)
            
            // Safety breaks to avoid infinite loops
            if (streak == 0 && checkDate.isBefore(today.minusDays(365))) break
            if (streak > 0 && !logsByDate.containsKey(checkDate.toString()) && checkDate.isBefore(today.minusYears(2))) break
        }

        return streak
    }

    private fun calculateScoreHistory(habit: Habit?, logs: List<HabitLog>): List<ScorePoint> {
        if (habit == null || habit.startDate.isEmpty()) return emptyList()
        
        val logsByDate = logs.associateBy { it.date }
        val startDate = LocalDate.parse(habit.startDate)
        val today = LocalDate.now()
        
        val history = mutableListOf<ScorePoint>()
        var currentScore = 0f
        var checkDate = startDate
        
        while (!checkDate.isAfter(today)) {
            val dateStr = checkDate.toString()
            val log = logsByDate[dateStr]
            val originalState = log?.state ?: HabitState.UNRECORDED
            val strictState = evaluateStrictStateUseCase(originalState, dateStr)
            
            currentScore = calculateScoreUseCase(currentScore, strictState)
            history.add(ScorePoint(checkDate, currentScore))
            
            checkDate = checkDate.plusDays(1)
        }
        
        return history
    }

    fun moveWeek(weeks: Long) {
        val newWeekStart = _uiState.value.currentWeekStart.plusWeeks(weeks)
        val today = LocalDate.now()
        
        // No permitimos ir al futuro
        if (newWeekStart.isAfter(today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)))) {
            return
        }
        
        _uiState.update { it.copy(currentWeekStart = newWeekStart) }
    }

    fun updateLogForDate(date: String, state: HabitState, value: Float?) {
        viewModelScope.launch {
            val existingLog = habitDao.getLogForDate(habitId, date)
            val newLog = existingLog?.copy(state = state, measuredValue = value) 
                ?: HabitLog(habitId = habitId, date = date, state = state, measuredValue = value)
            
            habitDao.insertLog(newLog)
            
            // Recalcular score y racha exhaustivamente
            val habit = habitDao.getHabitById(habitId)
            val allLogs = habitDao.getLogsForHabit(habitId).first()
            
            val history = calculateScoreHistory(habit, allLogs)
            val finalScore = history.lastOrNull()?.score ?: 0f
            val streak = calculateCurrentStreak(allLogs)
            
            habit?.let {
                habitDao.updateHabit(it.copy(score = finalScore, currentStreak = streak))
            }
        }
    }

    // Funciones para la gráfica por periodos
    fun getDailyPoints(): List<ScorePoint> = _uiState.value.scoreHistory

    fun getWeeklyPoints(): List<ScorePoint> {
        return _uiState.value.scoreHistory
            .groupBy { it.date.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)) }
            .map { (weekStart, points) ->
                ScorePoint(weekStart, points.maxOf { it.score })
            }
    }

    fun getMonthlyPoints(): List<ScorePoint> {
        return _uiState.value.scoreHistory
            .groupBy { it.date.withDayOfMonth(1) }
            .map { (monthStart, points) ->
                ScorePoint(monthStart, points.maxOf { it.score })
            }
    }
}