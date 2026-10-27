package com.powermax.patchstudio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.ui.motion.PsMotion
import com.powermax.patchstudio.ui.motion.bounceClick
import com.powermax.patchstudio.ui.theme.PsColors
import com.powermax.patchstudio.ui.theme.PsShapes
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun ToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val iconPop = remember { Animatable(1f) }
    val iconSpin = remember { Animatable(0f) }
    LaunchedEffect(checked) {
        iconPop.snapTo(0.6f)
        iconSpin.snapTo(if (checked) -25f else 25f)
        iconPop.animateTo(1f, PsMotion.Spring)
        iconSpin.animateTo(0f, PsMotion.Spring)
    }
    val iconTint by animateColorAsState(
        targetValue = if (checked) PsColors.Secondary else PsColors.TextSecondary,
        animationSpec = PsMotion.tween(300),
        label = "iconTint",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .bounceClick(haptic = true) { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(PsShapes.Card)
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = iconPop.value
                            scaleY = iconPop.value
                            rotationZ = iconSpin.value
                        },
                )
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = PsColors.TextPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PsColors.TextSecondary)
            }
        }
        Spacer(Modifier.width(12.dp))
        PsSwitch(checked)
    }
}

/** Switch custom : pouce à ressort qui s'étire pendant le mouvement. */
@Composable
private fun PsSwitch(checked: Boolean) {
    val progress = animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = PsMotion.Spring,
        label = "switch",
    )
    Canvas(Modifier.size(width = 54.dp, height = 32.dp)) {
        val p = progress.value
        val pc = p.coerceIn(0f, 1f)
        val w = size.width
        val h = size.height
        val pad = 4.dp.toPx()
        val baseThumb = h - 2 * pad
        val stretch = 10.dp.toPx() * sin(PI.toFloat() * pc)
        val thumbW = baseThumb + stretch
        val travel = w - 2 * pad - baseThumb
        val left = pad + travel * p - stretch * pc
        // piste
        val offColor = PsColors.SurfaceHigh
        drawRoundRect(color = offColor, size = Size(w, h), cornerRadius = CornerRadius(h / 2f))
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(PsColors.Primary, PsColors.Secondary)),
            size = Size(w, h),
            cornerRadius = CornerRadius(h / 2f),
            alpha = pc,
        )
        // halo du pouce
        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f * pc),
            topLeft = Offset(left - 3.dp.toPx(), pad - 3.dp.toPx()),
            size = Size(thumbW + 6.dp.toPx(), baseThumb + 6.dp.toPx()),
            cornerRadius = CornerRadius((baseThumb + 6.dp.toPx()) / 2f),
        )
        // pouce
        drawRoundRect(
            color = lerp(PsColors.TextSecondary, Color.White, pc),
            topLeft = Offset(left, pad),
            size = Size(thumbW, baseThumb),
            cornerRadius = CornerRadius(baseThumb / 2f),
        )
    }
}
