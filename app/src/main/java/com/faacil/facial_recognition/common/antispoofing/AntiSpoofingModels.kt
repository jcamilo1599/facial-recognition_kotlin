package com.faacil.facial_recognition.common.antispoofing

import android.graphics.Rect

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

data class AntiSpoofingConfig(
    val centerTolerance: Float = 0.20f, // 20% del ancho/alto
    val minLaplacianVar: Double = 100.0,
    val minBrightness: Double = 60.0,
    val maxBrightness: Double = 220.0,
    val maxGlarePercentage: Double = 0.06,
    val headPoseMaxDegrees: Float = 15f,
)
