package com.powermax.patchstudio.ui.app

import android.graphics.Typeface as AndroidTypeface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powermax.patchstudio.core.ColorMath
import com.powermax.patchstudio.core.EntranceAnimation
import com.powermax.patchstudio.core.FontChoice
import com.powermax.patchstudio.core.LyricsAlignment
import com.powermax.patchstudio.core.LyricsBackground
import com.powermax.patchstudio.core.StyleConfig
import com.powermax.patchstudio.core.TargetApp
import com.powermax.patchstudio.core.ThemePresets
import com.powermax.patchstudio.ui.components.SegmentedControl
import com.powermax.patchstudio.ui.components.SettingsGroup
import com.powermax.patchstudio.ui.motion.PsMotion
import com.powermax.patchstudio.ui.motion.pressScale
import kotlinx.coroutines.launch

/** Couleurs « d'origine » approximatives de l'appli cible quand aucun thème n'est appliqué. */
private val OriginalBackground = Color(0xFF121216)
private val OriginalSurface = Color(0xFF1E1E24)

private enum class PreviewTab { PLAYER, LYRICS }

private val SampleTracks = listOf(
    "Midnight City" to "M83",
    "Instant Crush" to "Daft Punk",
    "Nightcall" to "Kavinsky",
)

private val SampleLyrics = listOf(
    "Waiting in a car",
    "Waiting for a ride in the dark",
    "The night city grows",
    "Look at the horizon glow",
)

/** Même famille système que celle appliquée dans l'appli cible. */
internal fun fontFamilyFor(f: FontChoice): FontFamily {
    val name = when (f) {
        FontChoice.DEFAULT -> return FontFamily.Default
        FontChoice.CONDENSED -> "sans-serif-condensed"
        FontChoice.LIGHT -> "sans-serif-light"
        FontChoice.MEDIUM -> "sans-serif-medium"
        FontChoice.SERIF -> "serif"
        FontChoice.MONO -> "monospace"
        FontChoice.CASUAL -> "casual"
    }
    return try {
        FontFamily(Typeface(AndroidTypeface.create(name, AndroidTypeface.NORMAL)))
    } catch (_: Exception) {
        FontFamily.Default
    }
}

/**
 * Aperçu en direct double mode (Lecteur / Paroles) : reflète la palette, les coins arrondis,
 * la police, l'aurore, les réglages de paroles et permet de tester l'animation d'entrée.
 */
@Composable
fun PlayerPreview(config: StyleConfig, target: TargetApp, modifier: Modifier = Modifier) {
    val palette = config.resolvedPalette()
    val spec = tween<Color>(PsMotion.DurationMedium, easing = PsMotion.Standard)
    val background by animateColorAsState(
        palette?.let { Color(it.background) } ?: OriginalBackground, spec, label = "pvBg",
    )
    val surface by animateColorAsState(
        palette?.let { Color(it.surface) } ?: OriginalSurface, spec, label = "pvSurface",
    )
    val accent by animateColorAsState(
        palette?.let { Color(it.accent) } ?: Color(target.brandColor), spec, label = "pvAccent",
    )
    val onAccent by animateColorAsState(
        palette?.let { Color(it.onAccent) } ?: Color(ColorMath.onColorFor(target.brandColor)), spec, label = "pvOnAccent",
    )
    val radiusTarget = if (config.enabled) (config.cornerRadiusDp / 2f).dp else 6.dp
    val radius by animateDpAsState(radiusTarget, tween(PsMotion.DurationMedium), label = "pvRadius")
    val fontFamily = remember(config.enabled, config.font) {
        fontFamilyFor(if (config.enabled) config.font else FontChoice.DEFAULT)
    }

    var tab by remember { mutableStateOf(PreviewTab.PLAYER) }
    val themeName = if (!config.enabled) "Original" else ThemePresets.byId(config.themeId).name

    SettingsGroup(
        modifier = modifier.semantics { contentDescription = "Aperçu du thème $themeName" },
    ) {
        Column(Modifier.padding(12.dp)) {
            SegmentedControl(
                options = PreviewTab.values().toList(),
                selected = tab,
                label = { if (it == PreviewTab.PLAYER) "Lecteur" else "Paroles" },
                onSelect = { tab = it },
                modifier = Modifier.padding(bottom = 10.dp),
            )
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    fadeIn(tween(PsMotion.DurationShort)) togetherWith fadeOut(tween(PsMotion.DurationShort))
                },
                label = "previewTab",
            ) { currentTab ->
                when (currentTab) {
                    PreviewTab.PLAYER -> MiniPlayerCard(
                        config = config,
                        background = background,
                        surface = surface,
                        accent = accent,
                        onAccent = onAccent,
                        radius = radius,
                        fontFamily = fontFamily,
                    )
                    PreviewTab.LYRICS -> LyricsPreviewCard(
                        config = config,
                        background = background,
                        accent = accent,
                        fontFamily = fontFamily,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerCard(
    config: StyleConfig,
    background: Color,
    surface: Color,
    accent: Color,
    onAccent: Color,
    radius: Dp,
    fontFamily: FontFamily,
) {
    val onBackground = Color(ColorMath.onColorFor(background.toArgb()))
    val strong = onBackground.copy(alpha = 0.92f)
    val soft = onBackground.copy(alpha = 0.56f)
    val faint = onBackground.copy(alpha = 0.16f)

    var playing by remember { mutableStateOf(true) }
    val playInteraction = remember { MutableInteractionSource() }
    val animProgress = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val slideOffsetPx = with(LocalDensity.current) { 18.dp.toPx() }

    val replayEntrance: () -> Unit = {
        playing = !playing
        if (config.enabled && config.entrance != EntranceAnimation.NONE) {
            scope.launch {
                val dur = (380f / config.animationSpeed.coerceIn(0.5f, 2f)).toInt()
                animProgress.snapTo(0f)
                if (config.entrance == EntranceAnimation.BOUNCE) {
                    animProgress.animateTo(
                        1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                    )
                } else {
                    animProgress.animateTo(1f, tween(dur, easing = PsMotion.Standard))
                }
            }
        }
    }

    val p = animProgress.value
    val entranceModifier = Modifier.graphicsLayer {
        if (!config.enabled || config.entrance == EntranceAnimation.NONE || p >= 0.999f) return@graphicsLayer
        alpha = p.coerceIn(0f, 1f)
        when (config.entrance) {
            EntranceAnimation.SLIDE_UP -> translationY = (1f - p) * slideOffsetPx
            EntranceAnimation.ZOOM -> {
                val s = 0.92f + 0.08f * p
                scaleX = s
                scaleY = s
            }
            EntranceAnimation.BOUNCE -> {
                val s = 0.88f + 0.12f * p
                scaleX = s
                scaleY = s
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background),
    ) {
        if (config.enabled && config.aurora) {
            AuroraPreviewBackdrop(
                accent = accent,
                intensity = config.auroraIntensity,
                modifier = Modifier.matchParentSize(),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(entranceModifier)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(radius.coerceAtLeast(2.dp)))
                        .background(
                            Brush.linearGradient(
                                listOf(accent, Color(ColorMath.blend(accent.toArgb(), surface.toArgb(), 0.45f))),
                            ),
                        ),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = SampleTracks[0].first,
                        fontFamily = fontFamily,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = strong,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = SampleTracks[0].second,
                        fontFamily = fontFamily,
                        fontSize = 13.sp,
                        color = soft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(40.dp)
                        .then(if (config.enabled && config.pressBounce) Modifier.pressScale(playInteraction, 0.92f) else Modifier)
                        .clip(CircleShape)
                        .background(accent)
                        .clickable(
                            interactionSource = playInteraction,
                            indication = null,
                            role = Role.Button,
                            onClick = replayEntrance,
                        )
                        .semantics { contentDescription = "Tester l'animation dans l'aperçu" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = onAccent,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(faint),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.42f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
            Spacer(Modifier.height(14.dp))
            SampleTracks.forEachIndexed { i, (title, artist) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radius))
                        .background(if (i == 0) surface.copy(alpha = 0.85f) else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape((radius / 2).coerceAtLeast(2.dp)))
                            .background(if (i == 0) accent.copy(alpha = 0.85f) else surface),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontFamily = fontFamily,
                            fontSize = 13.sp,
                            fontWeight = if (i == 0) FontWeight.Medium else FontWeight.Normal,
                            color = if (i == 0) accent else strong.copy(alpha = 0.82f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = artist,
                            fontFamily = fontFamily,
                            fontSize = 11.sp,
                            color = soft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuroraPreviewBackdrop(accent: Color, intensity: Float, modifier: Modifier = Modifier) {
    val hsl = remember(accent) { ColorMath.toHsl(accent.toArgb()) }
    val c1 = accent
    val c2 = remember(hsl) { Color(ColorMath.fromHsl(hsl[0] + 40f, hsl[1], hsl[2])) }
    val c3 = remember(hsl) { Color(ColorMath.fromHsl(hsl[0] - 40f, hsl[1], hsl[2])) }
    val alpha = (intensity.coerceIn(0.05f, 0.6f) * 0.55f)

    Canvas(modifier) {
        val r = maxOf(size.width, size.height) * 0.65f
        val centers = listOf(
            Offset(size.width * 0.2f, size.height * 0.25f) to c1,
            Offset(size.width * 0.85f, size.height * 0.45f) to c2,
            Offset(size.width * 0.45f, size.height * 0.9f) to c3,
        )
        for ((center, color) in centers) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = center,
                    radius = r,
                ),
                radius = r,
                center = center,
            )
        }
    }
}

@Composable
private fun LyricsPreviewCard(
    config: StyleConfig,
    background: Color,
    accent: Color,
    fontFamily: FontFamily,
) {
    val enhanced = config.enabled && config.lyricsEnhanced
    var activeIndex by remember { mutableIntStateOf(1) }

    val baseBg = when {
        !enhanced || config.lyricsBackground == LyricsBackground.NONE -> OriginalBackground
        else -> background
    }
    val gradientTop = remember(accent) {
        val hsl = ColorMath.toHsl(accent.toArgb())
        Color(ColorMath.fromHsl(hsl[0], hsl[1] * 0.85f, 0.35f))
    }
    val bgBrush = when {
        enhanced && config.lyricsBackground == LyricsBackground.GRADIENT -> Brush.verticalGradient(
            listOf(gradientTop, Color(ColorMath.blend(gradientTop.toArgb(), baseBg.toArgb(), 0.55f)), baseBg),
        )
        else -> Brush.verticalGradient(listOf(baseBg, baseBg))
    }

    val onBg = Color(ColorMath.onColorFor(baseBg.toArgb()))
    val centered = enhanced && config.lyricsAlignment == LyricsAlignment.CENTER
    val scaleFactor = if (enhanced) config.lyricsTextScale.coerceIn(0.9f, 1.6f) else 1f
    val fontSizeSp = (13.5f * scaleFactor).coerceIn(12f, 20f).sp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgBrush)
            .clickable(role = Role.Button) { activeIndex = (activeIndex + 1) % SampleLyrics.size }
            .semantics { contentDescription = "Aperçu des paroles, appuyer pour changer la ligne active" },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        ) {
            SampleLyrics.forEachIndexed { idx, line ->
                val isActive = idx == activeIndex
                val zoomScale by animateFloatAsState(
                    targetValue = if (enhanced && config.lyricsActiveZoom && isActive) 1.06f else 1f,
                    animationSpec = tween(PsMotion.DurationShort),
                    label = "lyricZoom",
                )
                val lineAlpha by animateFloatAsState(
                    targetValue = when {
                        isActive -> 1f
                        enhanced && config.lyricsActiveZoom -> 0.5f
                        else -> 0.65f
                    },
                    animationSpec = tween(PsMotion.DurationShort),
                    label = "lyricAlpha",
                )
                val lineColor by animateColorAsState(
                    targetValue = if (enhanced && config.lyricsAccentActive && isActive) accent else onBg,
                    animationSpec = tween(PsMotion.DurationShort),
                    label = "lyricColor",
                )
                Text(
                    text = line,
                    fontFamily = fontFamily,
                    fontSize = fontSizeSp,
                    lineHeight = (fontSizeSp.value * 1.18f).sp,
                    fontWeight = if (enhanced) FontWeight.Bold else FontWeight.Medium,
                    color = lineColor,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(lineAlpha)
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(if (centered) 0.5f else 0f, 0.5f)
                            scaleX = zoomScale
                            scaleY = zoomScale
                        },
                )
            }
        }
        if (enhanced && config.lyricsFadeEdges) {
            val topEdge = if (config.lyricsBackground == LyricsBackground.GRADIENT) gradientTop else baseBg
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(topEdge, Color.Transparent))),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, baseBg))),
            )
        }
    }
}
