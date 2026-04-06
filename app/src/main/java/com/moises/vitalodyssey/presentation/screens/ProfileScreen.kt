package com.moises.vitalodyssey.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.composables.icons.lucide.*
import com.moises.vitalodyssey.presentation.viewmodels.ProfileUiState
import com.moises.vitalodyssey.presentation.viewmodels.ProfileViewModel
import com.moises.vitalodyssey.ui.theme.VitalOdysseyTheme
import org.koin.androidx.compose.koinViewModel

enum class ProfileSection {
    MAIN_MENU, ACCOUNT, SETTINGS
}

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = koinViewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentSection by remember { mutableStateOf(ProfileSection.MAIN_MENU) }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) {
            onNavigateToLogin()
        }
    }

    BackHandler(enabled = currentSection != ProfileSection.MAIN_MENU) {
        currentSection = ProfileSection.MAIN_MENU
    }

    ProfileScreenContent(
        uiState = uiState,
        currentSection = currentSection,
        onSectionChange = { currentSection = it },
        onNavigateBack = {
            if (currentSection == ProfileSection.MAIN_MENU) {
                onNavigateBack()
            } else {
                currentSection = ProfileSection.MAIN_MENU
            }
        },
        onLogout = { viewModel.logout() },
        onDeleteAccount = { viewModel.deleteAccount() },
        onUpdateName = { viewModel.updateName(it) },
        onUpdateStartOfWeek = { viewModel.updateStartOfWeek(it) },
        onUpdateCutoffTime = { viewModel.updateCutoffTime(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenContent(
    uiState: ProfileUiState,
    currentSection: ProfileSection,
    onSectionChange: (ProfileSection) -> Unit,
    onNavigateBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onUpdateName: (String) -> Unit = {},
    onUpdateStartOfWeek: (String) -> Unit = {},
    onUpdateCutoffTime: (String) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentSection) {
                            ProfileSection.MAIN_MENU -> "Perfil del Héroe"
                            ProfileSection.ACCOUNT -> "Gestión de Cuenta"
                            ProfileSection.SETTINGS -> "Ajustes de Odisea"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentSection,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "ProfileSectionTransition"
            ) { section ->
                when (section) {
                    ProfileSection.MAIN_MENU -> MainMenuContent(uiState, onSectionChange)
                    ProfileSection.ACCOUNT -> AccountSectionContent(uiState, onLogout, onDeleteAccount, onUpdateName)
                    ProfileSection.SETTINGS -> SettingsSectionContent(uiState, onUpdateStartOfWeek, onUpdateCutoffTime)
                }
            }
        }
    }

    if (uiState.showDeleteSuccess) {
        DeleteSuccessDialog(uiState.deleteProgress)
    }
}

@Composable
fun MainMenuContent(
    uiState: ProfileUiState,
    onSectionChange: (ProfileSection) -> Unit
) {
    val xpProgress = if (uiState.xpForNextLevel > 0) uiState.currentXp.toFloat() / uiState.xpForNextLevel else 0f
    val remainingXp = uiState.xpForNextLevel - uiState.currentXp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Header: Avatar, Nombre y Profesión
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(160.dp)
                    .shadow(10.dp, CircleShape),
                shape = CircleShape,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                color = MaterialTheme.colorScheme.surface
            ) {
                AsyncImage(
                    model = "https://picsum.photos/seed/${uiState.playerName}/400",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.clip(CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = uiState.playerName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = uiState.playerClass.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primaryContainer
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Barra de Experiencia
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "NIVEL ${uiState.level}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${(xpProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { xpProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = Color(0xFF4CAF50),
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "Faltan $remainingXp XP para el nivel ${uiState.level + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Atributos de Combate (Fila de 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileAttributeCard(
                modifier = Modifier.weight(1f),
                label = "ATAQUE",
                value = uiState.attackStat.toString(),
                iconPainter = rememberVectorPainter(Lucide.Sword),
                color = MaterialTheme.colorScheme.primary
            )
            ProfileAttributeCard(
                modifier = Modifier.weight(1f),
                label = "DEFENSA",
                value = uiState.defenseStat.toString(),
                iconPainter = rememberVectorPainter(Lucide.Shield),
                color = MaterialTheme.colorScheme.primary
            )
            ProfileAttributeCard(
                modifier = Modifier.weight(1f),
                label = "VIDA",
                value = "${uiState.currentHp}/${uiState.maxHp}",
                iconPainter = rememberVectorPainter(Lucide.Heart),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Registros de Odisea
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "REGISTROS DE ODISEA",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                RecordItem("Racha de Presencia", "${uiState.presenceStreak} Días", MaterialTheme.colorScheme.primary)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                RecordItem("Mayor Racha Histórica", "${uiState.highestStreak} Días", MaterialTheme.colorScheme.primary)
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                RecordItem("Jefes Derrotados", uiState.bossesDefeatedCount.toString(), MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Celdas de Navegación Estilo iOS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            ProfileMenuCell(
                iconPainter = rememberVectorPainter(Lucide.User),
                title = "Gestión de Cuenta",
                onClick = { onSectionChange(ProfileSection.ACCOUNT) }
            )
            HorizontalDivider(modifier = Modifier.padding(start = 56.dp), thickness = 0.5.dp)
            ProfileMenuCell(
                iconPainter = rememberVectorPainter(Lucide.Settings),
                title = "Ajustes de Odisea",
                onClick = { onSectionChange(ProfileSection.SETTINGS) }
            )
        }
        
        Spacer(modifier = Modifier.height(18.dp))
    }
}

@Composable
fun ProfileMenuCell(
    iconPainter: Painter,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun AccountSectionContent(
    uiState: ProfileUiState,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onUpdateName: (String) -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var nameText by remember(uiState.playerName) { mutableStateOf(uiState.playerName) }
    val isGoogleUser = uiState.loginProvider == "google.com"
    val hasNameChanged = nameText != uiState.playerName && nameText.isNotBlank()

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar Sesión") },
            text = { Text("¿Estás seguro de que quieres salir de la Odisea?") },
            confirmButton = {
                TextButton(onClick = onLogout) { Text("CERRAR SESIÓN") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar cuenta?") },
            text = { Text("Esta acción es irreversible y borrará todos tus datos en la nube conforme a la RGPD.") },
            confirmButton = {
                TextButton(
                    onClick = onDeleteAccount, 
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("BORRAR PERMANENTEMENTE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("CANCELAR") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "DATOS DEL PERFIL",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = nameText,
            onValueChange = { nameText = it },
            label = { Text("Nombre del Héroe") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (hasNameChanged) {
                    TextButton(onClick = { onUpdateName(nameText) }) {
                        Text("GUARDAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.playerEmail,
            onValueChange = {},
            label = { Text("Correo Electrónico") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = !isGoogleUser,
            trailingIcon = {
                if (isGoogleUser) {
                    Text("G ", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }
        )

        if (!isGoogleUser) {
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { /* Abrir diálogo cambio email */ }) {
                    Text("Cambiar Email")
                }
                TextButton(onClick = { /* Abrir diálogo cambio pass */ }) {
                    Text("Cambiar Contraseña")
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "GESTIÓN DE CUENTA",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("Cerrar Sesión", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(rememberVectorPainter(Lucide.LogOut), null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { showLogoutDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                ListItem(
                    headlineContent = { Text("Eliminar Cuenta y Datos", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(rememberVectorPainter(Lucide.Trash2), null, tint = MaterialTheme.colorScheme.error) },
                    supportingContent = { Text("Borrado definitivo conforme a RGPD", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.clickable { showDeleteDialog = true },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSectionContent(
    uiState: ProfileUiState,
    onUpdateStartOfWeek: (String) -> Unit = {},
    onUpdateCutoffTime: (String) -> Unit = {}
) {
    val daysMap = remember {
        linkedMapOf(
            "MONDAY" to "Lunes",
            "TUESDAY" to "Martes",
            "WEDNESDAY" to "Miércoles",
            "THURSDAY" to "Jueves",
            "FRIDAY" to "Viernes",
            "SATURDAY" to "Sábado",
            "SUNDAY" to "Domingo"
        )
    }
    
    var showDaysMenu by remember { mutableStateOf(false) }
    val isExtendedDay = uiState.cutoffTime == "03:00"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "PREFERENCIAS DE LA ODISEA",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Inicio de la semana",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Determina cuándo se reinician tus objetivos semanales.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Box {
                    OutlinedCard(
                        onClick = { showDaysMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = daysMap[uiState.startOfWeek] ?: "Lunes",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showDaysMenu,
                        onDismissRequest = { showDaysMenu = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        daysMap.forEach { (key, value) ->
                            DropdownMenuItem(
                                text = { Text(value) },
                                onClick = {
                                    onUpdateStartOfWeek(key)
                                    showDaysMenu = false
                                },
                                trailingIcon = {
                                    if (uiState.startOfWeek == key) {
                                        Icon(rememberVectorPainter(Lucide.Zap), null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Modo Final de Día Postergado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = isExtendedDay,
                        onCheckedChange = { extended ->
                            onUpdateCutoffTime(if (extended) "03:00" else "00:00")
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Si se activa, el día actual contará hasta las 3:00 AM del día siguiente. Ideal para héroes nocturnos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteSuccessDialog(progress: Float) {
    BasicAlertDialog(onDismissRequest = { }) {
        Surface(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(rememberVectorPainter(Lucide.Trash2), null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("CUENTA ELIMINADA", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tus datos han sido borrados. Saliendo...", style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                LinearProgressIndicator(progress = { 1f - progress }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ProfileAttributeCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    iconPainter: Painter,
    color: Color
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(iconPainter, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
fun RecordItem(label: String, value: String, highlightColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = highlightColor)
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    VitalOdysseyTheme {
        ProfileScreenContent(
            uiState = ProfileUiState(
                playerName = "Moisés Sánchez",
                playerEmail = "moises@example.com",
                playerClass = "MAGO",
                attackStat = 150,
                defenseStat = 80,
                currentHp = 1200,
                maxHp = 1200,
                highestStreak = 15,
                bossesDefeatedCount = 5
            ),
            currentSection = ProfileSection.MAIN_MENU,
            onSectionChange = {},
            onNavigateBack = {},
            onLogout = {},
            onDeleteAccount = {},
            onUpdateName = {},
            onUpdateStartOfWeek = {},
            onUpdateCutoffTime = {}
        )
    }
}
