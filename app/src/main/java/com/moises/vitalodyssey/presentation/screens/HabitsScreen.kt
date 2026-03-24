package com.moises.vitalodyssey.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.moises.vitalodyssey.domain.model.Habit
import com.moises.vitalodyssey.domain.model.HabitRole
import com.moises.vitalodyssey.domain.model.HabitType
import com.moises.vitalodyssey.presentation.viewmodels.HabitsUiState
import com.moises.vitalodyssey.presentation.viewmodels.HabitsViewModel
import com.moises.vitalodyssey.ui.theme.VitalOdysseyTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel = koinViewModel(),
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    HabitsScreenContent(
        uiState = uiState,
        onNavigateToDashboard = onNavigateToDashboard,
        onNavigateToProfile = onNavigateToProfile,
        onToggleComplete = { habitId, isCompleted ->
            viewModel.toggleHabitStatus(habitId, isCompleted)
        }
    )
}

@Composable
fun HabitsScreenContent(
    uiState: HabitsUiState,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onToggleComplete: (Int, Boolean) -> Unit = { _, _ -> }
) {
    Scaffold(
        bottomBar = { HabitsBottomNavBar(onNavigateToDashboard, onNavigateToProfile) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            "Hábitos Activos", 
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Ofensivos: ${uiState.offensiveCount} | Defensivos: ${uiState.defensiveCount}", 
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Mis Hábitos", 
                        style = MaterialTheme.typography.displayLarge, 
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                    IconButton(
                        onClick = { /* TODO: Navigate to Add Habit */ },
                        colors = IconButtonDefaults.filledIconButtonColors(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Habit", tint = MaterialTheme.colorScheme.surface)
                    }
                }
            }
            
            items(uiState.habits) { habit ->
                HabitCard(
                    habit = habit,
                    onToggleComplete = { isCompleted ->
                        onToggleComplete(habit.id, isCompleted)
                    }
                )
            }

            if (uiState.habits.isEmpty()) {
                item {
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
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun HabitCard(
    habit: Habit,
    onToggleComplete: (Boolean) -> Unit
) {
    val roleColor = if (habit.role == HabitRole.OFFENSIVE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (habit.isCompleted) roleColor else Color.Transparent)
                .border(2.dp, roleColor, CircleShape)
                .clickable { onToggleComplete(!habit.isCompleted) },
            contentAlignment = Alignment.Center
        ) {
            if (habit.isCompleted) {
                Icon(
                    Icons.Default.CheckCircle, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.surfaceVariant, 
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                habit.name, 
                style = MaterialTheme.typography.headlineMedium, 
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                habit.role.name, 
                style = MaterialTheme.typography.bodySmall, 
                color = roleColor
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                "Bonus", 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                "+1", 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun HabitsBottomNavBar(onNavigateToDashboard: () -> Unit, onNavigateToProfile: () -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)) {
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.List, null) }, 
            label = { Text("Hábitos", style = MaterialTheme.typography.labelSmall) }, 
            selected = true, 
            onClick = {}
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.PlayArrow, null) }, 
            label = { Text("Combate", style = MaterialTheme.typography.labelSmall) }, 
            selected = false, 
            onClick = onNavigateToDashboard
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, null) }, 
            label = { Text("Perfil", style = MaterialTheme.typography.labelSmall) }, 
            selected = false, 
            onClick = onNavigateToProfile
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HabitsScreenPreview() {
    VitalOdysseyTheme {
        HabitsScreenContent(
            uiState = HabitsUiState(
                habits = listOf(
                    Habit(
                        id = 1,
                        name = "Hacer ejercicio",
                        role = HabitRole.OFFENSIVE,
                        type = HabitType.BOOLEAN,
                        isCompleted = true
                    ),
                    Habit(
                        id = 2,
                        name = "Beber agua",
                        role = HabitRole.DEFENSIVE,
                        type = HabitType.BOOLEAN,
                        isCompleted = false
                    )
                ),
                offensiveCount = 1,
                defensiveCount = 1
            )
        )
    }
}
