package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.domain.model.HabitState
import java.time.LocalDate

class EvaluateStrictStateUseCase {
    /**
     * Evalúa el estado estricto de un log.
     * En una Dense Time Series, transforma UNRECORDED en MISSED si la fecha ya pasó
     * y no hay tolerancia por periodo activo.
     */
    operator fun invoke(
        state: HabitState, 
        date: String,
        isPeriodOngoing: Boolean = false
    ): HabitState {
        if (state != HabitState.UNRECORDED) return state
        
        val logDate = try { LocalDate.parse(date) } catch (e: Exception) { return state }
        val today = LocalDate.now()
        
        // Si es hoy o futuro, mantenemos UNRECORDED
        if (!logDate.isBefore(today)) return HabitState.UNRECORDED
        
        // Si es anterior a hoy:
        // Si el periodo sigue abierto (tolerancia), mantenemos UNRECORDED (beneficio de la duda)
        // Si el periodo ya cerró, es un MISSED
        return if (isPeriodOngoing) HabitState.UNRECORDED else HabitState.MISSED
    }
}
