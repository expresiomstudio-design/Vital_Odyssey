package com.moises.vitalodyssey.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.moises.vitalodyssey.R
import com.moises.vitalodyssey.domain.model.BodyType
import com.moises.vitalodyssey.domain.model.PlayerClass
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.*
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
        onFinish = onFinish,
        onError = viewModel::triggerError
    )
}

@Composable
fun OnboardingContent(
    uiState: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onTypeSelected: (BodyType) -> Unit,
    onClassSelected: (PlayerClass) -> Unit,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    onError: (String) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            // Optional: you can show a Snackbar or Toast here if preferred. 
            // The text is also shown below the buttons.
        }
    }

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onFinish()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo de imagen según el paso
            Image(
                painter = painterResource(id = if (currentStep == 1) R.drawable.bg_omboard_body else R.drawable.bg_omboard_class),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Capa de degradado para legibilidad
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                Color.Transparent,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Espaciado superior del 10% (reducido a la mitad)
                Spacer(modifier = Modifier.fillMaxHeight(0.1f))

                if (currentStep == 1) {
                    StepOneContent(
                        name = uiState.name,
                        onNameChange = onNameChange,
                        selectedType = uiState.selectedBodyType,
                        onTypeSelected = onTypeSelected,
                        errorMessage = uiState.errorMessage,
                        onContinue = { 
                            if (uiState.name.isBlank()) {
                                onError("Debes escribir un nombre para tu héroe.")
                            } else if (uiState.selectedBodyType == null) {
                                onError("Debes seleccionar un tipo de cuerpo.")
                            } else {
                                currentStep = 2 
                            }
                        }
                    )
                } else {
                    StepTwoContent(
                        selectedClass = uiState.selectedPlayerClass,
                        bodyType = uiState.selectedBodyType,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        onClassSelected = onClassSelected,
                        onStart = onStart
                    )
                }
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
    errorMessage: String?,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                "FORJA TU AVATAR",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                textAlign = TextAlign.Center
            )
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    "Define tu identidad en esta odisea",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            TextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nombre del Héroe", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorContainerColor = Color.Transparent
                ),
                isError = name.isBlank() && errorMessage?.contains("nombre") == true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "TIPO DE CUERPO",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Personaje Masculino (60% de ancho para mayor tamaño)
            BodyTypeCard(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.6f)
                    .fillMaxHeight(),
                imageResId = R.drawable.char_body_male,
                isSelected = selectedType == BodyType.MALE,
                onClick = { onTypeSelected(BodyType.MALE) }
            )
            // Personaje Femenino (60% de ancho para mayor tamaño)
            BodyTypeCard(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth(0.6f)
                    .fillMaxHeight(),
                imageResId = R.drawable.char_body_female,
                isSelected = selectedType == BodyType.FEMALE,
                onClick = { onTypeSelected(BodyType.FEMALE) }
            )
        }

        Box(modifier = Modifier.padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp).background(MaterialTheme.colorScheme.surface.copy(alpha=0.8f), RoundedCornerShape(4.dp)).padding(4.dp)
                    )
                }
                val isEnabled = selectedType != null && name.isNotBlank()
                Button(
                    onClick = onContinue,
                    // We don't disable the button so they can click and see the error message
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .alpha(if (isEnabled) 1f else 0.8f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.5f)
                    )
                ) {
                    Text(
                        "CONTINUAR", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.ExtraBold, 
                        letterSpacing = 2.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun BodyTypeCard(
    modifier: Modifier,
    imageResId: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = (-30).dp) // Subirlos un 20% aprox desde su posición anterior
                .graphicsLayer {
                    alpha = if (isSelected) 1f else 0.4f
                    scaleX = if (isSelected) 1.25f else 1f
                    scaleY = if (isSelected) 1.25f else 1f
                },
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.BottomCenter
        )
    }
}

@Composable
fun StepTwoContent(
    selectedClass: PlayerClass?,
    bodyType: BodyType?,
    isLoading: Boolean,
    errorMessage: String?,
    onClassSelected: (PlayerClass) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                "ELIGE TU CLASE",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp)) // Más espacio para bajar las tarjetas
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "¿Qué camino seguirás en esta odisea?",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .padding(24.dp)
        ) {
            items(PlayerClass.values()) { pClass ->
                PlayerClassItem(
                    playerClass = pClass,
                    bodyType = bodyType,
                    isSelected = selectedClass == pClass,
                    onClick = { onClassSelected(pClass) }
                )
            }
        }

        Box(modifier = Modifier.padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp).background(MaterialTheme.colorScheme.surface.copy(alpha=0.8f), RoundedCornerShape(4.dp)).padding(4.dp)
                    )
                }
                
                val isEnabled = selectedClass != null && !isLoading
                Button(
                    onClick = {
                        if (selectedClass == null) {
                            // Si quieres mostrar un mensaje local, el onStart también lo valida.
                        }
                        onStart()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .alpha(if (isEnabled) 1f else 0.8f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.5f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                painter = rememberVectorPainter(Lucide.Sword), 
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "EMPEZAR ODISEA",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.Black,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerClassItem(
    playerClass: PlayerClass,
    bodyType: BodyType?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val name = when(playerClass) {
        PlayerClass.WARRIOR -> "GUERRERO"
        PlayerClass.MAGE -> "MAGO"
        PlayerClass.DWARF -> "ENANO"
        PlayerClass.ROGUE -> "PÍCARO"
    }
    
    val imageResId = when (playerClass) {
        PlayerClass.WARRIOR -> if (bodyType == BodyType.FEMALE) R.drawable.char_warrior_female else R.drawable.char_warrior_male
        PlayerClass.MAGE -> if (bodyType == BodyType.FEMALE) R.drawable.char_mage_female else R.drawable.char_mage_male
        PlayerClass.DWARF -> if (bodyType == BodyType.FEMALE) R.drawable.char_dwarf_female else R.drawable.char_dwarf_male
        PlayerClass.ROGUE -> if (bodyType == BodyType.FEMALE) R.drawable.char_rogue_female else R.drawable.char_rogue_male
    }

    val backgroundColor = if (isSelected) 
        Color.White.copy(alpha = 0.95f) 
    else 
        Color.White.copy(alpha = 0.7f)

    Box(
        modifier = Modifier.aspectRatio(0.8f),
        contentAlignment = Alignment.Center
    ) {
        // Efecto Neón Dorado
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize(1.1f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFFD700).copy(alpha = 0.6f), Color.Transparent),
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .then(
                    if (isSelected) Modifier.border(2.dp, Color(0xFFFFD700), RoundedCornerShape(24.dp)) else Modifier
                )
                .background(backgroundColor)
                .clickable { onClick() }
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = null,
                modifier = Modifier.weight(1f).fillMaxWidth().graphicsLayer {
                    scaleX = 1.3f
                    scaleY = 1.3f
                },
                contentScale = ContentScale.Fit,
                alpha = 1f
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                name,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif),
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) Color.Black else Color.Gray,
                letterSpacing = 1.sp
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
            onFinish = {},
            onError = {}
        )
    }
}
