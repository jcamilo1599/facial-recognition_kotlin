package com.faacil.facial_recognition.common.ml

import com.google.mlkit.vision.face.Face

/**
 * Contenedor con los datos mínimos de un frame analizado por ML Kit.
 * - [faces]: lista de rostros detectados en el frame actual.
 * - [frameWidth]/[frameHeight]: dimensiones del frame entregado por CameraX.
 */
data class FaceFrame(
    val faces: List<Face>,
    val frameWidth: Int,
    val frameHeight: Int,
)