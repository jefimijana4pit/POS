package com.example.marketpos.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Light Scheme - Crisp Emerald & Cool Slate
val LightPrimary = Color(0xFF006C4C)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFF89F8C7)
val LightOnPrimaryContainer = Color(0xFF002114)

val LightSecondary = Color(0xFF4D6357)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFCFE9D9)
val LightOnSecondaryContainer = Color(0xFF0A1F16)

val LightTertiary = Color(0xFF3D6373)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFC1E8FB)
val LightOnTertiaryContainer = Color(0xFF001F29)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

val LightBackground = Color(0xFFF5FBF6)
val LightOnBackground = Color(0xFF171D1A)
val LightSurface = Color(0xFFF5FBF6)
val LightOnSurface = Color(0xFF171D1A)
val LightSurfaceVariant = Color(0xFFDBE5DD)
val LightOnSurfaceVariant = Color(0xFF404943)
val LightOutline = Color(0xFF707973)
val LightOutlineVariant = Color(0xFFBFC9C2)

val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFEFF5F0)
val LightSurfaceContainer = Color(0xFFE9EFEA)
val LightSurfaceContainerHigh = Color(0xFFE4EAE5)
val LightSurfaceContainerHighest = Color(0xFFDEE4DF)

// Dark Scheme - Low-Glare Slate & Mint Emerald
val DarkPrimary = Color(0xFF6CDBAC)
val DarkOnPrimary = Color(0xFF003826)
val DarkPrimaryContainer = Color(0xFF005138)
val DarkOnPrimaryContainer = Color(0xFF89F8C7)

val DarkSecondary = Color(0xFFB3CCBE)
val DarkOnSecondary = Color(0xFF1F352A)
val DarkSecondaryContainer = Color(0xFF354B40)
val DarkOnSecondaryContainer = Color(0xFFCFE9D9)

val DarkTertiary = Color(0xFFA5CCDF)
val DarkOnTertiary = Color(0xFF073543)
val DarkTertiaryContainer = Color(0xFF244C5B)
val DarkOnTertiaryContainer = Color(0xFFC1E8FB)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackground = Color(0xFF0F1512)
val DarkOnBackground = Color(0xFFDEE4DF)
val DarkSurface = Color(0xFF0F1512)
val DarkOnSurface = Color(0xFFDEE4DF)
val DarkSurfaceVariant = Color(0xFF404943)
val DarkOnSurfaceVariant = Color(0xFFBFC9C2)
val DarkOutline = Color(0xFF8A938C)
val DarkOutlineVariant = Color(0xFF404943)

val DarkSurfaceContainerLowest = Color(0xFF0A0F0D)
val DarkSurfaceContainerLow = Color(0xFF171D1A)
val DarkSurfaceContainer = Color(0xFF1B211E)
val DarkSurfaceContainerHigh = Color(0xFF252B28)
val DarkSurfaceContainerHighest = Color(0xFF303633)

val MarketLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
)

val MarketDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
)

// Semantic Warning Colors (Amber)
val LightWarning = Color(0xFFB45309)
val LightWarningContainer = Color(0xFFFEF3C7)
val LightOnWarningContainer = Color(0xFF451A03)

val DarkWarning = Color(0xFFFBBF24)
val DarkWarningContainer = Color(0xFF78350F)
val DarkOnWarningContainer = Color(0xFFFEF3C7)
