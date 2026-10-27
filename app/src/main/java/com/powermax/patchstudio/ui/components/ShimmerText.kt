package com.powermax.patchstudio.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.text.TextStyle
import com.powermax.patchstudio.ui.theme.PsGradients

@Composable
fun ShimmerText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    colors: List<Color> = PsGradients.Brand,
) {
    val transition = rememberInfiniteTransition(label = "shimmerText")
    val sweep = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2800, easing = LinearEasing)), label = "sweep")
    val base = if (colors.size >= 2) colors else listOf(colors.firstOrNull() ?: Color.White, Color.White)

    Text(
        text = text,
        style = style.copy(color = Color.White),
        modifier = modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val w = size.width
                val h = size.height
                // teinte dégradée (conserve l'alpha du texte)
                drawRect(
                    brush = Brush.linearGradient(base, start = Offset.Zero, end = Offset(w, h * 0.4f)),
                    blendMode = BlendMode.SrcIn,
                )
                // reflet qui balaie
                val band = w * 0.45f + 1f
                val x = -band + (w + 2f * band) * sweep.value
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.9f), Color.Transparent),
                        start = Offset(x - band, 0f),
                        end = Offset(x + band, h * 0.5f),
                    ),
                    blendMode = BlendMode.SrcAtop,
                )
            },
    )
}
