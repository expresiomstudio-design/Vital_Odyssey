package com.moises.vitalodyssey.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moises.vitalodyssey.presentation.viewmodels.ProfileViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Usamos los colores del prototipo (Digital Relic)
    val SurfaceDark = Color(0xFF121416)
    val PrimaryGold = Color(0xFFFEB300)
    val SecondaryGold = Color(0xFFFFD799)
    val HealthRed = Color(0xFFFFB4AB)
    val CardBg = Color(0xFF1A1C1E)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil del Héroe", color = PrimaryGold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = PrimaryGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        },
        containerColor = SurfaceDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Avatar con Brillo
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier
                        .size(128.dp)
                        .shadow(20.dp, CircleShape, spotColor = PrimaryGold),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, PrimaryGold),
                    color = SurfaceDark
                ) {
                    AsyncImage(
                        model = "https://picsum.photos/seed/warrior/400", 
                        contentDescription = "Avatar de Usuario",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.clip(CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = uiState.playerName, 
                fontSize = 28.sp, 
                fontWeight = FontWeight.Bold, 
                color = PrimaryGold
            )
            Text(
                text = uiState.playerClass, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.SemiBold, 
                color = Color.Gray,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Grid de Atributos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileAttributeCard(
                    modifier = Modifier.weight(1f),
                    label = "ATAQUE",
                    value = uiState.attackStat.toString(),
                    icon = Icons.Default.FlashOn,
                    color = SecondaryGold,
                    cardBg = CardBg
                )
                ProfileAttributeCard(
                    modifier = Modifier.weight(1f),
                    label = "DEFENSA",
                    value = uiState.defenseStat.toString(),
                    icon = Icons.Default.Security,
                    color = Color(0xFFEBC07D),
                    cardBg = CardBg
                )
                ProfileAttributeCard(
                    modifier = Modifier.weight(1f),
                    label = "VIDA",
                    value = uiState.hpText,
                    icon = Icons.Default.Bolt,
                    color = HealthRed,
                    cardBg = CardBg
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Récords Históricos
            Text(
                text = "Récords Históricos", 
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryGold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBg),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RecordItem("Mayor Racha de Hábitos", "24 Días", PrimaryGold)
                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))
                    RecordItem("Jefes Derrotados", "3", PrimaryGold)
                    HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))
                    RecordItem("Días de Disciplina Total", "45", PrimaryGold)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileAttributeCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    cardBg: Color
) {
    Column(
        modifier = modifier
            .background(cardBg, RoundedCornerShape(12.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon, 
            contentDescription = null, 
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value, 
            color = color, 
            fontSize = 18.sp, 
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label, 
            color = Color.Gray, 
            fontSize = 10.sp, 
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun RecordItem(label: String, value: String, highlightColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            color = highlightColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
