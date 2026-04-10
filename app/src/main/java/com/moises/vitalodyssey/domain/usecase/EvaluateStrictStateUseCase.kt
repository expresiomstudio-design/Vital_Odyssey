package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.domain.model.HabitState
import java.time.LocalDate

class EvaluateStrictStateUseCase {
    operator fun invoke(state: HabitState, date: String): HabitState {
        val logDate = try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            return state
        }
        val today = LocalDate.now()
        
        return if (state == HabitState.UNRECORDED && logDate.isBefore(today)) {
            HabitState.MISSED
        } else {
            state
        }
    }
}