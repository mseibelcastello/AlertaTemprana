package com.example.alertatemprana.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Primario,
    onPrimary = Color.White,
    primaryContainer = PrimarioClaro,
    onPrimaryContainer = TextoPrincipal,
    secondary = Secundario,
    onSecondary = Color.White,
    secondaryContainer = SecundarioClaro,
    onSecondaryContainer = TextoPrincipal,
    tertiary = Advertencia,
    onTertiary = Color.White,
    background = Fondo,
    onBackground = TextoPrincipal,
    surface = Superficie,
    onSurface = TextoPrincipal,
    surfaceVariant = SuperficieVariante,
    onSurfaceVariant = TextoSecundario,
    error = Emergencia,
    onError = Color.White
)

@Composable
fun AlertaTempranaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}