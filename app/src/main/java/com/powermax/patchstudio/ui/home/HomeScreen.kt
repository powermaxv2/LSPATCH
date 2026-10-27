package com.powermax.patchstudio.ui.home

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.core.TargetApp
import com.powermax.patchstudio.core.TargetApps
import com.powermax.patchstudio.core.ThemePresets
import com.powermax.patchstudio.data.ConfigRepository
import com.powermax.patchstudio.ui.components.AnimatedLogo
import com.powermax.patchstudio.ui.components.AuroraBackground
import com.powermax.patchstudio.ui.components.EqualizerBars
import com.powermax.patchstudio.ui.components.GlassCard
import com.powermax.patchstudio.ui.components.PulsingDot
import com.powermax.patchstudio.ui.components.SectionHeader
import com.powermax.patchstudio.ui.components.ShimmerText
import com.powermax.patchstudio.ui.motion.staggeredEntrance
import com.powermax.patchstudio.ui.theme.PsColors
import com.powermax.patchstudio.ui.theme.PsShapes

private const val DAY_MS = 24L * 60L * 60L * 1000L
private val StatusGreen = Color(0xFF3DDC84)
private val StatusOrange = Color(0xFFFFA726)

@Composable
fun HomeScreen(onOpenApp: (String) -> Unit, onOpenGuide: () -> Unit) {
    val context = LocalContext.current
    val repo = remember(context) { ConfigRepository.get(context) }
    val sys = WindowInsets.systemBars.asPaddingValues()
    val padding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = sys.calculateTopPadding() + 24.dp,
        bottom = sys.calculateBottomPadding() + 24.dp,
    )

    AuroraBackground(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Header(Modifier.staggeredEntrance(0))
            }
            item(key = "status") {
                StatusCard(repo, Modifier.staggeredEntrance(1))
            }
            item(key = "title") {
                SectionHeader(
                    title = "Tes applis",
                    modifier = Modifier.staggeredEntrance(2),
                )
            }
            items(TargetApps.ALL, key = { it.id }) { app ->
                val index = TargetApps.ALL.indexOf(app)
                AppCard(
                    app = app,
                    repo = repo,
                    onClick = { onOpenApp(app.id) },
                    modifier = Modifier.staggeredEntrance(3 + index),
                )
            }
            item(key = "soon") {
                SoonCard(Modifier.staggeredEntrance(3 + TargetApps.ALL.size))
            }
            item(key = "guide") {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(4 + TargetApps.ALL.size),
                    accent = PsColors.Secondary,
                    onClick = onOpenGuide,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = PsColors.Secondary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Guide LSPatch",
                                style = MaterialTheme.typography.titleMedium,
                                color = PsColors.TextPrimary,
                            )
                            Text(
                                "Installe et active le module en quelques étapes",
                                style = MaterialTheme.typography.bodySmall,
                                color = PsColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedLogo(size = 96.dp)
        Spacer(Modifier.height(12.dp))
        ShimmerText(
            text = "Patch Studio",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Ton studio de style pour tes applis",
            style = MaterialTheme.typography.bodyMedium,
            color = PsColors.TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StatusCard(repo: ConfigRepository, modifier: Modifier = Modifier) {
    val target = TargetApps.DEEZER
    val hello by repo.lastHelloFlow(target.id).collectAsState()
    val active = hello > 0L && System.currentTimeMillis() - hello < DAY_MS
    val color = if (active) StatusGreen else StatusOrange
    GlassCard(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        accent = color,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulsingDot(color = color, size = 12.dp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (active) {
                    "Module actif dans ${target.displayName}"
                } else {
                    "Module pas encore détecté — ouvre le guide"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = PsColors.TextPrimary,
            )
        }
    }
}

@Composable
private fun AppCard(
    app: TargetApp,
    repo: ConfigRepository,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = Color(app.brandColor)
    val config by repo.configFlow(app.id).collectAsState()
    val installed = remember(app.id) { repo.isInstalled(app.id) }
    val themeLabel = if (config.enabled) {
        "Thème : ${ThemePresets.byId(config.themeId).name}"
    } else {
        "Patch Studio désactivé"
    }
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        accent = brand,
        onClick = onClick,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(brand.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                EqualizerBars(color = brand, modifier = Modifier.size(width = 28.dp, height = 24.dp), bars = 4)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    app.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = PsColors.TextPrimary,
                )
                Text(
                    app.tagline,
                    style = MaterialTheme.typography.bodySmall,
                    color = PsColors.TextSecondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    themeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = brand,
                )
            }
            Spacer(Modifier.width(8.dp))
            InstallBadge(installed)
        }
    }
}

@Composable
private fun InstallBadge(installed: Boolean) {
    val color = if (installed) StatusGreen else StatusOrange
    Box(
        Modifier
            .clip(PsShapes.Pill)
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            if (installed) "Installée" else "Non installée",
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
private fun SoonCard(modifier: Modifier = Modifier) {
    val dash = PsColors.Outline
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = dash,
                    size = Size(size.width, size.height),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f)),
                    ),
                )
            }
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = PsColors.TextSecondary)
            Spacer(Modifier.width(12.dp))
            ShimmerText(
                text = "Bientôt d'autres applis",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
