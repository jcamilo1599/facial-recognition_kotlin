package com.faacil.facial_recognition.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Material 3 Expressive shapes - más redondeadas y dinámicas
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),  // 4dp -> 8dp más expresivo
    small = RoundedCornerShape(12.dp),      // 8dp -> 12dp más redondeado
    medium = RoundedCornerShape(20.dp),     // 12dp -> 20dp más expresivo
    large = RoundedCornerShape(28.dp),      // 16dp -> 28dp más dinámico
    extraLarge = RoundedCornerShape(32.dp)  // 28dp -> 32dp máxima expresividad
)

// Formas personalizadas para componentes específicos
object ExpressiveShapes {
    // Para botones principales más expresivos
    val primaryButton = RoundedCornerShape(24.dp)

    // Para tarjetas con más personalidad
    val expressiveCard = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 8.dp,
        bottomStart = 8.dp,
        bottomEnd = 24.dp
    )

    // Para elementos de detección facial (circular suave)
    val faceOverlay = RoundedCornerShape(16.dp)

    // Para diálogos más amigables
    val dialog = RoundedCornerShape(32.dp)

    // Para campos de entrada más modernos
    val inputField = RoundedCornerShape(16.dp)
}
