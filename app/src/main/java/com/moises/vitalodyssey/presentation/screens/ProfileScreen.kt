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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.moises.vitalodyssey.presentation.viewmodels.ProfileUiState
import com.moises.vitalodyssey.presentation.viewmodels.ProfileViewModel
import com.moises.vitalodyssey.ui.theme.VitalOdysseyTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    ProfileScreenContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onLogout = { viewModel.logout() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Perfil del Héroe", 
                        style = MaterialTheme.typography.headlineMedium, 
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Volver", 
                            tint = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            tint = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
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
            
            Box(contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier
                        .size(128.dp)
                        .shadow(20.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = CircleShape,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer),
                    color = MaterialTheme.colorScheme.surface
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
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primaryContainer
            )
            Text(
                text = uiState.playerClass, 
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileAttributeCard(
                    modifier = Modifier.weight(1f),
                    label = "ATAQUE",
                    value = uiState.attackStat.toString(),
                    icon = Icons.Default.FlashOn,
                    color = MaterialTheme.colorScheme.primary
                )
                ProfileAttributeCard(
                    modifier = Modifier.weight(1f),
                    label = "DEFENSA",
                    value = uiState.defenseStat.toString(),
                    icon = Icons.Default.Security,
                    color = MaterialTheme.colorScheme.secondary
                )
                ProfileAttributeCard(
                    modifier = Modifier.weight(1f),
                    label = "VIDA",
                    value = uiState.hpText,
                    icon = Icons.Default.Bolt,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Récords Históricos", 
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RecordItem("Mayor Racha de Hábitos", "24 Días", MaterialTheme.colorScheme.primaryContainer)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))
                    RecordItem("Jefes Derrotados", "3", MaterialTheme.colorScheme.primaryContainer)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))
                    RecordItem("Días de Disciplina Total", "45", MaterialTheme.colorScheme.primaryContainer)
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
    color: Color
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
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
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = highlightColor
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    VitalOdysseyTheme {
        ProfileScreenContent(
            uiState = ProfileUiState(
                playerName = "Moisés Sánchez",
                playerClass = "MAGO DE GREMIO • CLASE DE HONOR",
                attackStat = 150,
                defenseStat = 80,
                hpText = "1200/1200"
            ),
            onNavigateBack = {},
            onLogout = {}
        )
    }
}
