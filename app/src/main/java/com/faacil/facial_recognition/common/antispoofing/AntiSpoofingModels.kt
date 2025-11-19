package com.faacil.facial_recognition.common.antispoofing

import android.graphics.Rect

/**
 * Resultado agregado de validaciones de anti-spoofing/liveness.
 * Cada flag representa una verificación individual que puede usarse para feedback en tiempo real.
 *
 * - [singleFace]: exactamente un rostro en el frame.
 * - [centered]: el rostro está centrado dentro de una tolerancia.
 * - [brightnessOk]: iluminación suficiente (calculada p.ej. por histograma de luminancia).
 * - [sharpnessOk]: nitidez adecuada (p.ej. varianza del Laplaciano por bordes).
 * - [headPoseOk]: pose dentro de rangos (pitch/roll/yaw aceptables).
 * - [blinkDetected]: parpadeo detectado (señal de liveness).
 * - [glareFree]: sin brillos especulares excesivos.
 * - [depthLikelyReal]: heurística de profundidad consistente con volumen real (no foto plana).
 * - [faceBounds]: posición del rostro en coordenadas del frame para dibujar overlays.
 */
data class AntiSpoofingResult(
    val singleFace: Boolean,
    val centered: Boolean,
    val brightnessOk: Boolean,
    val sharpnessOk: Boolean,
    val headPoseOk: Boolean,
    val blinkDetected: Boolean,
    val glareFree: Boolean,
    val depthLikelyReal: Boolean,
    val faceBounds: Rect? = null,
) {
    /** Verdadero si todas las validaciones pasaron. */
    val allPassed: Boolean = listOf(
        singleFace,
        centered,
        brightnessOk,
        sharpnessOk,
        headPoseOk,
        blinkDetected,
        glareFree,
        depthLikelyReal
    ).all { it }
}

/**
 * Parámetros de umbral y tolerancias para las verificaciones de anti-spoofing.
 */
data class AntiSpoofingConfig(
    val centerTolerance: Float = 0.20f, // 20% del ancho/alto
    val minLaplacianVar: Double = 100.0,
    val minBrightness: Double = 60.0,
    val maxBrightness: Double = 220.0,
    val maxGlarePercentage: Double = 0.06,
    val headPoseMaxDegrees: Float = 15f,
)
