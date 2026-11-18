package com.example.marketpos.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class SemanticColors(
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color
)

val LightSemanticColors = SemanticColors(
    warning = LightWarning,
    warningContainer = LightWarningContainer,
    onWarningContainer = LightOnWarningContainer
)

val DarkSemanticColors = SemanticColors(
    warning = DarkWarning,
    warningContainer = DarkWarningContainer,
    onWarningContainer = DarkOnWarningContainer
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemanticColors }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class FontScaleOption(val multiplier: Float) {
    NORMAL(1.0f),
    LARGE(1.15f),
    EXTRA_LARGE(1.30f)
}

@Composable
fun MarketPosTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    fontScaleOption: FontScaleOption = FontScaleOption.NORMAL,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    // Android 12+ (API 31+) native dynamic color without legacy version checks
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(context)
        darkTheme -> MarketDarkColorScheme
        else -> MarketLightColorScheme
    }

    val scaledTypography = remember(fontScaleOption) {
        scaleTypography(MarketTypography, fontScaleOption.multiplier)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

    CompositionLocalProvider(LocalSemanticColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = scaledTypography,
            content = content
        )
    }
}

private fun scaleTypography(base: Typography, scale: Float): Typography {
    return Typography(
        displayLarge = base.displayLarge.copy(
            fontSize = base.displayLarge.fontSize * scale,
            lineHeight = base.displayLarge.lineHeight * scale
        ),
        displayMedium = base.displayMedium.copy(
            fontSize = base.displayMedium.fontSize * scale,
            lineHeight = base.displayMedium.lineHeight * scale
        ),
        displaySmall = base.displaySmall.copy(
            fontSize = base.displaySmall.fontSize * scale,
            lineHeight = base.displaySmall.lineHeight * scale
        ),
        headlineLarge = base.headlineLarge.copy(
            fontSize = base.headlineLarge.fontSize * scale,
            lineHeight = base.headlineLarge.lineHeight * scale
        ),
        headlineMedium = base.headlineMedium.copy(
            fontSize = base.headlineMedium.fontSize * scale,
            lineHeight = base.headlineMedium.lineHeight * scale
        ),
        headlineSmall = base.headlineSmall.copy(
            fontSize = base.headlineSmall.fontSize * scale,
            lineHeight = base.headlineSmall.lineHeight * scale
        ),
        titleLarge = base.titleLarge.copy(
            fontSize = base.titleLarge.fontSize * scale,
            lineHeight = base.titleLarge.lineHeight * scale
        ),
        titleMedium = base.titleMedium.copy(
            fontSize = base.titleMedium.fontSize * scale,
            lineHeight = base.titleMedium.lineHeight * scale
        ),
        titleSmall = base.titleSmall.copy(
            fontSize = base.titleSmall.fontSize * scale,
            lineHeight = base.titleSmall.lineHeight * scale
        ),
        bodyLarge = base.bodyLarge.copy(
            fontSize = base.bodyLarge.fontSize * scale,
            lineHeight = base.bodyLarge.lineHeight * scale
        ),
        bodyMedium = base.bodyMedium.copy(
            fontSize = base.bodyMedium.fontSize * scale,
            lineHeight = base.bodyMedium.lineHeight * scale
        ),
        bodySmall = base.bodySmall.copy(
            fontSize = base.bodySmall.fontSize * scale,
            lineHeight = base.bodySmall.lineHeight * scale
        ),
        labelLarge = base.labelLarge.copy(
            fontSize = base.labelLarge.fontSize * scale,
            lineHeight = base.labelLarge.lineHeight * scale
        ),
        labelMedium = base.labelMedium.copy(
            fontSize = base.labelMedium.fontSize * scale,
            lineHeight = base.labelMedium.lineHeight * scale
        ),
        labelSmall = base.labelSmall.copy(
            fontSize = base.labelSmall.fontSize * scale,
            lineHeight = base.labelSmall.lineHeight * scale
        ),
    )
}
