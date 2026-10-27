package com.powermax.patchstudio.ui.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.core.EntranceAnimation
import com.powermax.patchstudio.core.FontChoice
import com.powermax.patchstudio.core.StyleConfig
import com.powermax.patchstudio.core.TargetApps
import com.powermax.patchstudio.core.ThemePresets
import com.powermax.patchstudio.data.ConfigRepository
import com.powermax.patchstudio.ui.components.AuroraBackground
import com.powermax.patchstudio.ui.components.ChipSelector
import com.powermax.patchstudio.ui.components.GlassCard
import com.powermax.patchstudio.ui.components.GradientButton
import com.powermax.patchstudio.ui.components.HueSlider
import com.powermax.patchstudio.ui.components.SectionHeader
import com.powermax.patchstudio.ui.components.SliderRow
import com.powermax.patchstudio.ui.components.ThemeSwatch
import com.powermax.patchstudio.ui.components.ToggleRow
import com.powermax.patchstudio.ui.motion.staggeredEntrance
import com.powermax.patchstudio.ui.theme.PsColors
import java.util.Locale
import kotlinx.coroutines.launch

private fun entranceLabel(e: EntranceAnimation): String = when (e) {
    EntranceAnimation.NONE -> "Aucune"
    EntranceAnimation.FADE -> "Fondu"
    EntranceAnimation.SLIDE_UP -> "Glissement"
    EntranceAnimation.ZOOM -> "Zoom"
    EntranceAnimation.BOUNCE -> "Rebond"
}

private fun fontLabel(f: FontChoice): String = when (f) {
    FontChoice.DEFAULT -> "Par défaut"
    FontChoice.CONDENSED -> "Condensée"
    FontChoice.LIGHT -> "Fine"
    FontChoice.MEDIUM -> "Medium"
    FontChoice.SERIF -> "Serif"
    FontChoice.MONO -> "Mono"
    FontChoice.CASUAL -> "Manuscrite"
}

@Composable
fun AppSettingsScreen(appId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember(context) { ConfigRepository.get(context) }
    val target = TargetApps.byId(appId) ?: TargetApps.DEEZER
    val config by repo.configFlow(appId).collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val sys = WindowInsets.systemBars.asPaddingValues()
    val bottomBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val padding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = sys.calculateTopPadding() + 8.dp,
        bottom = bottomBar + 190.dp,
    )

    fun update(transform: (StyleConfig) -> StyleConfig) {
        repo.update(appId, transform)
    }

    val sectionModifier = Modifier.fillMaxWidth().animateContentSize()

    AuroraBackground(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "top") {
                Row(
                    modifier = Modifier.fillMaxWidth().staggeredEntrance(0),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = PsColors.TextPrimary,
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Réglages ${target.displayName}",
                        style = MaterialTheme.typography.titleLarge,
                        color = PsColors.TextPrimary,
                    )
                }
            }
            item(key = "preview") {
                Box(Modifier.staggeredEntrance(1)) {
                    TargetPreview(config = config, target = target)
                }
            }

            // 1. Thème
            item(key = "theme") {
                Column(Modifier.staggeredEntrance(2)) {
                    SectionHeader(title = "Thème")
                    Spacer(Modifier.height(8.dp))
                    GlassCard(modifier = sectionModifier, accent = Color(target.brandColor)) {
                        ToggleRow(
                            title = "Activer Patch Studio",
                            subtitle = "Désactive pour retrouver l'apparence d'origine",
                            checked = config.enabled,
                            onCheckedChange = { v -> update { it.copy(enabled = v) } },
                        )
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(ThemePresets.ALL, key = { it.id }) { preset ->
                                val isCustom = preset.id == ThemePresets.CUSTOM_ID
                                val accent = when {
                                    isCustom -> Color(config.customAccent)
                                    preset.palette != null -> Color(preset.palette!!.accent)
                                    else -> Color(target.brandColor)
                                }
                                val background = when {
                                    isCustom -> Color(config.customBackground)
                                    preset.palette != null -> Color(preset.palette!!.background)
                                    else -> Color(0xFF121216)
                                }
                                ThemeSwatch(
                                    name = preset.name,
                                    accent = accent,
                                    background = background,
                                    selected = config.themeId == preset.id,
                                    onClick = { update { it.copy(themeId = preset.id) } },
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = config.themeId == ThemePresets.CUSTOM_ID,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Column(Modifier.padding(top = 12.dp)) {
                                Text("Couleur d'accent", style = MaterialTheme.typography.labelLarge, color = PsColors.TextSecondary)
                                HueSlider(
                                    color = Color(config.customAccent),
                                    onColorChange = { c -> update { it.copy(customAccent = c.toArgb()) } },
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("Couleur de fond", style = MaterialTheme.typography.labelLarge, color = PsColors.TextSecondary)
                                HueSlider(
                                    color = Color(config.customBackground),
                                    onColorChange = { c -> update { it.copy(customBackground = c.toArgb()) } },
                                )
                            }
                        }
                    }
                }
            }

            // 2. Animations
            item(key = "anim") {
                Column(Modifier.staggeredEntrance(3)) {
                    SectionHeader(title = "Animations")
                    Spacer(Modifier.height(8.dp))
                    GlassCard(modifier = sectionModifier, accent = PsColors.Secondary) {
                        Text("Entrée des éléments", style = MaterialTheme.typography.labelLarge, color = PsColors.TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        ChipSelector(
                            options = EntranceAnimation.values().toList(),
                            selected = config.entrance,
                            label = { entranceLabel(it) },
                            onSelect = { e -> update { it.copy(entrance = e) } },
                        )
                        ToggleRow(
                            title = "Transitions d'écran",
                            subtitle = null,
                            checked = config.screenTransitions,
                            onCheckedChange = { v -> update { it.copy(screenTransitions = v) } },
                        )
                        ToggleRow(
                            title = "Animation des listes",
                            subtitle = null,
                            checked = config.listAnimations,
                            onCheckedChange = { v -> update { it.copy(listAnimations = v) } },
                        )
                        ToggleRow(
                            title = "Rebond au toucher",
                            subtitle = null,
                            checked = config.pressBounce,
                            onCheckedChange = { v -> update { it.copy(pressBounce = v) } },
                        )
                        ToggleRow(
                            title = "Vibrations",
                            subtitle = "Léger retour haptique",
                            checked = config.haptics,
                            onCheckedChange = { v -> update { it.copy(haptics = v) } },
                        )
                        SliderRow(
                            title = "Vitesse",
                            value = config.animationSpeed,
                            valueRange = 0.5f..2f,
                            onValueChange = { v -> update { it.copy(animationSpeed = v) } },
                            valueLabel = { String.format(Locale.US, "x%.1f", it) },
                        )
                    }
                }
            }

            // 3. Style
            item(key = "style") {
                Column(Modifier.staggeredEntrance(4)) {
                    SectionHeader(title = "Style")
                    Spacer(Modifier.height(8.dp))
                    GlassCard(modifier = sectionModifier, accent = PsColors.Tertiary) {
                        Text("Police", style = MaterialTheme.typography.labelLarge, color = PsColors.TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        ChipSelector(
                            options = FontChoice.values().toList(),
                            selected = config.font,
                            label = { fontLabel(it) },
                            onSelect = { f -> update { it.copy(font = f) } },
                        )
                        SliderRow(
                            title = "Coins arrondis",
                            value = config.cornerRadiusDp.toFloat(),
                            valueRange = 0f..32f,
                            onValueChange = { v -> update { it.copy(cornerRadiusDp = v.toInt()) } },
                            valueLabel = { "${it.toInt()} dp" },
                        )
                        ToggleRow(
                            title = "Aurore",
                            subtitle = "Halo coloré animé en arrière-plan",
                            checked = config.aurora,
                            onCheckedChange = { v -> update { it.copy(aurora = v) } },
                        )
                        AnimatedVisibility(
                            visible = config.aurora,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            SliderRow(
                                title = "Intensité de l'aurore",
                                value = config.auroraIntensity,
                                valueRange = 0.05f..0.6f,
                                onValueChange = { v -> update { it.copy(auroraIntensity = v) } },
                                valueLabel = { "${(it * 100).toInt()} %" },
                            )
                        }
                    }
                }
            }

            // 4. Système
            item(key = "system") {
                Column(Modifier.staggeredEntrance(5)) {
                    SectionHeader(title = "Système")
                    Spacer(Modifier.height(8.dp))
                    GlassCard(modifier = sectionModifier, accent = PsColors.Primary) {
                        ToggleRow(
                            title = "Teinter les barres système",
                            subtitle = "Barre d'état et de navigation aux couleurs du thème",
                            checked = config.tintSystemBars,
                            onCheckedChange = { v -> update { it.copy(tintSystemBars = v) } },
                        )
                        TextButton(onClick = { repo.reset(appId) }) {
                            Text("Réinitialiser")
                        }
                    }
                }
            }
        }

        // Bas d'écran flottant
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, PsColors.Background.copy(alpha = 0.92f)),
                        ),
                    )
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = bottomBar + 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SnackbarHost(hostState = snackbar)
                GradientButton(
                    text = "Appliquer et ouvrir ${target.displayName}",
                    onClick = {
                        if (!repo.launchTarget(context, appId)) {
                            scope.launch { snackbar.showSnackbar("${target.displayName} n'est pas installé") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.PlayArrow,
                )
                TextButton(
                    onClick = {
                        repo.pushToTarget(appId)
                        scope.launch { snackbar.showSnackbar("Réglages envoyés à ${target.displayName}") }
                    },
                ) {
                    Text("Appliquer sans ouvrir")
                }
            }
        }
    }
}
