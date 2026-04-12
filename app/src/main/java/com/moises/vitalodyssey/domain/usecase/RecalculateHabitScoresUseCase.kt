package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.data.local.HabitDao
import com.moises.vitalodyssey.domain.model.Habit
import com.moises.vitalodyssey.domain.model.HabitLog
import com.moises.vitalodyssey.domain.model.HabitState
import com.moises.vitalodyssey.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class RecalculateHabitScoresUseCase(
    private val habitDao: HabitDao,
    private val userRepository: UserRepository,
    private val calculateScoreUseCase: CalculateHabitScoreUseCase,
    private val evaluateStrictStateUseCase: EvaluateStrictStateUseCase
) {
    suspend operator fun invoke(habitId: Int) {
        val habit = habitDao.getHabitById(habitId) ?: return
        val logs = habitDao.getLogsForHabit(habitId).first().sortedBy { it.date }
        
        val parsedStart = if (habit.startDate.isNotEmpty()) LocalDate.parse(habit.startDate) else LocalDate.now()
        val firstLogDate = logs.firstOrNull()?.let { LocalDate.parse(it.date) } ?: parsedStart
        
        val today = LocalDate.now()
        val startDay = DayOfWeek.valueOf(userRepository.getUserProfileOnce()?.startOfWeek ?: "MONDAY")

        val logsByDate = logs.associateBy { it.date }.toMutableMap()
        
        var currentScore = 0f
        var checkDate = if (parsedStart.isBefore(firstLogDate)) parsedStart else firstLogDate
        
        val updatedLogs = mutableListOf<HabitLog>()

        while (!checkDate.isAfter(today)) {
            val dateStr = checkDate.toString()
            val existingLog = logsByDate[dateStr]
            val originalState = existingLog?.state ?: HabitState.UNRECORDED
            
            val isPeriodOngoing = isDateInOngoingPeriod(checkDate, today, habit.frequencyType, startDay)
            val strictState = evaluateStrictStateUseCase(
                state = originalState,
                date = dateStr,
                isCumulative = habit.isCumulative,
                isPeriodOngoing = isPeriodOngoing
            )
            
            currentScore = calculateScoreUseCase(
                currentScore = currentScore,
                state = strictState,
                input = existingLog?.measuredValue ?: 0f,
                targetValue = habit.targetValue
            )

            val updatedLog = (existingLog ?: HabitLog(
                habitId = habitId,
                date = dateStr,
                state = HabitState.UNRECORDED
            )).copy(currentScore = currentScore)
            
            updatedLogs.add(updatedLog)
            checkDate = checkDate.plusDays(1)
        }

        // Batch update/insert logs
        updatedLogs.forEach { habitDao.insertLog(it) }

        // Update main habit score
        habitDao.updateHabit(habit.copy(score = currentScore))
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
}