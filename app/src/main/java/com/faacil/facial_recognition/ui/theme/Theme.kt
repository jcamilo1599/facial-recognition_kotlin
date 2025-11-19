package com.faacil.facial_recognition.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PastelBlue60,
    onPrimary = PastelBlue95,
    primaryContainer = PastelBlue90,
    onPrimaryContainer = PastelBlue10,

    secondary = AquaBlue60,
    onSecondary = AquaBlue95,
    secondaryContainer = AquaBlue90,
    onSecondaryContainer = AquaBlue10,

    tertiary = SoftPink60,
    onTertiary = SoftPink95,
    tertiaryContainer = SoftPink90,
    onTertiaryContainer = SoftPink10,

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = ExpressiveNeutral99,
    onBackground = ExpressiveNeutral10,
    surface = ExpressiveNeutral99,
    onSurface = ExpressiveNeutral10,

    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    outline = NeutralVariant60,
    outlineVariant = NeutralVariant80,

    inverseSurface = ExpressiveNeutral10,
    inverseOnSurface = ExpressiveNeutral99,
    inversePrimary = PastelBlue40,

    surfaceTint = PastelBlue60,
    scrim = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = PastelBlue60,
    onPrimary = Color.White,
    primaryContainer = PastelBlue20,
    onPrimaryContainer = PastelBlue95,

    secondary = AquaBlue70,
    onSecondary = Color.White,
    secondaryContainer = AquaBlue20,
    onSecondaryContainer = AquaBlue95,

    tertiary = SoftPink70,
    onTertiary = Color.White,
    tertiaryContainer = SoftPink20,
    onTertiaryContainer = SoftPink95,

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = PastelBlue10,
    onBackground = ExpressiveNeutral99,
    surface = PastelBlue10,
    onSurface = ExpressiveNeutral99,

    surfaceVariant = NeutralVariant20,
    onSurfaceVariant = NeutralVariant80,
    outline = NeutralVariant70,
    outlineVariant = NeutralVariant40,

    inverseSurface = ExpressiveNeutral90,
    inverseOnSurface = ExpressiveNeutral10,
    inversePrimary = PastelBlue60,

    surfaceTint = PastelBlue60,
    scrim = Color.Black
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}