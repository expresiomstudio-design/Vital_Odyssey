package com.moises.vitalodyssey.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moises.vitalodyssey.data.local.HabitDao
import com.moises.vitalodyssey.domain.model.*
import com.moises.vitalodyssey.domain.repository.UserRepository
import com.moises.vitalodyssey.presentation.components.ChartPoint
import com.moises.vitalodyssey.domain.usecase.EvaluateStrictStateUseCase
import com.moises.vitalodyssey.domain.usecase.RecordHabitLogUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class HabitTrackingUiState(
    val habit: Habit? = null,
    val logs: List<HabitLog> = emptyList(),
    val scoreHistory: List<ChartPoint> = emptyList(),
    val currentWeekStart: LocalDate = LocalDate.now().minusDays(6), // Hoy a la derecha por defecto
    val currentStreak: Int = 0,
    val startDay: DayOfWeek = DayOfWeek.MONDAY,
    val isLoading: Boolean = false,
    val canMoveChartLeft: Boolean = false,
    val canMoveChartRight: Boolean = false,
    val chartPoints: List<ChartPoint> = emptyList(),
    val selectedPeriod: String = "Día"
)

class HabitTrackingViewModel(
    private val habitId: Int,
    private val habitDao: HabitDao,
    private val userRepository: UserRepository,
    private val evaluateStrictStateUseCase: EvaluateStrictStateUseCase,
    private val recordHabitLogUseCase: RecordHabitLogUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitTrackingUiState())
    val uiState: StateFlow<HabitTrackingUiState> = _uiState.asStateFlow()

    private val _chartOffsetIndex = MutableStateFlow(0)
    private val _chartPeriod = MutableStateFlow("Día")

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val habitFlow = habitDao.getHabitByIdFlow(habitId).filterNotNull()
            val logsFlow = habitDao.getLogsForHabit(habitId)
            val profileFlow = userRepository.getUserProfile()

            combine(habitFlow, logsFlow, profileFlow, _chartOffsetIndex, _chartPeriod) { habit, logs, profile, offset, period ->
                val startDay = DayOfWeek.valueOf(profile?.startOfWeek ?: "MONDAY")
                
                val history = logs.sortedBy { it.date }.map { 
                    ChartPoint(LocalDate.parse(it.date), it.currentScore) 
                }
                val streak = calculateCurrentStreak(habit, logs, startDay)
                
                val groupedPoints = when(period) {
                    "Semana" -> history
                        .groupBy { it.date.with(TemporalAdjusters.previousOrSame(startDay)) }
                        .map { (weekStart, points) -> ChartPoint(weekStart, points.maxOf { it.value }) }
                        .sortedBy { it.date }
                    "Mes" -> history
                        .groupBy { it.date.withDayOfMonth(1) }
                        .map { (monthStart, points) -> ChartPoint(monthStart, points.maxOf { it.value }) }
                        .sortedBy { it.date }
                    else -> history
                }

                val end = (groupedPoints.size - offset).coerceIn(0, groupedPoints.size)
                val start = (end - 12).coerceIn(0, groupedPoints.size)
                val paginatedPoints = groupedPoints.subList(start, end)
                
                // Devolvemos el nuevo estado para ser colectado
                _uiState.value.copy(
                    habit = habit,
                    logs = logs,
                    scoreHistory = history,
                    currentStreak = streak,
                    startDay = startDay,
                    isLoading = false,
                    canMoveChartLeft = (groupedPoints.size - offset) > 12,
                    canMoveChartRight = offset > 0,
                    chartPoints = paginatedPoints,
                    selectedPeriod = period
                )
            }
            .flowOn(kotlinx.coroutines.Dispatchers.Default) // Ejecutar cálculos fuera del Main Thread
            .collect { newState ->
                _uiState.update { newState }
            }
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
            val strictState = evaluateStrictStateUseCase(
                state = originalState,
                date = dateStr,
                isPeriodOngoing = isPeriodOngoing
            )
            
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

    fun moveChart(direction: Int) {
        val newOffset = _chartOffsetIndex.value + direction
        if (newOffset >= 0) {
            _chartOffsetIndex.value = newOffset
        }
    }

    fun setChartPeriod(period: String) {
        _chartPeriod.value = period
        _chartOffsetIndex.value = 0
    }

    // Funciones obsoletas eliminadas/refactorizadas internamente en combine
    fun getDailyPoints(): List<ChartPoint> = _uiState.value.chartPoints
    fun getWeeklyPoints(): List<ChartPoint> = _uiState.value.chartPoints
    fun getMonthlyPoints(): List<ChartPoint> = _uiState.value.chartPoints

    fun deleteHabit() {
        viewModelScope.launch {
            habitDao.getHabitById(habitId)?.let {
                habitDao.deleteHabit(it)
            }
        }
    }

    fun updateLogForDate(dateStr: String, state: HabitState, value: Float?) {
        viewModelScope.launch {
            recordHabitLogUseCase(habitId, dateStr, state, value)
        }
    }

    // Funciones auxiliares eliminadas porque ahora están en RecordHabitLogUseCase
}
