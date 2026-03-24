package com.moises.vitalodyssey.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moises.vitalodyssey.presentation.viewmodels.DashboardUiState
import com.moises.vitalodyssey.presentation.viewmodels.DashboardViewModel
import com.moises.vitalodyssey.ui.theme.OdysseyTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel(),
    onNavigateToHabits: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    DashboardScreenContent(
        uiState = uiState,
        onSimulateAttack = { viewModel.simulateAttack() },
        onNavigateToHabits = onNavigateToHabits,
        onNavigateToProfile = onNavigateToProfile
    )
}

@Composable
fun DashboardScreenContent(
    uiState: DashboardUiState,
    onSimulateAttack: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Scaffold(
        topBar = { TopProfileBar(uiState, onNavigateToProfile) },
        bottomBar = { BottomNavBar(onNavigateToHabits, onNavigateToProfile) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            StatusBarsSection(uiState)

            AttributesRow(uiState)

            BossEncounterCard(uiState.combatLog)

            ActionSection(uiState) { onSimulateAttack() }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TopProfileBar(
    state: DashboardUiState,
    onNavigateToProfile: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .clickable { onNavigateToProfile() }
        ) {
            AsyncImage(
                model = "https://picsum.photos/seed/warrior/200",
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Moisés Sánchez", 
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primaryContainer
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "NIVEL ${state.level}", 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
                LinearProgressIndicator(
                    progress = { state.visualXpPercent },
                    modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                )
                Text(
                    state.xpText, 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        IconButton(onClick = onNavigateToProfile) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primaryContainer)
        }
    }
}

@Composable
fun StatusBarsSection(state: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatusBar(
            label = "VIDA",
            valueText = state.hpText,
            progress = state.visualHpPercent,
            color = MaterialTheme.colorScheme.error,
            icon = Icons.Default.Favorite
        )
        StatusBar(
            label = "ESTAMINA",
            valueText = "${state.currentStamina}%",
            progress = state.currentStamina / 100f,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            icon = Icons.Default.Bolt
        )
    }
}

@Composable
fun StatusBar(label: String, valueText: String, progress: Float, color: androidx.compose.ui.graphics.Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(800), label = "StatusBarProgress")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Text(
                    label, 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Text(
                valueText, 
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun AttributesRow(state: DashboardUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AttributeCard(
            Modifier.weight(1f), 
            "ATAQUE", 
            state.attackStat.toString(), 
            MaterialTheme.colorScheme.primary, 
            Icons.Default.FlashOn
        )
        AttributeCard(
            Modifier.weight(1f), 
            "DEFENSA", 
            state.defenseStat.toString(), 
            MaterialTheme.colorScheme.secondary, 
            Icons.Default.Security
        )
    }
}

@Composable
fun AttributeCard(modifier: Modifier, label: String, value: String, color: androidx.compose.ui.graphics.Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)), 
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color)
        }
        Column {
            Text(
                label, 
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                value, 
                style = MaterialTheme.typography.headlineMedium,
                color = color
            )
        }
    }
}

@Composable
fun BossEncounterCard(logText: String) {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(32.dp))
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = "https://picsum.photos/seed/titan/800/1200",
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.4f),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = MaterialTheme.colorScheme.error, shape = CircleShape) {
                Text(
                    "JEFE ELITE", 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "TITAN PROCRASTINADOR", 
                style = MaterialTheme.typography.headlineMedium, 
                color = MaterialTheme.colorScheme.onSurface, 
                textAlign = TextAlign.Center
            )
            Text(
                logText, 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), 
                textAlign = TextAlign.Center, 
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun ActionSection(state: DashboardUiState, onAttack: () -> Unit) {
    val isEnabled = state.currentStamina >= 33
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = onAttack,
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isEnabled) {
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isEnabled) "ATACAR" else "SIN ESTAMINA",
                    style = MaterialTheme.typography.headlineMedium, 
                    color = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha=0.3f), 
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(onNavigateToHabits: () -> Unit, onNavigateToProfile: () -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)) {
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.List, null) }, 
            label = { Text("Hábitos", style = MaterialTheme.typography.labelSmall) }, 
            selected = false, 
            onClick = onNavigateToHabits
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.PlayArrow, null) }, 
            label = { Text("Combate", style = MaterialTheme.typography.labelSmall) }, 
            selected = true, 
            onClick = {}
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, null) }, 
            label = { Text("Perfil", style = MaterialTheme.typography.labelSmall) }, 
            selected = false, 
            onClick = onNavigateToProfile
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121416)
@Composable
fun DashboardScreenPreview() {
    OdysseyTheme(darkTheme = true) {
        DashboardScreenContent(
            uiState = DashboardUiState(
                level = 10,
                hpText = "1200 / 1200 HP",
                visualHpPercent = 1f,
                xpText = "450 / 1000 XP",
                visualXpPercent = 0.45f,
                currentStamina = 67,
                attackStat = 150,
                defenseStat = 80,
                combatLog = "El dragón ruge ferozmente."
            ),
            onSimulateAttack = {},
            onNavigateToHabits = {},
            onNavigateToProfile = {}
        )
    }
}
