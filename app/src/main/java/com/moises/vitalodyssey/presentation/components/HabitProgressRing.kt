package com.moises.vitalodyssey.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moises.vitalodyssey.domain.model.HabitState
import kotlin.math.roundToInt

@Composable
fun HabitProgressRing(
    score: Float,
    state: HabitState,
    measuredValue: Float?,
    isBoolean: Boolean,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val sweepAngle = (score / 100f) * 360f
    val backgroundColor = primaryColor.copy(alpha = 0.2f)

    Box(modifier = modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(56.dp)) {
            drawCircle(
                color = backgroundColor,
                style = Stroke(width = 6.dp.toPx())
            )
            drawArc(
                color = if (score > 0) primaryColor else Color.Transparent,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        val contentText = when (state) {
            HabitState.UNRECORDED -> "?"
            HabitState.SKIPPED -> "-"
            HabitState.MISSED -> "X"
            HabitState.COMPLETED, HabitState.COMPLETED_BY_PERIOD, HabitState.CONTRIBUTED -> {
                if (isBoolean) "✓"
                else {
                    val value = measuredValue ?: 0f
                    val formatted = if (value % 1 == 0f) value.toInt().toString() else value.toString()
                    if (state == HabitState.CONTRIBUTED && value > 0) "+$formatted" else formatted
                }
            }
        }

        Text(
            text = contentText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (contentText.length > 2) 10.sp else if (contentText.length > 1) 14.sp else 18.sp,
                color = when (state) {
                    HabitState.UNRECORDED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    HabitState.CONTRIBUTED -> primaryColor // Color positivo
                    else -> primaryColor
                }
            )
        )
    }
}