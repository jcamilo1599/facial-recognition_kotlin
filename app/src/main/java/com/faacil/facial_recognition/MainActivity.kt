package com.faacil.facial_recognition

/**
 * Punto de entrada principal de la app.
 *
 * Responsabilidades:
 * - Configurar el tema y superficie raíz de Compose.
 * - Gestionar la navegación entre Home, Registro y Login.
 * - Mostrar un AlertDialog en Home con la respuesta literal devuelta por el backend
 *   después de completar los flujos de cámara (registro/login).
 *
 * Notas:
 * - No mantiene estado de cámara ni flujos ML; esos viven dentro de cada pantalla.
 */

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.navigation.compose.rememberNavController
import com.faacil.facial_recognition.ui.theme.FacialRecognitionTheme
import com.faacil.facial_recognition.feature.home.HomeScreen
import com.faacil.facial_recognition.feature.login.presentation.LoginScreen
import com.faacil.facial_recognition.feature.registration.presentation.RegistrationScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FacialRecognitionTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController: NavHostController = rememberNavController()
                    // Estado local para mostrar la alerta con la respuesta literal
                    var resultMessage by remember { mutableStateOf<String?>(null) }

                    NavHost(navController = navController, startDestination = Routes.HOME) {
                        composable(Routes.HOME) {
                            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                HomeScreen(
                                    onRegister = { navController.navigate(Routes.REGISTRATION) },
                                    onLogin = { navController.navigate(Routes.LOGIN) }
                                )

                                // Muestra alerta literal con la respuesta del servicio cuando exista
                                resultMessage?.let { msg ->
                                    AlertDialog(
                                        onDismissRequest = { resultMessage = null },
                                        title = { androidx.compose.material3.Text(text = "Respuesta del servicio") },
                                        text = { androidx.compose.material3.Text(text = msg) },
                                        confirmButton = {
                                            Button(onClick = { resultMessage = null }) { androidx.compose.material3.Text("OK") }
                                        }
                                    )
                                }
                            }
                        }
                        // Pantalla de Registro: abre cámara, ejecuta liveness y sube imagen
                        composable(Routes.REGISTRATION) {
                            RegistrationScreen(
                                onBack = { navController.popBackStack() },
                                onDone = { result ->
                                    // Guardar y volver a HOME
                                    resultMessage = result
                                    navController.popBackStack()
                                }
                            )
                        }
                        // Pantalla de Login facial: mismo flujo que registro pero llama a /login
                        composable(Routes.LOGIN) {
                            LoginScreen(
                                onBack = { navController.popBackStack() },
                                onDone = { result ->
                                    resultMessage = result
                                    navController.popBackStack()
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