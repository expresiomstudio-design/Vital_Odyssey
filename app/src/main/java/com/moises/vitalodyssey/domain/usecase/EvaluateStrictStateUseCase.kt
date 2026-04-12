package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.domain.model.HabitState
import java.time.LocalDate

class EvaluateStrictStateUseCase {
    operator fun invoke(
        state: HabitState, 
        date: String,
        isCumulative: Boolean = false,
        isPeriodOngoing: Boolean = false
    ): HabitState {
        val logDate = try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            return state
        }
        val today = LocalDate.now()
        
        if (state == HabitState.UNRECORDED && logDate.isBefore(today)) {
            return if (isCumulative) {
                if (isPeriodOngoing) {
                    HabitState.UNRECORDED // Beneficio de la duda
                } else {
                    HabitState.MISSED // El periodo cerró sin cumplir la meta
                }
            } else {
                HabitState.MISSED // Castigo inmediato para no acumulativos
            }
        }
        
        return state
    }
}