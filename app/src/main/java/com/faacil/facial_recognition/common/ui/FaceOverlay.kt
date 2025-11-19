package com.faacil.facial_recognition.common.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Composable superpuesto a la vista de cámara que dibuja:
 * - Una máscara oscurecida fuera de un marco guía redondeado para posicionar el rostro.
 * - Un texto [prompt] dinámico con la instrucción actual (parpadea, gira, etc.).
 * - Una barra de progreso ligada al avance del liveness [progress] (0..1).
 *
 * Parámetros de personalización:
 * - [guideSizeRatio]: porcentaje del ancho disponible que ocupa el marco guía.
 * - [guideCornerRadius]: radio de las esquinas del marco.
 */
@Composable
fun FaceOverlay(
    modifier: Modifier = Modifier,
    prompt: String,
    progress: Float,
    guideSizeRatio: Float = 0.7f, // porcentaje del ancho
    guideCornerRadius: Dp = 24.dp,
) {
    // Obtiene el color primario de la aplicación
    val primaryColor = MaterialTheme.colorScheme.primary

    // Se obtienen los tamaños del contenedor para poder posicionar textos relativamente al
    // rectángulo guía
    var containerWidth by remember { mutableStateOf(0) }
    var containerHeight by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    // Cálculo del rectángulo guía (en px) consistente con el Canvas
    fun guideRectPx(): Rect {
        val width = containerWidth.toFloat()
        val height = containerHeight.toFloat()
        val guideWidth = width * guideSizeRatio
        val guideHeight = guideWidth * 1.2f
        val left = (width - guideWidth) / 2f
        val top = (height - guideHeight) / 2f

        return Rect(left, top, left + guideWidth, top + guideHeight)
    }

    // Calculamos el porcentaje de progreso
    val percent = (progress.coerceIn(0f, 1f) * 100).roundToInt()

    Box(
        modifier = modifier.onSizeChanged { size ->
            containerWidth = size.width
            containerHeight = size.height
        }
    ) {
        // Dibujo de máscara y marco guía
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val rect = guideRectPx()

            // Sombreado fuera del marco
            val outer = Path().apply { addRect(Rect(0f, 0f, width, height)) }
            val inner = Path().apply {
                addRoundRect(
                    androidx.compose.ui.geometry.RoundRect(
                        rect,
                        CornerRadius(guideCornerRadius.toPx())
                    )
                )
            }

            val mask = Path.combine(PathOperation.Difference, outer, inner)
            drawPath(mask, color = Color(0x99000000), style = Fill)

            // Marco
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(rect.left, rect.top),
                size = rect.size,
                cornerRadius = CornerRadius(guideCornerRadius.toPx()),
                style = Stroke(width = 6f)
            )
        }

        // Texto de prompt centrado sobre el recuadro (dentro, en la parte superior)
        if (containerWidth > 0 && containerHeight > 0) {
            val rect = guideRectPx()
            val xDp = with(density) { rect.left.toDp() }
            val yPromptDp = with(density) { (rect.top + 12f).toDp() }
            val rectWidthDp = with(density) { rect.width.toDp() }

            // Fondo sutil para legibilidad del texto
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(0.dp)
                    .then(
                        Modifier
                            .padding(start = xDp, top = yPromptDp)
                            .width(rectWidthDp)
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0x66000000), shape = MaterialTheme.shapes.small)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                }
            }
        }

        // Indicador de progreso
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        ) {
            // Etiqueta de porcentaje encima de la barra
            Text(
                text = "Progreso ${percent}%",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 22.dp)
                    .padding(start = 16.dp)
            )

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(10.dp),
                trackColor = Color.White.copy(alpha = 0.25f),
                color = Color(0xFF00E5FF)
            )
        }
    }
}
