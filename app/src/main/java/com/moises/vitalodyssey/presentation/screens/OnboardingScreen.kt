package com.moises.vitalodyssey.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moises.vitalodyssey.domain.model.BodyType
import com.moises.vitalodyssey.domain.model.PlayerClass
import androidx.compose.ui.tooling.preview.Preview
import com.moises.vitalodyssey.presentation.viewmodels.OnboardingUiState
import com.moises.vitalodyssey.presentation.viewmodels.OnboardingViewModel
import com.moises.vitalodyssey.ui.theme.VitalOdysseyTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
    onFinish: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    OnboardingContent(
        uiState = uiState,
        onNameChange = viewModel::onNameChange,
        onTypeSelected = viewModel::selectBodyType,
        onClassSelected = viewModel::selectPlayerClass,
        onStart = viewModel::completeOnboarding,
        onFinish = onFinish
    )
}

@Composable
fun OnboardingContent(
    uiState: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onTypeSelected: (BodyType) -> Unit,
    onClassSelected: (PlayerClass) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onFinish()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentStep == 1) {
                StepOneContent(
                    name = uiState.name,
                    onNameChange = onNameChange,
                    selectedType = uiState.selectedBodyType,
                    onTypeSelected = onTypeSelected,
                    onContinue = { currentStep = 2 }
                )
            } else {
                StepTwoContent(
                    selectedClass = uiState.selectedPlayerClass,
                    isLoading = uiState.isLoading,
                    onClassSelected = onClassSelected,
                    onStart = onStart
                )
            }
        }
    }
}

@Composable
fun StepOneContent(
    name: String,
    onNameChange: (String) -> Unit,
    selectedType: BodyType?,
    onTypeSelected: (BodyType) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "FORJA TU AVATAR",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Define tu identidad en esta odisea",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nombre del Héroe") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BodyTypeCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Male,
                label = "HOMBRE",
                isSelected = selectedType == BodyType.MALE,
                onClick = { onTypeSelected(BodyType.MALE) }
            )
            BodyTypeCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Female,
                label = "MUJER",
                isSelected = selectedType == BodyType.FEMALE,
                onClick = { onTypeSelected(BodyType.FEMALE) }
            )
        }

        Button(
            onClick = onContinue,
            enabled = selectedType != null && name.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text("CONTINUAR", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun BodyTypeCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleLarge,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StepTwoContent(
    selectedClass: PlayerClass?,
    isLoading: Boolean,
    onClassSelected: (PlayerClass) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "ELIGE TU CLASE",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "¿Qué camino seguirás en esta odisea?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(1),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 24.dp)
        ) {
            items(PlayerClass.values()) { pClass ->
                PlayerClassItem(
                    playerClass = pClass,
                    isSelected = selectedClass == pClass,
                    onClick = { onClassSelected(pClass) }
                )
            }
        }

        Button(
            onClick = onStart,
            enabled = selectedClass != null && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            contentPadding = PaddingValues()
        ) {
            val isEnabled = selectedClass != null && !isLoading
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isEnabled) {
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primaryContainer, Color(0xFFFFA000))
                            )
                        } else {
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        "EMPEZAR ODISEA",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerClassItem(
    playerClass: PlayerClass,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val description = when(playerClass) {
        PlayerClass.WARRIOR -> "Maestro del combate físico y la resistencia. Ideal para quienes no temen al sudor."
        PlayerClass.MAGE -> "Canaliza el poder de la mente. Perfecto para los estudiosos y enfocados."
        PlayerClass.DWARF -> "Resistente y tenaz. La disciplina es su mayor virtud."
        PlayerClass.ROGUE -> "Agilidad y precisión. Encuentra el camino más eficiente hacia sus metas."
    }

    val name = when(playerClass) {
        PlayerClass.WARRIOR -> "GUERRERO"
        PlayerClass.MAGE -> "MAGO"
        PlayerClass.DWARF -> "ENANO"
        PlayerClass.ROGUE -> "PÍCARO"
    }

    val borderColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    VitalOdysseyTheme {
        OnboardingContent(
            uiState = OnboardingUiState(
                name = "Moises",
                selectedBodyType = BodyType.MALE,
                selectedPlayerClass = PlayerClass.WARRIOR
            ),
            onNameChange = {},
            onTypeSelected = {},
            onClassSelected = {},
            onStart = {},
            onFinish = {}
        )
    }
}
