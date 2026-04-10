package com.moises.vitalodyssey.domain.usecase

import com.moises.vitalodyssey.domain.model.HabitState

class CalculateHabitScoreUseCase {
    private val SCORE_FACTOR = 5.2f

    operator fun invoke(currentScore: Float, state: HabitState): Float {
        val result = when (state) {
            HabitState.COMPLETED, HabitState.COMPLETED_BY_PERIOD -> {
                currentScore + (SCORE_FACTOR * (1f - (currentScore / 100f)))
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