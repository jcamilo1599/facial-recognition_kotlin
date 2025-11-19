package com.faacil.facial_recognition.common.ml

/**
 * Interfaz para notificar cuando se ha detectado un frame.
 */
fun interface OnFaceFrame { fun onFrame(result: FaceFrame) }