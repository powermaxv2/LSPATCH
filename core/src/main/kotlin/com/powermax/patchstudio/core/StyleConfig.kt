package com.powermax.patchstudio.core

enum class EntranceAnimation { NONE, FADE, SLIDE_UP, ZOOM, BOUNCE }

enum class FontChoice { DEFAULT, CONDENSED, LIGHT, MEDIUM, SERIF, MONO, CASUAL }

/** Fond de l'écran des paroles. */
enum class LyricsBackground {
    /** Fond d'origine de l'app. */
    NONE,
    /** Dégradé lent et sobre dérivé de l'accent. */
    GRADIENT,
    /** Fond uni très sombre. */
    DARK,
}

enum class LyricsAlignment { LEFT, CENTER }

data class StyleConfig(
    val enabled: Boolean = true,
    val themeId: String = "amoled",
    val customAccent: Int = DEFAULT_CUSTOM_ACCENT,
    val customBackground: Int = DEFAULT_CUSTOM_BACKGROUND,
    val tintSystemBars: Boolean = true,
    val entrance: EntranceAnimation = EntranceAnimation.SLIDE_UP,
    val screenTransitions: Boolean = true,
    val animationSpeed: Float = 1f,
    val listAnimations: Boolean = true,
    val pressBounce: Boolean = true,
    val haptics: Boolean = false,
    val cornerRadiusDp: Int = 14,
    val font: FontChoice = FontChoice.DEFAULT,
    val aurora: Boolean = false,
    val auroraIntensity: Float = 0.25f,
    val lyricsEnhanced: Boolean = true,
    val lyricsBackground: LyricsBackground = LyricsBackground.GRADIENT,
    val lyricsTextScale: Float = 1.15f,
    val lyricsAlignment: LyricsAlignment = LyricsAlignment.LEFT,
    val lyricsActiveZoom: Boolean = true,
    val lyricsFadeEdges: Boolean = true,
) {
    /** null si thème original ou si !enabled */
    fun resolvedPalette(): Palette? {
        if (!enabled) return null
        if (themeId == ThemePresets.CUSTOM_ID) return ThemePresets.paletteOf(customAccent, customBackground)
        return ThemePresets.byId(themeId).palette
    }

    fun toMap(): Map<String, String> = linkedMapOf(
        K_ENABLED to enabled.toString(),
        K_THEME to themeId,
        K_CUSTOM_ACCENT to hex(customAccent),
        K_CUSTOM_BG to hex(customBackground),
        K_SYSTEM_BARS to tintSystemBars.toString(),
        K_ENTRANCE to entrance.name,
        K_TRANSITIONS to screenTransitions.toString(),
        K_SPEED to animationSpeed.toString(),
        K_LISTS to listAnimations.toString(),
        K_BOUNCE to pressBounce.toString(),
        K_HAPTICS to haptics.toString(),
        K_CORNERS to cornerRadiusDp.toString(),
        K_FONT to font.name,
        K_AURORA to aurora.toString(),
        K_AURORA_INTENSITY to auroraIntensity.toString(),
        K_LYRICS to lyricsEnhanced.toString(),
        K_LYRICS_BG to lyricsBackground.name,
        K_LYRICS_SCALE to lyricsTextScale.toString(),
        K_LYRICS_ALIGN to lyricsAlignment.name,
        K_LYRICS_ZOOM to lyricsActiveZoom.toString(),
        K_LYRICS_FADE to lyricsFadeEdges.toString(),
    )

    companion object {
        const val DEFAULT_CUSTOM_ACCENT: Int = 0xFF00E5FF.toInt()
        const val DEFAULT_CUSTOM_BACKGROUND: Int = 0xFF05070D.toInt()

        val SPEED_RANGE = 0.5f..2f
        val CORNER_RANGE = 0..32
        val AURORA_RANGE = 0.05f..0.6f
        val LYRICS_SCALE_RANGE = 0.9f..1.6f

        private const val K_ENABLED = "enabled"
        private const val K_THEME = "themeId"
        private const val K_CUSTOM_ACCENT = "customAccent"
        private const val K_CUSTOM_BG = "customBackground"
        private const val K_SYSTEM_BARS = "tintSystemBars"
        private const val K_ENTRANCE = "entrance"
        private const val K_TRANSITIONS = "screenTransitions"
        private const val K_SPEED = "animationSpeed"
        private const val K_LISTS = "listAnimations"
        private const val K_BOUNCE = "pressBounce"
        private const val K_HAPTICS = "haptics"
        private const val K_CORNERS = "cornerRadiusDp"
        private const val K_FONT = "font"
        private const val K_AURORA = "aurora"
        private const val K_AURORA_INTENSITY = "auroraIntensity"
        private const val K_LYRICS = "lyricsEnhanced"
        private const val K_LYRICS_BG = "lyricsBackground"
        private const val K_LYRICS_SCALE = "lyricsTextScale"
        private const val K_LYRICS_ALIGN = "lyricsAlignment"
        private const val K_LYRICS_ZOOM = "lyricsActiveZoom"
        private const val K_LYRICS_FADE = "lyricsFadeEdges"

        val DEFAULT: StyleConfig = StyleConfig()

        fun fromMap(map: Map<String, String>): StyleConfig {
            val d = DEFAULT
            fun bool(k: String, def: Boolean) = when (map[k]?.trim()?.lowercase()) {
                "true", "1" -> true
                "false", "0" -> false
                else -> def
            }
            fun float(k: String, def: Float, range: ClosedFloatingPointRange<Float>): Float {
                val v = map[k]?.trim()?.toFloatOrNull() ?: return def
                return if (v.isNaN()) def else v.coerceIn(range.start, range.endInclusive)
            }
            fun int(k: String, def: Int, range: IntRange): Int =
                map[k]?.trim()?.toIntOrNull()?.coerceIn(range.first, range.last) ?: def
            fun color(k: String, def: Int): Int = parseHex(map[k]) ?: def
            val theme = map[K_THEME]?.trim()?.takeIf { id -> ThemePresets.ALL.any { it.id == id } } ?: d.themeId
            return StyleConfig(
                enabled = bool(K_ENABLED, d.enabled),
                themeId = theme,
                customAccent = color(K_CUSTOM_ACCENT, d.customAccent),
                customBackground = color(K_CUSTOM_BG, d.customBackground),
                tintSystemBars = bool(K_SYSTEM_BARS, d.tintSystemBars),
                entrance = enumOrNull<EntranceAnimation>(map[K_ENTRANCE]) ?: d.entrance,
                screenTransitions = bool(K_TRANSITIONS, d.screenTransitions),
                animationSpeed = float(K_SPEED, d.animationSpeed, SPEED_RANGE),
                listAnimations = bool(K_LISTS, d.listAnimations),
                pressBounce = bool(K_BOUNCE, d.pressBounce),
                haptics = bool(K_HAPTICS, d.haptics),
                cornerRadiusDp = int(K_CORNERS, d.cornerRadiusDp, CORNER_RANGE),
                font = enumOrNull<FontChoice>(map[K_FONT]) ?: d.font,
                aurora = bool(K_AURORA, d.aurora),
                auroraIntensity = float(K_AURORA_INTENSITY, d.auroraIntensity, AURORA_RANGE),
                lyricsEnhanced = bool(K_LYRICS, d.lyricsEnhanced),
                lyricsBackground = enumOrNull<LyricsBackground>(map[K_LYRICS_BG]) ?: d.lyricsBackground,
                lyricsTextScale = float(K_LYRICS_SCALE, d.lyricsTextScale, LYRICS_SCALE_RANGE),
                lyricsAlignment = enumOrNull<LyricsAlignment>(map[K_LYRICS_ALIGN]) ?: d.lyricsAlignment,
                lyricsActiveZoom = bool(K_LYRICS_ZOOM, d.lyricsActiveZoom),
                lyricsFadeEdges = bool(K_LYRICS_FADE, d.lyricsFadeEdges),
            )
        }

        private inline fun <reified E : Enum<E>> enumOrNull(v: String?): E? {
            val s = v?.trim()?.uppercase() ?: return null
            return enumValues<E>().firstOrNull { it.name == s }
        }

        /** "#AARRGGBB" */
        fun hex(color: Int): String = "#%08X".format(color)

        /** Accepte "#AARRGGBB", "#RRGGBB" (alpha FF), avec ou sans '#'. */
        fun parseHex(v: String?): Int? {
            val s = v?.trim()?.removePrefix("#") ?: return null
            if (s.isEmpty() || !s.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
            return when (s.length) {
                8 -> s.toLong(16).toInt()
                6 -> (0xFF000000L or s.toLong(16)).toInt()
                else -> null
            }
        }
    }
}
