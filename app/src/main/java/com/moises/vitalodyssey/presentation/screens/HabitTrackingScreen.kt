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
import com.moises.vitalodyssey.presentation.components.HabitLogDialog
import com.moises.vitalodyssey.presentation.components.HabitStatusGridItem
import com.moises.vitalodyssey.presentation.components.TimeEvolutionChartSection
import com.moises.vitalodyssey.presentation.components.ChartPoint
import com.moises.vitalodyssey.presentation.viewmodels.HabitTrackingViewModel
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
        containerColor = MaterialTheme.colorScheme.surface,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            TimeEvolutionChartSection(
                title = "Evolución",
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
        HabitLogDialog(
            habit = uiState.habit ?: return@let,
            dateStr = log.date,
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
                HabitState.COMPLETED_BY_PERIOD -> Lucide.CircleCheck // Universal para todos
                HabitState.COMPLETED, HabitState.CONTRIBUTED -> if (!isMeasurable) Lucide.CircleCheckBig else null
                else -> null
            }

            if (iconToDraw != null) {
                Icon(
                    imageVector = iconToDraw,
                    contentDescription = null,
                    tint = if (enabled) color else color.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp)
                )
            } else if (isMeasurable && (state == HabitState.COMPLETED || state == HabitState.CONTRIBUTED)) {
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
fun StatsSummary(habit: Habit?, currentStreak: Int) {
    if (habit == null) return
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(label = "Racha Actual", value = "$currentStreak d", icon = Lucide.Flame, modifier = Modifier.weight(1f))
        StatCard(label = "Score Total", value = "${habit.score.toInt()}%", icon = Lucide.Trophy, modifier = Modifier.weight(1f))
    }
}

// StatCard eliminado porque era pequeño y local, o movido si se desea. Mantener si no se pidió eliminar explícitamente.
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