package com.powermax.patchstudio.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.runtime.State
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.random.Random

@Composable
fun EqualizerBars(
    color: Color,
    modifier: Modifier = Modifier,
    bars: Int = 5,
    playing: Boolean = true,
) {
    val count = bars.coerceAtLeast(1)
    val transition = rememberInfiniteTransition(label = "equalizer")
    val seeds = remember(count) {
        val r = Random(count * 31 + 5)
        List(count) { Triple(0.15f + r.nextFloat() * 0.3f, 0.6f + r.nextFloat() * 0.4f, 380 + r.nextInt(420)) }
    }
    val heights: List<State<Float>> = seeds.mapIndexed { i, (low, high, dur) ->
        transition.animateFloat(
            initialValue = low,
            targetValue = high,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = dur, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "bar$i",
        )
    }
    val activity = animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = tween(450),
        label = "eqActivity",
    )

    Canvas(modifier.defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)) {
        val gap = size.width / (count * 4f)
        val barW = (size.width - gap * (count - 1)) / count
        val minH = barW.coerceAtMost(size.height)
        for (i in 0 until count) {
            val f = 0.12f + (heights[i].value - 0.12f) * activity.value
            val h = (size.height * f).coerceAtLeast(minH)
            val x = i * (barW + gap)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(color, color.copy(alpha = 0.45f)),
                    startY = size.height - h,
                    endY = size.height,
                ),
                topLeft = Offset(x, size.height - h),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f),
            )
        }
    }
}
