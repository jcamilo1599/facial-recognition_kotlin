package com.faacil.facial_recognition.common.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FaceOverlay(
    modifier: Modifier = Modifier,
    prompt: String,
    progress: Float,
    guideSizeRatio: Float = 0.7f, // porcentaje del ancho
    guideCornerRadius: Dp = 24.dp,
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val guideWidth = width * guideSizeRatio
            val guideHeight = guideWidth * 1.2f
            val left = (width - guideWidth) / 2f
            val top = (height - guideHeight) / 2f
            val rect = Rect(left, top, left + guideWidth, top + guideHeight)

            // Sombreado fuera del marco
            val outer = Path().apply { addRect(Rect(0f, 0f, width, height)) }
            val inner = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(rect, CornerRadius(guideCornerRadius.toPx()))) }
            val mask = Path.combine(PathOperation.Difference, outer, inner)
            drawPath(mask, color = Color(0x99000000), style = Fill)

            // Marco
            drawRoundRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(rect.left, rect.top),
                size = rect.size,
                cornerRadius = CornerRadius(guideCornerRadius.toPx()),
                style = Stroke(width = 6f)
            )
        }

        Text(
            text = prompt,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            trackColor = Color.White.copy(alpha = 0.3f),
            color = Color(0xFF00E5FF)
        )
    }
}
