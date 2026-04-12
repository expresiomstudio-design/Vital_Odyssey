package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.data.local.HabitDao
import com.moises.vitalodyssey.domain.model.*
import com.moises.vitalodyssey.domain.repository.UserRepository
import com.moises.vitalodyssey.domain.usecase.EvaluateStrictStateUseCase
import com.moises.vitalodyssey.domain.usecase.RecalculateHabitScoresUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class HabitTrackingUiState(
    val habit: Habit? = null,
    val logs: List<HabitLog> = emptyList(),
    val scoreHistory: List<ScorePoint> = emptyList(),
    val currentWeekStart: LocalDate = LocalDate.now().minusDays(6), // Hoy a la derecha por defecto
    val currentStreak: Int = 0,
    val startDay: DayOfWeek = DayOfWeek.MONDAY,
    val isLoading: Boolean = false
)

data class ScorePoint(
    val date: LocalDate,
    val score: Float
)

class HabitTrackingViewModel(
    private val habitId: Int,
    private val habitDao: HabitDao,
    private val userRepository: UserRepository,
    private val evaluateStrictStateUseCase: EvaluateStrictStateUseCase,
    private val recalculateHabitScoresUseCase: RecalculateHabitScoresUseCase
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
            val profileFlow = userRepository.getUserProfile()

            combine(habitFlow, logsFlow, profileFlow) { habit, logs, profile ->
                val startDay = DayOfWeek.valueOf(profile?.startOfWeek ?: "MONDAY")
                
                // Ahora el historial se lee directamente de los logs guardados
                val history = logs.sortedBy { it.date }.map { 
                    ScorePoint(LocalDate.parse(it.date), it.currentScore) 
                }
                val streak = calculateCurrentStreak(habit, logs, startDay)
                
                _uiState.update { 
                    it.copy(
                        habit = habit,
                        logs = logs,
                        scoreHistory = history,
                        currentStreak = streak,
                        startDay = startDay,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    private fun calculateCurrentStreak(habit: Habit?, logs: List<HabitLog>, startDay: DayOfWeek): Int {
        val today = LocalDate.now()
        val logsByDate = logs.associateBy { it.date }
        
        var streak = 0
        var checkDate = today
        
        while (true) {
            val dateStr = checkDate.toString()
            val log = logsByDate[dateStr]
            val originalState = log?.state ?: HabitState.UNRECORDED
            
            val isPeriodOngoing = isDateInOngoingPeriod(checkDate, today, habit?.frequencyType ?: "DAILY", startDay)
            val strictState = evaluateStrictStateUseCase(originalState, dateStr, habit?.isCumulative ?: false, isPeriodOngoing)
            
            when (strictState) {
                HabitState.COMPLETED, HabitState.COMPLETED_BY_PERIOD, HabitState.CONTRIBUTED -> {
                    streak++
                }
                HabitState.MISSED -> {
                    return streak
                }
                HabitState.SKIPPED, HabitState.UNRECORDED -> {
                    // Racha congelada (no suma, no rompe)
                }
            }
            
            checkDate = checkDate.minusDays(1)
            
            // Seguridad para evitar bucles infinitos
            if (streak == 0 && checkDate.isBefore(today.minusDays(365))) break
            if (streak > 0 && !logsByDate.containsKey(checkDate.toString()) && checkDate.isBefore(today.minusYears(2))) break
        }

        return streak
    }

    private fun isDateInOngoingPeriod(
        date: LocalDate, 
        today: LocalDate, 
        frequencyType: String,
        startDay: DayOfWeek
    ): Boolean {
        return when (frequencyType) {
            "DAILY" -> date.isEqual(today)
            "WEEKLY" -> {
                val todayWeekStart = today.with(TemporalAdjusters.previousOrSame(startDay))
                val todayWeekEnd = todayWeekStart.plusDays(6)
                !date.isBefore(todayWeekStart) && !date.isAfter(todayWeekEnd)
            }
            "MONTHLY" -> {
                val monthStart = today.with(TemporalAdjusters.firstDayOfMonth())
                val monthEnd = today.with(TemporalAdjusters.lastDayOfMonth())
                !date.isBefore(monthStart) && !date.isAfter(monthEnd)
            }
            else -> date.isEqual(today)
        }
    }

    fun moveWeek(weeks: Long) {
        val newWeekStart = _uiState.value.currentWeekStart.plusWeeks(weeks)
        val today = LocalDate.now()
        
        // No permitimos que el final de la semana sea después de hoy
        if (newWeekStart.plusDays(6).isAfter(today)) {
            _uiState.update { it.copy(currentWeekStart = today.minusDays(6)) }
            return
        }
        
        _uiState.update { it.copy(currentWeekStart = newWeekStart) }
    }

    fun setWeekFromDate(date: LocalDate) {
        // La fecha seleccionada se coloca en el extremo derecho (presente de esa vista)
        _uiState.update { it.copy(currentWeekStart = date.minusDays(6)) }
    }

    fun deleteHabit() {
        viewModelScope.launch {
            habitDao.getHabitById(habitId)?.let {
                habitDao.deleteHabit(it)
            }
        }
    }

    fun updateLogForDate(dateStr: String, state: HabitState, value: Float?) {
        viewModelScope.launch {
            val habit = habitDao.getHabitById(habitId) ?: return@launch
            var finalState = state
            val input = value ?: 0f
            
            val startDay = DayOfWeek.valueOf(userRepository.getUserProfileOnce()?.startOfWeek ?: "MONDAY")

            // Lógica de Acumulación y Auto-completado
            if (habit.isCumulative && habit.type == HabitType.MEASURABLE) {
                val allLogs = habitDao.getLogsForHabit(habitId).first()
                val date = LocalDate.parse(dateStr)
                val periodRange = getPeriodRange(date, habit.frequencyType, startDay)
                
                val currentTotal = allLogs.filter {
                    val logDate = LocalDate.parse(it.date)
                    it.date != dateStr && !logDate.isBefore(periodRange.first) && !logDate.isAfter(periodRange.second)
                }.sumOf { it.measuredValue?.toDouble() ?: 0.0 }.toFloat()

                val totalWithNewInput = currentTotal + input
                
                if (totalWithNewInput >= habit.targetValue) {
                    finalState = HabitState.COMPLETED
                    // Auto-completar el resto del periodo
                    autoCompletePeriod(habit, periodRange, allLogs, dateStr)
                } else if (input > 0) {
                    finalState = HabitState.CONTRIBUTED
                }
            }

            val existingLog = habitDao.getLogForDate(habitId, dateStr)
            val newLog = existingLog?.copy(state = finalState, measuredValue = value) 
                ?: HabitLog(habitId = habitId, date = dateStr, state = finalState, measuredValue = value)
            
            habitDao.insertLog(newLog)
            
            // Recalcular todo el historial cronológicamente
            recalculateHabitScoresUseCase(habitId)
        }
    }

    private suspend fun autoCompletePeriod(habit: Habit, range: Pair<LocalDate, LocalDate>, logs: List<HabitLog>, currentUpdateDate: String) {
        val logsByDate = logs.associateBy { it.date }
        var check = range.first
        while (!check.isAfter(range.second)) {
            val dStr = check.toString()
            if (dStr != currentUpdateDate) {
                val log = logsByDate[dStr]
                if (log == null || log.state == HabitState.UNRECORDED) {
                    habitDao.insertLog(HabitLog(
                        habitId = habit.id,
                        date = dStr,
                        state = HabitState.COMPLETED_BY_PERIOD,
                        measuredValue = 0f
                    ))
                }
            }
            check = check.plusDays(1)
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

    // Funciones para la gráfica
    fun getDailyPoints(): List<ScorePoint> = _uiState.value.scoreHistory

    fun getWeeklyPoints(): List<ScorePoint> {
        val startDay = _uiState.value.startDay
        return _uiState.value.scoreHistory
            .groupBy { it.date.with(TemporalAdjusters.previousOrSame(startDay)) }
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