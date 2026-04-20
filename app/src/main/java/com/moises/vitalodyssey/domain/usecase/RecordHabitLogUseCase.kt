package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.data.local.HabitDao
import com.moises.vitalodyssey.domain.model.*
import com.moises.vitalodyssey.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class RecordHabitLogUseCase(
    private val habitDao: HabitDao,
    private val userRepository: UserRepository,
    private val recalculateHabitScoresUseCase: RecalculateHabitScoresUseCase
) {
    suspend operator fun invoke(habitId: Int, dateStr: String, state: HabitState, value: Float?) {
        val habit = habitDao.getHabitById(habitId) ?: return

        // 1. Relleno (Dense Time Series)
        ensureDenseTimeSeries(habitId, habit.startDate, dateStr)

        // 2. Acción del Usuario (Update)
        val currentLog = habitDao.getLogForDate(habitId, dateStr)!!
        val input = value ?: 0f
        var finalState = state

        // Evaluar el estado inicial del día (independiente del periodo)
        if (habit.type == HabitType.MEASURABLE && value != null) {
            finalState = if (habit.isCumulative) {
                if (input > 0) HabitState.CONTRIBUTED else HabitState.UNRECORDED
            } else {
                if (input >= habit.targetValue) HabitState.COMPLETED
                else if (input > 0) HabitState.CONTRIBUTED
                else HabitState.UNRECORDED
            }
        } else if (finalState == HabitState.COMPLETED_BY_PERIOD) {
            // Protección: el usuario no puede marcar manualmente "COMPLETED_BY_PERIOD"
            finalState = HabitState.UNRECORDED
        }

        val startDay = DayOfWeek.valueOf(userRepository.getUserProfileOnce()?.startOfWeek ?: "MONDAY")

        // CORRECCIÓN 1: La variable isPeriodic permite a Booleanos, Medibles e Intervalos entrar.
        val isPeriodic = habit.frequencyType == "WEEKLY" || habit.frequencyType == "MONTHLY" || habit.frequencyType == "INTERVAL"

        // 3. Evaluación de Periodo y Actualización Segura
        if (isPeriodic) {
            val date = LocalDate.parse(dateStr)
            val periodRange = getPeriodRange(date, habit.frequencyType, habit.frequencyTarget, startDay, habit.startDate)
            val allLogs = habitDao.getLogsForHabit(habitId).first()

            // Filtrar logs de este periodo excluyendo el día actual que estamos modificando
            val logsInPeriod = allLogs.filter {
                val logDate = LocalDate.parse(it.date)
                it.date != dateStr && !logDate.isBefore(periodRange.first) && !logDate.isAfter(periodRange.second)
            }

            val isPeriodReached: Boolean

            if (habit.type == HabitType.MEASURABLE && habit.isCumulative) {
                // Suma para Acumulativos
                val currentTotal = logsInPeriod.sumOf { it.measuredValue?.toDouble() ?: 0.0 }.toFloat()
                val totalWithNewInput = currentTotal + input
                isPeriodReached = totalWithNewInput >= habit.targetValue
            } else {
                // Conteo para Booleanos y Medibles por Repetición (Ej: 3xS)
                val currentCompletedCount = logsInPeriod.count { it.state == HabitState.COMPLETED || it.state == HabitState.CONTRIBUTED }
                val addsToCount = if (finalState == HabitState.COMPLETED || finalState == HabitState.CONTRIBUTED) 1 else 0
                isPeriodReached = (currentCompletedCount + addsToCount) >= habit.frequencyTarget
            }

            if (isPeriodReached) {
                // Si alcanzó la meta, mejoramos el estado de hoy y autocompletamos el resto
                if (finalState == HabitState.CONTRIBUTED) {
                    finalState = HabitState.COMPLETED
                } else if (finalState == HabitState.UNRECORDED) {
                    finalState = HabitState.COMPLETED_BY_PERIOD
                }
                autoCompletePeriod(periodRange, allLogs, dateStr)
            } else {
                undoAutoCompletePeriod(periodRange, allLogs, dateStr)
            }
        }

        // CORRECCIÓN 2: Guardado Final blindado contra el 0f.
        val finalValue = if (finalState == HabitState.UNRECORDED || finalState == HabitState.COMPLETED_BY_PERIOD) null else input
        habitDao.updateLog(currentLog.copy(state = finalState, measuredValue = finalValue))

        recalculateHabitScoresUseCase(habitId)
    }

    private suspend fun autoCompletePeriod(
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
                if (log != null && log.state == HabitState.UNRECORDED) {
                    habitDao.updateLog(
                        log.copy(
                            state = HabitState.COMPLETED_BY_PERIOD,
                            measuredValue = null // CORRECCIÓN 2.1: NUNCA 0f
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
                    // CORRECCIÓN 2.2: NUNCA 0f
                    habitDao.updateLog(log.copy(state = HabitState.UNRECORDED, measuredValue = null))
                }
            }
            check = check.plusDays(1)
        }
    }

    private suspend fun ensureDenseTimeSeries(habitId: Int, habitStartDate: String, targetDateStr: String) {
        val oldestLog = habitDao.getOldestLog(habitId)
        val newestLog = habitDao.getNewestLog(habitId)

        val startH = if (habitStartDate.isNotEmpty()) LocalDate.parse(habitStartDate) else LocalDate.now()
        val startOldest = oldestLog?.let { LocalDate.parse(it.date) } ?: startH
        val startTarget = LocalDate.parse(targetDateStr)

        val rangoInicio = listOf(startH, startOldest, startTarget).minOrNull() ?: startH

        val endNow = LocalDate.now()
        val endNewest = newestLog?.let { LocalDate.parse(it.date) } ?: endNow
        val endTarget = startTarget

        val rangoFin = listOf(endNow, endNewest, endTarget).maxOrNull() ?: endNow

        var current = rangoInicio
        while (!current.isAfter(rangoFin)) {
            val currentStr = current.toString()
            if (habitDao.getLogForDate(habitId, currentStr) == null) {
                habitDao.insertLog(
                    HabitLog(
                        habitId = habitId,
                        date = currentStr,
                        state = HabitState.UNRECORDED,
                        measuredValue = null
                    )
                )
            }
            current = current.plusDays(1)
        }
    }

    // CORRECCIÓN 3: Cálculo real para intervalos basado en la fecha de inicio.
    private fun getPeriodRange(
        date: LocalDate,
        frequency: String,
        frequencyTarget: Int,
        startDay: DayOfWeek,
        habitStartDateStr: String
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
            "INTERVAL" -> {
                val startDate = if (habitStartDateStr.isNotEmpty()) LocalDate.parse(habitStartDateStr) else date
                if (date.isBefore(startDate)) return Pair(date, date)

                // Calcula el bloque de días actual desde que se creó el hábito
                val daysBetween = ChronoUnit.DAYS.between(startDate, date)
                val target = if (frequencyTarget > 0) frequencyTarget else 1
                val periodIndex = daysBetween / target

                val periodStart = startDate.plusDays(periodIndex * target)
                val periodEnd = periodStart.plusDays(target.toLong() - 1)

                Pair(periodStart, periodEnd)
            }
            else -> Pair(date, date)
        }
    }
}
