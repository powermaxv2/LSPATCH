package com.powermax.patchstudio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.ui.motion.PsMotion
import com.powermax.patchstudio.ui.theme.PsColors
import com.powermax.patchstudio.ui.theme.PsShapes
import com.powermax.patchstudio.ui.theme.PsType
import kotlin.math.roundToInt

private fun snap(fraction: Float, steps: Int): Float {
    if (steps <= 0) return fraction
    val intervals = steps + 1
    return (fraction * intervals).roundToInt().toFloat() / intervals
}

/**
 * Curseur fin : piste 4dp, pouce blanc 20dp. Zone tactile 40dp de haut.
 * Si [trackBrush] est fourni, la piste entière est peinte avec (ex. sélecteur de teinte)
 * et le pouce prend [thumbColor].
 */
@Composable
fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    trackBrush: Brush? = null,
    thumbColor: Color = Color.White,
    activeColor: Color = PsColors.Accent,
    inactiveColor: Color = PsColors.Control,
    contentDescription: String? = null,
    stateLabel: String? = null,
) {
    val start = valueRange.start
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - start) / span).coerceIn(0f, 1f)
    val onChange by rememberUpdatedState(onValueChange)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
                if (stateLabel != null) stateDescription = stateLabel
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange, steps)
                setProgress { target ->
                    onChange(target.coerceIn(valueRange.start, valueRange.endInclusive))
                    true
                }
            }
            .pointerInput(valueRange, steps) {
                val radius = 10.dp.toPx()
                fun valueAt(x: Float): Float {
                    val usable = (size.width - 2 * radius).coerceAtLeast(1f)
                    val f = snap(((x - radius) / usable).coerceIn(0f, 1f), steps)
                    return start + f * span
                }
                detectTapGestures { offset -> onChange(valueAt(offset.x)) }
            }
            .pointerInput(valueRange, steps) {
                val radius = 10.dp.toPx()
                fun valueAt(x: Float): Float {
                    val usable = (size.width - 2 * radius).coerceAtLeast(1f)
                    val f = snap(((x - radius) / usable).coerceIn(0f, 1f), steps)
                    return start + f * span
                }
                detectHorizontalDragGestures(
                    onDragStart = { offset -> onChange(valueAt(offset.x)) },
                ) { change, _ ->
                    change.consume()
                    onChange(valueAt(change.position.x))
                }
            },
    ) {
        val radius = 10.dp.toPx()
        val trackHeight = (if (trackBrush != null) 6.dp else 4.dp).toPx()
        val cy = size.height / 2f
        val left = radius
        val right = size.width - radius
        val thumbX = left + (right - left) * fraction
        val corner = CornerRadius(trackHeight / 2f, trackHeight / 2f)
        val trackTop = cy - trackHeight / 2f

        if (trackBrush != null) {
            drawRoundRect(
                brush = trackBrush,
                topLeft = Offset(left, trackTop),
                size = Size(right - left, trackHeight),
                cornerRadius = corner,
            )
        } else {
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(left, trackTop),
                size = Size(right - left, trackHeight),
                cornerRadius = corner,
            )
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(left, trackTop),
                size = Size((thumbX - left).coerceAtLeast(trackHeight), trackHeight),
                cornerRadius = corner,
            )
            if (steps in 1..12) {
                val intervals = steps + 1
                for (i in 1 until intervals) {
                    val x = left + (right - left) * i / intervals
                    drawCircle(
                        color = if (x <= thumbX) Color.White.copy(alpha = 0.5f) else PsColors.TextTertiary,
                        radius = 1.5.dp.toPx(),
                        center = Offset(x, cy),
                    )
                }
            }
        }
        // Pouce : ombre légère + disque.
        drawCircle(
            color = Color.Black.copy(alpha = 0.35f),
            radius = radius + 1.dp.toPx(),
            center = Offset(thumbX, cy + 1.dp.toPx()),
        )
        drawCircle(color = thumbColor, radius = radius, center = Offset(thumbX, cy))
        if (trackBrush != null) {
            drawCircle(
                color = Color.White,
                radius = radius,
                center = Offset(thumbX, cy),
                style = Stroke(width = 2.dp.toPx()),
            )
        }
    }
}

/** Dégradé de teinte complet (fonctionnel : sert de repère pour choisir une teinte). */
fun hueBrush(saturation: Float = 0.8f, lightness: Float = 0.6f): Brush {
    val stops = (0..6).map { i -> Color.hsl(hue = i * 60f % 360f, saturation = saturation, lightness = lightness) }
    return Brush.horizontalGradient(stops)
}

/** Contrôle segmenté sobre (style iOS) ; le segment sélectionné est légèrement éclairci. */
@Composable
fun <T> SegmentedControl(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(PsShapes.Segment)
            .background(PsColors.Background)
            .padding(3.dp)
            .selectableGroup(),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val bg by animateColorAsState(
                targetValue = if (isSelected) PsColors.Control else Color.Transparent,
                animationSpec = tween(PsMotion.DurationShort),
                label = "segmentBg",
            )
            val fg by animateColorAsState(
                targetValue = if (isSelected) PsColors.TextPrimary else PsColors.TextSecondary,
                animationSpec = tween(PsMotion.DurationShort),
                label = "segmentFg",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(PsShapes.SegmentItem)
                    .background(bg)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = PsType.Caption.copy(fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal),
                    color = fg,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }
    }
}

/** Pastille de couleur (zone tactile 40dp) ; anneau clair si sélectionnée. */
@Composable
fun ColorDot(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val ring by animateColorAsState(
        targetValue = if (selected) PsColors.TextPrimary else Color.Transparent,
        animationSpec = tween(PsMotion.DurationShort),
        label = "dotRing",
    )
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .border(2.dp, ring, CircleShape)
                .padding(4.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
        )
    }
}

/** Petite ligne cliquable texte (lien). */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(PsShapes.SegmentItem)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Text(text, style = PsType.RowSubtitle.copy(fontWeight = FontWeight.Medium), color = PsColors.Accent)
    }
}
