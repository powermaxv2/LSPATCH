package com.powermax.patchstudio.hook

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import com.powermax.patchstudio.core.ColorMath
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Surface "Liquid Glass" (verre liquide translucide façon iOS 26) :
 *  - corps en verre givré translucide, légèrement teinté par l'accent ;
 *  - ménisque spéculaire (reflet lumineux courbe en partie haute) ;
 *  - réfraction chromatique discrète dans les coins ;
 *  - bordure spéculaire diagonale haute netteté (brillance haut-gauche → reflet bas-droite) ;
 *  - réaction lumineuse subtile à l'appui (`state_pressed`).
 *
 * Aucune allocation dans [draw] : tous les shaders sont précalculés dans [onBoundsChange].
 */
internal class LiquidGlassDrawable(
    surfaceColor: Int,
    accentColor: Int,
    intensity: Float,
    tinted: Boolean,
    private val style: GlassStyle,
    private var cornerRadiusPx: Float,
    private val density: Float,
    private val insetHPx: Float = 0f,
    private val insetVPx: Float = 0f,
) : Drawable() {

    enum class GlassStyle { CAPSULE, CARD, HEADER }

    private var surfaceColor: Int = surfaceColor
    private var accentColor: Int = accentColor
    private var intensity: Float = intensity.coerceIn(0.25f, 1f)
    private var tinted: Boolean = tinted
    private var pressed: Boolean = false
    private var globalAlpha: Int = 255

    private val rect = RectF()
    private val strokeRect = RectF()
    private val sheenRect = RectF()
    private val clipPath = Path()
    private var resolvedRadius = 0f

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val refractionPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.STROKE
        strokeWidth = (1.15f * density).coerceAtLeast(1.5f)
    }
    private val innerHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.style = Paint.Style.STROKE
        strokeWidth = (0.75f * density).coerceAtLeast(1f)
    }
    private val pressedOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
    }

    fun updateStyle(
        surfaceColor: Int,
        accentColor: Int,
        intensity: Float,
        tinted: Boolean,
        cornerRadiusPx: Float = this.cornerRadiusPx,
    ) {
        val clamped = intensity.coerceIn(0.25f, 1f)
        if (
            this.surfaceColor == surfaceColor &&
            this.accentColor == accentColor &&
            this.intensity == clamped &&
            this.tinted == tinted &&
            this.cornerRadiusPx == cornerRadiusPx
        ) {
            return
        }
        this.surfaceColor = surfaceColor
        this.accentColor = accentColor
        this.intensity = clamped
        this.tinted = tinted
        this.cornerRadiusPx = cornerRadiusPx
        rebuildGeometryAndShaders(bounds)
        invalidateSelf()
    }

    override fun isStateful(): Boolean = true

    override fun onStateChange(state: IntArray): Boolean {
        val nowPressed = state.any { it == android.R.attr.state_pressed }
        if (nowPressed == pressed) return false
        pressed = nowPressed
        invalidateSelf()
        return true
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        rebuildGeometryAndShaders(bounds)
    }

    private fun rebuildGeometryAndShaders(b: Rect) {
        if (b.isEmpty) return
        val maxH = (b.width() * 0.15f).coerceAtMost(insetHPx)
        val maxV = (b.height() * 0.15f).coerceAtMost(insetVPx)
        rect.set(b.left + maxH, b.top + maxV, b.right - maxH, b.bottom - maxV)
        if (rect.width() <= 0f || rect.height() <= 0f) return

        val halfStroke = rimPaint.strokeWidth / 2f
        strokeRect.set(
            rect.left + halfStroke,
            rect.top + halfStroke,
            rect.right - halfStroke,
            rect.bottom - halfStroke,
        )

        resolvedRadius = when (style) {
            GlassStyle.HEADER -> 0f
            GlassStyle.CAPSULE -> min(cornerRadiusPx, rect.height() / 2f)
            GlassStyle.CARD -> min(cornerRadiusPx, min(rect.width(), rect.height()) / 2f)
        }

        clipPath.reset()
        if (resolvedRadius > 0f) {
            clipPath.addRoundRect(rect, resolvedRadius, resolvedRadius, Path.Direction.CW)
        } else {
            clipPath.addRect(rect, Path.Direction.CW)
        }

        sheenRect.set(rect.left, rect.top, rect.right, rect.top + rect.height() * 0.46f)

        val k = intensity
        // Plus l'intensité est élevée, plus le verre est translucide et lumineux (tout en restant lisible).
        val baseTint = if (tinted) ColorMath.blend(surfaceColor, accentColor, 0.16f * k) else surfaceColor
        val topAlpha = (224 - 44 * k).roundToInt().coerceIn(165, 232)
        val midAlpha = (212 - 52 * k).roundToInt().coerceIn(150, 224)
        val botAlpha = (232 - 36 * k).roundToInt().coerceIn(180, 238)

        val topGlass = ColorMath.withAlpha(ColorMath.blend(baseTint, Color.WHITE, 0.11f * k), topAlpha)
        val midGlass = ColorMath.withAlpha(baseTint, midAlpha)
        val botGlass = ColorMath.withAlpha(ColorMath.blend(baseTint, Color.BLACK, 0.22f), botAlpha)

        bodyPaint.shader = LinearGradient(
            rect.left, rect.top, rect.left, rect.bottom,
            intArrayOf(topGlass, midGlass, botGlass),
            floatArrayOf(0f, 0.48f, 1f),
            Shader.TileMode.CLAMP,
        )

        // Ménisque spéculaire en haut de la capsule/carte.
        val sheenTopAlpha = ((0.10f + 0.14f * k) * 255f).roundToInt().coerceIn(18, 76)
        sheenPaint.shader = LinearGradient(
            sheenRect.left, sheenRect.top, sheenRect.left, sheenRect.bottom,
            intArrayOf(
                ColorMath.withAlpha(Color.WHITE, sheenTopAlpha),
                ColorMath.withAlpha(Color.WHITE, (sheenTopAlpha * 0.28f).roundToInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )

        // Réfraction chromatique discrète (reflet d'accent à travers le bord du verre).
        if (tinted) {
            val glowAlpha = ((0.08f + 0.14f * k) * 255f).roundToInt().coerceIn(14, 64)
            val glowRadius = maxOf(rect.width(), rect.height()) * 0.72f
            refractionPaint.shader = RadialGradient(
                rect.left + rect.width() * 0.18f,
                rect.top + rect.height() * 0.12f,
                glowRadius.coerceAtLeast(1f),
                intArrayOf(ColorMath.withAlpha(accentColor, glowAlpha), Color.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        } else {
            refractionPaint.shader = null
        }

        // Bordure spéculaire diagonale (signature visuelle Liquid Glass).
        val rimPeak = ((0.30f + 0.38f * k) * 255f).roundToInt().coerceIn(65, 195)
        val rimMidColor = if (tinted) ColorMath.blend(Color.WHITE, accentColor, 0.55f) else Color.WHITE
        val rimMid = ((0.12f + 0.16f * k) * 255f).roundToInt().coerceIn(28, 86)
        val rimLow = ((0.05f + 0.06f * k) * 255f).roundToInt().coerceIn(12, 34)
        val rimTail = ((0.16f + 0.22f * k) * 255f).roundToInt().coerceIn(36, 110)

        rimPaint.shader = LinearGradient(
            strokeRect.left, strokeRect.top, strokeRect.right, strokeRect.bottom,
            intArrayOf(
                ColorMath.withAlpha(Color.WHITE, rimPeak),
                ColorMath.withAlpha(rimMidColor, rimMid),
                ColorMath.withAlpha(Color.WHITE, rimLow),
                ColorMath.withAlpha(rimMidColor, rimTail),
            ),
            floatArrayOf(0f, 0.28f, 0.70f, 1f),
            Shader.TileMode.CLAMP,
        )

        val innerTopAlpha = ((0.16f + 0.20f * k) * 255f).roundToInt().coerceIn(32, 100)
        innerHighlightPaint.shader = LinearGradient(
            rect.left, rect.top, rect.right, rect.top,
            intArrayOf(
                Color.TRANSPARENT,
                ColorMath.withAlpha(Color.WHITE, innerTopAlpha),
                ColorMath.withAlpha(rimMidColor, (innerTopAlpha * 0.7f).roundToInt()),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0.06f, 0.30f, 0.72f, 0.94f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun draw(canvas: Canvas) {
        if (rect.width() <= 0f || rect.height() <= 0f) return
        val alphaScale = globalAlpha / 255f
        bodyPaint.alpha = (255 * alphaScale).roundToInt()
        sheenPaint.alpha = (255 * alphaScale).roundToInt()
        refractionPaint.alpha = (255 * alphaScale).roundToInt()
        rimPaint.alpha = (255 * alphaScale).roundToInt()
        innerHighlightPaint.alpha = (255 * alphaScale).roundToInt()

        val save = canvas.save()
        canvas.clipPath(clipPath)
        if (resolvedRadius > 0f) {
            canvas.drawRoundRect(rect, resolvedRadius, resolvedRadius, bodyPaint)
        } else {
            canvas.drawRect(rect, bodyPaint)
        }
        if (refractionPaint.shader != null) {
            canvas.drawRect(rect, refractionPaint)
        }
        canvas.drawRect(sheenRect, sheenPaint)
        if (pressed) {
            pressedOverlayPaint.alpha = ((0.08f + 0.08f * intensity) * 255f * alphaScale).roundToInt()
            canvas.drawRect(rect, pressedOverlayPaint)
        }
        canvas.restoreToCount(save)

        when (style) {
            GlassStyle.HEADER -> {
                val y = rect.bottom - rimPaint.strokeWidth / 2f
                canvas.drawLine(rect.left, y, rect.right, y, rimPaint)
            }
            GlassStyle.CAPSULE, GlassStyle.CARD -> {
                canvas.drawRoundRect(strokeRect, resolvedRadius, resolvedRadius, rimPaint)
                // Micro-reflet supérieur pour l'effet lentille polie.
                val inset = rimPaint.strokeWidth * 1.4f
                val topY = rect.top + inset
                canvas.drawLine(
                    rect.left + resolvedRadius.coerceAtLeast(inset),
                    topY,
                    rect.right - resolvedRadius.coerceAtLeast(inset),
                    topY,
                    innerHighlightPaint,
                )
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        globalAlpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun getAlpha(): Int = globalAlpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bodyPaint.colorFilter = colorFilter
        rimPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
