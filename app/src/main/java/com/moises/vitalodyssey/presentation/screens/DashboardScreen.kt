package com.moises.vitalodyssey.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moises.vitalodyssey.presentation.viewmodels.DashboardViewModel
import org.koin.androidx.compose.koinViewModel

// Paleta de Colores "Digital Relic" de tu prototipo
val SurfaceDark = Color(0xFF121416)
val PrimaryGold = Color(0xFFFEB300)
val SecondaryGold = Color(0xFFFFD799)
val HealthRed = Color(0xFFFFB4AB)
val StaminaBlue = Color(0xFF00D2FD)
val CardBg = Color(0xFF1A1C1E)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel(),
    onNavigateToProfile: () -> Unit = {}
) {
    // Observamos el estado real del ViewModel
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopProfileBar(uiState, onNavigateToProfile) },
        bottomBar = { BottomNavBar(onNavigateToProfile) },
        containerColor = SurfaceDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Sección de Barras de Estado (Vida y Energía/XP)
            StatusBarsSection(uiState)

            // Sección de Atributos (Ataque/Defensa reales)
            AttributesRow(uiState)

            // Tarjeta del Jefe con narrativa
            BossEncounterCard(uiState.combatLog)

            // Sección de Acción (Botón de ataque conectado)
            ActionSection { viewModel.simulateAttack() }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun TopProfileBar(
    state: com.moises.vitalodyssey.presentation.viewmodels.DashboardUiState,
    onNavigateToProfile: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .border(2.dp, PrimaryGold, CircleShape)
                .clickable { onNavigateToProfile() }
        ) {
            AsyncImage(
                model = "https://picsum.photos/seed/warrior/200",
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text("Moisés Sánchez", color = PrimaryGold, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("NIVEL ${state.level}", color = PrimaryGold.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(
                    progress = state.visualXpPercent,
                    modifier = Modifier.weight(1f).height(6.dp).clip(CircleShape),
                    color = PrimaryGold,
                    trackColor = PrimaryGold.copy(alpha = 0.2f)
                )
                Text(state.xpText, color = PrimaryGold.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }

        IconButton(onClick = onNavigateToProfile) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = PrimaryGold)
        }
    }
}

@Composable
fun StatusBarsSection(state: com.moises.vitalodyssey.presentation.viewmodels.DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatusBar(
            label = "VIDA",
            valueText = state.hpText,
            progress = state.visualHpPercent,
            color = if (state.visualHpPercent > 0.25f) HealthRed else Color.Red,
            icon = Icons.Default.Favorite
        )
        StatusBar(
            label = "EXPERIENCIA",
            valueText = state.xpText,
            progress = state.visualXpPercent,
            color = StaminaBlue,
            icon = Icons.Default.Star
        )
    }
}

@Composable
fun StatusBar(label: String, valueText: String, progress: Float, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(800))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Text(valueText, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = animatedProgress,
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
            color = color,
            trackColor = Color(0xFF333537)
        )
    }
}

@Composable
fun AttributesRow(state: com.moises.vitalodyssey.presentation.viewmodels.DashboardUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AttributeCard(Modifier.weight(1f), "ATAQUE", state.attackStat.toString(), SecondaryGold, Icons.Default.FlashOn)
        AttributeCard(Modifier.weight(1f), "DEFENSA", state.defenseStat.toString(), Color(0xFFEBC07D), Icons.Default.Security)
    }
}

@Composable
fun AttributeCard(modifier: Modifier, label: String, value: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = modifier
            .background(CardBg, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF514532).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(40.dp).background(Color(0xFF1E2022), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = color)
        }
        Column {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun BossEncounterCard(logText: String) {
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(32.dp))
            .border(1.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(32.dp)).background(Color(0xFF1E2022))
    ) {
        AsyncImage(
            model = "https://picsum.photos/seed/titan/800/1200",
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.4f),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = Color(0xFF93000A), shape = CircleShape) {
                Text("JEFE ELITE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("TITAN PROCRASTINADOR", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(logText, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun ActionSection(onAttack: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = onAttack,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(SecondaryGold, PrimaryGold))),
                contentAlignment = Alignment.Center
            ) {
                Text("ATACAR", color = Color(0xFF432C00), fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            }
        }
    }
}

@Composable
fun BottomNavBar(onNavigateToProfile: () -> Unit) {
    NavigationBar(containerColor = Color(0xFF1E2022).copy(alpha = 0.8f)) {
        NavigationBarItem(icon = { Icon(Icons.Default.List, null) }, label = { Text("Hábitos") }, selected = false, onClick = {})
        NavigationBarItem(icon = { Icon(Icons.Default.PlayArrow, null) }, label = { Text("Combate") }, selected = true, onClick = {})
        NavigationBarItem(icon = { Icon(Icons.Default.Person, null) }, label = { Text("Perfil") }, selected = false, onClick = onNavigateToProfile)
    }
}
