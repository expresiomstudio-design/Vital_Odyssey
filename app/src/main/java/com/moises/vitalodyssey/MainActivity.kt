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
import androidx.navigation.compose.currentBackStackEntryAsState
import com.moises.vitalodyssey.presentation.components.VitalOdysseyBottomNavBar
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
                    color = MaterialTheme.colorScheme.background,
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
                OnboardingScreen {
                    navController.navigate("main_container") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            }
            composable("main_container") {
                MainContainerScreen(
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToForm = { habitId ->
                        navController.navigate("habit_form/$habitId")
                    },
                    onNavigateToTracking = { habitId ->
                        navController.navigate("habit_tracking/$habitId")
                    },
                    onNavigateToAppRuleForm = { ruleId ->
                        navController.navigate("app_rule_form/$ruleId")
                    }
                )
            }
            // Mantenemos estas rutas fuera del contenedor para que ocupen toda la pantalla sin la BottomBar
            composable(
                route = "habit_tracking/{habitId}",
                arguments = listOf(navArgument("habitId") { type = NavType.IntType })
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getInt("habitId") ?: -1
                HabitTrackingScreen(
                    habitId = habitId,
                    onNavigateToEdit = { id -> navController.navigate("habit_form/$id") },
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
            composable(
                route = "app_rule_form/{ruleId}",
                arguments = listOf(navArgument("ruleId") { type = NavType.IntType })
            ) { backStackEntry ->
                val ruleId = backStackEntry.arguments?.getInt("ruleId") ?: 0
                AppRuleFormScreen(
                    ruleId = ruleId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            // Redirecciones de compatibilidad por si se navega a rutas antiguas
            composable("dashboard") { LaunchedEffect(Unit) { navController.navigate("main_container") } }
            composable("habits") { LaunchedEffect(Unit) { navController.navigate("main_container") } }
            composable("profile") { LaunchedEffect(Unit) { navController.navigate("main_container") } }
        }
    }
}

@Composable
fun MainContainerScreen(
    onLogout: () -> Unit,
    onNavigateToForm: (Int) -> Unit,
    onNavigateToTracking: (Int) -> Unit,
    onNavigateToAppRuleForm: (Int) -> Unit
) {
    val innerNavController = rememberNavController()
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            VitalOdysseyBottomNavBar(
                currentRoute = currentRoute,
                onNavigate = { targetRoute ->
                    if (currentRoute != targetRoute) {
                        innerNavController.navigate(targetRoute) {
                            popUpTo(innerNavController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = innerNavController,
            startDestination = "dashboard",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    onNavigateToProfile = { innerNavController.navigate("profile") }
                )
            }
            composable("habits") {
                HabitsScreen(
                    onNavigateToForm = onNavigateToForm,
                    onNavigateToTracking = onNavigateToTracking
                )
            }
            composable("app_rules") {
                AppRulesScreen(
                    onNavigateToForm = onNavigateToAppRuleForm
                )
            }
            composable("profile") {
                ProfileScreen(
                    onNavigateToLogin = onLogout
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
