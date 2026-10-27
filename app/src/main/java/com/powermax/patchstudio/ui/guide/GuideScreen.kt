package com.powermax.patchstudio.ui.guide

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.ui.components.AuroraBackground
import com.powermax.patchstudio.ui.components.GlassCard
import com.powermax.patchstudio.ui.motion.staggeredEntrance
import com.powermax.patchstudio.ui.theme.PsColors

private data class GuideStep(
    val title: String,
    val lines: List<String>,
    val icon: ImageVector,
)

private val Steps = listOf(
    GuideStep(
        "Installer Patch Studio",
        listOf("Installe cet APK sur ton téléphone (c'est déjà fait si tu lis ceci)."),
        Icons.Default.Home,
    ),
    GuideStep(
        "Installer LSPatch",
        listOf("Installe l'application LSPatch (le manager) sur ton téléphone."),
        Icons.Default.Add,
    ),
    GuideStep(
        "Patcher Deezer",
        listOf(
            "Dans LSPatch, ouvre l'onglet Patcher et choisis Deezer.",
            "Sélectionne le mode « Local » (recommandé : pas besoin de repatcher quand Patch Studio est mis à jour).",
            "Choisis le niveau de contournement de signature 2.",
            "Lance Patcher puis installe. Si on te le demande, désinstalle la version Deezer d'origine : tes téléchargements hors-ligne seront à refaire.",
        ),
        Icons.Default.Build,
    ),
    GuideStep(
        "Activer le module",
        listOf("Dans LSPatch, ouvre l'onglet Modules et active Patch Studio pour Deezer."),
        Icons.Default.Check,
    ),
    GuideStep(
        "Choisir ton style",
        listOf("Reviens ici, choisis un thème puis appuie sur « Appliquer et ouvrir Deezer »."),
        Icons.Default.PlayArrow,
    ),
    GuideStep(
        "Astuce",
        listOf("Passe Deezer en mode sombre pour un meilleur rendu."),
        Icons.Default.Info,
    ),
)

@Composable
fun GuideScreen(onBack: () -> Unit) {
    val sys = WindowInsets.systemBars.asPaddingValues()
    val padding = PaddingValues(
        start = 16.dp,
        end = 16.dp,
        top = sys.calculateTopPadding() + 8.dp,
        bottom = sys.calculateBottomPadding() + 24.dp,
    )
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
                    Column {
                        Text(
                            "Guide LSPatch",
                            style = MaterialTheme.typography.titleLarge,
                            color = PsColors.TextPrimary,
                        )
                        Text(
                            "Six étapes pour habiller Deezer",
                            style = MaterialTheme.typography.bodySmall,
                            color = PsColors.TextSecondary,
                        )
                    }
                }
            }
            itemsIndexed(Steps) { index, step ->
                StepCard(index + 1, step, Modifier.staggeredEntrance(index + 1))
            }
            item(key = "note") {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().staggeredEntrance(Steps.size + 1),
                    accent = PsColors.Tertiary,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = PsColors.Tertiary)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Patch Studio est 100 % cosmétique : il ne modifie pas ton abonnement ni le fonctionnement de l'application.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PsColors.TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCard(number: Int, step: GuideStep, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier.fillMaxWidth(), accent = PsColors.Primary) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(PsColors.Primary, PsColors.Secondary))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$number",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    step.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PsColors.TextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                step.lines.forEach { line ->
                    Text(
                        if (step.lines.size > 1) "• $line" else line,
                        style = MaterialTheme.typography.bodySmall,
                        color = PsColors.TextSecondary,
                    )
                    Spacer(Modifier.height(4.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            PulsingIcon(step.icon)
        }
    }
}

@Composable
private fun PulsingIcon(icon: ImageVector) {
    val transition = rememberInfiniteTransition(label = "stepIcon")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale",
    )
    Icon(
        icon,
        contentDescription = null,
        tint = PsColors.Primary,
        modifier = Modifier.size(24.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    )
}
