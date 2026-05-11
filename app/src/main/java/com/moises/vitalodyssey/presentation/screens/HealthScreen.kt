package com.moises.vitalodyssey.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import com.moises.vitalodyssey.presentation.components.PermissionExplanationDialog
import com.moises.vitalodyssey.presentation.viewmodels.HealthUiState
import com.moises.vitalodyssey.presentation.viewmodels.HealthViewModel
import org.koin.androidx.compose.koinViewModel

// ── Colores semánticos locales ────────────────────────────────────────────────
private val ColorPerfect = Color(0xFFFFD799)   // Dorado — bono perfecto (1.5×)
private val ColorPartial = Color(0xFF80D8FF)   // Cian — bono parcial (1.2×)
private val ColorNeutral = Color(0xFF8E9199)   // Gris — neutro (1.0×)
private val ColorPenalty = Color(0xFFEA2B14)   // Rojo — penalización (0.8×)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(viewModel: HealthViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Permisos requeridos (extraídos para reutilizar en launcher y diálogo)
    val requiredPermissions = remember {
        setOf(
            androidx.health.connect.client.permission.HealthPermission
                .getReadPermission(androidx.health.connect.client.records.StepsRecord::class),
            androidx.health.connect.client.permission.HealthPermission
                .getReadPermission(androidx.health.connect.client.records.SleepSessionRecord::class)
        )
    }

    // ── Launcher de permisos — debe declararse antes de cualquier layout ──────
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.health.connect.client.PermissionController
            .createRequestPermissionResultContract()
    ) { granted ->
        viewModel.onPermissionResult(granted.containsAll(requiredPermissions))
    }

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

            // ── Cabecera ──────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Salud",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                }
            }

            // ── Tarjeta del Escudo ────────────────────────────────────────────
            item { ShieldStatusCard(multiplier = uiState.defenseMultiplier) }

            // ── Datos del Día Anterior ────────────────────────────────────────
            item { YesterdayDataCard(uiState = uiState) }

            // ── Sección Sincronización ────────────────────────────────────────
            item { SyncModeCard(uiState = uiState, onToggle = viewModel::onToggleAutomaticMode) }

            // ── Botón de reporte manual (sólo en modo Manual) ─────────────────
            if (!uiState.isAutomatic) {
                item {
                    Button(
                        onClick = { viewModel.toggleManualDialog(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            Lucide.ClipboardPen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "REPORTAR DATOS MANUALMENTE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 13.sp,
                                letterSpacing = 1.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(110.dp)) }
        }
    }

    // ── Diálogo de permisos de Health Connect ─────────────────────────────────
    if (uiState.showPermissionDialog) {
        PermissionExplanationDialog(
            title = "Acceso a Health Connect",
            description = "Para potenciar tu escudo automáticamente, necesitamos leer tus pasos y sueño de ayer.",
            icon = Lucide.Shield,
            steps = listOf(
                "Pulsa el botón para abrir Health Connect.",
                "Permite el acceso a Pasos y Sueño.",
                "Vuelve a la aplicación."
            ),
            onConfirm = {
                val providerPackageName = "com.google.android.apps.healthdata"
                val status = androidx.health.connect.client.HealthConnectClient.getSdkStatus(context)
                
                when (status) {
                    androidx.health.connect.client.HealthConnectClient.SDK_AVAILABLE -> {
                        try {
                            permissionLauncher.launch(requiredPermissions)
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "Error al lanzar ventana de permisos.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {
                        android.widget.Toast.makeText(
                            context,
                            "Instala Health Connect para continuar",
                            android.widget.Toast.LENGTH_LONG
                        ).show()

                        try {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("market://details?id=$providerPackageName")
                            ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            try {
                                val fallbackIntent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://play.google.com/store/apps/details?id=$providerPackageName")
                                ).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }
                                context.startActivity(fallbackIntent)
                            } catch (e2: Exception) {
                                android.widget.Toast.makeText(
                                    context, 
                                    "No se pudo abrir la tienda de aplicaciones. Búscalo manualmente.", 
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        viewModel.onToggleAutomaticMode(false)
                        viewModel.onPermissionResult(false)
                    }
                }
            },
            onDismiss = {
                viewModel.onToggleAutomaticMode(false)
                viewModel.onPermissionResult(false)
            }
        )
    }

    // ── Diálogo de reporte manual ─────────────────────────────────────────────
    if (uiState.showManualDialog) {
        ManualReportDialog(
            onDismiss = { viewModel.toggleManualDialog(false) },
            onConfirm = { steps, sleep -> viewModel.submitManualReport(steps, sleep) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ShieldStatusCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ShieldStatusCard(multiplier: Float) {
    val shieldColor = when {
        multiplier >= 1.5f -> ColorPerfect
        multiplier >= 1.2f -> ColorPartial
        multiplier <= 0.8f -> ColorPenalty
        else               -> ColorNeutral
    }
    val animatedColor by animateColorAsState(
        targetValue = shieldColor,
        animationSpec = tween(600),
        label = "shieldColor"
    )

    // Pulso continuo sólo en el estado perfecto
    val infiniteTransition = rememberInfiniteTransition(label = "shieldPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (multiplier >= 1.5f) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val (label, subtitle) = when {
        multiplier >= 1.5f -> "✦ ESCUDO PERFECTO"   to "Cumpliste todas tus metas. ¡Defensa máxima!"
        multiplier >= 1.2f -> "◈ ESCUDO REFORZADO"  to "Superaste al menos una meta."
        multiplier <= 0.8f -> "✕ ESCUDO DEBILITADO"  to "Abandono extremo en alguna métrica."
        else               -> "◇ ESCUDO ESTÁNDAR"   to "Progreso moderado. Sin penalización."
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.5.dp,
            color = animatedColor.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icono del escudo con halo animado
            Box(contentAlignment = Alignment.Center) {
                // Halo difuso
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    animatedColor.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Icon(
                    Lucide.ShieldCheck,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .scale(pulseScale)
                        .graphicsLayer {
                            shadowElevation = if (multiplier >= 1.5f) 20f else 0f
                        },
                    tint = animatedColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = animatedColor,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Chip del multiplicador
            Surface(
                shape = RoundedCornerShape(50),
                color = animatedColor.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, animatedColor.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "×${"%.1f".format(multiplier)} Defensa",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = animatedColor,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// YesterdayDataCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun YesterdayDataCard(uiState: HealthUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Lucide.CalendarClock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Datos del Día Anterior",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HealthMetricPill(
                    icon = Lucide.Footprints,
                    value = "%,d".format(uiState.steps),
                    label = "Pasos",
                    color = MaterialTheme.colorScheme.primary
                )
                HealthMetricPill(
                    icon = Lucide.Moon,
                    value = "${"%.1f".format(uiState.sleepHours)} h",
                    label = "Sueño",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            if (!uiState.isAutomatic && uiState.steps == 0L && uiState.sleepHours == 0f) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ColorPenalty.copy(alpha = 0.08f))
                        .border(1.dp, ColorPenalty.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Lucide.TriangleAlert,
                        contentDescription = null,
                        tint = ColorPenalty,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Sin reporte para ayer. Tu escudo está debilitado.",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorPenalty
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthMetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f))
                .border(1.dp, color.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SyncModeCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SyncModeCard(
    uiState: HealthUiState,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Lucide.RefreshCw,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Sincronización",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.isAutomatic) "Modo Automático" else "Modo Manual",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (uiState.isAutomatic)
                            "Datos desde Health Connect"
                        else
                            "Introduces los datos tú mismo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.isAutomatic,
                    onCheckedChange = onToggle,
                    thumbContent = if (uiState.isAutomatic) {
                        {
                            Icon(
                                Lucide.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    } else null,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedIconColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            // Badge de estado de permisos
            if (uiState.isAutomatic) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (uiState.hasPermissions) ColorPartial.copy(alpha = 0.08f)
                            else ColorPenalty.copy(alpha = 0.08f)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (uiState.hasPermissions) Lucide.ShieldCheck else Lucide.ShieldAlert,
                        contentDescription = null,
                        tint = if (uiState.hasPermissions) ColorPartial else ColorPenalty,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.hasPermissions)
                            "Permisos de Health Connect concedidos"
                        else
                            "Permisos no concedidos — los datos serán 0",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (uiState.hasPermissions) ColorPartial else ColorPenalty
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ManualReportDialog
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualReportDialog(
    onDismiss: () -> Unit,
    onConfirm: (steps: Long, sleep: Float) -> Unit
) {
    var stepsInput by remember { mutableStateOf("") }
    var sleepInput by remember { mutableStateOf("") }
    val stepsError = stepsInput.isNotBlank() && stepsInput.toLongOrNull() == null
    val sleepError = sleepInput.isNotBlank() && sleepInput.toFloatOrNull() == null

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Cabecera
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Lucide.ClipboardPen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Reporte Manual",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    "Introduce los datos de ayer",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Campo pasos
                OutlinedTextField(
                    value = stepsInput,
                    onValueChange = { stepsInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Pasos dados") },
                    leadingIcon = {
                        Icon(Lucide.Footprints, null, modifier = Modifier.size(18.dp))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = stepsError,
                    supportingText = if (stepsError) {
                        { Text("Introduce un número entero válido") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Campo sueño
                OutlinedTextField(
                    value = sleepInput,
                    onValueChange = { sleepInput = it },
                    label = { Text("Horas de sueño") },
                    leadingIcon = {
                        Icon(Lucide.Moon, null, modifier = Modifier.size(18.dp))
                    },
                    placeholder = { Text("ej. 7.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = sleepError,
                    supportingText = if (sleepError) {
                        { Text("Introduce un número válido (ej. 7.5)") }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Acciones
                Button(
                    onClick = {
                        val steps = stepsInput.toLongOrNull() ?: 0L
                        val sleep = sleepInput.toFloatOrNull() ?: 0f
                        onConfirm(steps, sleep)
                    },
                    enabled = stepsInput.isNotBlank() && sleepInput.isNotBlank() && !stepsError && !sleepError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("GUARDAR REPORTE", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CANCELAR", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
