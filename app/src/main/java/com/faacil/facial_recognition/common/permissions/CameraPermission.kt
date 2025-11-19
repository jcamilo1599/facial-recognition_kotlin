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
