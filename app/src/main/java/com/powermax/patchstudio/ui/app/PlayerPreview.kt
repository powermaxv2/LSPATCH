package com.powermax.patchstudio.ui.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.core.ColorMath
import com.powermax.patchstudio.core.StyleConfig
import com.powermax.patchstudio.core.TargetApp
import com.powermax.patchstudio.core.ThemePresets
import com.powermax.patchstudio.ui.components.SettingsGroup
import com.powermax.patchstudio.ui.motion.PsMotion

/** Couleurs « d'origine » approximatives de l'appli cible quand aucun thème n'est appliqué. */
private val OriginalBackground = Color(0xFF121216)
private val OriginalSurface = Color(0xFF1E1E24)

/**
 * Aperçu sobre : mini lecteur (pochette, titre/artiste, progression) et trois lignes
 * de liste, dans les couleurs de la palette choisie.
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

    val onBackground = Color(ColorMath.onColorFor(background.toArgb()))
    val strong = onBackground.copy(alpha = 0.88f)
    val soft = onBackground.copy(alpha = 0.38f)
    val faint = onBackground.copy(alpha = 0.14f)

    val themeName = if (!config.enabled) "Original" else ThemePresets.byId(config.themeId).name

    SettingsGroup(
        modifier = modifier.semantics { contentDescription = "Aperçu du thème $themeName" },
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(background)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(radius.coerceAtLeast(2.dp)))
                        .background(accent),
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Line(fraction = 0.7f, height = 10.dp, color = strong)
                    Line(fraction = 0.45f, height = 8.dp, color = soft)
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = onAccent, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            // Progression
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(faint),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.38f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
            }
            Spacer(Modifier.height(16.dp))
            repeat(3) { i ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(radius))
                        .background(if (i == 0) surface else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape((radius / 2).coerceAtLeast(2.dp)))
                            .background(if (i == 0) accent.copy(alpha = 0.85f) else surface),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Line(fraction = listOf(0.6f, 0.5f, 0.66f)[i], height = 7.dp, color = if (i == 0) accent else strong.copy(alpha = 0.7f))
                        Line(fraction = listOf(0.35f, 0.42f, 0.3f)[i], height = 6.dp, color = soft)
                    }
                }
            }
        }
    }
}

@Composable
private fun Line(fraction: Float, height: Dp, color: Color) {
    Box(
        Modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(CircleShape)
            .background(color),
    )
}
