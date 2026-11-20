package com.sara.assistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.sara.assistant.data.settings.AccentColorOption
import com.sara.assistant.data.settings.TextSizeOption
import com.sara.assistant.data.settings.ThemeModeOption

data class SaraExtendedColors(
    val isDark: Boolean,
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
        isDark = true,
        canvas = NavyDeep,
        topWash = Color(0xFF091229),
        surface = NavySurface,
        elevated = NavyElevated,
        border = NavyBorder,
        accentPrimary = CyanGlow,
        accentSecondary = VioletSubtle,
        textPrimary = TextPrimary,
        textSecondary = TextSecondary,
        textMuted = TextMuted
    )
}

@Composable
fun SaraTheme(
    themeMode: ThemeModeOption = ThemeModeOption.DARK,
    accentColor: AccentColorOption = AccentColorOption.CYAN,
    textSize: TextSizeOption = TextSizeOption.MEDIUM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeModeOption.DARK -> true
        ThemeModeOption.LIGHT -> false
        ThemeModeOption.SYSTEM -> isSystemInDarkTheme()
    }

    val primaryAccent = Color(accentColor.primaryHex)
    val secondaryAccent = Color(accentColor.secondaryHex)

    val extendedColors = if (isDark) {
        SaraExtendedColors(
            isDark = true,
            canvas = NavyDeep,
            topWash = Color(0xFF091229),
            surface = NavySurface,
            elevated = NavyElevated,
            border = NavyBorder,
            accentPrimary = primaryAccent,
            accentSecondary = secondaryAccent,
            textPrimary = TextPrimary,
            textSecondary = TextSecondary,
            textMuted = TextMuted
        )
    } else {
        SaraExtendedColors(
            isDark = false,
            canvas = LightCanvas,
            topWash = Color(0xFFE4ECF8),
            surface = LightSurface,
            elevated = LightElevated,
            border = LightBorder,
            accentPrimary = primaryAccent,
            accentSecondary = secondaryAccent,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            textMuted = LightTextMuted
        )
    }

    val materialScheme = if (isDark) {
        darkColorScheme(
            primary = primaryAccent,
            onPrimary = NavyDeep,
            secondary = secondaryAccent,
            onSecondary = TextPrimary,
            background = NavyDeep,
            onBackground = TextPrimary,
            surface = NavySurface,
            onSurface = TextPrimary,
            surfaceVariant = NavyElevated,
            onSurfaceVariant = TextSecondary,
            outline = NavyBorder
        )
    } else {
        lightColorScheme(
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
    }

    CompositionLocalProvider(LocalSaraColors provides extendedColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = buildSaraTypography(textSize.scaleFactor),
            content = content
        )
    }
}
