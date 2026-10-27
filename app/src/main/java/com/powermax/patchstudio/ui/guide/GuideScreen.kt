package com.powermax.patchstudio.ui.guide

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.ui.components.SectionFooter
import com.powermax.patchstudio.ui.components.SectionHeader
import com.powermax.patchstudio.ui.components.SettingsGroup
import com.powermax.patchstudio.ui.components.SettingsRow
import com.powermax.patchstudio.ui.theme.PsColors
import com.powermax.patchstudio.ui.theme.PsType

private data class GuideStep(val title: String, val lines: List<String>)

private val Steps = listOf(
    GuideStep(
        "Installer Patch Studio",
        listOf("Installe cet APK sur ton téléphone (c'est déjà fait si tu lis ceci)."),
    ),
    GuideStep(
        "Installer LSPatch",
        listOf("Installe l'application LSPatch (le manager) sur ton téléphone."),
    ),
    GuideStep(
        "Patcher Deezer",
        listOf(
            "Dans LSPatch, ouvre l'onglet Patcher et choisis Deezer.",
            "Sélectionne le mode « Local » : pas besoin de repatcher quand Patch Studio est mis à jour.",
            "Choisis le niveau de contournement de signature 2.",
            "Lance le patch puis installe. Si on te le demande, désinstalle la version d'origine de Deezer : tes téléchargements hors ligne seront à refaire.",
        ),
    ),
    GuideStep(
        "Activer le module",
        listOf("Dans LSPatch, ouvre l'onglet Modules et active Patch Studio pour Deezer."),
    ),
    GuideStep(
        "Choisir ton style",
        listOf("Reviens ici, choisis un thème puis appuie sur « Appliquer et ouvrir Deezer »."),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(onBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = PsColors.Background,
        contentColor = PsColors.TextPrimary,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text("Guide d'installation", maxLines = 1, modifier = Modifier.padding(start = 4.dp))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = PsColors.Background,
                    scrolledContainerColor = PsColors.Background,
                    navigationIconContentColor = PsColors.TextPrimary,
                    titleContentColor = PsColors.TextPrimary,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = inner.calculateTopPadding() + 8.dp,
                bottom = inner.calculateBottomPadding() + 32.dp,
            ),
        ) {
            item(key = "intro") {
                Text(
                    "Cinq étapes pour appliquer Patch Studio à Deezer avec LSPatch, sans root.",
                    style = PsType.RowSubtitle,
                    color = PsColors.TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 24.dp),
                )
            }
            itemsIndexed(Steps, key = { _, step -> step.title }) { index, step ->
                StepItem(number = index + 1, step = step, last = index == Steps.lastIndex)
            }
            item(key = "tip") {
                Column(Modifier.padding(top = 32.dp)) {
                    SectionHeader("Astuce")
                    SettingsGroup {
                        SettingsRow(
                            title = "Active le mode sombre",
                            subtitle = "Passe Deezer en mode sombre pour un meilleur rendu des thèmes.",
                            icon = Icons.Outlined.DarkMode,
                        )
                    }
                    SectionFooter("Patch Studio est uniquement cosmétique : il ne modifie ni ton abonnement, ni le fonctionnement de l'application.")
                }
            }
        }
    }
}

@Composable
private fun StepItem(number: Int, step: GuideStep, last: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = if (last) 0.dp else 28.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, PsColors.Control, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = PsType.Caption.copy(fontWeight = FontWeight.Medium),
                color = PsColors.TextSecondary,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                step.title,
                style = PsType.RowTitle.copy(fontWeight = FontWeight.Medium),
                color = PsColors.TextPrimary,
                modifier = Modifier
                    .padding(top = 3.dp)
                    .semantics { heading() },
            )
            step.lines.forEach { line ->
                Row {
                    if (step.lines.size > 1) {
                        Text("–", style = PsType.RowSubtitle, color = PsColors.TextTertiary)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(line, style = PsType.RowSubtitle, color = PsColors.TextSecondary)
                }
            }
        }
    }
}
