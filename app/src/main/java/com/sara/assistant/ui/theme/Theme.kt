package com.sara.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.sara.assistant.data.settings.AccentColorOption
import com.sara.assistant.data.settings.TextSizeOption

data class SaraExtendedColors(
    val isDark: Boolean = false,
    val canvas: Color,
    val topWash: Color,
    val surface: Color,
    val elevated: Color,
    val border: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color
)

val LocalSaraColors = staticCompositionLocalOf {
    SaraExtendedColors(
        isDark = false,
        canvas = LightCanvas,
        topWash = LightTopWash,
        surface = LightSurface,
        elevated = LightElevated,
        border = LightBorder,
        accentPrimary = CyanAccent,
        accentSecondary = ElectricBlue,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        textMuted = LightTextMuted
    )
}

/**
 * Light-only premium SARA theme. Never switches to dark mode regardless of system theme.
 */
@Composable
fun SaraTheme(
    accentColor: AccentColorOption = AccentColorOption.CYAN,
    textSize: TextSizeOption = TextSizeOption.MEDIUM,
    content: @Composable () -> Unit
) {
    val primaryAccent = Color(accentColor.primaryHex)
    val secondaryAccent = Color(accentColor.secondaryHex)

    val extendedColors = SaraExtendedColors(
        isDark = false,
        canvas = LightCanvas,
        topWash = LightTopWash,
        surface = LightSurface,
        elevated = LightElevated,
        border = LightBorder,
        accentPrimary = primaryAccent,
        accentSecondary = secondaryAccent,
        textPrimary = LightTextPrimary,
        textSecondary = LightTextSecondary,
        textMuted = LightTextMuted
    )

    val materialScheme = lightColorScheme(
        primary = primaryAccent,
        onPrimary = Color.White,
        secondary = secondaryAccent,
        onSecondary = Color.White,
        background = LightCanvas,
        onBackground = LightTextPrimary,
        surface = LightSurface,
        onSurface = LightTextPrimary,
        surfaceVariant = LightElevated,
        onSurfaceVariant = LightTextSecondary,
        outline = LightBorder
    )

    CompositionLocalProvider(LocalSaraColors provides extendedColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = buildSaraTypography(textSize.scaleFactor),
            content = content
        )
    }
}
