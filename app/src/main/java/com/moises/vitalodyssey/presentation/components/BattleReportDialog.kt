package com.moises.vitalodyssey.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.ShieldAlert
import com.composables.icons.lucide.Sword
import com.composables.icons.lucide.Star
import com.moises.vitalodyssey.domain.usecase.BattleResult

@Composable
fun BattleReportDialog(
    result: BattleResult,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                Text(
                    text = "REPORTE DE BATALLA",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                HorizontalDivider(modifier = Modifier.alpha(0.5f))

                // Daño Infligido
                ReportRow(icon = Lucide.Sword, color = androidx.compose.ui.graphics.Color(0xFF4CAF50), label = "Daño al Jefe", value = "${result.damageDealtToBoss}")
                
                // Daño Recibido
                ReportRow(icon = Lucide.ShieldAlert, color = androidx.compose.ui.graphics.Color(0xFFF44336), label = "Daño Recibido", value = "${result.damageReceivedFromBoss}")
                
                // Curación
                if (result.hpHealed > 0) {
                    ReportRow(icon = Lucide.Heart, color = androidx.compose.ui.graphics.Color(0xFFE91E63), label = "Salud Recuperada", value = "+${result.hpHealed}")
                }
                
                // XP
                ReportRow(icon = Lucide.Star, color = androidx.compose.ui.graphics.Color(0xFFFFC107), label = "Experiencia Ganada", value = "+${result.xpEarned} XP")

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("CONTINUAR", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ReportRow(icon: androidx.compose.ui.graphics.vector.ImageVector, color: androidx.compose.ui.graphics.Color, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
    }
}
