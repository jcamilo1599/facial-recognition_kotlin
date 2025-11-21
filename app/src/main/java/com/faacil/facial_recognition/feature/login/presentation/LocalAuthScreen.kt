package com.faacil.facial_recognition.feature.login.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.faacil.facial_recognition.common.antispoofing.LivenessProcessor
import com.faacil.facial_recognition.common.camera.CameraPreview
import com.faacil.facial_recognition.common.camera.CaptureController
import com.faacil.facial_recognition.common.ml.FaceAnalyzer
import com.faacil.facial_recognition.common.ml.FaceFrame
import com.faacil.facial_recognition.common.ml.FaceNet
import com.faacil.facial_recognition.common.permissions.WithCameraPermission
import com.faacil.facial_recognition.common.storage.LocalEmbeddingStorage
import com.faacil.facial_recognition.common.ui.FaceOverlay
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

@Composable
fun LocalAuthScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Carga asíncrona del modelo para evitar congelar la UI al entrar
    var faceNet by remember { mutableStateOf<FaceNet?>(null) }
    var isLoadingModel by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            faceNet = FaceNet(context)
        }
        isLoadingModel = false
    }

    val storage = remember { LocalEmbeddingStorage(context) }
    val storedEmbedding = remember { storage.getEmbedding() }

    var resultMessage by remember { mutableStateOf<String?>(null) }

    // Si no hay embedding, mostrar error y salir
    if (storedEmbedding == null) {
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text("Error de Autenticación") },
            text = { Text("No se encontró un perfil biométrico local.\n\nPor favor, inicia sesión online primero para descargar tu perfil.") },
            confirmButton = { Button(onClick = onBack) { Text("Entendido") } }
        )
        return
    }

    Scaffold { inner ->
        if (isLoadingModel) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cargando modelo de reconocimiento...")
                }
            }
        } else {
            WithCameraPermission(rationale = "Se requiere cámara para autenticación local") {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)) {
                    val liveness = remember { LivenessProcessor() }
                    var state by remember {
                        mutableStateOf(
                            liveness.onFrame(
                                FaceFrame(
                                    emptyList(),
                                    0,
                                    0
                                )
                            )
                        )
                    }
                    var captureController: CaptureController? by remember { mutableStateOf(null) }
                    var isCapturing by remember { mutableStateOf(false) }
                    val scope = rememberCoroutineScope()
                    var message by remember { mutableStateOf("Mira a la cámara para validar") }

                    fun updatePrompt() {
                        message = when (state.currentStep) {
                            LivenessProcessor.Step.Blink -> "Parpadea"
                            LivenessProcessor.Step.TurnRight, LivenessProcessor.Step.TurnLeft -> "Gira tu cabeza a la derecha/izquierda"
                            LivenessProcessor.Step.Completed -> "Verificando..."
                        }
                    }

                    LaunchedEffect(state.currentStep) { updatePrompt() }

                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        analyzerProvider = { _ ->
                            FaceAnalyzer { frame ->
                                state = liveness.onFrame(frame)
                                if (state.completed && !isCapturing && faceNet != null) {
                                    isCapturing = true
                                }
                            }
                        },
                        onCaptureController = { controller -> captureController = controller }
                    )

                    // Overlay visual igual que en Login/Registro
                    FaceOverlay(
                        prompt = message,
                        progress = state.progress
                    )

                    // Botón volver
                    FloatingActionButton(
                        onClick = onBack,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }

                    // Lógica de captura con delay
                    LaunchedEffect(isCapturing) {
                        if (isCapturing) {
                            kotlinx.coroutines.delay(1000) // Pequeña pausa para estabilizar
                            captureController?.captureBitmap { bitmap ->
                                if (bitmap == null) {
                                    resultMessage = "Error capturando imagen"
                                    isCapturing = false
                                    return@captureBitmap
                                }

                                // Detectar y recortar rostro antes de inferencia
                                val options = FaceDetectorOptions.Builder()
                                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                                    .build()
                                val detector = FaceDetection.getClient(options)
                                val inputImage = InputImage.fromBitmap(bitmap, 0)

                                detector.process(inputImage)
                                    .addOnSuccessListener { faces ->
                                        if (faces.isEmpty()) {
                                            resultMessage =
                                                "No se detectó ningún rostro en la captura."
                                            isCapturing = false
                                            return@addOnSuccessListener
                                        }

                                        // Tomar el primer rostro (el más prominente)
                                        val face = faces[0]
                                        val box = face.boundingBox

                                        // Asegurar que el recorte esté dentro de la imagen
                                        val left = box.left.coerceAtLeast(0)
                                        val top = box.top.coerceAtLeast(0)
                                        val width = box.width().coerceAtMost(bitmap.width - left)
                                        val height = box.height().coerceAtMost(bitmap.height - top)

                                        if (width <= 0 || height <= 0) {
                                            resultMessage = "Error al recortar el rostro."
                                            isCapturing = false
                                            return@addOnSuccessListener
                                        }

                                        val croppedBitmap = android.graphics.Bitmap.createBitmap(
                                            bitmap,
                                            left,
                                            top,
                                            width,
                                            height
                                        )

                                        scope.launch {
                                            try {
                                                // Ejecutar inferencia en background con el rostro recortado
                                                val rawEmbedding =
                                                    faceNet!!.getFaceEmbedding(croppedBitmap)

                                                // Normalización L2
                                                val embedding = l2Normalize(rawEmbedding)
                                                val storedNorm =
                                                    l2Normalize(storedEmbedding.toFloatArray())

                                                // Validar dimensiones
                                                if (embedding.size != storedNorm.size) {
                                                    throw Exception("Dimensiones incompatibles: Local=${embedding.size}, Remoto=${storedNorm.size}")
                                                }

                                                val distance = calculateEuclideanDistance(
                                                    embedding,
                                                    storedNorm
                                                )

                                                // Umbral estricto para evitar falsos positivos
                                                val threshold = 0.8f
                                                val isMatch = distance < threshold

                                                resultMessage = if (isMatch) {
                                                    "¡Identidad Verificada!\n\nDistancia: %.4f".format(
                                                        distance
                                                    )
                                                } else {
                                                    "No coincide con el perfil guardado.\n\nDistancia: %.4f".format(
                                                        distance
                                                    )
                                                }
                                            } catch (e: Exception) {
                                                resultMessage = "Error técnico: ${e.message}"
                                                isCapturing = false
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        resultMessage = "Error detectando rostro: ${e.message}"
                                        isCapturing = false
                                    }
                            }
                        }
                    }

                    resultMessage?.let { msg ->
                        AlertDialog(
                            onDismissRequest = {
                                resultMessage = null
                                onBack()
                            },
                            title = { Text("Resultado") },
                            text = { Text(msg) },
                            confirmButton = {
                                Button(onClick = {
                                    resultMessage = null
                                    onBack()
                                }) { Text("OK") }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun calculateEuclideanDistance(emb1: FloatArray, emb2: FloatArray): Float {
    var sum = 0.0f
    for (i in emb1.indices) {
        val diff = emb1[i] - emb2[i]
        sum += diff * diff
    }
    return sqrt(sum)
}

private fun l2Normalize(embedding: FloatArray): FloatArray {
    val sum = embedding.map { it * it }.sum()
    val magnitude = sqrt(sum)
    return if (magnitude > 0) embedding.map { it / magnitude }.toFloatArray() else embedding
}
