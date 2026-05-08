package com.moises.vitalodyssey.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

data class ChartPoint(
    val date: LocalDate,
    val value: Float
)

@Composable
fun TimeEvolutionChartSection(
    title: String = "Evolución",
    points: List<ChartPoint>,
    selectedPeriod: String,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onPeriodChange: (String) -> Unit,
    onMoveChart: (Int) -> Unit,
    primaryColor: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onMoveChart(1) },
                        enabled = canMoveLeft,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.ChevronLeft,
                            contentDescription = "Anterior",
                            modifier = Modifier.size(16.dp),
                            tint = if (canMoveLeft) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    
                    listOf("Día", "Semana", "Mes").forEach { period ->
                        val isSelected = selectedPeriod == period
                        Text(
                            text = period,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) primaryColor else Color.Transparent)
                                .clickable { onPeriodChange(period) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = { onMoveChart(-1) },
                        enabled = canMoveRight,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.ChevronRight,
                            contentDescription = "Siguiente",
                            modifier = Modifier.size(16.dp),
                            tint = if (canMoveRight) primaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            TimeEvolutionCanvas(
                points = points, 
                selectedPeriod = selectedPeriod,
                primaryColor = primaryColor,
                modifier = Modifier.height(180.dp).fillMaxWidth()
            )
        }
    }
}

@Composable
fun TimeEvolutionCanvas(
    points: List<ChartPoint>, 
    selectedPeriod: String,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = onSurfaceColor.copy(alpha = 0.6f),
        fontSize = 10.sp
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 40.dp.toPx()
        val paddingBottom = 30.dp.toPx()
        val chartWidth = width - paddingLeft
        val chartHeight = height - paddingBottom
        
        val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        
        // Buscar el valor máximo para escalar la gráfica (si todos son 0, usar 100 como base)
        val maxValue = (points.maxOfOrNull { it.value } ?: 100f).coerceAtLeast(100f)
        
        for (i in 0..5) {
            val yLevel = i * (maxValue / 5)
            val yPos = chartHeight - (yLevel / maxValue * chartHeight)
            
            val labelText = if (maxValue <= 100f) "${yLevel.toInt()}%" else yLevel.toInt().toString()
            
            drawText(
                textMeasurer = textMeasurer,
                text = labelText,
                style = labelStyle,
                topLeft = Offset(0f, yPos - 10.dp.toPx())
            )
            
            if (i > 0) {
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.1f),
                    start = Offset(paddingLeft, yPos),
                    end = Offset(width, yPos),
                    pathEffect = if (i < 5) dashPathEffect else null
                )
            }
        }

        drawLine(
            color = onSurfaceColor.copy(alpha = 0.3f),
            start = Offset(paddingLeft, chartHeight),
            end = Offset(width, chartHeight),
            strokeWidth = 2f
        )
        drawLine(
            color = onSurfaceColor.copy(alpha = 0.3f),
            start = Offset(paddingLeft, 0f),
            end = Offset(paddingLeft, chartHeight),
            strokeWidth = 2f
        )

        if (points.isEmpty()) return@Canvas

        val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth / 2
        val path = Path()
        val pointOffsets = points.mapIndexed { index, point ->
            val x = if (points.size > 1) paddingLeft + (index * stepX) else paddingLeft + chartWidth / 2
            val y = chartHeight - (point.value / maxValue * chartHeight)
            Offset(x, y)
        }

        pointOffsets.forEachIndexed { index, offset ->
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        pointOffsets.forEach { offset ->
            drawCircle(color = surfaceColor, radius = 4.dp.toPx(), center = offset)
            drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = offset, style = Stroke(width = 2.dp.toPx()))
        }

        points.forEachIndexed { index, point ->
            val label = when(selectedPeriod) {
                "Día" -> point.date.dayOfMonth.toString()
                "Semana" -> point.date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR).toString()
                "Mes" -> point.date.month.getDisplayName(TextStyle.SHORT, Locale("es")).replaceFirstChar { it.uppercase() }
                else -> point.date.dayOfMonth.toString()
            }
            
            val textLayoutResult = textMeasurer.measure(label, labelStyle)
            val xPosition = if (points.size > 1) paddingLeft + (index * stepX) else paddingLeft + chartWidth / 2
            
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    xPosition - (textLayoutResult.size.width / 2),
                    chartHeight + 8.dp.toPx()
                )
            )
        }
    }
}
