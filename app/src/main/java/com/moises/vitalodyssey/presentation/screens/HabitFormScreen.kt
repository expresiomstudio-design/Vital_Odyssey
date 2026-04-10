package com.moises.vitalodyssey.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.*
import com.moises.vitalodyssey.domain.model.HabitRole
import com.moises.vitalodyssey.domain.model.HabitType
import com.moises.vitalodyssey.presentation.viewmodels.HabitFormViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitFormScreen(
    habitId: Int = 0,
    viewModel: HabitFormViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState = viewModel.uiState
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(habitId) {
        if (habitId > 0) {
            viewModel.loadHabit(habitId)
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Editar Hábito" else "Nuevo Hábito") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveHabit() }) {
                        Text("GUARDAR", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Selector de Tipo (Digital Relic Cards)
            Text("Tipo de Hábito", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TypeCard(
                    title = "Sí / No",
                    subtitle = "Estado binario",
                    icon = Lucide.Check,
                    selected = uiState.type == HabitType.BOOLEAN,
                    enabled = !uiState.isEditMode,
                    onClick = { viewModel.onTypeChange(HabitType.BOOLEAN) },
                    modifier = Modifier.weight(1f)
                )
                TypeCard(
                    title = "Medible",
                    subtitle = "Valor numérico",
                    icon = Lucide.Activity,
                    selected = uiState.type == HabitType.MEASURABLE,
                    enabled = !uiState.isEditMode,
                    onClick = { viewModel.onTypeChange(HabitType.MEASURABLE) },
                    modifier = Modifier.weight(1f)
                )
            }

            // 2. Información Básica
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.onNameChange(it) },
                label = { Text("Nombre del Hábito") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(rememberVectorPainter(Lucide.Type), null) }
            )

            OutlinedTextField(
                value = uiState.note,
                onValueChange = { viewModel.onNoteChange(it) },
                label = { Text("Nota o recordatorio (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(rememberVectorPainter(Lucide.Pencil), null) }
            )

            // 3. Rol del Hábito
            Text("Rol en Combate", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoleButton(
                    title = "Ofensivo",
                    icon = Lucide.Swords,
                    selected = uiState.role == HabitRole.OFFENSIVE,
                    onClick = { viewModel.onRoleChange(HabitRole.OFFENSIVE) },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                RoleButton(
                    title = "Defensivo",
                    icon = Lucide.Shield,
                    selected = uiState.role == HabitRole.DEFENSIVE,
                    onClick = { viewModel.onRoleChange(HabitRole.DEFENSIVE) },
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            // 4. Campos Dinámicos para Medibles
            if (uiState.type == HabitType.MEASURABLE) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.targetValue.toString(),
                        onValueChange = { it.toFloatOrNull()?.let { v -> viewModel.onTargetValueChange(v) } },
                        label = { Text("Meta") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = uiState.unit,
                        onValueChange = { viewModel.onUnitChange(it) },
                        label = { Text("Unidad (Ej: Km)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Frecuencia
            FrequencySelector(
                type = uiState.frequencyType,
                target = uiState.frequencyTarget,
                onTypeChange = { viewModel.onFrequencyTypeChange(it) },
                onTargetChange = { viewModel.onFrequencyTargetChange(it) }
            )

            // 6. Botón Eliminar
            if (uiState.isEditMode) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ELIMINAR HÁBITO")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar Hábito?") },
            text = { Text("Esta acción no se puede deshacer y perderás todo el historial de este hábito.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteHabit() }) {
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
}

@Composable
fun TypeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val alpha = if (enabled) 1f else 0.4f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = rememberVectorPainter(icon),
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RoleButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) color.copy(alpha = 0.1f) else Color.Transparent,
            contentColor = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = if (selected) borderStroke(2.dp, color) else borderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Icon(rememberVectorPainter(icon), null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title)
    }
}

fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencySelector(
    type: String,
    target: Int,
    onTypeChange: (String) -> Unit,
    onTargetChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("DAILY", "WEEKLY", "MONTHLY", "INTERVAL")
    val labelMap = mapOf("DAILY" to "Diario", "WEEKLY" to "Semanal", "MONTHLY" to "Mensual", "INTERVAL" to "Intervalo")

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Frecuencia", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = labelMap[type] ?: type,
                onValueChange = {},
                readOnly = true,
                label = { Text("Frecuencia") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                leadingIcon = { Icon(rememberVectorPainter(Lucide.Calendar), null) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(labelMap[option] ?: option) },
                        onClick = {
                            onTypeChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }

        when (type) {
            "WEEKLY", "MONTHLY" -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("¿Cuántas veces por ${if (type == "WEEKLY") "semana" else "mes"}?")
                    OutlinedTextField(
                        value = target.toString(),
                        onValueChange = { it.toIntOrNull()?.let { v -> onTargetChange(v) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp)
                    )
                }
            }
            "INTERVAL" -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Repetir cada")
                    OutlinedTextField(
                        value = target.toString(),
                        onValueChange = { it.toIntOrNull()?.let { v -> onTargetChange(v) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp)
                    )
                    Text("días")
                }
            }
        }
    }
}
