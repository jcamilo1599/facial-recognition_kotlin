package com.faacil.facial_recognition.feature.login.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.faacil.facial_recognition.common.antispoofing.LivenessProcessor
import com.faacil.facial_recognition.common.camera.CameraPreview
import com.faacil.facial_recognition.common.camera.CaptureController
import com.faacil.facial_recognition.common.ml.FaceAnalyzer
import com.faacil.facial_recognition.common.permissions.WithCameraPermission
import com.faacil.facial_recognition.common.ui.FaceOverlay

/**
 * Pantalla de autenticación facial.
 *
 * Flujo:
 * 1) Solicita permiso de cámara y abre CameraX.
 * 2) Analiza frames con ML Kit y guía al usuario a completar liveness:
 *    - Parpadeo → Giro a la izquierda → Giro a la derecha.
 * 3) Cuando finaliza, captura una imagen JPEG en memoria.
 * 4) Redimensiona/convierte a JPEG con tamaño razonable y envía como multipart `file` a /login.
 * 5) Navega atrás para cerrar la cámara y muestra en el inicio el texto de cargando y luego la
 *    respuesta del backend.
 */
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onCaptured: (ByteArray) -> Unit,
) {
    Scaffold { inner ->
        WithCameraPermission(
            rationale = "Se requiere acceso a la cámara para validar tu identidad"
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
            ) {
                val liveness = remember { LivenessProcessor() }
                var state by remember {
                    mutableStateOf(
                        liveness.onFrame(
                            com.faacil.facial_recognition.common.ml.FaceFrame(
                                emptyList(),
                                0,
                                0
                            )
                        )
                    )
                }
                var captureController: CaptureController? by remember { mutableStateOf(null) }

                // Evita reentradas durante la captura
                var isCapturing by remember { mutableStateOf(false) }
                var message by remember { mutableStateOf("Mira a la cámara para iniciar sesión") }
                var cameraReady by remember { mutableStateOf(false) }
                var cameraError by remember { mutableStateOf<String?>(null) }

                fun updatePrompt() {
                    message = when (state.currentStep) {
                        LivenessProcessor.Step.Blink -> "Parpadea"
                        LivenessProcessor.Step.TurnRight, LivenessProcessor.Step.TurnLeft -> "Gira tu cabeza a la derecha/izquierda"
                        LivenessProcessor.Step.Completed -> "Capturando foto..."
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

                                // Capturar y devolver bytes a la Activity para subir y cerrar la
                                // cámara inmediatamente
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

                // Controles superpuestos (estado de cámara y reintento)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                ) {
                    if (!cameraReady && cameraError == null) {
                        Text("Iniciando cámara...", style = MaterialTheme.typography.bodyMedium)
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
                }

                // Overlay que dibuja el marco guía y la barra de progreso
                FaceOverlay(
                    modifier = Modifier.fillMaxSize(),
                    prompt = message,
                    progress = state.progress
                )

                FloatingActionButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp, top = 16.dp)
                        .size(58.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
