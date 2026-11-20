package com.sara.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.sara.assistant.data.settings.AccentColorOption
import com.sara.assistant.data.settings.TextSizeOption

data class SaraExtendedColors(
    val isDark: Boolean = true,
    val canvas: Color,
    val topWash: Color,
    val surface: Color,
    val elevated: Color,
    val border: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val cardTint: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color
)

val LocalSaraColors = staticCompositionLocalOf {
    SaraExtendedColors(
        isDark = true,
        canvas = ObsidianCanvas,
        topWash = CrimsonGlowWash,
        surface = ObsidianSurface,
        elevated = ObsidianElevated,
        border = ObsidianBorder,
        accentPrimary = CrimsonAccent,
        accentSecondary = CrimsonDeep,
        cardTint = Color(0xFF1D1014),
        textPrimary = ObsidianTextPrimary,
        textSecondary = ObsidianTextSecondary,
        textMuted = ObsidianTextMuted
    )
}

/**
 * Obsidian Dark Aura Theme for SARA with switchable Aura Signatures
 * (Crimson Aura default matching flagship aesthetic, plus Cyan, Electric Blue, Violet, Emerald).
 */
@Composable
fun SaraTheme(
    accentColor: AccentColorOption = AccentColorOption.CRIMSON_AURA,
    textSize: TextSizeOption = TextSizeOption.MEDIUM,
    content: @Composable () -> Unit
) {
    val primaryAccent = Color(accentColor.primaryHex)
    val secondaryAccent = Color(accentColor.secondaryHex)
    val topGlow = Color(accentColor.topGlowHex)
    val cardTint = Color(accentColor.cardTintHex)

    val extendedColors = SaraExtendedColors(
        isDark = true,
        canvas = ObsidianCanvas,
        topWash = topGlow,
        surface = ObsidianSurface,
        elevated = ObsidianElevated,
        border = primaryAccent.copy(alpha = 0.22f),
        accentPrimary = primaryAccent,
        accentSecondary = secondaryAccent,
        cardTint = cardTint,
        textPrimary = ObsidianTextPrimary,
        textSecondary = ObsidianTextSecondary,
        textMuted = ObsidianTextMuted
    )

    val materialScheme = darkColorScheme(
        primary = primaryAccent,
        onPrimary = Color.White,
        secondary = secondaryAccent,
        onSecondary = Color.White,
        background = ObsidianCanvas,
        onBackground = ObsidianTextPrimary,
        surface = ObsidianSurface,
        onSurface = ObsidianTextPrimary,
        surfaceVariant = ObsidianElevated,
        onSurfaceVariant = ObsidianTextSecondary,
        outline = ObsidianBorder
    )

    CompositionLocalProvider(LocalSaraColors provides extendedColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = buildSaraTypography(textSize.scaleFactor),
            content = content
        )
    }
}
