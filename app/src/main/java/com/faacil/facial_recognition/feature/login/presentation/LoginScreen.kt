package com.faacil.facial_recognition.feature.login.presentation

/**
 * Pantalla de Login facial.
 *
 * Flujo:
 * 1) Solicita permiso de cámara y abre CameraX.
 * 2) Analiza frames con ML Kit y guía al usuario a completar liveness:
 *    - Parpadeo → Giro a la izquierda → Giro a la derecha.
 * 3) Cuando finaliza, captura una imagen JPEG en memoria.
 * 4) Redimensiona/convierte a JPEG con tamaño razonable y envía como multipart `file` a /login.
 * 5) Navega atrás para cerrar la cámara y muestra en Home el texto literal de la respuesta.
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
                val scope = rememberCoroutineScope()
                val liveness = remember { LivenessProcessor() }
                var state by remember { mutableStateOf(liveness.onFrame(com.faacil.facial_recognition.common.ml.FaceFrame(emptyList(),0,0))) }
                var captureController: CaptureController? by remember { mutableStateOf(null) }
                // Evita reentradas durante la captura
                var isCapturing by remember { mutableStateOf(false) }
                var message by remember { mutableStateOf("Mira a la cámara para iniciar sesión") }
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

                // Controles superpuestos (estado de cámara, prompt y reintento)
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

private fun responseText(resp: retrofit2.Response<ResponseBody>): String {
    return try {
        if (resp.isSuccessful) {
            resp.body()?.string() ?: "(Respuesta vacía)"
        } else {
            val err = try { resp.errorBody()?.string() } catch (_: Exception) { null }
            "HTTP ${resp.code()}: ${err ?: "(sin cuerpo)"}"
        }
    } catch (e: Exception) {
        "(Error leyendo respuesta): ${e.message}"
    }
}

private fun prepareJpegForUpload(
    originalJpeg: ByteArray,
    maxSide: Int = 1024,
    qualityStart: Int = 85,
    maxBytes: Int = 800 * 1024
): ByteArray {
    return try {
        val bitmap = BitmapFactory.decodeByteArray(originalJpeg, 0, originalJpeg.size) ?: return originalJpeg
        val w = bitmap.width
        val h = bitmap.height
        val scale = (maxOf(w, h).toFloat() / maxSide).coerceAtLeast(1f)
        val targetW = (w / scale).toInt().coerceAtLeast(1)
        val targetH = (h / scale).toInt().coerceAtLeast(1)
        val resized: Bitmap = if (scale > 1f) Bitmap.createScaledBitmap(bitmap, targetW, targetH, true) else bitmap

        var quality = qualityStart
        var result: ByteArray
        do {
            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            result = baos.toByteArray()
            baos.close()
            quality -= 10
        } while (result.size > maxBytes && quality >= 60)

        if (resized !== bitmap) resized.recycle()

        result
    } catch (_: Exception) {
        originalJpeg
    }
}
