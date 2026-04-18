package com.moises.vitalodyssey.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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
import com.composables.icons.lucide.*
import com.moises.vitalodyssey.domain.model.HabitState

@Composable
fun HabitProgressRing(
    score: Float,
    state: HabitState,
    measuredValue: Float?,
    isBoolean: Boolean,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    isCumulative: Boolean = false,
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

        when (state) {
            HabitState.UNRECORDED -> {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
            }
            HabitState.SKIPPED -> {
                Icon(
                    imageVector = Lucide.Minus,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            HabitState.MISSED -> {
                Icon(
                    imageVector = Lucide.X,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            HabitState.COMPLETED_BY_PERIOD -> {
                Icon(
                    imageVector = Lucide.CircleCheck, // Universal para autocompletado
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            HabitState.COMPLETED, HabitState.CONTRIBUTED -> {
                if (isBoolean) {
                    Icon(
                        imageVector = Lucide.CircleCheckBig,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    val value = measuredValue ?: 0f
                    val formatted = if (value % 1 == 0f) value.toInt().toString() else value.toString()
                    val labelText = if (isCumulative && value > 0) "+$formatted" else formatted
                    
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (labelText.length > 2) 12.sp else 16.sp,
                            color = primaryColor
                        )
                    )
                }
            }
        }
    }
}
