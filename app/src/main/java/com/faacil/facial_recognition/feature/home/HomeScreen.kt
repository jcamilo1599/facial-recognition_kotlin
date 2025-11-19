package com.faacil.facial_recognition.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onRegister: () -> Unit,
    onLogin: () -> Unit,
) {
    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = "Autenticación Facial",
                style = MaterialTheme.typography.headlineMedium
            )
            Button(onClick = onRegister, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
                Text("Registrar rostro")
            }
            Button(onClick = onLogin, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
                Text("Login con rostro")
            }
        }
    }
}
