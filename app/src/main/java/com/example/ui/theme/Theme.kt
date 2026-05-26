package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PremiumDarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    secondary = DarkBlueAccent,
    tertiary = AmberVibe,
    background = Slate950,
    surface = Slate900,
    onPrimary = CarbonBlack,
    onSecondary = WhiteNeutral,
    onTertiary = CarbonBlack,
    onBackground = WhiteNeutral,
    onSurface = WhiteNeutral,
    surfaceVariant = CarbonBlack,
    onSurfaceVariant = Slate400,
    outline = Slate800,
    error = RubyRose
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PremiumDarkColorScheme,
        typography = Typography,
        content = content
    )
}
