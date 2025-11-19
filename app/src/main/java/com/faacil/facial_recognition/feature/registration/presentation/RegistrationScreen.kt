package com.faacil.facial_recognition.feature.registration.presentation

/**
 * Pantalla de Registro de rostro.
 *
 * Flujo:
 * 1) Pide permiso de cámara y abre previsualización con CameraX.
 * 2) Procesa frames con ML Kit (FaceAnalyzer) y ejecuta liveness (LivenessProcessor):
 *    - Parpadeo → Giro a la izquierda → Giro a la derecha.
 * 3) Cuando se completan los gestos, captura una foto JPEG en memoria.
 * 4) Normaliza la imagen (redimensiona/comprime) y la envía a /register como multipart `file`.
 * 5) Cierra la cámara (navega atrás) y muestra en Home un diálogo con la respuesta literal del backend.
 *
 * Seguridad: la imagen solo se mantiene en memoria durante el envío y no se persiste en disco.
 */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.faacil.facial_recognition.common.antispoofing.LivenessProcessor
import com.faacil.facial_recognition.common.camera.CaptureController
import com.faacil.facial_recognition.common.camera.CameraPreview
import com.faacil.facial_recognition.common.ml.FaceAnalyzer
import com.faacil.facial_recognition.common.permissions.WithCameraPermission
import com.faacil.facial_recognition.common.ui.FaceOverlay
import com.faacil.facial_recognition.common.network.ApiClient
import com.faacil.facial_recognition.common.network.FaceApi
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

@Composable
fun RegistrationScreen(
    onBack: () -> Unit,
    onCaptured: (ByteArray) -> Unit,
) {
    Scaffold { inner ->
        WithCameraPermission(
            rationale = "Se requiere acceso a la cámara para registrar tu rostro"
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
            ) {
                val scope = rememberCoroutineScope()
                val liveness = remember { LivenessProcessor() }
                var state by remember { mutableStateOf(liveness.onFrame(com.faacil.facial_recognition.common.ml.FaceFrame(emptyList(),0,0))) }
                var captureController: CaptureController? by remember { mutableStateOf(null) }
                // Bandera para evitar capturas/subidas duplicadas al completar liveness
                var isCapturing by remember { mutableStateOf(false) }
                var message by remember { mutableStateOf("Alinea tu rostro dentro del marco") }
                var cameraReady by remember { mutableStateOf(false) }
                var cameraError by remember { mutableStateOf<String?>(null) }

                fun updatePrompt() {
                    message = when (state.currentStep) {
                        LivenessProcessor.Step.Blink -> "Parpadea"
                        LivenessProcessor.Step.TurnLeft -> "Gira tu cabeza a la izquierda"
                        LivenessProcessor.Step.TurnRight -> "Gira tu cabeza a la derecha"
                        LivenessProcessor.Step.Completed -> "Mantén la posición… capturando"
                    }
                }

                LaunchedEffect(state.currentStep) { updatePrompt() }

                // Vista de cámara + analizador de rostros en tiempo real
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    analyzerProvider = { _ ->
                        FaceAnalyzer { frame ->
                            state = liveness.onFrame(frame)
                            if (state.completed && !isCapturing) {
                                isCapturing = true
                                // Capturar y devolver bytes a la Activity para subir y cerrar la cámara inmediatamente
                                captureController?.captureJpeg { bytes ->
                                    onCaptured(bytes ?: ByteArray(0))
                                }
                            }
                        }
                    },
                    onCaptureController = { captureController = it },
                    onCameraReady = {
                        cameraReady = true
                        cameraError = null
                    },
                    onCameraError = {
                        cameraReady = false
                        cameraError = it.message ?: "Error desconocido al iniciar la cámara"
                    }
                )

                // Controles superpuestos (estado de cámara, prompt y retry)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    if (!cameraReady && cameraError == null) {
                        Text("Inicializando cámara…", style = MaterialTheme.typography.bodyMedium)
                    }
                    cameraError?.let { err ->
                        Text(
                            text = "No se pudo abrir la cámara: $err",
                            color = androidx.compose.ui.graphics.Color.Red,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { cameraError = null; captureController?.rebind() }) {
                            Text("Reintentar")
                        }
                    }
                    Text(message)
                }

                // Overlay que dibuja el marco guía, el prompt y la barra de progreso
                FaceOverlay(
                    modifier = Modifier.fillMaxSize(),
                    prompt = message,
                    progress = state.progress
                )

                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) { Text("Volver") }
            }
        }
    }
}
