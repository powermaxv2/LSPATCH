package com.powermax.patchstudio.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Utilitaires couleur purs (ARGB Int, comme android.graphics.Color). */
object ColorMath {
    fun alpha(c: Int): Int = (c ushr 24) and 0xFF
    fun red(c: Int): Int = (c shr 16) and 0xFF
    fun green(c: Int): Int = (c shr 8) and 0xFF
    fun blue(c: Int): Int = c and 0xFF

    fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a.coerceIn(0, 255) shl 24) or (r.coerceIn(0, 255) shl 16) or
            (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    /** [h 0..360, s 0..1, l 0..1] */
    fun toHsl(color: Int): FloatArray {
        val r = red(color) / 255f
        val g = green(color) / 255f
        val b = blue(color) / 255f
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val l = (mx + mn) / 2f
        val d = mx - mn
        if (d == 0f) return floatArrayOf(0f, 0f, l)
        val s = d / (1f - abs(2f * l - 1f))
        var h = when (mx) {
            r -> ((g - b) / d) % 6f
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        } * 60f
        if (h < 0f) h += 360f
        return floatArrayOf(h, s.coerceIn(0f, 1f), l)
    }

    fun fromHsl(h: Float, s: Float, l: Float, alpha: Int = 255): Int {
        val hh = ((h % 360f) + 360f) % 360f
        val ss = s.coerceIn(0f, 1f)
        val ll = l.coerceIn(0f, 1f)
        val c = (1f - abs(2f * ll - 1f)) * ss
        val x = c * (1f - abs((hh / 60f) % 2f - 1f))
        val m = ll - c / 2f
        val (r, g, b) = when ((hh / 60f).toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return argb(
            alpha,
            ((r + m) * 255f).roundToInt(),
            ((g + m) * 255f).roundToInt(),
            ((b + m) * 255f).roundToInt(),
        )
    }

    /** Interpolation linéaire canal par canal (alpha inclus), t borné 0..1. */
    fun blend(a: Int, b: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun mix(x: Int, y: Int) = (x + (y - x) * k).roundToInt()
        return argb(
            mix(alpha(a), alpha(b)),
            mix(red(a), red(b)),
            mix(green(a), green(b)),
            mix(blue(a), blue(b)),
        )
    }

    /** Luminance relative WCAG (0..1). */
    fun luminance(color: Int): Float {
        fun ch(v: Int): Double {
            val c = v / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return (0.2126 * ch(red(color)) + 0.7152 * ch(green(color)) + 0.0722 * ch(blue(color))).toFloat()
    }

    /** Noir ou blanc, selon le meilleur contraste. */
    fun onColorFor(background: Int): Int {
        val l = luminance(background)
        val contrastWhite = 1.05f / (l + 0.05f)
        val contrastBlack = (l + 0.05f) / 0.05f
        return if (contrastWhite >= contrastBlack) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
    }

    fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
}
