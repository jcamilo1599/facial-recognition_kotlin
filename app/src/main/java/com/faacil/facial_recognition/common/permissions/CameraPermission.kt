package com.faacil.facial_recognition.common.permissions

import android.Manifest
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Wrapper de contenido que solicita y gestiona el permiso de cámara en tiempo de ejecución.
 *
 * - Si el permiso está concedido, renderiza [content].
 * - Si no, muestra un diálogo explicando el motivo y permite volver a solicitarlo.
 *
 * Notas:
 * - Si el usuario marcó "No volver a preguntar", Android no mostrará el diálogo del sistema.
 *   En ese caso, el botón simplemente no concederá el permiso; el usuario debe ir a Ajustes y
 *   habilitar el permiso manualmente.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WithCameraPermission(
    rationale: String = "Se requiere acceso a la cámara para continuar",
    content: @Composable () -> Unit,
) {
    val permissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    if (permissionState.status.isGranted) {
        content()
    } else {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Permiso de cámara") },
            text = { Text(rationale) },
            confirmButton = {
                Button(onClick = { permissionState.launchPermissionRequest() }) {
                    Text("Conceder")
                }
            }
        )
    }
}
