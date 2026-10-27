package com.powermax.patchstudio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.ui.motion.PsMotion
import com.powermax.patchstudio.ui.theme.PsColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedLogo(modifier: Modifier = Modifier, size: Dp = 96.dp) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, PsMotion.Spring) }

    val transition = rememberInfiniteTransition(label = "logo")
    val spin = transition.animateFloat(0f, 360f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "spin")
    val phase = transition.animateFloat(
        0f, (2.0 * PI).toFloat(), infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "phase",
    )
    val breathe = transition.animateFloat(
        0.92f, 1.08f,
        infiniteRepeatable(tween(2200, easing = PsMotion.EmphasizedEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val path = remember { Path() }

    Canvas(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = appear.value
                scaleY = appear.value
            },
    ) {
        val c = center
        val r = this.size.minDimension / 2f
        val stroke = r * 0.045f
        val sweepColors = listOf(
            PsColors.Primary, PsColors.Secondary, PsColors.Tertiary, PsColors.Primary,
        )
        // anneau extérieur (sens horaire)
        rotate(spin.value, c) {
            drawArc(
                brush = Brush.sweepGradient(sweepColors, c),
                startAngle = 0f, sweepAngle = 250f, useCenter = false,
                topLeft = Offset(c.x - r * 0.96f, c.y - r * 0.96f),
                size = Size(r * 1.92f, r * 1.92f),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        // anneau médian (sens inverse)
        rotate(-spin.value * 1.6f, c) {
            drawArc(
                brush = Brush.sweepGradient(sweepColors.reversed(), c),
                startAngle = 40f, sweepAngle = 200f, useCenter = false,
                topLeft = Offset(c.x - r * 0.78f, c.y - r * 0.78f),
                size = Size(r * 1.56f, r * 1.56f),
                style = Stroke(stroke * 0.8f, cap = StrokeCap.Round),
            )
        }
        // anneau intérieur fin (sens horaire, plus rapide)
        rotate(spin.value * 2.2f, c) {
            drawArc(
                color = Color.White.copy(alpha = 0.35f),
                startAngle = 100f, sweepAngle = 120f, useCenter = false,
                topLeft = Offset(c.x - r * 0.62f, c.y - r * 0.62f),
                size = Size(r * 1.24f, r * 1.24f),
                style = Stroke(stroke * 0.6f, cap = StrokeCap.Round),
            )
        }
        // forme centrale qui respire (blob à 3 lobes)
        val base = r * 0.40f * breathe.value
        path.reset()
        val n = 72
        for (i in 0..n) {
            val th = i / n.toFloat() * 2f * PI.toFloat()
            val rad = base * (1f + 0.14f * sin(3f * th + phase.value) + 0.05f * sin(5f * th - phase.value * 2f))
            val x = c.x + rad * cos(th)
            val y = c.y + rad * sin(th)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawCircle(
            brush = Brush.radialGradient(
                listOf(PsColors.Primary.copy(alpha = 0.45f), Color.Transparent),
                center = c, radius = base * 2.2f,
            ),
            radius = base * 2.2f, center = c,
        )
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                listOf(PsColors.Primary, PsColors.Secondary, PsColors.Tertiary),
                start = Offset(c.x - base, c.y - base),
                end = Offset(c.x + base, c.y + base),
            ),
        )
        drawCircle(Color.White.copy(alpha = 0.85f), radius = base * 0.16f, center = c)
    }
}
