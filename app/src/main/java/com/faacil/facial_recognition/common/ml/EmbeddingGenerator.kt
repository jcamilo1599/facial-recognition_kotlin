package com.faacil.facial_recognition.common.ml

import android.graphics.Bitmap

/**
 * Generador de embeddings faciales usando TensorFlow Lite.
 * Seguridad: no persistir bitmaps; el llamador debe limpiar referencias tras uso.
 */
interface EmbeddingGenerator {
    /**
     * Genera un embedding normalizado (128 o 256 dimensiones) a partir de un rostro recortado y alineado.
     * Retorna null si no es posible por entrada inválida o modelo no cargado.
     */
    suspend fun generateEmbedding(faceBitmap: Bitmap): FloatArray?
}
