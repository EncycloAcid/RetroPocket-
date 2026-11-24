package com.retropocket.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RetroColorScheme = darkColorScheme(
    primary = Phosphor,
    onPrimary = Charcoal,
    primaryContainer = PhosphorDim,
    onPrimaryContainer = Phosphor,
    secondary = Mist,
    onSecondary = Charcoal,
    background = Charcoal,
    onBackground = Mist,
    surface = CharcoalElevated,
    onSurface = Mist,
    surfaceVariant = CharcoalSoft,
    onSurfaceVariant = MistMuted,
    outline = BezelEdge,
    error = Danger,
    onError = Color.White
)

@Composable
fun RetroPocketTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RetroColorScheme,
        typography = RetroTypography,
        content = content
    )
}
