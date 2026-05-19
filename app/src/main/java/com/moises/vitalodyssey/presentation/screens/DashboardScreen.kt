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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.composables.icons.lucide.*
import com.moises.vitalodyssey.data.local.BossEntity
import com.moises.vitalodyssey.presentation.components.BattleReportDialog
import com.moises.vitalodyssey.presentation.components.VitalOdysseyBottomNavBar
import com.moises.vitalodyssey.presentation.viewmodels.DashboardUiState
import com.moises.vitalodyssey.presentation.viewmodels.DashboardViewModel
import com.moises.vitalodyssey.presentation.utils.AvatarUtils
import com.moises.vitalodyssey.ui.theme.OdysseyTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel(),
    onNavigateToProfile: () -> Unit = {},
    onNavigateToHabits: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    DashboardScreenContent(
        uiState = uiState,
        onAttackClicked = { viewModel.onAttackClicked() },
        onDismissBattleReport = { viewModel.dismissBattleReport() },
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToHabits = onNavigateToHabits,
        onDismissNoOffensiveHabitsDialog = { viewModel.dismissNoOffensiveHabitsDialog() }
    )
}

@Composable
fun DashboardScreenContent(
    uiState: DashboardUiState,
    onAttackClicked: () -> Unit,
    onDismissBattleReport: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHabits: () -> Unit,
    onDismissNoOffensiveHabitsDialog: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { TopProfileBar(uiState, onNavigateToProfile) },
            containerColor = MaterialTheme.colorScheme.surface,
            contentWindowInsets = WindowInsets(0.dp)
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                StatusBarsSection(uiState)

                AttributesRow(uiState)

                BossEncounterCard(boss = uiState.currentBoss, logText = uiState.combatLog)

                // ── Botón de ataque ─────────────────────────────────────────
                AttackFab(
                    isEnabled = uiState.developerMode || uiState.currentStamina >= 33,
                    onAttack = onAttackClicked,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (uiState.showBattleReport && uiState.lastBattleResult != null) {
            BattleReportDialog(
                result = uiState.lastBattleResult,
                onDismiss = onDismissBattleReport
            )
        }

        if (uiState.showNoOffensiveHabitsDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = onDismissNoOffensiveHabitsDialog) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Alerta",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )

                        Text(
                            text = "HÁBITO OFENSIVO REQUERIDO",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )

                        Text(
                            text = "Debes tener al menos un hábito ofensivo activo para poder atacar al jefe. El éxito en el combate depende de tu constancia diaria.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                onDismissNoOffensiveHabitsDialog()
                                onNavigateToHabits()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("IR A HÁBITOS", fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = onDismissNoOffensiveHabitsDialog,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CERRAR", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
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
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.White) // Fondo blanco puro como se pidió
                .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                .clickable { onNavigateToProfile() },
            contentAlignment = Alignment.TopCenter
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = AvatarUtils.getPlayerAvatar(state.playerClass, state.bodyType)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 2.2f // Zoom para ver la cara
                        scaleY = 2.2f
                        translationY = 45f // Bajado más para centrar mejor el rostro
                    }
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                state.playerName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                state.playerClass?.name ?: "SIN CLASE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            androidx.compose.foundation.Image(
                painter = painterResource(id = com.moises.vitalodyssey.R.drawable.ic_screen_config),
                contentDescription = null,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}

@Composable
fun StatusBarsSection(state: DashboardUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val currentHp = state.hpText.substringBefore(" / ")
        val maxHp = if (state.hpText.contains(" / ")) "/ " + state.hpText.substringAfter(" / ") else ""

        AttributeCard(
            modifier = Modifier.weight(1f),
            label = "VIDA",
            value = currentHp,
            subValue = maxHp,
            color = MaterialTheme.colorScheme.error,
            iconRes = com.moises.vitalodyssey.R.drawable.ic_stat_health,
            progress = state.visualHpPercent
        )

        AttributeCard(
            modifier = Modifier.weight(1f),
            label = "ESTAMINA",
            value = "${state.currentStamina}%",
            subValue = "",
            color = MaterialTheme.colorScheme.tertiaryContainer,
            iconRes = com.moises.vitalodyssey.R.drawable.ic_stat_stamine,
            progress = state.currentStamina / 100f
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
            iconRes = com.moises.vitalodyssey.R.drawable.ic_stat_attack,
            progress = attackProgress 
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
            iconRes = com.moises.vitalodyssey.R.drawable.ic_stat_defense,
            progress = defenseProgress
        )
    }
}

@Composable
fun AttributeCard(
    modifier: Modifier, 
    label: String, 
    value: String, 
    subValue: String, 
    color: Color, 
    iconRes: Int, 
    progress: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress, 
        animationSpec = tween(1000), 
        label = "progress"
    )
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Icono Webp
        androidx.compose.foundation.Image(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(46.dp)
        )
        
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label, 
                style = MaterialTheme.typography.labelSmall, 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontWeight = FontWeight.ExtraBold
            )
            
            // Barra de Progreso Estilo Nivel (Debajo del nombre)
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )

            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    value, 
                    style = MaterialTheme.typography.titleMedium, 
                    color = color,
                    fontWeight = FontWeight.Black
                )
                if (subValue.isNotEmpty()) {
                    Text(
                        text = subValue, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = color.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }                
            }
        }
    }
}

@Composable
fun BossEncounterCard(boss: BossEntity?, logText: String) {
    if (boss == null) {
        // Skeleton de carga si no hay jefe aún
        Box(modifier = Modifier.fillMaxWidth().height(240.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(32.dp)))
        return
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp) // Tarjeta más alta
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // 1. FONDO DINÁMICO DEL JEFE
        androidx.compose.foundation.Image(
            painter = painterResource(id = getBossBackground(boss.imageAssetId)),
            contentDescription = "Fondo del Jefe",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop, // Para que llene toda la tarjeta sin deformarse
            alpha = 0.6f // Ligeramente transparente para que no sature la vista
        )

        // 2. JEFE EN PRIMER PLANO (Abajo)
        androidx.compose.foundation.Image(
            painter = painterResource(id = getBossForeground(boss.imageAssetId)),
            contentDescription = "Jefe",
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp) // Jefe más grande
                .align(Alignment.BottomCenter)
                .offset(y = 20.dp), // Desplazado ligeramente hacia abajo
            contentScale = ContentScale.Fit
        )

        // 3. DEGRADADO OSCURO SUPERIOR (Para que los textos destaquen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .align(Alignment.TopCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.85f),
                            androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                )
        )

        // 4. CONTENIDO (Nombre, Dificultad, Barra HP) ARRIBA        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 16.dp, end = 16.dp), 
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top // Todo hacia arriba
        ) {
            Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(50)) {
                Text(
                    "NIVEL DE AMENAZA: ${boss.difficulty}", 
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                boss.name, 
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), 
                color = androidx.compose.ui.graphics.Color.White, // Forzamos blanco por el degradado
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "\"${boss.catchPhrase}\"",
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )

            // Barra de Vida del Jefe
            Spacer(modifier = Modifier.height(8.dp))
            val hpProgress = if (boss.maxHp > 0) boss.currentHp.toFloat() / boss.maxHp.toFloat() else 0f
            val animatedHp by androidx.compose.animation.core.animateFloatAsState(targetValue = hpProgress, label = "bossHp")
            LinearProgressIndicator(
                progress = { animatedHp },
                modifier = Modifier.fillMaxWidth(0.85f).height(12.dp).clip(RoundedCornerShape(50)),
                color = androidx.compose.ui.graphics.Color(0xFFE91E63),
                trackColor = androidx.compose.ui.graphics.Color.DarkGray.copy(alpha = 0.6f)
            )
            Text(
                "${boss.currentHp} / ${boss.maxHp} HP", 
                style = MaterialTheme.typography.labelMedium,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
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
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = com.moises.vitalodyssey.R.drawable.ic_action_attack),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
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
            onAttackClicked = {},
            onDismissBattleReport = {},
            onNavigateToProfile = {},
            onNavigateToHabits = {},
            onDismissNoOffensiveHabitsDialog = {}
        )
    }
}

@androidx.annotation.DrawableRes
private fun getBossBackground(assetId: String): Int {
    return when (assetId) {
        "boss_stone_golem", "boss_1", "titan_1" -> com.moises.vitalodyssey.R.drawable.bg_stone_golem
        "boss_sleep_spirit", "boss_2" -> com.moises.vitalodyssey.R.drawable.bg_boss_sleep_spirit
        "boss_shadow_mage", "boss_3" -> com.moises.vitalodyssey.R.drawable.bg_boss_shadow_mage
        "boss_fire_demon", "boss_4" -> com.moises.vitalodyssey.R.drawable.bg_boss_fire_demon
        "boss_silk_embrace", "boss_5" -> com.moises.vitalodyssey.R.drawable.bg_boss_silk_embrace
        "boss_mirror_king", "boss_6" -> com.moises.vitalodyssey.R.drawable.bg_boss_mirror_king
        else -> com.moises.vitalodyssey.R.drawable.ic_action_attack // Fondo por defecto de seguridad
    }
}

@androidx.annotation.DrawableRes
private fun getBossForeground(assetId: String): Int {
    return when (assetId) {
        "boss_stone_golem", "boss_1", "titan_1" -> com.moises.vitalodyssey.R.drawable.boss_stone_golem
        "boss_sleep_spirit", "boss_2" -> com.moises.vitalodyssey.R.drawable.boss_sleep_spirit
        "boss_shadow_mage", "boss_3" -> com.moises.vitalodyssey.R.drawable.boss_shadow_mage
        "boss_fire_demon", "boss_4" -> com.moises.vitalodyssey.R.drawable.boss_fire_demon
        "boss_silk_embrace", "boss_5" -> com.moises.vitalodyssey.R.drawable.boss_silk_embrace
        "boss_mirror_king", "boss_6" -> com.moises.vitalodyssey.R.drawable.boss_mirror_king
        else -> com.moises.vitalodyssey.R.drawable.ic_action_attack // Fondo por defecto de seguridad
    }
}
