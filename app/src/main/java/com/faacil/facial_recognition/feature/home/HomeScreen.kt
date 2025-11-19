package com.faacil.facial_recognition.feature.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pantalla de inicio con Material 3
 *
 * Acciones principales:
 * - Registrar rostro: navega al flujo de registro con cámara
 * - Login con rostro: navega al flujo de autenticación con cámara
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onRegister: () -> Unit,
    onLogin: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "background_animation")

    // Animación del fondo
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "offset_animation"
    )

    // Colores del gradiente animado
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Scaffold { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            // Fondo animado con gradiente
            AnimatedBackground(
                animatedOffset = animatedOffset,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                tertiaryColor = tertiaryColor
            )

            Box(modifier = Modifier.padding(16.dp)) {
                // Botones de acción
                ActionButtonsSection(
                    onRegister = onRegister,
                    onLogin = onLogin
                )
            }
        }
    }
}

@Composable
private fun AnimatedBackground(
    animatedOffset: Float,
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .blur(150.dp)
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = size.maxDimension / 3

        // Círculos animados en el fondo
        for (i in 0..5) {
            val angle = (animatedOffset + i * 60) * Math.PI / 180
            val offsetX = centerX + cos(angle).toFloat() * radius * 0.6f
            val offsetY = centerY + sin(angle).toFloat() * radius * 0.6f

            val color = when (i % 3) {
                0 -> primaryColor.copy(alpha = 0.15f)
                1 -> secondaryColor.copy(alpha = 0.12f)
                else -> tertiaryColor.copy(alpha = 0.10f)
            }

            drawCircle(
                color = color,
                radius = radius * (0.5f + i * 0.1f),
                center = Offset(offsetX, offsetY)
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onRegister: () -> Unit,
    onLogin: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Botón de registro
        ExpressiveActionButton(
            onClick = onRegister,
            icon = Icons.Default.Person,
            text = "Registrar Rostro",
            description = "Crear nuevo perfil biométrico",
            isPrimary = true
        )

        // Botón de login
        ExpressiveActionButton(
            onClick = onLogin,
            icon = Icons.Default.AccountCircle,
            text = "Iniciar Sesión",
            description = "Autenticar con rostro registrado",
            isPrimary = false
        )
    }
}

@Composable
private fun ExpressiveActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    description: String,
    isPrimary: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "button_shimmer")

    // Colores del tema
    val indicatorColor = if (isPrimary)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.secondary

    // Efecto sutil de brillo en botones
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_scale"
    )

    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 4f else 12f,
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "button_elevation"
    )

    val iconRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_rotation"
    )

    Card(
        onClick = {
            onClick()
        },
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
            else
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        // Overlay de brillo sutil
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Contenedor del icono con rotación suave
                Surface(
                    shape = CircleShape,
                    color = if (isPrimary)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            rotationZ = iconRotation * 0.5f
                        }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isPrimary)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Textos con énfasis mejorado
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Normal
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                // Indicador visual de acción
                Canvas(
                    modifier = Modifier
                        .size(8.dp)
                        .graphicsLayer {
                            alpha = shimmerAlpha * 0.6f
                        }
                ) {
                    drawCircle(color = indicatorColor)
                }
            }
        }
    }
}
