package com.moises.vitalodyssey.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import com.moises.vitalodyssey.domain.model.AppRuleWithUsage
import com.moises.vitalodyssey.presentation.viewmodels.AppRulesViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRulesScreen(
    viewModel: AppRulesViewModel = koinViewModel(),
    onNavigateToForm: (Int) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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

            item {
                SectionHeader(
                    title = "Mis Reglas",
                    onAddClick = { onNavigateToForm(0) }
                )
            }

            if (!uiState.hasPermission) {
                item {
                    OutlinedCard(
                        onClick = { showPermissionDialog = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Lucide.Eye, contentDescription = null, tint = MaterialTheme.colorScheme.primaryContainer)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Activar Foco Arcano", fontWeight = FontWeight.Bold)
                                Text("Requiere permisos de uso", style = MaterialTheme.typography.labelSmall)
                            }
                            Icon(Lucide.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.rules.isEmpty()) {
                item {
                    EmptyRulesView(onAddClick = { onNavigateToForm(0) })
                }
            } else {
                items(uiState.rules, key = { it.rule.id }) { ruleWithUsage ->
                    AppRuleCard(
                        ruleWithUsage = ruleWithUsage,
                        onToggle = { isEnabled -> viewModel.toggleRuleState(ruleWithUsage.rule.id, isEnabled) },
                        onClick = { onNavigateToForm(ruleWithUsage.rule.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showPermissionDialog) {
        com.moises.vitalodyssey.presentation.components.PermissionExplanationDialog(
            title = "Acceso de Uso Requerido",
            description = "Para calcular tu Foco Arcano y recargar estamina, necesitamos vigilar a los ladrones de tiempo.",
            icon = Lucide.Activity,
            steps = listOf(
                "Pulsa el botón de abajo para ir a Ajustes.",
                "Busca 'Vital Odyssey' en la lista.",
                "Activa el interruptor 'Permitir acceso de uso'.",
                "Vuelve a esta pantalla."
            ),
            onConfirm = {
                showPermissionDialog = false
                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
            },
            onDismiss = { showPermissionDialog = false }
        )
    }
}

@Composable
fun AppUsageProgressRing(
    usedMinutes: Int,
    limitMinutes: Int,
    modifier: Modifier = Modifier
) {
    val progress = (usedMinutes.toFloat() / limitMinutes.toFloat()).coerceIn(0f, 1.1f)
    val sweepAngle = (progress.coerceAtMost(1f)) * 360f
    
    val color = when {
        progress >= 1f -> MaterialTheme.colorScheme.error
        progress >= 0.75f -> Color(0xFFFFA000)
        else -> Color(0xFF4CAF50)
    }
    
    val backgroundColor = color.copy(alpha = 0.2f)

    Box(modifier = modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(52.dp)) {
            drawCircle(
                color = backgroundColor,
                style = Stroke(width = 5.dp.toPx())
            )
            drawArc(
                color = if (sweepAngle > 0) color else Color.Transparent,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        val remaining = limitMinutes - usedMinutes
        val displayText = if (remaining < 0) {
            "+${kotlin.math.abs(remaining)}m" // Muestra cuánto te pasaste (Ej: +15m)
        } else {
            "${remaining}m"
        }

        Text(
            text = displayText,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (displayText.length > 3) 10.sp else 12.sp,
                color = color
            )
        )
    }
}

@Composable
fun AppRuleCard(
    ruleWithUsage: AppRuleWithUsage,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val rule = ruleWithUsage.rule
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (rule.isEnabled) 1f else 0.5f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppUsageProgressRing(
            usedMinutes = ruleWithUsage.usageMinutes,
            limitMinutes = rule.timeLimitMinutes
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rule.appName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            MiniDaysRow(activeDays = rule.activeDays)
        }

        Switch(
            checked = rule.isEnabled,
            onCheckedChange = onToggle,
            modifier = Modifier.scale(0.8f),
            thumbContent = if (rule.isEnabled) {
                {
                    Icon(
                        imageVector = Lucide.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else null
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            painter = rememberVectorPainter(Lucide.ChevronRight),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun EmptyRulesView(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Lucide.Smartphone, 
            contentDescription = null, 
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No tienes reglas activas",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Añadir mi primera regla")
        }
    }
}

@Composable
fun MiniDaysRow(activeDays: List<Int>) {
    val days = listOf("L", "M", "M", "J", "V", "S", "D")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEachIndexed { index, day ->
            val isActive = activeDays.contains(index + 1)
            Text(
                text = day,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}
