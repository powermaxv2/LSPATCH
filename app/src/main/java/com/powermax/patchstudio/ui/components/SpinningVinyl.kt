package com.powermax.patchstudio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.CubicBezierEasing

private val EaseIn = CubicBezierEasing(0.5f, 0f, 1f, 1f)

@Composable
fun SpinningVinyl(accent: Color, modifier: Modifier = Modifier, spinning: Boolean = true) {
    val angle = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        if (spinning) {
            angle.animateTo(angle.value + 90f, tween(900, easing = EaseIn))
            while (true) {
                angle.animateTo(angle.value + 360f, tween(1800, easing = LinearEasing))
            }
        } else {
            angle.animateTo(angle.value + 140f, tween(1400, easing = FastOutSlowInEasing))
        }
    }

    Canvas(modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).aspectRatio(1f)) {
        val c = center
        val r = size.minDimension / 2f
        rotate(angle.value, c) {
            drawCircle(Color(0xFF0A0A10), radius = r, center = c)
            // sillons
            var rr = r * 0.97f
            var k = 0
            while (rr > r * 0.40f) {
                drawCircle(
                    color = Color.White.copy(alpha = if (k % 3 == 0) 0.09f else 0.04f),
                    radius = rr,
                    center = c,
                    style = Stroke(width = 0.6f),
                )
                rr -= r * 0.035f
                k++
            }
            // reflets coniques
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent, Color.White.copy(alpha = 0.16f), Color.Transparent,
                        Color.Transparent, Color.White.copy(alpha = 0.16f), Color.Transparent,
                    ),
                    center = c,
                ),
                radius = r,
                center = c,
            )
            // étiquette
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent, accent.copy(alpha = 0.7f)),
                    center = c,
                    radius = r * 0.36f,
                ),
                radius = r * 0.36f,
                center = c,
            )
            drawCircle(Color.White.copy(alpha = 0.55f), radius = r * 0.05f, center = Offset(c.x, c.y - r * 0.24f))
            drawCircle(Color(0xFF05060A), radius = r * 0.06f, center = c)
        }
        drawCircle(Color.White.copy(alpha = 0.10f), radius = r, center = c, style = Stroke(1f))
    }
}
