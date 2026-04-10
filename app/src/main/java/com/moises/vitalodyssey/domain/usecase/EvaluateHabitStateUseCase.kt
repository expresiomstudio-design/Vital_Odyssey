package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.domain.model.HabitState

class EvaluateHabitStateUseCase {
    operator fun invoke(targetValue: Float, input: Float): HabitState {
        return when {
            input >= targetValue -> HabitState.COMPLETED
            input > 0f -> HabitState.SKIPPED
            else -> HabitState.MISSED
        }
    }
}