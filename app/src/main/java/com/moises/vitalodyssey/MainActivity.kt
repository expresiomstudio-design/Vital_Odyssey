package com.moises.vitalodyssey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moises.vitalodyssey.presentation.screens.*
import com.moises.vitalodyssey.presentation.viewmodels.MainViewModel
import com.moises.vitalodyssey.ui.theme.VitalOdysseyTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VitalOdysseyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VitalOdysseyMainScreen()
                }
            }
        }
    }
}

@Composable
fun VitalOdysseyMainScreen(
    viewModel: MainViewModel = koinViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val startDestination by viewModel.startDestination.collectAsState()
    val navController = rememberNavController()

    if (isLoading) {
        SplashScreen()
    } else {
        NavHost(navController = navController, startDestination = startDestination) {
            composable("login") {
                val scope = rememberCoroutineScope()
                LoginScreen(
                    onLoginSuccess = {
                        scope.launch {
                            val destination = viewModel.getDestinationAfterLogin()
                            navController.navigate(destination) {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable("onboarding") {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate("dashboard") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                )
            }
            composable("dashboard") {
                DashboardScreen(
                    onNavigateToHabits = { navController.navigate("habits") },
                    onNavigateToProfile = { navController.navigate("profile") }
                )
            }
            composable("habits") {
                HabitsScreen(
                    onNavigateToDashboard = { navController.navigate("dashboard") },
                    onNavigateToProfile = { navController.navigate("profile") },
                    onNavigateToForm = { habitId ->
                        navController.navigate("habit_form/$habitId")
                    },
                    onNavigateToTracking = { habitId ->
                        navController.navigate("habit_tracking/$habitId")
                    }
                )
            }
            composable(
                route = "habit_tracking/{habitId}",
                arguments = listOf(navArgument("habitId") { type = NavType.IntType })
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getInt("habitId") ?: -1
                HabitTrackingScreen(
                    habitId = habitId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "habit_form/{habitId}",
                arguments = listOf(navArgument("habitId") { type = NavType.IntType })
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getInt("habitId") ?: -1
                HabitFormScreen(
                    habitId = habitId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable("profile") {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogin = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "VITAL ODYSSEY",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primaryContainer,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "CARGANDO MUNDO...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
        }
    }
}
