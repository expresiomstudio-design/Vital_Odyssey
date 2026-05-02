package com.moises.vitalodyssey.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Foco Arcano") },
                actions = {
                    IconButton(onClick = { onNavigateToForm(0) }) {
                        Icon(Icons.Default.Add, contentDescription = "Nueva Regla")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.rules.isEmpty()) {
            EmptyRulesState(onAddClick = { onNavigateToForm(0) }, modifier = Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.rules, key = { it.rule.id }) { ruleWithUsage ->
                    AppRuleCard(
                        ruleWithUsage = ruleWithUsage,
                        onToggle = { isEnabled -> viewModel.toggleRuleState(ruleWithUsage.rule.id, isEnabled) },
                        onClick = { onNavigateToForm(ruleWithUsage.rule.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppUsageProgressRing(
    usedMinutes: Int,
    limitMinutes: Int,
    modifier: Modifier = Modifier
) {
    val progress = (usedMinutes.toFloat() / limitMinutes.toFloat()).coerceIn(0f, 1.1f) // Permitimos un poco más para efecto visual
    val sweepAngle = (progress.coerceAtMost(1f)) * 360f
    
    val color = when {
        progress >= 1f -> MaterialTheme.colorScheme.error
        progress >= 0.75f -> Color(0xFFFFA000) // Ámbar/Amarillo
        else -> Color(0xFF4CAF50) // Verde
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
        Text(
            text = if (remaining <= 0) "!" else "${remaining}m",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = if (remaining > 99) 10.sp else 12.sp,
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
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Lucide.Clock, 
                        contentDescription = null, 
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Límite: ${rule.timeLimitMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = rule.isEnabled,
                onCheckedChange = onToggle,
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
        }
    }
}

@Composable
fun EmptyRulesState(onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
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
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Crea límites para tus aplicaciones",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Añadir mi primera regla")
        }
    }
}
