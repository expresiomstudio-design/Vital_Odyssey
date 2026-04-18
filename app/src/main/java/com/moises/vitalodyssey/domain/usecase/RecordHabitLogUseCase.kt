package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.data.local.HabitDao
import com.moises.vitalodyssey.domain.model.*
import com.moises.vitalodyssey.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class RecordHabitLogUseCase(
    private val habitDao: HabitDao,
    private val userRepository: UserRepository,
    private val recalculateHabitScoresUseCase: RecalculateHabitScoresUseCase
) {
    suspend operator fun invoke(habitId: Int, dateStr: String, state: HabitState, value: Float?) {
        val habit = habitDao.getHabitById(habitId) ?: return
        var finalState = state
        val input = value ?: 0f

        val startDay = DayOfWeek.valueOf(userRepository.getUserProfileOnce()?.startOfWeek ?: "MONDAY")

        // Lógica de Estado para Hábitos Medibles
        if (habit.type == HabitType.MEASURABLE && value != null) {
            if (habit.isCumulative) {
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
                    autoCompletePeriod(habit, periodRange, allLogs, dateStr)
                } else {
                    finalState = if (input > 0) HabitState.CONTRIBUTED else HabitState.UNRECORDED
                    undoAutoCompletePeriod(periodRange, allLogs, dateStr)
                }
            } else {
                finalState = when {
                    input >= habit.targetValue -> HabitState.COMPLETED
                    input > 0 -> HabitState.CONTRIBUTED
                    else -> HabitState.UNRECORDED
                }
            }
        } else if (habit.isCumulative) {
            // Si es acumulativo pero no se pasó valor (cambio manual de estado), revertimos autocompletado
            val allLogs = habitDao.getLogsForHabit(habitId).first()
            val date = LocalDate.parse(dateStr)
            val periodRange = getPeriodRange(date, habit.frequencyType, startDay)
            undoAutoCompletePeriod(periodRange, allLogs, dateStr)
        }

        val existingLog = habitDao.getLogForDate(habitId, dateStr)

        if (finalState == HabitState.UNRECORDED) {
            existingLog?.let { habitDao.deleteLog(it) }
        } else {
            val log = existingLog?.copy(
                state = finalState,
                measuredValue = value
            ) ?: HabitLog(
                habitId = habitId,
                date = dateStr,
                state = finalState,
                measuredValue = value
            )
            habitDao.insertLog(log)
        }

        // Recalcular todo el historial para asegurar consistencia
        recalculateHabitScoresUseCase(habitId)
    }

    private suspend fun autoCompletePeriod(
        habit: Habit,
        range: Pair<LocalDate, LocalDate>,
        logs: List<HabitLog>,
        currentUpdateDate: String
    ) {
        val logsByDate = logs.associateBy { it.date }
        var check = range.first
        while (!check.isAfter(range.second)) {
            val dStr = check.toString()
            if (dStr != currentUpdateDate) {
                val log = logsByDate[dStr]
                if (log == null || log.state == HabitState.UNRECORDED) {
                    habitDao.insertLog(
                        HabitLog(
                            habitId = habit.id,
                            date = dStr,
                            state = HabitState.COMPLETED_BY_PERIOD,
                            measuredValue = 0f
                        )
                    )
                }
            }
            check = check.plusDays(1)
        }
    }

    private suspend fun undoAutoCompletePeriod(
        range: Pair<LocalDate, LocalDate>,
        logs: List<HabitLog>,
        currentUpdateDate: String
    ) {
        val logsByDate = logs.associateBy { it.date }
        var check = range.first
        while (!check.isAfter(range.second)) {
            val dStr = check.toString()
            if (dStr != currentUpdateDate) {
                val log = logsByDate[dStr]
                if (log?.state == HabitState.COMPLETED_BY_PERIOD) {
                    habitDao.deleteLog(log)
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
}
