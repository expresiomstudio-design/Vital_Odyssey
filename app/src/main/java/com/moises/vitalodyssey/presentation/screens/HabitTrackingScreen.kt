package com.moises.vitalodyssey.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import com.moises.vitalodyssey.domain.model.Habit
import com.moises.vitalodyssey.domain.model.HabitLog
import com.moises.vitalodyssey.domain.model.HabitState
import com.moises.vitalodyssey.domain.model.HabitType
import com.moises.vitalodyssey.presentation.components.HabitStatusGridItem
import com.moises.vitalodyssey.presentation.viewmodels.HabitTrackingViewModel
import com.moises.vitalodyssey.presentation.viewmodels.ScorePoint
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.IsoFields
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitTrackingScreen(
    habitId: Int,
    viewModel: HabitTrackingViewModel = koinViewModel { parametersOf(habitId) },
    onNavigateToEdit: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedLogForEdit by remember { mutableStateOf<HabitLog?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.habit?.name ?: "Hábito", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToEdit(habitId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            WeekBar(
                habit = uiState.habit,
                currentWeekStart = uiState.currentWeekStart,
                logs = uiState.logs,
                onDayClick = { date ->
                    val log = uiState.logs.find { it.date == date.toString() } 
                        ?: HabitLog(habitId = habitId, date = date.toString(), state = HabitState.UNRECORDED)
                    selectedLogForEdit = log
                },
                onPreviousWeek = { viewModel.moveWeek(-1) },
                onNextWeek = { viewModel.moveWeek(1) },
                onCalendarClick = { showDatePicker = true }
            )

            ScoreChartSection(
                points = uiState.chartPoints,
                selectedPeriod = uiState.selectedPeriod,
                canMoveLeft = uiState.canMoveChartLeft,
                canMoveRight = uiState.canMoveChartRight,
                onPeriodChange = { viewModel.setChartPeriod(it) },
                onMoveChart = { viewModel.moveChart(it) }
            )

            StatsSummary(uiState.habit, uiState.currentStreak)
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar Hábito?") },
            text = { Text("Esta acción no se puede deshacer y perderás todo el historial de este hábito.") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.deleteHabit()
                    onNavigateBack()
                }) {
                    Text("ELIMINAR", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCELAR")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.currentWeekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val selectedDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                        viewModel.setWeekFromDate(selectedDate)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("CANCELAR") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    selectedLogForEdit?.let { log ->
        HabitEditLogDialog(
            habitName = uiState.habit?.name ?: "Hábito",
            log = log,
            habitType = uiState.habit?.type ?: HabitType.BOOLEAN,
            targetValue = uiState.habit?.targetValue ?: 0f,
            isCumulative = uiState.habit?.isCumulative ?: false,
            unit = uiState.habit?.unit ?: "",
            onDismiss = { selectedLogForEdit = null },
            onConfirm = { state, value ->
                viewModel.updateLogForDate(log.date, state, value)
                selectedLogForEdit = null
            }
        )
    }
}

@Composable
fun WeekBar(
    habit: Habit?,
    currentWeekStart: LocalDate,
    logs: List<HabitLog>,
    onDayClick: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onCalendarClick: () -> Unit
) {
    val today = LocalDate.now()
    val weekEnd = currentWeekStart.plusDays(6)
    val canMoveForward = weekEnd.isBefore(today)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentWeekStart.month.getDisplayName(TextStyle.FULL, Locale("es"))} ${currentWeekStart.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = onPreviousWeek) { Icon(Icons.Default.ChevronLeft, null) }
                IconButton(onClick = onCalendarClick) { Icon(Icons.Default.CalendarMonth, null) }
                IconButton(
                    onClick = onNextWeek,
                    enabled = canMoveForward
                ) { 
                    Icon(
                        imageVector = Icons.Default.ChevronRight, 
                        contentDescription = null,
                        tint = if (canMoveForward) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    ) 
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            (0..6).forEach { dayOffset ->
                val date = currentWeekStart.plusDays(dayOffset.toLong())
                val log = logs.find { it.date == date.toString() }
                val isFuture = date > today
                
                DayItem(
                    date = date,
                    log = log,
                    habit = habit,
                    isToday = date == today,
                    enabled = !isFuture,
                    onClick = { if (!isFuture) onDayClick(date) }
                )
            }
        }
    }
}

@Composable
fun DayItem(
    date: LocalDate,
    log: HabitLog?,
    habit: Habit?,
    isToday: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val state = log?.state ?: HabitState.UNRECORDED
    val color = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        state == HabitState.COMPLETED || state == HabitState.COMPLETED_BY_PERIOD -> MaterialTheme.colorScheme.primary
        state == HabitState.MISSED -> MaterialTheme.colorScheme.error
        state == HabitState.SKIPPED -> MaterialTheme.colorScheme.secondary
        state == HabitState.CONTRIBUTED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    }

    val isMeasurable = habit?.type == HabitType.MEASURABLE

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(enabled = enabled) { onClick() }
    ) {
        // Indicador de "Hoy" (Flecha superior)
        Box(
            modifier = Modifier.height(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isToday) {
                Icon(
                    imageVector = Lucide.ChevronDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Text(
            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es")).first().toString().uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val iconToDraw = when (state) {
                HabitState.UNRECORDED -> Lucide.Circle
                HabitState.SKIPPED -> Lucide.Minus
                HabitState.MISSED -> Lucide.X
                HabitState.COMPLETED, HabitState.COMPLETED_BY_PERIOD -> if (!isMeasurable) Lucide.Check else null
                HabitState.CONTRIBUTED -> if (!isMeasurable) Lucide.Check else null
                else -> null
            }

            if (iconToDraw != null) {
                Icon(
                    imageVector = iconToDraw,
                    contentDescription = null,
                    tint = if (enabled) color else color.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp)
                )
            } else if (isMeasurable && (state == HabitState.COMPLETED || state == HabitState.COMPLETED_BY_PERIOD || state == HabitState.CONTRIBUTED)) {
                val value = log?.measuredValue ?: 0f
                val formatted = if (value % 1 == 0f) value.toInt().toString() else value.toString()
                val labelText = if (habit?.isCumulative == true && value > 0) "+$formatted" else formatted
                
                Text(
                    text = labelText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) color else color.copy(alpha = 0.3f)
                )
            }
        }
        Text(
            text = date.dayOfMonth.toString(), 
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@Composable
fun ScoreChartSection(
    points: List<ScorePoint>,
    selectedPeriod: String,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onPeriodChange: (String) -> Unit,
    onMoveChart: (Int) -> Unit
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
                Text("Evolución", fontWeight = FontWeight.Bold)
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
                            tint = if (canMoveLeft) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                    
                    listOf("Día", "Semana", "Mes").forEach { period ->
                        val isSelected = selectedPeriod == period
                        Text(
                            text = period,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
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
                            tint = if (canMoveRight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            ScoreChart(
                points = points, 
                selectedPeriod = selectedPeriod,
                modifier = Modifier.height(180.dp).fillMaxWidth()
            )
        }
    }
}

@Composable
fun ScoreChart(
    points: List<ScorePoint>, 
    selectedPeriod: String,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
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
        
        for (i in 0..5) {
            val yLevel = i * 20f
            val yPos = chartHeight - (yLevel / 100f * chartHeight)
            
            drawText(
                textMeasurer = textMeasurer,
                text = "${yLevel.toInt()}%",
                style = labelStyle,
                topLeft = androidx.compose.ui.geometry.Offset(0f, yPos - 10.dp.toPx())
            )
            
            if (i > 0) {
                drawLine(
                    color = onSurfaceColor.copy(alpha = 0.1f),
                    start = androidx.compose.ui.geometry.Offset(paddingLeft, yPos),
                    end = androidx.compose.ui.geometry.Offset(width, yPos),
                    pathEffect = if (i < 5) dashPathEffect else null
                )
            }
        }

        drawLine(
            color = onSurfaceColor.copy(alpha = 0.3f),
            start = androidx.compose.ui.geometry.Offset(paddingLeft, chartHeight),
            end = androidx.compose.ui.geometry.Offset(width, chartHeight),
            strokeWidth = 2f
        )
        drawLine(
            color = onSurfaceColor.copy(alpha = 0.3f),
            start = androidx.compose.ui.geometry.Offset(paddingLeft, 0f),
            end = androidx.compose.ui.geometry.Offset(paddingLeft, chartHeight),
            strokeWidth = 2f
        )

        if (points.isEmpty()) return@Canvas

        val stepX = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth / 2
        val path = Path()
        val pointOffsets = points.mapIndexed { index, point ->
            val x = if (points.size > 1) paddingLeft + (index * stepX) else paddingLeft + chartWidth / 2
            val y = chartHeight - (point.score / 100f * chartHeight)
            androidx.compose.ui.geometry.Offset(x, y)
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
                topLeft = androidx.compose.ui.geometry.Offset(
                    xPosition - (textLayoutResult.size.width / 2),
                    chartHeight + 8.dp.toPx()
                )
            )
        }
    }
}

@Composable
fun StatsSummary(habit: Habit?, currentStreak: Int) {
    if (habit == null) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(label = "Racha Actual", value = "$currentStreak d", icon = Lucide.Flame, modifier = Modifier.weight(1f))
        StatCard(label = "Score Total", value = "${habit.score.toInt()}%", icon = Lucide.Trophy, modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun HabitEditLogDialog(
    habitName: String,
    log: HabitLog,
    habitType: HabitType,
    targetValue: Float,
    isCumulative: Boolean,
    unit: String,
    onDismiss: () -> Unit,
    onConfirm: (HabitState, Float?) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Registro: ${log.date}") },
        text = {
            HabitLoggingContent(
                habitName = habitName,
                habitType = habitType,
                targetValue = targetValue,
                isCumulative = isCumulative,
                unit = unit,
                onRecord = { state, value ->
                    onConfirm(state, value)
                }
            )
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun HabitLoggingContent(
    habitName: String,
    habitType: HabitType,
    targetValue: Float,
    isCumulative: Boolean,
    unit: String,
    onRecord: (HabitState, Float?) -> Unit
) {
    var textValue by remember { mutableStateOf("") }
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(habitName, style = MaterialTheme.typography.headlineSmall)
        
        if (habitType == HabitType.MEASURABLE) {
            val label = if (isCumulative) "Suma a la meta: ${targetValue.toInt()} $unit" else "Meta: ${targetValue.toInt()} $unit"
            Text(label, color = primaryColor)
            OutlinedTextField(
                value = textValue,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) textValue = it },
                label = { Text(if (isCumulative) "Cantidad a sumar" else "Valor alcanzado") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = { 
                    val value = textValue.toFloatOrNull() ?: 0f
                    val state = if (isCumulative && value > 0) HabitState.CONTRIBUTED else HabitState.COMPLETED
                    onRecord(state, value) 
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isCumulative) "Sumar Aportación" else "Guardar Registro")
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Cuadrícula de acciones (2x2)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (habitType == HabitType.BOOLEAN) {
                    HabitStatusGridItem(
                        label = "Completado",
                        icon = Lucide.CircleCheck,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onRecord(HabitState.COMPLETED, null) },
                        modifier = Modifier.weight(1f)
                    )
                }
                HabitStatusGridItem(
                    label = "Saltado",
                    icon = Lucide.CircleMinus,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { onRecord(HabitState.SKIPPED, null) },
                    modifier = if (habitType == HabitType.BOOLEAN) Modifier.weight(1f) else Modifier.fillMaxWidth()
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitStatusGridItem(
                    label = "No Realizado",
                    icon = Lucide.CircleX,
                    color = MaterialTheme.colorScheme.error,
                    onClick = { onRecord(HabitState.MISSED, null) },
                    modifier = Modifier.weight(1f)
                )
                HabitStatusGridItem(
                    label = "Borrar",
                    icon = Lucide.Eraser,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onRecord(HabitState.UNRECORDED, null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}