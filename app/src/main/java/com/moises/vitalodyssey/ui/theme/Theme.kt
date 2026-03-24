package com.moises.vitalodyssey.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// Odyssey Colors
val Surface = Color(0xFF121416)
val OnSurface = Color(0xFFE2E2E5)
val Primary = Color(0xFFFFD799)
val PrimaryContainer = Color(0xFFFEB300)
val Secondary = Color(0xFFEBC07D)
val TertiaryContainer = Color(0xFF80D8FF) // Azul claro para Estamina/Foco
val Error = Color(0xFFEA2B14)
val OutlineVariant = Color(0xFF514532)
val SurfaceVariant = Color(0xFF1A1C1E)
val OnPrimary = Color(0xFF432C00)
val OnSurfaceVariant = Color(0xFF8E9199)

val OdysseyColorScheme = darkColorScheme(
    surface = Surface,
    onSurface = OnSurface,
    primary = Primary,
    primaryContainer = PrimaryContainer,
    secondary = Secondary,
    tertiaryContainer = TertiaryContainer,
    error = Error,
    surfaceVariant = SurfaceVariant,
    outlineVariant = OutlineVariant,
    onPrimary = OnPrimary,
    onSurfaceVariant = OnSurfaceVariant
)

val OdysseyTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun OdysseyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> OdysseyColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OdysseyTypography,
        content = content
    )
}

// Keep the old name for compatibility but redirecting to the new one
@Composable
fun VitalOdysseyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    OdysseyTheme(darkTheme, dynamicColor, content)
}
