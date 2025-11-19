package com.faacil.facial_recognition.common.ml

import android.annotation.SuppressLint
import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

data class FaceFrame(
    val faces: List<Face>,
    val frameWidth: Int,
    val frameHeight: Int,
)

fun interface OnFaceFrame { fun onFrame(result: FaceFrame) }

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
                // Ignore per-frame failures
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
