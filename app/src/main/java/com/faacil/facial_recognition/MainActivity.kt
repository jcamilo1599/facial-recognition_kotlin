package com.faacil.facial_recognition

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.faacil.facial_recognition.common.network.ApiClient
import com.faacil.facial_recognition.common.network.FaceApi
import com.faacil.facial_recognition.feature.home.HomeScreen
import com.faacil.facial_recognition.feature.login.presentation.LoginScreen
import com.faacil.facial_recognition.feature.registration.presentation.RegistrationScreen
import com.faacil.facial_recognition.ui.theme.AppTheme
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import java.io.ByteArrayOutputStream

/**
 * Punto de entrada y lógica principal de la app.
 */
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController: NavHostController = rememberNavController()
                    val scope = rememberCoroutineScope()

                    // Estado local para mostrar la alerta
                    var resultMessage by remember { mutableStateOf<String?>(null) }

                    // Loading global mientras se sube la imagen (después de cerrar la cámara)
                    var isUploading by remember { mutableStateOf(false) }

                    NavHost(navController = navController, startDestination = Routes.HOME) {
                        composable(Routes.HOME) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                HomeScreen(
                                    onRegister = { navController.navigate(Routes.REGISTRATION) },
                                    onLogin = { navController.navigate(Routes.LOGIN) }
                                )

                                // Muestra alerta con la respuesta del servicio si existe
                                resultMessage?.let { msg ->
                                    AlertDialog(
                                        onDismissRequest = { resultMessage = null },
                                        title = { Text(text = "Respuesta del servicio") },
                                        text = { Text(text = msg) },
                                        confirmButton = {
                                            Button(onClick = {
                                                resultMessage = null
                                            }) { Text("OK") }
                                        }
                                    )
                                }

                                // Diálogo de cargando mientras se procesa el upload
                                if (isUploading) {
                                    AlertDialog(
                                        onDismissRequest = { /* bloqueado mientras se sube */ },
                                        confirmButton = {},
                                        title = { Text("Procesando...") },
                                        text = {
                                            Column {
                                                LoadingIndicator(
                                                    modifier = Modifier
                                                        .width(64.dp)
                                                        .height(64.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Estamos analizando tu foto, esto puede tardar unos segundos...")
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        // Pantalla de Registro: abre cámara, ejecuta liveness y sube imagen
                        composable(Routes.REGISTRATION) {
                            RegistrationScreen(
                                onBack = { navController.popBackStack() },
                                onCaptured = { jpegBytes ->
                                    // Cerrar cámara inmediatamente
                                    navController.popBackStack()

                                    // Lanzar upload en Home con loading
                                    isUploading = true

                                    scope.launch {
                                        val api = ApiClient.retrofit.create(FaceApi::class.java)
                                        val result = try {
                                            if (jpegBytes.isEmpty()) {
                                                throw IllegalStateException("No se pudo capturar la imagen")
                                            }

                                            val prepared = prepareJpegForUpload(jpegBytes)
                                            var body: RequestBody = RequestBody.create(
                                                "image/jpeg".toMediaType(),
                                                prepared
                                            )
                                            var part = MultipartBody.Part.createFormData(
                                                "file",
                                                "face.jpg",
                                                body
                                            )
                                            var resp = api.register(part)
                                            var text = responseText(resp)

                                            if (!resp.isSuccessful && resp.code() == 503) {
                                                val smaller = prepareJpegForUpload(
                                                    jpegBytes,
                                                    maxSide = 640,
                                                    qualityStart = 75
                                                )

                                                body = RequestBody.create(
                                                    "image/jpeg".toMediaType(),
                                                    smaller
                                                )

                                                part = MultipartBody.Part.createFormData(
                                                    "file",
                                                    "face.jpg",
                                                    body
                                                )

                                                resp = api.register(part)
                                                text = responseText(resp)
                                            }

                                            text
                                        } catch (e: Exception) {
                                            "Error al registrar: ${e.message}"
                                        }

                                        isUploading = false
                                        resultMessage = result
                                    }
                                }
                            )
                        }
                        // Pantalla de Login facial: mismo flujo que registro pero llama a /login
                        composable(Routes.LOGIN) {
                            LoginScreen(
                                onBack = { navController.popBackStack() },
                                onCaptured = { jpegBytes ->
                                    navController.popBackStack()
                                    isUploading = true
                                    scope.launch {
                                        val api = ApiClient.retrofit.create(FaceApi::class.java)
                                        val result = try {
                                            if (jpegBytes.isEmpty()) {
                                                throw IllegalStateException("No se pudo capturar la imagen")
                                            }

                                            val prepared = prepareJpegForUpload(jpegBytes)
                                            var body: RequestBody = RequestBody.create(
                                                "image/jpeg".toMediaType(),
                                                prepared
                                            )
                                            var part = MultipartBody.Part.createFormData(
                                                "file",
                                                "face.jpg",
                                                body
                                            )
                                            var resp = api.login(part)
                                            var text = responseText(resp)

                                            if (!resp.isSuccessful && resp.code() == 503) {
                                                val smaller = prepareJpegForUpload(
                                                    jpegBytes,
                                                    maxSide = 640,
                                                    qualityStart = 75
                                                )

                                                body = RequestBody.create(
                                                    "image/jpeg".toMediaType(),
                                                    smaller
                                                )

                                                part = MultipartBody.Part.createFormData(
                                                    "file",
                                                    "face.jpg",
                                                    body
                                                )

                                                resp = api.login(part)
                                                text = responseText(resp)
                                            }

                                            text
                                        } catch (e: Exception) {
                                            "Error al autenticar: ${e.message}"
                                        }

                                        isUploading = false
                                        resultMessage = result
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val REGISTRATION = "registration"
    const val LOGIN = "login"
}

// Helpers de normalización y lectura de respuesta, replicados de las pantallas para uso centralizado
private fun responseText(resp: retrofit2.Response<ResponseBody>): String {
    return try {
        if (resp.isSuccessful) {
            resp.body()?.string() ?: "(Respuesta vacía)"
        } else {
            val err = try {
                resp.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
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
        val bitmap =
            BitmapFactory.decodeByteArray(originalJpeg, 0, originalJpeg.size) ?: return originalJpeg
        val w = bitmap.width
        val h = bitmap.height
        val scale = (maxOf(w, h).toFloat() / maxSide).coerceAtLeast(1f)
        val targetW = (w / scale).toInt().coerceAtLeast(1)
        val targetH = (h / scale).toInt().coerceAtLeast(1)
        val resized: Bitmap =
            if (scale > 1f) Bitmap.createScaledBitmap(bitmap, targetW, targetH, true) else bitmap

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