package com.powermax.patchstudio.hook

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import com.powermax.patchstudio.core.ColorMath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Voile "aurore" : 3 halos radiaux (accent, teinte ±40°) qui dérivent lentement. */
internal class AuroraDrawable(accent: Int, intensity: Float) : Drawable() {

    private val colors: IntArray
    private val paints = Array(3) { Paint(Paint.ANTI_ALIAS_FLAG) }
    private val matrix = Matrix()
    private var radius = 0f
    private var phase = 0f
    private var globalAlpha = 255

    private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 12_000L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.RESTART
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidateSelf()
        }
    }

    init {
        val hsl = ColorMath.toHsl(accent)
        colors = intArrayOf(
            accent,
            ColorMath.fromHsl(hsl[0] + 40f, hsl[1], hsl[2]),
            ColorMath.fromHsl(hsl[0] - 40f, hsl[1], hsl[2]),
        )
        globalAlpha = (intensity.coerceIn(0f, 1f) * 255f * 0.5f).toInt().coerceIn(0, 255)
    }

    fun start() {
        try {
            if (animator.isPaused) animator.resume() else if (!animator.isStarted) animator.start()
        } catch (_: Throwable) {
        }
    }

    fun pause() {
        try {
            if (animator.isRunning) animator.pause()
        } catch (_: Throwable) {
        }
    }

    fun stop() {
        try {
            animator.cancel()
        } catch (_: Throwable) {
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        radius = max(bounds.width(), bounds.height()) * 0.55f
        if (radius <= 0f) return
        for (i in 0 until 3) {
            paints[i].shader = RadialGradient(
                0f, 0f, radius,
                intArrayOf(colors[i], ColorMath.withAlpha(colors[i], 0)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (radius <= 0f || b.isEmpty) return
        val w = b.width().toFloat()
        val h = b.height().toFloat()
        val t = phase * 2f * PI.toFloat()
        for (i in 0 until 3) {
            val off = i * 2.1f
            val cx = b.left + w * (0.5f + 0.38f * cos(t + off))
            val cy = b.top + h * (0.35f + 0.3f * sin(t * (if (i == 1) 2f else 1f) + off))
            matrix.setTranslate(cx, cy)
            val paint = paints[i]
            paint.shader?.setLocalMatrix(matrix)
            paint.alpha = globalAlpha
            canvas.drawCircle(cx, cy, radius, paint)
        }
    }

    override fun setAlpha(alpha: Int) {
        globalAlpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        for (p in paints) p.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
