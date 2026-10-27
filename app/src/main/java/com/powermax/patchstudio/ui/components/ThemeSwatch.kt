package com.powermax.patchstudio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.ui.motion.PsMotion
import com.powermax.patchstudio.ui.motion.bounceClick
import com.powermax.patchstudio.ui.theme.PsColors

@Composable
fun ThemeSwatch(
    name: String,
    accent: Color,
    background: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = animateFloatAsState(if (selected) 1.06f else 1f, PsMotion.Spring, label = "swatchScale")
    val ring = animateFloatAsState(if (selected) 1f else 0f, PsMotion.tween(350), label = "swatchRing")
    val check = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        if (selected) check.animateTo(1f, PsMotion.Spring) else check.snapTo(0f)
    }

    Box(
        modifier = modifier
            .size(width = 96.dp, height = 128.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .bounceClick(onClick = onClick),
    ) {
        Canvas(Modifier.size(width = 96.dp, height = 128.dp)) {
            val w = size.width
            val h = size.height
            val corner = CornerRadius(16.dp.toPx())
            drawRoundRect(color = background, cornerRadius = corner)
            // barre du haut
            val pad = 10.dp.toPx()
            drawRoundRect(
                color = accent.copy(alpha = 0.9f),
                topLeft = Offset(pad, pad),
                size = Size(w * 0.38f, 5.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx()),
            )
            drawCircle(accent.copy(alpha = 0.6f), radius = 3.dp.toPx(), center = Offset(w - pad - 3.dp.toPx(), pad + 2.5.dp.toPx()))
            // cover
            val cover = w - 2 * pad - 14.dp.toPx()
            val cx = pad + 7.dp.toPx()
            val cy = pad + 18.dp.toPx()
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(accent, accent.copy(alpha = 0.35f)),
                    start = Offset(cx, cy),
                    end = Offset(cx + cover, cy + cover),
                ),
                topLeft = Offset(cx, cy),
                size = Size(cover, cover),
                cornerRadius = CornerRadius(10.dp.toPx()),
            )
            // lignes
            val ly = cy + cover + 10.dp.toPx()
            drawRoundRect(
                color = Color.White.copy(alpha = 0.75f),
                topLeft = Offset(pad, ly),
                size = Size(w * 0.55f, 4.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(pad, ly + 9.dp.toPx()),
                size = Size(w * 0.38f, 4.dp.toPx()),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
            // contour
            drawRoundRect(
                color = Color.White.copy(alpha = 0.10f),
                cornerRadius = corner,
                style = Stroke(1.dp.toPx()),
            )
            // anneau dégradé de sélection
            val r = ring.value
            if (r > 0.01f) {
                val sw = 2.5.dp.toPx()
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(PsColors.Primary, PsColors.Secondary, PsColors.Tertiary),
                        start = Offset.Zero,
                        end = Offset(w, h),
                    ),
                    topLeft = Offset(sw / 2f, sw / 2f),
                    size = Size(w - sw, h - sw),
                    cornerRadius = CornerRadius(16.dp.toPx() - sw / 2f),
                    style = Stroke(sw),
                    alpha = r,
                )
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 6.dp, vertical = 8.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(22.dp)
                .graphicsLayer {
                    scaleX = check.value
                    scaleY = check.value
                    alpha = check.value.coerceIn(0f, 1f)
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(22.dp)) {
                drawCircle(
                    brush = Brush.linearGradient(listOf(PsColors.Primary, PsColors.Tertiary)),
                )
            }
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}
