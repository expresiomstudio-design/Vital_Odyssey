package com.moises.vitalodyssey.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.composables.icons.lucide.*
import com.moises.vitalodyssey.presentation.components.AppSelectorSheet
import com.moises.vitalodyssey.presentation.viewmodels.AppRuleFormViewModel
import org.koin.androidx.compose.koinViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRuleFormScreen(
    ruleId: Int = 0,
    viewModel: AppRuleFormViewModel = koinViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAppSelector by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showTimePickerForStart by remember { mutableStateOf(false) }
    var showTimePickerForEnd by remember { mutableStateOf(false) }

    LaunchedEffect(ruleId) {
        viewModel.loadRule(ruleId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.id == 0) "Nueva Regla" else "Editar Regla") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.saveRule() }) {
                        Icon(Icons.Default.Check, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Selección de App
            AppSelectionCard(
                packageName = uiState.packageName,
                appName = uiState.appName,
                icon = uiState.installedApps.find { it.packageName == uiState.packageName }?.icon,
                onClick = { showAppSelector = true }
            )

            if (uiState.hasOtherRules) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Lucide.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Esta app ya tiene otras reglas. Recuerda que los temporizadores son independientes: si tienes una regla diaria y una por horario, el tiempo se descontará de ambas simultáneamente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Nombre de la Regla
            OutlinedTextField(
                value = uiState.ruleName,
                onValueChange = { viewModel.onRuleNameChange(it) },
                label = { Text("Nombre de la Regla") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Lucide.Type, contentDescription = null) },
                placeholder = { Text("Ej: Limitar Redes Sociales") },
                shape = RoundedCornerShape(12.dp)
            )

            // 3. Configuración de Límite
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Límite de Uso", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                
                DailyLimitSelector(
                    totalMinutes = uiState.timeLimitMinutes,
                    onMinutesChange = { viewModel.onTimeLimitChange(it) }
                )

                Text("Modo de Activación", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !uiState.isBlockMode,
                        onClick = { viewModel.onBlockModeChange(false) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("Diario") }
                    )
                    SegmentedButton(
                        selected = uiState.isBlockMode,
                        onClick = { viewModel.onBlockModeChange(true) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("Horario") }
                    )
                }

                if (uiState.isBlockMode) {
                    TimeRangeSelector(
                        startTime = uiState.startTime,
                        endTime = uiState.endTime,
                        onStartClick = { showTimePickerForStart = true },
                        onEndClick = { showTimePickerForEnd = true }
                    )
                }

                Text(
                    text = if (uiState.isBlockMode) 
                        "El tiempo de uso solo se contabilizará dentro de esta franja horaria. Fuera de este horario, el uso es completamente libre."
                    else 
                        "El tiempo de uso se contabilizará durante tu día lógico (desde las ${uiState.cutoffTime} hasta la misma hora del día siguiente).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 4. Días Activos
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Días de Activación", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                DaysSelector(
                    selectedDays = uiState.activeDays,
                    onDaysChanged = { viewModel.onDaysChanged(it) }
                )
            }

            // 5. Botón de Guardar Principal
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = { viewModel.saveRule() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GUARDAR REGLA", fontWeight = FontWeight.Bold)
                }
            }

            // 6. Botón de Eliminar
            if (uiState.id != 0) {
                TextButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ELIMINAR REGLA")
                }
            }

            Spacer(modifier = Modifier.height(110.dp))
        }
    }

    if (showAppSelector) {
        AppSelectorSheet(
            installedApps = uiState.installedApps,
            onAppSelected = { 
                viewModel.onPackageSelected(it)
                showAppSelector = false
            },
            onDismiss = { showAppSelector = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar Regla?") },
            text = { Text("Esta aplicación dejará de estar monitoreada por el Foco Arcano.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteRule() }) {
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

    // Implementación de Time Pickers
    if (showTimePickerForStart) {
        TimePickerDialogWrapper(
            initialTime = uiState.startTime,
            onTimeSelected = { 
                viewModel.onStartTimeChange(it)
                showTimePickerForStart = false
            },
            onDismiss = { showTimePickerForStart = false }
        )
    }

    if (showTimePickerForEnd) {
        TimePickerDialogWrapper(
            initialTime = uiState.endTime,
            onTimeSelected = { 
                viewModel.onEndTimeChange(it)
                showTimePickerForEnd = false
            },
            onDismiss = { showTimePickerForEnd = false }
        )
    }
}

@Composable
fun AppSelectionCard(
    packageName: String,
    appName: String,
    icon: android.graphics.drawable.Drawable?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (packageName.isEmpty()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Plus, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "Seleccionar Aplicación",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Image(
                    painter = rememberAsyncImagePainter(icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(Lucide.RefreshCw, contentDescription = "Cambiar", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun DailyLimitSelector(
    totalMinutes: Int,
    onMinutesChange: (Int) -> Unit
) {
    var hoursText by remember(totalMinutes) { 
        mutableStateOf(if (totalMinutes / 60 > 0) (totalMinutes / 60).toString() else "") 
    }
    var minsText by remember(totalMinutes) { 
        mutableStateOf(if (totalMinutes % 60 > 0) (totalMinutes % 60).toString() else "") 
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = hoursText,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() }) {
                    hoursText = newValue
                    val h = newValue.toIntOrNull() ?: 0
                    val m = minsText.toIntOrNull() ?: 0
                    onMinutesChange(h * 60 + m)
                }
            },
            label = { Text("Horas") },
            placeholder = { Text("0") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp)
        )
        OutlinedTextField(
            value = minsText,
            onValueChange = { newValue ->
                if (newValue.all { it.isDigit() }) {
                    minsText = newValue
                    val h = hoursText.toIntOrNull() ?: 0
                    val m = newValue.toIntOrNull() ?: 0
                    onMinutesChange(h * 60 + m)
                }
            },
            label = { Text("Minutos") },
            placeholder = { Text("0") },
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
fun TimeRangeSelector(
    startTime: String,
    endTime: String,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedCard(
            onClick = onStartClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Inicia", style = MaterialTheme.typography.labelSmall)
                Text(startTime, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        OutlinedCard(
            onClick = onEndClick,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Finaliza", style = MaterialTheme.typography.labelSmall)
                Text(endTime, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DaysSelector(
    selectedDays: List<Int>,
    onDaysChanged: (List<Int>) -> Unit
) {
    val days = listOf("L", "M", "M", "J", "V", "S", "D")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, day ->
            val dayNum = index + 1
            val isSelected = selectedDays.contains(dayNum)
            
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                        else Color.Transparent
                    )
                    .border(
                        1.dp, 
                        if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant, 
                        CircleShape
                    )
                    .clickable {
                        val newDays = if (isSelected) {
                            selectedDays.filter { it != dayNum }
                        } else {
                            (selectedDays + dayNum).sorted()
                        }
                        onDaysChanged(newDays)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialogWrapper(
    initialTime: String,
    onTimeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val parts = initialTime.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val initialMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val formattedTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                onTimeSelected(formattedTime)
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        },
        text = {
            TimePicker(state = timePickerState)
        }
    )
}
