package com.powermax.patchstudio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.ui.motion.PsMotion
import com.powermax.patchstudio.ui.theme.PsColors

private data class Hsv(val h: Float, val s: Float, val v: Float) {
    fun toColor(): Color = Color.hsv(h.coerceIn(0f, 359.99f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
}

private fun Color.toHsv(): Hsv {
    val out = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), out)
    return Hsv(out[0], out[1], out[2])
}

private fun Modifier.fractionDrag(onFraction: (Float) -> Unit): Modifier = composed {
    val latest by rememberUpdatedState(onFraction)
    pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val w = size.width.toFloat().coerceAtLeast(1f)
            latest((down.position.x / w).coerceIn(0f, 1f))
            drag(down.id) { change ->
                latest((change.position.x / w).coerceIn(0f, 1f))
                change.consume()
            }
        }
    }
}

@Composable
fun HueSlider(color: Color, onColorChange: (Color) -> Unit, modifier: Modifier = Modifier) {
    var hsv by remember { mutableStateOf(color.toHsv()) }
    var lastEmitted by remember { mutableStateOf(color) }
    LaunchedEffect(color) {
        if (color != lastEmitted) {
            hsv = color.toHsv()
            lastEmitted = color
        }
    }
    val latest by rememberUpdatedState(onColorChange)
    fun emit(next: Hsv) {
        hsv = next
        val c = next.toColor()
        lastEmitted = c
        latest(c)
    }

    val bump = remember { Animatable(1f) }
    LaunchedEffect(hsv) {
        bump.snapTo(1.18f)
        bump.animateTo(1f, PsMotion.Spring)
    }
    val current = hsv.toColor()
    val hueColors = remember { List(7) { Color.hsv(it * 60f % 360f, 1f, 1f) } }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = bump.value
                        scaleY = bump.value
                    }
                    .clip(CircleShape)
                    .background(current),
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text("TEINTE", style = MaterialTheme.typography.labelSmall, color = PsColors.TextSecondary)
                val argb = current.toArgb() and 0xFFFFFF
                Text(
                    "#" + argb.toString(16).padStart(6, '0').uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = PsColors.TextPrimary,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        GradientBar(
            colors = hueColors,
            fraction = hsv.h / 360f,
            thumbColor = Color.hsv(hsv.h.coerceIn(0f, 359.99f), 1f, 1f),
            onFraction = { f -> emit(hsv.copy(h = f * 360f)) },
        )
        Spacer(Modifier.height(12.dp))
        Text("LUMINOSITÉ", style = MaterialTheme.typography.labelSmall, color = PsColors.TextSecondary)
        Spacer(Modifier.height(6.dp))
        val vMin = 0.25f
        GradientBar(
            colors = listOf(
                Color.hsv(hsv.h.coerceIn(0f, 359.99f), hsv.s.coerceIn(0f, 1f), vMin),
                Color.hsv(hsv.h.coerceIn(0f, 359.99f), hsv.s.coerceIn(0f, 1f), 1f),
            ),
            fraction = ((hsv.v - vMin) / (1f - vMin)).coerceIn(0f, 1f),
            thumbColor = current,
            onFraction = { f -> emit(hsv.copy(v = vMin + f * (1f - vMin))) },
        )
    }
}

@Composable
private fun GradientBar(
    colors: List<Color>,
    fraction: Float,
    thumbColor: Color,
    onFraction: (Float) -> Unit,
) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(32.dp)
            .fractionDrag(onFraction),
    ) {
        val barH = 14.dp.toPx()
        val top = (size.height - barH) / 2f
        drawRoundRect(
            brush = Brush.horizontalGradient(colors),
            topLeft = Offset(0f, top),
            size = androidx.compose.ui.geometry.Size(size.width, barH),
            cornerRadius = CornerRadius(barH / 2f),
        )
        val r = 12.dp.toPx()
        val cx = r + (size.width - 2 * r) * fraction
        val cy = size.height / 2f
        drawCircle(Color.Black.copy(alpha = 0.35f), radius = r + 2.dp.toPx(), center = Offset(cx, cy + 1.dp.toPx()))
        drawCircle(Color.White, radius = r, center = Offset(cx, cy))
        drawCircle(thumbColor, radius = r - 3.5.dp.toPx(), center = Offset(cx, cy))
        drawCircle(Color.White.copy(alpha = 0.5f), radius = r, center = Offset(cx, cy), style = Stroke(1.dp.toPx()))
    }
}
