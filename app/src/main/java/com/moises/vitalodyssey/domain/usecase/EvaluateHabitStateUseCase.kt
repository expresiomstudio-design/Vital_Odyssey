package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.domain.model.HabitState

class EvaluateHabitStateUseCase {
    operator fun invoke(
        targetValue: Float, 
        input: Float, 
        isCumulative: Boolean = false,
        currentPeriodTotal: Float = 0f
    ): HabitState {
        if (isCumulative) {
            val totalWithInput = currentPeriodTotal + input
            return when {
                totalWithInput >= targetValue -> HabitState.COMPLETED
                input > 0f -> HabitState.CONTRIBUTED
                else -> HabitState.SKIPPED
            }
        }

        return when {
            input >= targetValue -> HabitState.COMPLETED
            input > 0f -> HabitState.CONTRIBUTED
            else -> HabitState.MISSED
        }
    }
}