package com.powermax.patchstudio.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CoreTest {
    private val deezer = TargetApps.DEEZER
    private val synth = ThemePresets.byId("synthwave").palette!!
    private val remapper = ColorRemapper(synth, deezer.brandHueRange)

    private fun hueDist(a: Float, b: Float): Float { val d = abs(a - b) % 360f; return if (d > 180f) 360f - d else d }
    private fun channelDist(a: Int, b: Int): Int =
        maxOf(abs(ColorMath.red(a) - ColorMath.red(b)), abs(ColorMath.green(a) - ColorMath.green(b)), abs(ColorMath.blue(a) - ColorMath.blue(b)))

    @Test fun brandHslReference() {
        val hsl = ColorMath.toHsl(deezer.brandColor)
        assertEquals(0.61f, hsl[2], 0.01f)
        assertTrue(hsl[0] in deezer.brandHueRange)
    }

    @Test fun hslRoundtrip() {
        for (c in listOf(0xFFA238FF.toInt(), 0xFF00D1FF.toInt(), 0xFF3DDC84.toInt(), 0xFF123456.toInt())) {
            val h = ColorMath.toHsl(c)
            assertTrue(channelDist(c, ColorMath.fromHsl(h[0], h[1], h[2])) <= 1)
        }
    }

    @Test fun deezerAccentMapsToThemeAccent() {
        val out = remapper.map(deezer.brandColor)
        assertTrue("got ${StyleConfig.hex(out)}", channelDist(out, synth.accent) <= 2)
        val lighter = remapper.map(0xFFC77DFF.toInt())
        assertTrue(hueDist(ColorMath.toHsl(lighter)[0], ColorMath.toHsl(synth.accent)[0]) < 2f)
    }

    @Test fun darkBackgroundMapsNearThemeBackground() {
        val out = remapper.map(0xFF121216.toInt())
        val expected = ColorMath.blend(synth.background, synth.surface, ColorMath.toHsl(0xFF121216.toInt())[2] / 0.2f)
        assertTrue(channelDist(out, expected) <= 1)
        assertTrue(channelDist(out, synth.background) <= channelDist(synth.surface, synth.background))
        assertEquals(synth.background, remapper.map(0xFF000000.toInt()))
    }

    @Test fun whiteAndRedUnchanged() {
        assertEquals(0xFFFFFFFF.toInt(), remapper.map(0xFFFFFFFF.toInt()))
        assertEquals(0xFFFF0000.toInt(), remapper.map(0xFFFF0000.toInt()))
        assertEquals(0x00A238FF, remapper.map(0x00A238FF))
    }

    @Test fun alphaPreserved() {
        val out = remapper.map(0x80A238FF.toInt())
        assertEquals(0x80, ColorMath.alpha(out))
        assertEquals(0x33, ColorMath.alpha(remapper.map(0x33121216)))
    }

    @Test fun remapIsIdempotent() {
        for (theme in listOf("amoled", "sakura", "synthwave", "custom")) {
            val pal = StyleConfig(themeId = theme).resolvedPalette()!!
            val r = ColorRemapper(pal, deezer.brandHueRange)
            for (c in listOf(0xFF121216, 0xFF1E1E1E, 0xFF000000, 0xFFA238FF, 0xFFC77DFF, 0xFF232328, 0x99121212)) {
                val once = r.map(c.toInt())
                assertEquals("$theme ${StyleConfig.hex(c.toInt())}", once, r.map(once))
            }
        }
    }

    @Test fun mapRoundtrip() {
        val cfg = StyleConfig(
            enabled = false, themeId = "custom", customAccent = 0x80112233.toInt(), customBackground = 0xFF010203.toInt(),
            tintSystemBars = false, entrance = EntranceAnimation.BOUNCE, screenTransitions = false, animationSpeed = 1.5f,
            listAnimations = false, pressBounce = false, haptics = true, cornerRadiusDp = 20, font = FontChoice.MONO,
            aurora = true, auroraIntensity = 0.4f,
            lyricsEnhanced = false, lyricsBackground = LyricsBackground.DARK, lyricsTextScale = 1.4f,
            lyricsAlignment = LyricsAlignment.CENTER, lyricsActiveZoom = false, lyricsFadeEdges = false,
        )
        assertEquals(cfg, StyleConfig.fromMap(cfg.toMap()))
        assertEquals(StyleConfig.DEFAULT, StyleConfig.fromMap(StyleConfig.DEFAULT.toMap()))
    }

    @Test fun fromMapGarbage() {
        val cfg = StyleConfig.fromMap(mapOf(
            "enabled" to "peut-être", "themeId" to "inconnu", "customAccent" to "#ZZZ", "entrance" to "WARP",
            "animationSpeed" to "99", "cornerRadiusDp" to "-5", "auroraIntensity" to "NaN", "font" to "serif",
            "customBackground" to "112233",
        ))
        assertEquals(true, cfg.enabled)
        assertEquals("amoled", cfg.themeId)
        assertEquals(StyleConfig.DEFAULT.customAccent, cfg.customAccent)
        assertEquals(0xFF112233.toInt(), cfg.customBackground)
        assertEquals(EntranceAnimation.SLIDE_UP, cfg.entrance)
        assertEquals(2f, cfg.animationSpeed, 0f)
        assertEquals(0, cfg.cornerRadiusDp)
        assertEquals(0.25f, cfg.auroraIntensity, 0f)
        assertEquals(FontChoice.SERIF, cfg.font)
        assertEquals(StyleConfig.DEFAULT, StyleConfig.fromMap(emptyMap()))
    }

    @Test fun lyricsDefaults() {
        val d = StyleConfig.DEFAULT
        assertEquals(true, d.lyricsEnhanced)
        assertEquals(LyricsBackground.GRADIENT, d.lyricsBackground)
        assertEquals(1.15f, d.lyricsTextScale, 0f)
        assertEquals(LyricsAlignment.LEFT, d.lyricsAlignment)
        assertEquals(true, d.lyricsActiveZoom)
        assertEquals(true, d.lyricsFadeEdges)
    }

    @Test fun lyricsFromMapTolerant() {
        val low = StyleConfig.fromMap(mapOf(
            "lyricsEnhanced" to "0", "lyricsBackground" to " none ", "lyricsTextScale" to "0.1",
            "lyricsAlignment" to "center", "lyricsActiveZoom" to "nope", "lyricsFadeEdges" to "false",
        ))
        assertEquals(false, low.lyricsEnhanced)
        assertEquals(LyricsBackground.NONE, low.lyricsBackground)
        assertEquals(0.9f, low.lyricsTextScale, 0f)
        assertEquals(LyricsAlignment.CENTER, low.lyricsAlignment)
        assertEquals(true, low.lyricsActiveZoom)
        assertEquals(false, low.lyricsFadeEdges)
        val bad = StyleConfig.fromMap(mapOf(
            "lyricsBackground" to "RAINBOW", "lyricsTextScale" to "42", "lyricsAlignment" to "RIGHT",
        ))
        assertEquals(LyricsBackground.GRADIENT, bad.lyricsBackground)
        assertEquals(1.6f, bad.lyricsTextScale, 0f)
        assertEquals(LyricsAlignment.LEFT, bad.lyricsAlignment)
        assertEquals(1.15f, StyleConfig.fromMap(mapOf("lyricsTextScale" to "NaN")).lyricsTextScale, 0f)
    }

    @Test fun protocolEncodeDecode() {
        val cfg = StyleConfig(themeId = "ocean", haptics = true)
        val enc = ConfigProtocol.encode(cfg)
        assertTrue(enc.keys.all { it.startsWith(ConfigProtocol.EXTRA_CONFIG_PREFIX) })
        assertEquals(cfg, ConfigProtocol.decode(enc + mapOf("other" to "x", "ps_cfg_null" to null)))
        assertNull(ConfigProtocol.decode(mapOf("foo" to "bar")))
    }

    @Test fun palettes() {
        assertNull(StyleConfig(themeId = "original").resolvedPalette())
        assertNull(StyleConfig(enabled = false).resolvedPalette())
        val custom = StyleConfig(themeId = "custom", customAccent = 0xFF00FF00.toInt()).resolvedPalette()
        assertNotNull(custom)
        assertEquals(0xFF00FF00.toInt(), custom!!.accent)
        assertEquals(8, ThemePresets.ALL.size)
        assertEquals(ThemePresets.ORIGINAL, ThemePresets.byId("nope"))
        assertEquals(deezer, TargetApps.byPackage("deezer.android.app"))
        assertEquals(0xFF000000.toInt(), ColorMath.onColorFor(0xFFFFFFFF.toInt()))
        assertEquals(0xFFFFFFFF.toInt(), ColorMath.onColorFor(0xFF000000.toInt()))
    }
}
