package com.moises.vitalodyssey.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.moises.vitalodyssey.R

// Definimos los destinos de navegación
sealed class Destination(
    val route: String,
    val title: String,
    @DrawableRes val iconRes: Int
) {
    object Habits : Destination("habits", "Hábitos", R.drawable.ic_screen_habits)
    object Health : Destination("health", "Salud", R.drawable.ic_screen_health)
    object Dashboard : Destination("dashboard", "Vital Odyssey", R.drawable.logo_vital_odyssey_stacked)
    object AppRules : Destination("app_rules", "Apps", R.drawable.ic_screen_apps)
    object Profile : Destination("profile", "Perfil", R.drawable.ic_screen_profile)
}

@Composable
fun VitalOdysseyBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 1️⃣ Primero el color de fondo → llena hasta el borde físico del dispositivo
            .background(MaterialTheme.colorScheme.surface)
            // 2️⃣ Luego el padding → empuja el CONTENIDO por encima de la nav bar del sistema
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp) // Altura más equilibrada
                .padding(bottom = 8.dp) // Subimos el contenido para alejarlo de los botones de Android
        ) {
            val destinations = listOf(
                Destination.Habits,
                Destination.Health,
                Destination.Dashboard,
                Destination.AppRules,
                Destination.Profile
            )

            destinations.forEach { destination ->
                val isCenter = destination is Destination.Dashboard
                NavigationBarItem(
                    selected = currentRoute == destination.route,
                    onClick = { onNavigate(destination.route) },
                    icon = { 
                        val iconSize = if (isCenter) 64.dp else 44.dp
                        Icon(
                            painter = painterResource(id = destination.iconRes), 
                            contentDescription = destination.title,
                            modifier = Modifier.size(iconSize),
                            tint = Color.Unspecified
                        ) 
                    },
                    label = { 
                        if (!isCenter) {
                            Text(
                                destination.title, 
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.offset(y = (-2).dp)
                            ) 
                        }
                    },
                    alwaysShowLabel = !isCenter,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = Color.Transparent
                    )
                )
            }
        }

    }
}
