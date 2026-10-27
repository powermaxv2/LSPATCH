package com.powermax.patchstudio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.ui.motion.PsMotion
import com.powermax.patchstudio.ui.motion.bounceClick
import com.powermax.patchstudio.ui.theme.PsColors
import com.powermax.patchstudio.ui.theme.PsShapes
import kotlinx.coroutines.launch
import kotlin.math.hypot

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color = PsColors.Primary,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "glassBorder")
    val angle = transition.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "angle")
    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val clickable = if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier
    val tiltInput = if (onClick != null) {
        Modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var pos = down.position
                while (true) {
                    val nx = ((pos.x / size.width.coerceAtLeast(1)) - 0.5f) * 2f
                    val ny = ((pos.y / size.height.coerceAtLeast(1)) - 0.5f) * 2f
                    scope.launch {
                        tiltY.snapTo(nx.coerceIn(-1f, 1f))
                        tiltX.snapTo(ny.coerceIn(-1f, 1f))
                    }
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    pos = change.position
                }
                scope.launch { tiltY.animateTo(0f, PsMotion.Spring) }
                scope.launch { tiltX.animateTo(0f, PsMotion.Spring) }
            }
        }
    } else Modifier

    Column(
        modifier = modifier
            .graphicsLayer {
                rotationX = -tiltX.value * 5f
                rotationY = tiltY.value * 5f
                cameraDistance = 14f * density
            }
            .then(clickable)
            .then(tiltInput)
            .clip(PsShapes.Card)
            .background(
                Brush.verticalGradient(listOf(Color(0xB8141A2E), Color(0xB30A0D19)))
            )
            .drawBehind {
                // lueur de l'accent en haut à gauche
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width * 0.15f, 0f),
                        radius = size.width * 0.9f,
                    )
                )
            }
            .drawWithCache {
                val w = size.width
                val h = size.height
                val r = 24.dp.toPx()
                val bw = 1.3.dp.toPx()
                val outer = Path().apply { addRoundRect(RoundRect(0f, 0f, w, h, CornerRadius(r))) }
                val inner = Path().apply {
                    addRoundRect(RoundRect(bw, bw, w - bw, h - bw, CornerRadius(r - bw)))
                }
                val center = Offset(w / 2f, h / 2f)
                val sweep = Brush.sweepGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.95f),
                        accent.copy(alpha = 0.05f),
                        PsColors.Secondary.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.04f),
                        accent.copy(alpha = 0.95f),
                    ),
                    center = center,
                )
                val rad = hypot(w, h) / 2f * 1.05f
                onDrawWithContent {
                    drawContent()
                    clipPath(outer) {
                        clipPath(inner, clipOp = ClipOp.Difference) {
                            rotate(angle.value, center) {
                                drawCircle(brush = sweep, radius = rad, center = center)
                            }
                        }
                    }
                }
            }
            .padding(16.dp),
        content = content,
    )
}
