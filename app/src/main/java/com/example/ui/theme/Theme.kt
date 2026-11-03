package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StudioDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF04131A),
    primaryContainer = Color(0xFF0B3B49),
    onPrimaryContainer = Color(0xFFB8F4FF),
    secondary = StudioViolet,
    onSecondary = Color(0xFF1B093B),
    secondaryContainer = Color(0xFF311B5E),
    onSecondaryContainer = Color(0xFFEDE0FF),
    tertiary = NeonEmerald,
    background = StudioCarbonBg,
    onBackground = TextPrimary,
    surface = StudioPanelBg,
    onSurface = TextPrimary,
    surfaceVariant = StudioCardBg,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder
)

@Composable
fun RoutineWidgetStudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = StudioTypography,
        content = content
    )
}
