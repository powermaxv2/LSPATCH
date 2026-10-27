package com.powermax.patchstudio.ui.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermax.patchstudio.core.EntranceAnimation
import com.powermax.patchstudio.core.FontChoice
import com.powermax.patchstudio.core.StyleConfig
import com.powermax.patchstudio.core.TargetApp
import com.powermax.patchstudio.ui.components.EqualizerBars
import com.powermax.patchstudio.ui.components.SpinningVinyl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ORIGINAL_BACKGROUND = 0xFF121216.toInt()
private const val ORIGINAL_SURFACE = 0xFF1E1E26.toInt()
private const val ROWS = 4

private val RowTitles = listOf("Soirée néon", "Route de nuit", "Matin calme", "Dernier métro")
private val RowArtists = listOf("Les Voyageurs", "Aube", "Kelvin Rey", "Maya & Co")

private fun fontFor(choice: FontChoice): Triple<FontFamily, FontWeight, Float> = when (choice) {
    FontChoice.DEFAULT -> Triple(FontFamily.Default, FontWeight.Normal, 0f)
    FontChoice.CONDENSED -> Triple(FontFamily.SansSerif, FontWeight.Normal, -0.6f)
    FontChoice.LIGHT -> Triple(FontFamily.SansSerif, FontWeight.Light, 0f)
    FontChoice.MEDIUM -> Triple(FontFamily.SansSerif, FontWeight.Medium, 0f)
    FontChoice.SERIF -> Triple(FontFamily.Serif, FontWeight.Normal, 0f)
    FontChoice.MONO -> Triple(FontFamily.Monospace, FontWeight.Normal, 0f)
    FontChoice.CASUAL -> Triple(FontFamily.Cursive, FontWeight.Normal, 0f)
}

@Composable
fun TargetPreview(config: StyleConfig, target: TargetApp, modifier: Modifier = Modifier) {
    val palette = config.resolvedPalette()
    val accentTarget = if (palette != null) Color(palette.accent) else Color(target.brandColor)
    val bgTarget = if (palette != null) Color(palette.background) else Color(ORIGINAL_BACKGROUND)
    val surfaceTarget = if (palette != null) Color(palette.surface) else Color(ORIGINAL_SURFACE)
    val onAccentTarget = if (palette != null) Color(palette.onAccent) else Color.White

    val accent by animateColorAsState(accentTarget, tween(500), label = "accent")
    val bg by animateColorAsState(bgTarget, tween(500), label = "bg")
    val surface by animateColorAsState(surfaceTarget, tween(500), label = "surface")
    val onAccent by animateColorAsState(onAccentTarget, tween(500), label = "onAccent")
    val corner by animateDpAsState(config.cornerRadiusDp.dp, tween(300), label = "corner")

    val (family, weight, spacing) = fontFor(config.font)
    val titleStyle = TextStyle(
        fontFamily = family, fontWeight = if (weight == FontWeight.Normal) FontWeight.SemiBold else weight,
        fontSize = 16.sp, letterSpacing = spacing.sp, color = Color.White,
    )
    val subStyle = TextStyle(
        fontFamily = family, fontWeight = weight, fontSize = 12.sp,
        letterSpacing = spacing.sp, color = Color.White.copy(alpha = 0.6f),
    )

    // Animation de liste rejouée en boucle quand les réglages changent.
    val progress = remember { List(ROWS) { Animatable(0f) } }
    val entrance = if (config.listAnimations) config.entrance else EntranceAnimation.NONE
    LaunchedEffect(entrance, config.animationSpeed) {
        val speed = config.animationSpeed.coerceIn(0.5f, 2f)
        while (true) {
            progress.forEach { it.snapTo(if (entrance == EntranceAnimation.NONE) 1f else 0f) }
            if (entrance != EntranceAnimation.NONE) {
                progress.forEachIndexed { i, anim ->
                    launch {
                        delay((i * 110 / speed).toLong())
                        if (entrance == EntranceAnimation.BOUNCE) {
                            anim.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f * speed))
                        } else {
                            anim.animateTo(1f, tween((520 / speed).toInt()))
                        }
                    }
                }
            }
            delay((3200 / speed).toLong())
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .border(3.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(30.dp))
            .background(bg),
    ) {
        if (config.aurora) {
            val a = config.auroraIntensity.coerceIn(0.05f, 0.6f)
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = a), Color.Transparent, accent.copy(alpha = a * 0.6f)),
                        ),
                    ),
            )
        }
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SpinningVinyl(accent = accent, modifier = Modifier.size(76.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Nuit électrique", style = titleStyle.copy(fontSize = 18.sp))
                    Text("Les Voyageurs", style = subStyle)
                }
                EqualizerBars(color = accent, modifier = Modifier.size(width = 30.dp, height = 26.dp))
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
            ) {
                Box(Modifier.fillMaxWidth(0.42f).height(4.dp).background(accent))
            }
            Spacer(Modifier.height(14.dp))
            for (i in 0 until ROWS) {
                val p = progress[i].value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .graphicsLayer {
                            when (entrance) {
                                EntranceAnimation.NONE -> Unit
                                EntranceAnimation.FADE -> alpha = p.coerceIn(0f, 1f)
                                EntranceAnimation.SLIDE_UP -> {
                                    alpha = p.coerceIn(0f, 1f)
                                    translationY = (1f - p) * 48.dp.toPx()
                                }
                                EntranceAnimation.ZOOM -> {
                                    alpha = p.coerceIn(0f, 1f)
                                    scaleX = 0.6f + 0.4f * p
                                    scaleY = 0.6f + 0.4f * p
                                }
                                EntranceAnimation.BOUNCE -> {
                                    alpha = p.coerceIn(0f, 1f)
                                    translationY = (1f - p) * 80.dp.toPx()
                                }
                            }
                        }
                        .clip(RoundedCornerShape(corner))
                        .background(surface)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(corner / 2))
                            .background(if (i == 0) accent else accent.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (i == 0) {
                            Text("▶", style = TextStyle(color = onAccent, fontSize = 12.sp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(RowTitles[i], style = titleStyle.copy(fontSize = 14.sp))
                        Text(RowArtists[i], style = subStyle.copy(fontSize = 11.sp))
                    }
                    Text("3:2$i", style = subStyle.copy(fontSize = 11.sp))
                }
            }
        }
    }
}
