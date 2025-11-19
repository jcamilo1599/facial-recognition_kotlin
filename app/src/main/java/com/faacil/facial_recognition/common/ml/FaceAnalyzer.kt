package com.faacil.facial_recognition.common.ml

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * [FaceAnalyzer]
 *
 * Implementa [ImageAnalysis.Analyzer] para procesar cada frame proveniente de CameraX usando
 * "ML Kit Face Detection" y emitir un [FaceFrame] hacia el consumidor.
 *
 * Opciones activadas:
 * - Landmarks: ALL (ojos, nariz, boca) para permitir liveness por parpadeo.
 * - Classification: ALL para probabilidades de ojos abiertos.
 * - Tracking: habilitado, optimiza seguimiento entre frames.
 *
 * Notas de rendimiento:
 * - Se usa PERFORMANCE_MODE_FAST para menor latencia; ajustar si se requiere mayor precisión.
 * - Cierra siempre el [ImageProxy] en onComplete para evitar backpressure.
 */
class FaceAnalyzer(
    private val listener: OnFaceFrame,
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close(); return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val input = InputImage.fromMediaImage(mediaImage, rotation)

        detector.process(input)
            .addOnSuccessListener { faces ->
                listener.onFrame(
                    FaceFrame(
                        faces = faces,
                        frameWidth = imageProxy.width,
                        frameHeight = imageProxy.height
                    )
                )
            }
            .addOnFailureListener {
                // Ignorar fallos por fotograma
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
