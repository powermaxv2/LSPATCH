package com.powermax.patchstudio.core

import java.util.concurrent.ConcurrentHashMap

/**
 * Remappe les couleurs d'origine de l'appli vers une palette :
 *  - teintes de marque (accent) -> teinte/saturation de l'accent, luminosité relative conservée ;
 *  - fonds sombres grisâtres -> dégradé background..surface ;
 *  - le reste est inchangé. Thread-safe.
 */
class ColorRemapper(private val palette: Palette, private val brandHueRange: ClosedFloatingPointRange<Float>) {

    private val cache = ConcurrentHashMap<Int, Int>()
    // Couleurs déjà produites : map() doit être idempotent (une couleur peut repasser par plusieurs hooks).
    private val produced: MutableSet<Int> = ConcurrentHashMap.newKeySet()
    private val accentHsl = ColorMath.toHsl(palette.accent)

    fun map(color: Int): Int {
        if (ColorMath.alpha(color) == 0) return color
        cache[color]?.let { return it }
        if (isPaletteColor(color) || produced.contains(color)) return color
        val out = compute(color)
        if (cache.size > MAX_CACHE) {
            cache.clear()
            produced.clear()
        }
        cache[color] = out
        if (out != color) produced.add(out)
        return out
    }

    private fun isPaletteColor(color: Int): Boolean {
        val rgb = color and 0x00FFFFFF
        return rgb == palette.background and 0x00FFFFFF || rgb == palette.surface and 0x00FFFFFF ||
            rgb == palette.accent and 0x00FFFFFF
    }

    private fun compute(color: Int): Int {
        val a = ColorMath.alpha(color)
        val (h, s, l) = ColorMath.toHsl(color).let { Triple(it[0], it[1], it[2]) }
        if (s >= 0.30f && l in 0.18f..0.88f && h in brandHueRange) {
            val newL = (accentHsl[2] + (l - BRAND_LIGHTNESS)).coerceIn(0.05f, 0.95f)
            return ColorMath.fromHsl(accentHsl[0], accentHsl[1], newL, a)
        }
        if (s < 0.35f && l < 0.20f) {
            val t = l / 0.20f
            return ColorMath.withAlpha(ColorMath.blend(palette.background, palette.surface, t), a)
        }
        return color
    }

    companion object {
        /** Luminosité HSL de référence de la couleur de marque (~#A238FF). */
        const val BRAND_LIGHTNESS = 0.61f
        private const val MAX_CACHE = 4096
    }
}
