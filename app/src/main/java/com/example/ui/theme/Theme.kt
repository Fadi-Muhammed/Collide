package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CollideColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Plate,
    onPrimaryContainer = Ink,
    secondary = Ink2,
    onSecondary = Paper,
    secondaryContainer = Plate,
    onSecondaryContainer = Ink,
    tertiary = Ink3,
    onTertiary = Paper,
    background = Plate,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Plate,
    onSurfaceVariant = Ink2,
    outline = Rule,
    outlineVariant = Rule
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CollideColorScheme,
        typography = Typography,
        content = content
    )
}
