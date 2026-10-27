package com.powermax.patchstudio.ui.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/** Mouvements de l'app : courts, utiles, jamais en boucle. */
object PsMotion {
    const val DurationShort = 150
    const val DurationMedium = 300
    val Standard: Easing = FastOutSlowInEasing

    /**
     * Transition « shared axis X » façon Material : l'écran sortant s'efface vite,
     * l'entrant glisse de [offsetPx] et apparaît en fondu.
     */
    fun sharedAxisX(forward: Boolean, offsetPx: Int): ContentTransform {
        val sign = if (forward) 1 else -1
        val enter = slideInHorizontally(tween(DurationMedium, easing = Standard)) { sign * offsetPx } +
            fadeIn(tween(210, delayMillis = 90, easing = LinearOutSlowInEasing))
        val exit = slideOutHorizontally(tween(DurationMedium, easing = Standard)) { -sign * offsetPx } +
            fadeOut(tween(90, easing = FastOutSlowInEasing))
        return enter togetherWith exit
    }

    /** Pour les options qui apparaissent sous un réglage. */
    val Expand: EnterTransition =
        expandVertically(tween(DurationMedium, easing = Standard)) + fadeIn(tween(220, delayMillis = 60))
    val Collapse: ExitTransition =
        shrinkVertically(tween(250, easing = Standard)) + fadeOut(tween(DurationShort))
}

/** Légère réduction d'échelle pendant l'appui (à combiner avec un clickable partageant [interactionSource]). */
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.98f): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(PsMotion.DurationShort, easing = PsMotion.Standard),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
