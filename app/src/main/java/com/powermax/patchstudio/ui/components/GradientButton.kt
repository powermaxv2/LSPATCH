package com.powermax.patchstudio.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.ui.motion.bounceClick
import com.powermax.patchstudio.ui.motion.glow
import com.powermax.patchstudio.ui.theme.PsGradients
import com.powermax.patchstudio.ui.theme.PsShapes

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    colors: List<Color> = PsGradients.Brand,
) {
    val transition = rememberInfiniteTransition(label = "gradientButton")
    val flow = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "flow")
    val shine = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "shine")
    val palette = if (colors.size >= 2) colors else listOf(colors.firstOrNull() ?: Color.White, Color.White)

    Row(
        modifier = modifier
            .glow(palette.first(), radius = 18.dp)
            .bounceClick(onClick = onClick)
            .clip(PsShapes.Pill)
            .drawWithContent {
                val w = size.width
                val h = size.height
                val shift = flow.value * 2f * w
                drawRect(
                    brush = Brush.linearGradient(
                        colors = palette,
                        start = Offset(shift, 0f),
                        end = Offset(shift + w, h * 0.3f),
                        tileMode = TileMode.Mirror,
                    )
                )
                val band = w * 0.35f + 1f
                val x = -band + (w + 2f * band) * shine.value
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.32f), Color.Transparent),
                        start = Offset(x - band, 0f),
                        end = Offset(x + band, h),
                    )
                )
                drawContent()
            }
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            maxLines = 1,
        )
    }
}
