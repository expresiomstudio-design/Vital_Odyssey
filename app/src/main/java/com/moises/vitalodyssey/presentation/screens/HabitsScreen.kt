package com.moises.vitalodyssey.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.moises.vitalodyssey.domain.model.*
import com.moises.vitalodyssey.presentation.components.HabitLogDialog
import com.moises.vitalodyssey.presentation.components.HabitProgressRing
import com.moises.vitalodyssey.presentation.components.HabitStatusGridItem
import com.moises.vitalodyssey.presentation.components.VitalOdysseyBottomNavBar
import com.moises.vitalodyssey.presentation.viewmodels.HabitWithLog
import com.moises.vitalodyssey.presentation.viewmodels.HabitsUiState
import com.moises.vitalodyssey.presentation.viewmodels.HabitsViewModel
import com.moises.vitalodyssey.ui.theme.VitalOdysseyTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel = koinViewModel(),
    onNavigateToForm: (Int) -> Unit = {},
    onNavigateToTracking: (Int) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedHabitForLog by remember { mutableStateOf<Habit?>(null) }

    HabitsScreenContent(
        uiState = uiState,
        onHabitLogClick = { selectedHabitForLog = it },
        onHabitClick = { onNavigateToTracking(it.id) },
        onAddHabit = { onNavigateToForm(-1) },
        onEditHabit = { onNavigateToForm(it.id) }
    )

    if (selectedHabitForLog != null) {
        HabitLogDialog(
            habit = selectedHabitForLog!!,
            dateStr = LocalDate.now().toString(),
            onDismiss = { selectedHabitForLog = null },
            onConfirm = { state, value ->
                viewModel.saveLogForToday(selectedHabitForLog!!, state, value)
                selectedHabitForLog = null
            }
        )
    }
}

@Composable
fun HabitsScreenContent(
    uiState: HabitsUiState,
    onHabitLogClick: (Habit) -> Unit = {},
    onHabitClick: (Habit) -> Unit = {},
    onAddHabit: () -> Unit = {},
    onEditHabit: (Habit) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Mis Hábitos", onAddClick = onAddHabit)
                    Text(
                        text = "Ofensivos: ${uiState.offensiveCount} | Defensivos: ${uiState.defensiveCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            items(uiState.habitsWithLogs) { habitWithLog ->
                HabitCard(
                    habitWithLog = habitWithLog,
                    onLogClick = { onHabitLogClick(habitWithLog.habit) },
                    onClick = { onHabitClick(habitWithLog.habit) },
                    onLongClick = { onEditHabit(habitWithLog.habit) }
                )
            }

            if (uiState.habitsWithLogs.isEmpty()) {
                item { EmptyHabitsView() }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title, 
            style = MaterialTheme.typography.displayLarge, 
            color = MaterialTheme.colorScheme.primaryContainer
        )
        IconButton(
            onClick = onAddClick,
            colors = IconButtonDefaults.filledIconButtonColors(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Habit", tint = MaterialTheme.colorScheme.surface)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HabitCard(
    habitWithLog: HabitWithLog,
    onLogClick: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val habit = habitWithLog.habit
    val log = habitWithLog.todayLog
    val roleColor = if (habit.role == HabitRole.OFFENSIVE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, roleColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onLogClick)
        ) {
            HabitProgressRing(
                score = habit.score,
                state = log?.state ?: HabitState.UNRECORDED,
                measuredValue = log?.measuredValue,
                isBoolean = habit.type == HabitType.BOOLEAN,
                primaryColor = roleColor,
                isCumulative = habit.isCumulative
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                habit.name, 
                style = MaterialTheme.typography.headlineSmall, 
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                habit.role.name, 
                style = MaterialTheme.typography.labelSmall, 
                color = roleColor
            )
        }

        Icon(
            painter = rememberVectorPainter(Lucide.ChevronRight),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun EmptyHabitsView() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "No tienes hábitos registrados aún.", 
            style = MaterialTheme.typography.bodyMedium, 
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

