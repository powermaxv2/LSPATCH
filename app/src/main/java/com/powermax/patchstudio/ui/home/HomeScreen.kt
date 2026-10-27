package com.powermax.patchstudio.ui.home

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.powermax.patchstudio.BuildConfig
import com.powermax.patchstudio.core.TargetApp
import com.powermax.patchstudio.core.TargetApps
import com.powermax.patchstudio.core.ThemePresets
import com.powermax.patchstudio.data.ConfigRepository
import com.powermax.patchstudio.ui.components.AppIcon
import com.powermax.patchstudio.ui.components.NavigationRow
import com.powermax.patchstudio.ui.components.RowDivider
import com.powermax.patchstudio.ui.components.SectionFooter
import com.powermax.patchstudio.ui.components.SectionHeader
import com.powermax.patchstudio.ui.components.SettingsGroup
import com.powermax.patchstudio.ui.components.SettingsRow
import com.powermax.patchstudio.ui.components.IconBadge
import com.powermax.patchstudio.ui.components.StatusDot
import com.powermax.patchstudio.ui.components.TextLink
import com.powermax.patchstudio.ui.motion.PsMotion
import com.powermax.patchstudio.ui.theme.PsColors
import com.powermax.patchstudio.ui.theme.PsType

private const val DAY_MS = 24L * 60L * 60L * 1000L

@Composable
fun HomeScreen(onOpenApp: (String) -> Unit, onOpenGuide: () -> Unit) {
    val context = LocalContext.current
    val repo = remember(context) { ConfigRepository.get(context) }
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val listState = rememberLazyListState()

    // Rafraîchit l'état « installé » à chaque retour sur l'écran.
    var resumeTick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumeTick++
        onPauseOrDispose { }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = insets.calculateTopPadding() + 32.dp,
            bottom = insets.calculateBottomPadding() + 24.dp,
        ),
    ) {
        item(key = "header") {
            Column(Modifier.padding(start = 4.dp, end = 4.dp, bottom = 24.dp)) {
                Text(
                    "Patch Studio",
                    style = PsType.ScreenTitle,
                    color = PsColors.TextPrimary,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Personnalise l'apparence de tes applications.",
                    style = PsType.RowSubtitle,
                    color = PsColors.TextSecondary,
                )
            }
        }

        item(key = "status") {
            StatusCard(repo = repo, onOpenGuide = onOpenGuide)
        }

        item(key = "apps") {
            Column(Modifier.padding(top = 32.dp)) {
                SectionHeader("Applications")
                SettingsGroup {
                    TargetApps.ALL.forEachIndexed { index, app ->
                        if (index > 0) RowDivider(indent = 72.dp)
                        AppRow(app = app, repo = repo, resumeTick = resumeTick, onClick = { onOpenApp(app.id) })
                    }
                    RowDivider(indent = 72.dp)
                    SettingsRow(
                        title = "D'autres applications arrivent",
                        enabled = false,
                        leading = {
                            IconBadge(
                                Icons.Outlined.MoreHoriz,
                                modifier = Modifier.padding(4.dp),
                                tint = PsColors.TextSecondary,
                            )
                        },
                    )
                }
            }
        }

        item(key = "help") {
            Column(Modifier.padding(top = 32.dp)) {
                SectionHeader("Aide")
                SettingsGroup {
                    NavigationRow(
                        title = "Guide d'installation LSPatch",
                        subtitle = "Patcher Deezer et activer le module",
                        icon = Icons.Outlined.Description,
                        onClick = onOpenGuide,
                    )
                }
                SectionFooter("Patch Studio est uniquement cosmétique : il ne touche ni à ton abonnement, ni au fonctionnement de l'application.")
            }
        }

        item(key = "footer") {
            Text(
                text = "Patch Studio ${BuildConfig.VERSION_NAME}",
                style = PsType.Caption,
                color = PsColors.TextTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
            )
        }
    }
}

@Composable
private fun StatusCard(repo: ConfigRepository, onOpenGuide: () -> Unit) {
    val target = TargetApps.DEEZER
    val hello by repo.lastHelloFlow(target.id).collectAsState()
    val now = System.currentTimeMillis()
    val active = hello > 0L && now - hello < DAY_MS

    SettingsGroup {
        AnimatedContent(
            targetState = active,
            transitionSpec = {
                fadeIn(tween(PsMotion.DurationMedium)) togetherWith fadeOut(tween(PsMotion.DurationShort))
            },
            label = "status",
        ) { isActive ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(color = if (isActive) PsColors.Positive else PsColors.Neutral)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (isActive) "Module actif dans ${target.displayName}" else "Module non détecté",
                        style = PsType.RowTitle,
                        color = PsColors.TextPrimary,
                    )
                    Text(
                        text = if (isActive) {
                            "Dernier signal " + DateUtils.getRelativeTimeSpanString(
                                hello, now, DateUtils.MINUTE_IN_MILLIS,
                            ).toString().replaceFirstChar { it.lowercase() }
                        } else {
                            "Ouvre ${target.displayName} patché avec LSPatch"
                        },
                        style = PsType.RowSubtitle,
                        color = PsColors.TextSecondary,
                    )
                }
                if (!isActive) {
                    TextLink(text = "Voir le guide", onClick = onOpenGuide)
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: TargetApp, repo: ConfigRepository, resumeTick: Int, onClick: () -> Unit) {
    val config by repo.configFlow(app.id).collectAsState()
    val installed = remember(app.id, resumeTick) { repo.isInstalled(app.id) }
    val subtitle = when {
        !installed -> "Non installée"
        !config.enabled -> "Désactivé"
        else -> "Thème ${ThemePresets.byId(config.themeId).name}"
    }
    NavigationRow(
        title = app.displayName,
        subtitle = subtitle,
        onClick = onClick,
        leading = {
            AppIcon(
                packageName = app.packageName,
                label = app.displayName,
                fallbackColor = Color(app.brandColor),
                size = 40.dp,
            )
        },
    )
}
