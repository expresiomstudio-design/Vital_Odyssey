package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.domain.model.HabitState

class CalculateHabitScoreUseCase {
    private val SCORE_FACTOR = 5.2f

    operator fun invoke(
        currentScore: Float, 
        state: HabitState,
        input: Float = 0f,
        targetValue: Float = 1f
    ): Float {
        val result = when (state) {
            HabitState.COMPLETED, HabitState.COMPLETED_BY_PERIOD -> {
                currentScore + (SCORE_FACTOR * (1f - (currentScore / 100f)))
            }
            HabitState.CONTRIBUTED -> {
                if (targetValue > 0f) {
                    val gain = (SCORE_FACTOR * (1f - (currentScore / 100f))) * (input / targetValue)
                    currentScore + gain
                } else {
                    currentScore
                }
            }
            HabitState.MISSED -> {
                currentScore - (SCORE_FACTOR * (currentScore / 100f))
            }
            HabitState.SKIPPED, HabitState.UNRECORDED -> {
                currentScore
            }
        }
        return result.coerceIn(0f, 100f)
    }
}