package com.sara.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SaraDarkColorScheme = darkColorScheme(
    primary = CyanGlow,
    onPrimary = NavyDeep,
    secondary = VioletSubtle,
    onSecondary = TextPrimary,
    background = NavyDeep,
    onBackground = TextPrimary,
    surface = NavySurface,
    onSurface = TextPrimary,
    surfaceVariant = NavyElevated,
    onSurfaceVariant = TextSecondary,
    outline = NavyBorder
)

@Composable
fun SaraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SaraDarkColorScheme,
        typography = SaraTypography,
        content = content
    )
}
