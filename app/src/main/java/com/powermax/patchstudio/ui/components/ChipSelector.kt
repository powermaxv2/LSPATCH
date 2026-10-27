package com.powermax.patchstudio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.ui.motion.PsMotion
import com.powermax.patchstudio.ui.motion.bounceClick
import com.powermax.patchstudio.ui.theme.PsColors
import com.powermax.patchstudio.ui.theme.PsShapes

@Composable
fun <T> ChipSelector(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bounds = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }
    val scroll = rememberScrollState()
    val selectedIndex = options.indexOf(selected)
    val target = bounds[selectedIndex]
    val x = remember { Animatable(0f) }
    val w = remember { Animatable(0f) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(target) {
        if (target != null) {
            if (!initialized) {
                x.snapTo(target.first)
                w.snapTo(target.second)
                initialized = true
            } else {
                launch { x.animateTo(target.first, PsMotion.Spring) }
                launch { w.animateTo(target.second, PsMotion.Spring) }
            }
            scroll.animateScrollTo((target.first - 48f).toInt().coerceAtLeast(0))
        }
    }

    Box(
        modifier = modifier
            .horizontalScroll(scroll)
            .drawBehind {
                if (w.value > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(PsColors.Primary, PsColors.Tertiary),
                            startX = x.value,
                            endX = x.value + w.value,
                        ),
                        topLeft = Offset(x.value, 0f),
                        size = Size(w.value, size.height),
                        cornerRadius = CornerRadius(size.height / 2f),
                    )
                }
            },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEachIndexed { index, option ->
                val isSel = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (isSel) Color.White else PsColors.TextSecondary,
                    animationSpec = PsMotion.tween(250),
                    label = "chipText",
                )
                Box(
                    modifier = Modifier
                        .onGloballyPositioned {
                            bounds[index] = it.positionInParent().x to it.size.width.toFloat()
                        }
                        .clip(PsShapes.Pill)
                        .bounceClick(haptic = false) { onSelect(option) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
