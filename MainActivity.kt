package com.moises.vitalodyssey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.moises.vitalodyssey.domain.repository.UserRepository
import com.moises.vitalodyssey.presentation.screens.*
import com.moises.vitalodyssey.ui.theme.VitalOdysseyTheme
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VitalOdysseyTheme {
                Surface {
                    VitalOdysseyMainScreen()
                }
            }
        }
    }
}

@Composable
fun VitalOdysseyMainScreen(
    userRepository: UserRepository = koinInject()
) {
    val navController = rememberNavController()
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val userProfile by userRepository.getUserProfile().collectAsState(initial = null)
    
    // Estado para manejar la carga inicial
    var isCheckingAuth by remember { mutableStateOf(true) }
    var startDestination by remember { mutableStateOf("login") }

    LaunchedEffect(currentUser, userProfile) {
        startDestination = when {
            currentUser == null -> "login"
            userProfile == null -> "onboarding" // Si hay Auth pero no Profile, forzamos onboarding
            userProfile?.hasCompletedOnboarding == true -> "dashboard"
            else -> "onboarding"
        }
        isCheckingAuth = false
    }

    if (isCheckingAuth) {
        // Podrías mostrar una pantalla de Splash aquí
        return
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("onboarding") {
                        popUpTo("login") { inclusive = true }
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
                onNavigateToProfile = { navController.navigate("profile") }
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
