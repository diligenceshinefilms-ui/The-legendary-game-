package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LegendRacerColorScheme = darkColorScheme(
    primary = GalaxyPink,
    onPrimary = TextWhite,
    primaryContainer = GalaxySurface,
    onPrimaryContainer = GalaxyPink,
    secondary = GalaxyCyan,
    onSecondary = GalaxyVoid,
    secondaryContainer = GalaxyCard,
    onSecondaryContainer = GalaxyCyan,
    tertiary = GalaxyViolet,
    onTertiary = TextWhite,
    background = GalaxyVoid,
    onBackground = TextWhite,
    surface = GalaxyCard,
    onSurface = TextWhite,
    surfaceVariant = GalaxySurface,
    onSurfaceVariant = TextLavender,
    outline = GalaxyCardBorder,
    error = GalaxyRed,
    onError = TextWhite
)

@Composable
fun LegendRacerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LegendRacerColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    LegendRacerTheme(content = content)
}

