package com.bughunter.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val BhDarkColorScheme: ColorScheme = darkColorScheme(
    primary = BhAccent_818cf8,
    onPrimary = Color.White,
    primaryContainer = BhAccentSoftDark,
    onPrimaryContainer = BhText_eef2ff,
    secondary = BhAccent2_38bdf8,
    onSecondary = Color.White,
    secondaryContainer = BhAccent2SoftDark,
    onSecondaryContainer = BhText_eef2ff,
    tertiary = BhStatusReopened_a78bfa,
    onTertiary = Color.White,
    background = BhBg_0a0e1a,
    onBackground = BhText_eef2ff,
    surface = BhBgElev_131829,
    onSurface = BhText_eef2ff,
    surfaceVariant = BhBgElev2_1a2138,
    onSurfaceVariant = BhTextMuted_9aa6c4,
    surfaceTint = BhAccent_818cf8,
    outline = BhBorder_232c47,
    outlineVariant = BhBorderSoftDark,
    error = BhDanger_f43f5e,
    onError = Color.White,
    errorContainer = BhDangerSoftDark,
    onErrorContainer = BhText_eef2ff,
    inverseSurface = BhBgElevLight_ffffff,
    inverseOnSurface = BhTextLight_0f172a,
    inversePrimary = BhAccent_6366f1,
    scrim = BhModalScrim,
)

private val BhLightColorScheme: ColorScheme = lightColorScheme(
    primary = BhAccent_6366f1,
    onPrimary = Color.White,
    primaryContainer = BhAccentSoftLight,
    onPrimaryContainer = BhTextLight_0f172a,
    secondary = BhAccent2Light_0284c7,
    onSecondary = Color.White,
    secondaryContainer = BhAccent2SoftLight,
    onSecondaryContainer = BhTextLight_0f172a,
    tertiary = BhStatusReopenedLight_7c3aed,
    onTertiary = Color.White,
    background = BhBgLight_f4f6fb,
    onBackground = BhTextLight_0f172a,
    surface = BhBgElevLight_ffffff,
    onSurface = BhTextLight_0f172a,
    surfaceVariant = BhBgElev2Light_f1f4fb,
    onSurfaceVariant = BhTextMutedLight_475569,
    surfaceTint = BhAccent_6366f1,
    outline = BhBorderLight_e2e8f0,
    outlineVariant = BhBorderSoftLight,
    error = BhDangerLight_dc2626,
    onError = Color.White,
    errorContainer = BhDangerSoftLight,
    onErrorContainer = BhTextLight_0f172a,
    inverseSurface = BhBgElev_131829,
    inverseOnSurface = BhText_eef2ff,
    inversePrimary = BhAccent_818cf8,
    scrim = BhModalScrim,
)

// Brand identity is mandatory: dynamic Material You colours are off by design.
@Composable
internal fun BugHunterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) BhDarkColorScheme else BhLightColorScheme
    val brandTokens = buildBrandTokens(isDark = darkTheme)
    CompositionLocalProvider(
        LocalBrandTokens provides brandTokens,
        LocalAccentGradient provides brandTokens.accentGradient,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = BhTypography,
            shapes = BhShapes,
            content = content,
        )
    }
}
