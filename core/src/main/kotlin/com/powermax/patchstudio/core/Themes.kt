package com.powermax.patchstudio.core

data class Palette(val accent: Int, val background: Int, val surface: Int, val onAccent: Int)

/** palette == null => thème "original" (aucun remappage). */
data class ThemePreset(val id: String, val name: String, val description: String, val palette: Palette?)

object ThemePresets {
    const val CUSTOM_ID = "custom"

    /** Palette dérivée d'un accent + fond (surface = fond éclairci ~8 %). */
    fun paletteOf(accent: Int, background: Int, surface: Int? = null): Palette = Palette(
        accent = accent,
        background = background,
        surface = surface ?: ColorMath.blend(background, 0xFFFFFFFF.toInt(), 0.08f),
        onAccent = ColorMath.onColorFor(accent),
    )

    private fun preset(id: String, name: String, desc: String, accent: Long, bg: Long, surface: Long) =
        ThemePreset(id, name, desc, paletteOf(accent.toInt(), bg.toInt(), surface.toInt()))

    val ORIGINAL: ThemePreset = ThemePreset("original", "Original", "Les couleurs d'origine de l'appli", null)

    val ALL: List<ThemePreset> = listOf(
        ORIGINAL,
        preset("amoled", "AMOLED", "Noir pur, violet signature", 0xFFA238FF, 0xFF000000, 0xFF121212),
        preset("synthwave", "Synthwave", "Néons rose sur nuit violette", 0xFFFF2A6D, 0xFF0D0221, 0xFF1B0A3A),
        preset("ocean", "Océan", "Bleu cyan des profondeurs", 0xFF00D1FF, 0xFF001B2E, 0xFF06304D),
        preset("foret", "Forêt", "Vert frais sur sous-bois", 0xFF3DDC84, 0xFF08140D, 0xFF11281A),
        preset("sunset", "Sunset", "Orange chaud de fin de journée", 0xFFFF7A45, 0xFF1A0B08, 0xFF2E1610),
        preset("sakura", "Sakura", "Rose tendre des cerisiers", 0xFFFF8FB8, 0xFF160B12, 0xFF2A1622),
        ThemePreset(
            CUSTOM_ID, "Perso", "Tes propres couleurs",
            paletteOf(StyleConfig.DEFAULT_CUSTOM_ACCENT, StyleConfig.DEFAULT_CUSTOM_BACKGROUND),
        ),
    )

    fun byId(id: String): ThemePreset = ALL.firstOrNull { it.id == id } ?: ORIGINAL
}
