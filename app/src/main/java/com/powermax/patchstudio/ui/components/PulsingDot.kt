package com.powermax.patchstudio.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PulsingDot(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    val transition = rememberInfiniteTransition(label = "pulsingDot")
    val wave = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "wave")
    Spacer(
        modifier = modifier
            .size(size)
            .drawBehind {
                val r = this.size.minDimension / 2f
                for (k in 0..1) {
                    val p = (wave.value + k * 0.5f) % 1f
                    val a = (1f - p) * 0.55f
                    drawCircle(
                        color = color.copy(alpha = a),
                        radius = r * (1f + 2.2f * p),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
                drawCircle(color = color.copy(alpha = 0.28f), radius = r * 1.7f)
                drawCircle(color = color, radius = r)
            },
    )
}
