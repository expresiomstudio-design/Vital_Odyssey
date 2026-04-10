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

        val contentText = when {
            state == HabitState.UNRECORDED -> "?"
            state == HabitState.SKIPPED -> "-"
            isBoolean && state == HabitState.COMPLETED -> "✓"
            !isBoolean && measuredValue != null -> {
                val value = if (measuredValue % 1 == 0f) measuredValue.toInt().toString() else measuredValue.toString()
                value
            }
            else -> "?"
        }

        Text(
            text = contentText,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (contentText.length > 2) 12.sp else 18.sp,
                color = if (state != HabitState.UNRECORDED) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        )
    }
}