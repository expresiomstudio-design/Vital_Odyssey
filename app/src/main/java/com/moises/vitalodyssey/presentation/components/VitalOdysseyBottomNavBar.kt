package com.moises.vitalodyssey.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*

@Composable
fun VitalOdysseyBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(95.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            NavigationBarItem(
                selected = currentRoute == "habits",
                onClick = { onNavigate("habits") },
                icon = { 
                    Icon(
                        painter = rememberVectorPainter(Lucide.ScrollText), 
                        contentDescription = "Hábitos"
                    ) 
                },
                label = { Text("Hábitos", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                )
            )

            // Item central placeholder
            NavigationBarItem(
                selected = false,
                onClick = { onNavigate("dashboard") },
                icon = { Spacer(modifier = Modifier.size(24.dp)) },
                label = { 
                    Text(
                        "Combate", 
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Transparent 
                    ) 
                }
            )

            NavigationBarItem(
                selected = currentRoute == "profile",
                onClick = { onNavigate("profile") },
                icon = { 
                    Icon(
                        painter = rememberVectorPainter(Lucide.User), 
                        contentDescription = "Perfil"
                    ) 
                },
                label = { Text("Perfil", style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                )
            )
        }

        // Botón central elevado
        FloatingActionButton(
            onClick = { onNavigate("dashboard") },
            shape = CircleShape,
            containerColor = if (currentRoute == "dashboard") 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (currentRoute == "dashboard")
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.primary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(64.dp)
        ) {
            Icon(
                painter = rememberVectorPainter(Lucide.Swords),
                contentDescription = "Combate",
                modifier = Modifier.size(30.dp)
            )
        }
    }
}
