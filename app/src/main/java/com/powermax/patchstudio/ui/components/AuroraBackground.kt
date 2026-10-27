package com.powermax.patchstudio.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.powermax.patchstudio.ui.theme.PsColors
import com.powermax.patchstudio.ui.theme.PsGradients
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private const val TwoPi = (2.0 * PI).toFloat()

private class Star(val x: Float, val y: Float, val phase: Float, val size: Float)

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    colors: List<Color> = PsGradients.Aurora,
    content: @Composable BoxScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val t1 = transition.animateFloat(0f, TwoPi, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "t1")
    val t2 = transition.animateFloat(0f, TwoPi, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "t2")
    val t3 = transition.animateFloat(0f, TwoPi, infiniteRepeatable(tween(22000, easing = LinearEasing)), label = "t3")
    val t4 = transition.animateFloat(0f, TwoPi, infiniteRepeatable(tween(16000, easing = LinearEasing)), label = "t4")
    val twinkle = transition.animateFloat(0f, TwoPi, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "twinkle")
    val stars = remember {
        val r = Random(7)
        List(46) { Star(r.nextFloat(), r.nextFloat(), r.nextFloat() * TwoPi, 0.6f + r.nextFloat() * 1.4f) }
    }
    val palette = if (colors.isEmpty()) PsGradients.Aurora else colors

    Box(
        modifier = modifier
            .background(PsColors.Background)
            .drawBehind {
                val w = size.width
                val h = size.height
                val big = maxOf(w, h)
                val times = floatArrayOf(t1.value, t2.value, t3.value, t4.value)
                // (centre x, centre y, rayon relatif, déphasages) par blob
                for (i in 0 until 4) {
                    val c = palette[i % palette.size]
                    val t = times[i]
                    val cx = w * (0.5f + 0.38f * sin(t + i * 1.7f))
                    val cy = h * (0.5f + 0.36f * sin(2f * t + i * 2.3f))
                    val radius = big * (0.55f + 0.08f * i)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(c.copy(alpha = 0.34f), c.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = radius,
                        ),
                        radius = radius,
                        center = Offset(cx, cy),
                    )
                }
                val tw = twinkle.value
                for (s in stars) {
                    val a = 0.12f + 0.55f * (0.5f + 0.5f * sin(tw + s.phase))
                    drawCircle(
                        color = Color.White.copy(alpha = a),
                        radius = s.size * density,
                        center = Offset(s.x * w, s.y * h),
                    )
                }
            },
        content = content,
    )
}
