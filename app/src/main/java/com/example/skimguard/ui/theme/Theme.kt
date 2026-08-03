package com.example.skimguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ObsidianColorScheme = darkColorScheme(
    primary = ObsidianWhite,
    onPrimary = ObsidianBlack,
    secondary = ObsidianOnSurface,
    onSecondary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = ObsidianOnSurface,
    surface = ObsidianBlack,
    onSurface = ObsidianOnSurface,
    surfaceVariant = ObsidianSurface,
    onSurfaceVariant = ObsidianOnSurfaceMuted,
    outline = ObsidianOutline,
    outlineVariant = ObsidianOutlineSoft,
    error = ObsidianDanger,
    onError = ObsidianBlack,
)

@Composable
fun SkimGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ObsidianColorScheme,
        typography = Typography,
        content = content
    )
}
