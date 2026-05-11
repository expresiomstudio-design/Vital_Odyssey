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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.composables.icons.lucide.*
import com.moises.vitalodyssey.presentation.components.VitalOdysseyBottomNavBar
import com.moises.vitalodyssey.presentation.viewmodels.DashboardUiState
import com.moises.vitalodyssey.presentation.viewmodels.DashboardViewModel
import com.moises.vitalodyssey.ui.theme.OdysseyTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel(),
    onNavigateToProfile: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    DashboardScreenContent(
        uiState = uiState,
        onSimulateAttack = { viewModel.simulateAttack() },
        onNavigateToProfile = onNavigateToProfile
    )
}

@Composable
fun DashboardScreenContent(
    uiState: DashboardUiState,
    onSimulateAttack: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { TopProfileBar(uiState, onNavigateToProfile) },
            containerColor = MaterialTheme.colorScheme.surface
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusBarsSection(uiState)

                AttributesRow(uiState)

                BossEncounterCard(uiState.combatLog)

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // ── Botón de ataque flotante ─────────────────────────────────────────
        AttackFab(
            isEnabled = uiState.currentStamina >= 33,
            onAttack = onSimulateAttack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 120.dp)
        )
    }
}

@Composable
fun TopProfileBar(
    state: DashboardUiState,
    onNavigateToProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
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
            Icon(
                painter = rememberVectorPainter(Lucide.Settings), 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}

@Composable
fun StatusBarsSection(state: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusBar(
            label = "VIDA",
            valueText = state.hpText,
            progress = state.visualHpPercent,
            color = MaterialTheme.colorScheme.error,
            iconPainter = rememberVectorPainter(Lucide.Heart)
        )
        StatusBar(
            label = "ESTAMINA",
            valueText = "${state.currentStamina}%",
            progress = state.currentStamina / 100f,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            iconPainter = rememberVectorPainter(Lucide.Zap)
        )
    }
}

@Composable
fun StatusBar(label: String, valueText: String, progress: Float, color: androidx.compose.ui.graphics.Color, iconPainter: Painter) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(800), label = "StatusBarProgress")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(iconPainter, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
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
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun AttributesRow(state: DashboardUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // 1. Lógica del ATAQUE (Basado en racha de hábitos ofensivos, max 1.5f)
        val atkMult = try { state.attackMultiplier } catch (e: Exception) { 1.0f }
        val attackProgress = (atkMult / 1.5f).coerceIn(0f, 1f)
        val attackColor = when {
            atkMult >= 1.2f -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Verde: Racha Ofensiva Alta
            atkMult >= 0.9f -> androidx.compose.ui.graphics.Color(0xFFFFC107) // Amarillo: Normal
            else -> androidx.compose.ui.graphics.Color(0xFFF44336) // Rojo: Racha Perdida
        }
        val realAttack = (state.attackStat * atkMult).toInt()

        AttributeCard(
            modifier = Modifier.weight(1f), 
            label = "ATAQUE", 
            value = realAttack.toString(), 
            subValue = "(x${String.format(java.util.Locale.US, "%.1f", atkMult)})", 
            color = attackColor, 
            iconPainter = rememberVectorPainter(Lucide.Sword),
            ringProgress = attackProgress 
        )
        
        // 2. Lógica de DEFENSA (Basado en salud de Google Health Connect, max 1.5f)
        val defMult = try { state.defenseMultiplier } catch (e: Exception) { 1.0f }
        val defenseProgress = (defMult / 1.5f).coerceIn(0f, 1f)
        val defenseColor = when {
            defMult >= 1.2f -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Verde: Escudo de Salud Fuerte
            defMult >= 0.9f -> androidx.compose.ui.graphics.Color(0xFFFFC107) // Amarillo: Normal
            else -> androidx.compose.ui.graphics.Color(0xFFF44336) // Rojo: Escudo Roto (Falta de sueño/pasos)
        }
        val realDefense = (state.defenseStat * defMult).toInt()

        AttributeCard(
            modifier = Modifier.weight(1f), 
            label = "DEFENSA", 
            value = realDefense.toString(), 
            subValue = "(x${String.format(java.util.Locale.US, "%.1f", defMult)})", 
            color = defenseColor, 
            iconPainter = rememberVectorPainter(Lucide.Shield),
            ringProgress = defenseProgress
        )
    }
}

@Composable
fun AttributeCard(modifier: Modifier, label: String, value: String, subValue: String, color: androidx.compose.ui.graphics.Color, iconPainter: Painter, ringProgress: Float) {
    val animatedProgress by animateFloatAsState(targetValue = ringProgress, animationSpec = tween(1000), label = "ring")
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
                strokeWidth = 3.dp,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp)), 
                contentAlignment = Alignment.Center
            ) {
                Icon(iconPainter, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
        }
        Column(verticalArrangement = Arrangement.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(value, style = MaterialTheme.typography.titleLarge, color = color)
                Text(
                    text = subValue, 
                    style = MaterialTheme.typography.labelSmall, 
                    color = color.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
    }
}

@Composable
fun BossEncounterCard(logText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = "https://picsum.photos/seed/titan/800/1200",
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.3f),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                style = MaterialTheme.typography.titleLarge,
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
fun AttackFab(
    isEnabled: Boolean,
    onAttack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onAttack,
        enabled = isEnabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(20.dp),
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
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    },
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isEnabled) {
                    Icon(
                        painter = rememberVectorPainter(Lucide.Sword),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    if (isEnabled) "ATACAR" else "SIN ESTAMINA",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    letterSpacing = 2.sp
                )
            }
        }
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
                attackMultiplier = 1.0f,
                defenseStat = 80,
                defenseMultiplier = 1.2f,
                combatLog = "El dragón ruge ferozmente."
            ),
            onSimulateAttack = {},
            onNavigateToProfile = {}
        )
    }
}
