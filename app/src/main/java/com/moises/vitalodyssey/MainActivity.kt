package com.moises.vitalodyssey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.moises.vitalodyssey.presentation.screens.DashboardScreen
import com.moises.vitalodyssey.presentation.screens.HabitsScreen
import com.moises.vitalodyssey.presentation.screens.ProfileScreen
import com.moises.vitalodyssey.ui.theme.VitalOdysseyTheme
import androidx.compose.ui.tooling.preview.Preview

import androidx.compose.ui.platform.LocalContext
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import com.moises.vitalodyssey.di.appModule

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
fun VitalOdysseyMainScreen() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
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
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VitalOdysseyMainScreenPreview() {
    val context = LocalContext.current
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            androidContext(context)
            modules(appModule)
        }
    }
    
    VitalOdysseyTheme {
        Surface {
            VitalOdysseyMainScreen()
        }
    }
}
