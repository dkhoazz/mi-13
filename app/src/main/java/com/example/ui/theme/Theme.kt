package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PureAmoledColorScheme = darkColorScheme(
    primary = HyperBlue,
    onPrimary = Color(0xFF04142B),
    primaryContainer = HyperBlueContainer,
    onPrimaryContainer = HyperBlueOnContainer,
    secondary = HyperTeal,
    onSecondary = Color(0xFF00201C),
    secondaryContainer = HyperTealContainer,
    onSecondaryContainer = Color(0xFFB3FFF2),
    background = AmoledBackground,
    onBackground = AmoledOnSurface,
    surface = AmoledSurface,
    onSurface = AmoledOnSurface,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = AmoledOnSurfaceMuted,
    surfaceContainerLowest = AmoledBackground,
    surfaceContainerLow = AmoledSurface,
    surfaceContainer = AmoledSurfaceElevated,
    surfaceContainerHigh = AmoledSurfaceVariant,
    outline = AmoledOutline,
    outlineVariant = Color(0xFF1B2230),
    error = HyperRed,
    onError = Color.White,
    errorContainer = Color(0xFF3A1014),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun HyperBatteryTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PureAmoledColorScheme,
        typography = HyperTypography,
        content = content
    )
}
