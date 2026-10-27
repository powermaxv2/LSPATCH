package com.powermax.patchstudio.ui.theme

import androidx.compose.ui.graphics.Color

object PsColors {
    val Background = Color(0xFF05060A)
    val Surface = Color(0xFF0C0F1A)
    val SurfaceHigh = Color(0xFF151A2B)
    val Primary = Color(0xFFA238FF)
    val Secondary = Color(0xFF00E5FF)
    val Tertiary = Color(0xFFFF4FD8)
    val TextPrimary = Color(0xFFF2F4FF)
    val TextSecondary = Color(0xFF9AA3C2)
    val Outline = Color(0xFF2A3050)
}

/** Palettes de dégradés partagées par les composants. */
object PsGradients {
    val Brand: List<Color> = listOf(PsColors.Primary, PsColors.Secondary, PsColors.Tertiary)
    val Aurora: List<Color> = listOf(PsColors.Primary, PsColors.Secondary, PsColors.Tertiary, Color(0xFF3D5AFE))
}
