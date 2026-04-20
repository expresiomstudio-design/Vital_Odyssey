package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.data.local.HabitDao
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
        
        // Gracias a la Dense Time Series, iteramos directamente sobre los logs existentes
        val allLogs = habitDao.getLogsForHabit(habitId).first().sortedBy { it.date }
        
        val today = LocalDate.now()
        val startDay = DayOfWeek.valueOf(userRepository.getUserProfileOnce()?.startOfWeek ?: "MONDAY")

        var currentScore = 0f

        allLogs.forEach { log ->
            val logDate = LocalDate.parse(log.date)
            
            // Solo calculamos hasta hoy. Logs futuros mantienen score 0 o el score actual.
            if (logDate.isAfter(today)) return@forEach

            val isPeriodOngoing = isDateInOngoingPeriod(logDate, today, habit.frequencyType, startDay)
            
            // Transformamos UNRECORDED en MISSED si el periodo ya cerró
            val strictState = evaluateStrictStateUseCase(
                state = log.state,
                date = log.date,
                isPeriodOngoing = isPeriodOngoing
            )
            
            currentScore = calculateScoreUseCase(
                currentScore = currentScore,
                state = strictState,
                input = log.measuredValue ?: 0f,
                targetValue = habit.targetValue
            )

            // Actualizamos el log con su score histórico
            habitDao.updateLog(log.copy(currentScore = currentScore))
        }

        // Actualizamos el score global del hábito con el resultado final
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
