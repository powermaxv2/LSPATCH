package com.powermax.patchstudio.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PsColorScheme = darkColorScheme(
    primary = PsColors.Accent,
    onPrimary = PsColors.OnAccent,
    primaryContainer = PsColors.SurfaceHigh,
    onPrimaryContainer = PsColors.TextPrimary,
    secondary = PsColors.TextSecondary,
    onSecondary = PsColors.Background,
    secondaryContainer = PsColors.SurfaceHigh,
    onSecondaryContainer = PsColors.TextPrimary,
    tertiary = PsColors.TextSecondary,
    onTertiary = PsColors.Background,
    background = PsColors.Background,
    onBackground = PsColors.TextPrimary,
    surface = PsColors.Background,
    onSurface = PsColors.TextPrimary,
    surfaceVariant = PsColors.SurfaceHigh,
    onSurfaceVariant = PsColors.TextSecondary,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = PsColors.Background,
    surfaceContainerLow = PsColors.Surface,
    surfaceContainer = PsColors.Surface,
    surfaceContainerHigh = PsColors.SurfaceHigh,
    surfaceContainerHighest = PsColors.SurfaceHigh,
    inverseSurface = PsColors.TextPrimary,
    inverseOnSurface = PsColors.Background,
    inversePrimary = PsColors.Accent,
    outline = PsColors.Hairline,
    outlineVariant = PsColors.Hairline,
    error = PsColors.Destructive,
    onError = PsColors.Background,
    scrim = Color.Black,
)

private val PsMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Thème unique, toujours sombre. */
@Composable
fun PatchStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PsColorScheme,
        typography = PsTypography,
        shapes = PsMaterialShapes,
        content = content,
    )
}
