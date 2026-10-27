package com.powermax.patchstudio.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val PsColorScheme = darkColorScheme(
    primary = PsColors.Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1670),
    onPrimaryContainer = Color(0xFFEBD8FF),
    secondary = PsColors.Secondary,
    onSecondary = Color(0xFF00232A),
    secondaryContainer = Color(0xFF00424D),
    onSecondaryContainer = Color(0xFFB8F5FF),
    tertiary = PsColors.Tertiary,
    onTertiary = Color(0xFF3A0030),
    tertiaryContainer = Color(0xFF5C0F4D),
    onTertiaryContainer = Color(0xFFFFD6F5),
    background = PsColors.Background,
    onBackground = PsColors.TextPrimary,
    surface = PsColors.Surface,
    onSurface = PsColors.TextPrimary,
    surfaceVariant = PsColors.SurfaceHigh,
    onSurfaceVariant = PsColors.TextSecondary,
    surfaceTint = PsColors.Primary,
    outline = PsColors.Outline,
    outlineVariant = Color(0xFF1B2038),
    error = Color(0xFFFF5470),
    onError = Color.White,
    scrim = Color.Black,
)

private val PsMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun PatchStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PsColorScheme,
        typography = PsTypography,
        shapes = PsMaterialShapes,
        content = content,
    )
}
