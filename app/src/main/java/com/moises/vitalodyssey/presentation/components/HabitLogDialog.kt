package com.moises.vitalodyssey.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import com.moises.vitalodyssey.domain.model.Habit
import com.moises.vitalodyssey.domain.model.HabitRole
import com.moises.vitalodyssey.domain.model.HabitState
import com.moises.vitalodyssey.domain.model.HabitType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HabitLogDialog(
    habit: Habit,
    dateStr: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (HabitState, Float?) -> Unit
) {
    val displayDate = if (dateStr != null) {
        val date = LocalDate.parse(dateStr)
        if (date == LocalDate.now()) "Hoy" 
        else date.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es"))).replaceFirstChar { it.uppercase() }
    } else {
        "Hoy"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(habit.name, fontWeight = FontWeight.Bold)
                Text(
                    text = displayDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            HabitLoggingContent(
                habit = habit,
                onRecord = onConfirm
            )
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCELAR")
            }
        }
    )
}

@Composable
fun HabitLoggingContent(
    habit: Habit,
    onRecord: (HabitState, Float?) -> Unit
) {
    var textValue by remember { mutableStateOf("") }
    val roleColor = if (habit.role == HabitRole.OFFENSIVE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // El anillo de progreso central
        HabitProgressRing(
            score = habit.score,
            state = HabitState.UNRECORDED,
            measuredValue = null,
            isBoolean = habit.type == HabitType.BOOLEAN,
            primaryColor = roleColor,
            modifier = Modifier.size(80.dp),
            isCumulative = habit.isCumulative
        )

        if (habit.type == HabitType.MEASURABLE) {
            val label = if (habit.isCumulative) {
                "Suma a la meta: ${habit.targetValue.toInt()} ${habit.unit ?: ""}"
            } else {
                "Meta: ${habit.targetValue.toInt()} ${habit.unit ?: ""}"
            }
            Text(label, color = roleColor, style = MaterialTheme.typography.labelLarge)
            
            OutlinedTextField(
                value = textValue,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) textValue = it },
                label = { Text(if (habit.isCumulative) "Cantidad a sumar" else "Valor alcanzado") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Button(
                onClick = { 
                    val value = textValue.toFloatOrNull() ?: 0f
                    val state = if (habit.isCumulative && value > 0) HabitState.CONTRIBUTED else HabitState.COMPLETED
                    onRecord(state, value) 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = roleColor)
            ) {
                Text(if (habit.isCumulative) "SUMAR APORTACIÓN" else "GUARDAR REGISTRO")
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Cuadrícula de acciones (2x2)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (habit.type == HabitType.BOOLEAN) {
                    HabitStatusGridItem(
                        label = "Completado",
                        icon = Lucide.CircleCheck,
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { onRecord(HabitState.COMPLETED, null) },
                        modifier = Modifier.weight(1f)
                    )
                }
                HabitStatusGridItem(
                    label = "Saltado",
                    icon = Lucide.CircleMinus,
                    color = MaterialTheme.colorScheme.secondary,
                    onClick = { onRecord(HabitState.SKIPPED, null) },
                    modifier = if (habit.type == HabitType.BOOLEAN) Modifier.weight(1f) else Modifier.fillMaxWidth()
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HabitStatusGridItem(
                    label = "No Realizado",
                    icon = Lucide.CircleX,
                    color = MaterialTheme.colorScheme.error,
                    onClick = { onRecord(HabitState.MISSED, null) },
                    modifier = Modifier.weight(1f)
                )
                HabitStatusGridItem(
                    label = "Borrar",
                    icon = Lucide.Eraser,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onRecord(HabitState.UNRECORDED, null) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
