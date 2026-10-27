package com.powermax.patchstudio.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

object PsMotion {
    val Spring: SpringSpec<Float> = spring(dampingRatio = 0.62f, stiffness = 380f)
    val SpringSnappy: SpringSpec<Float> = spring(dampingRatio = 0.7f, stiffness = 900f)
    const val StaggerMs: Int = 45
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> tween(durationMs: Int = 420): TweenSpec<T> =
        TweenSpec(durationMillis = durationMs, delay = 0, easing = EmphasizedEasing)
}

/** Apparition décalée (fade + slide 24dp + scale 0.96), une seule fois. */
fun Modifier.staggeredEntrance(index: Int, baseDelayMs: Int = 0): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val wait = (baseDelayMs + (index.coerceAtLeast(0) * PsMotion.StaggerMs).coerceAtMost(900))
        delay(wait.toLong())
        progress.animateTo(1f, tween(durationMillis = 560, easing = PsMotion.EmphasizedEasing))
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 24.dp.toPx()
        val s = 0.96f + 0.04f * p
        scaleX = s
        scaleY = s
    }
}

/** Pression ressort (scale 0.95) + haptique, conserve le clic. */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    haptic: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.95f else 1f,
        animationSpec = PsMotion.SpringSnappy,
        label = "bounceScale",
    )
    val feedback = LocalHapticFeedback.current
    val latestClick by rememberUpdatedState(onClick)
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
            if (haptic) feedback.performHapticFeedback(HapticFeedbackType.LongPress)
            latestClick()
        }
}

/** Halo coloré doux autour de l'élément (couches de rectangles arrondis dégradées). */
fun Modifier.glow(color: Color, radius: Dp = 24.dp, animated: Boolean = true): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "glow")
    val pulse = transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(durationMillis = 2200, easing = PsMotion.EmphasizedEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )
    drawBehind {
        val k = if (animated) pulse.value else 1f
        val r = radius.toPx()
        val layers = 7
        val base = minOf(size.width, size.height) / 2f
        val corner = minOf(base, 28.dp.toPx())
        for (i in layers downTo 1) {
            val f = i / layers.toFloat()
            val e = r * f
            val a = 0.16f * k * (1f - f) * (1f - f) + 0.02f * k
            drawRoundRect(
                color = color.copy(alpha = a.coerceIn(0f, 1f)),
                topLeft = Offset(-e, -e),
                size = Size(size.width + 2 * e, size.height + 2 * e),
                cornerRadius = CornerRadius(corner + e),
            )
        }
    }
}
