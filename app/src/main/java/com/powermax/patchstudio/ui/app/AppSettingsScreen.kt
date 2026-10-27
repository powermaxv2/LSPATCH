package com.powermax.patchstudio.ui.app

import android.graphics.Typeface as AndroidTypeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.core.ColorMath
import com.powermax.patchstudio.core.EntranceAnimation
import com.powermax.patchstudio.core.FontChoice
import com.powermax.patchstudio.core.StyleConfig
import com.powermax.patchstudio.core.TargetApps
import com.powermax.patchstudio.core.ThemePresets
import com.powermax.patchstudio.data.ConfigRepository
import com.powermax.patchstudio.ui.components.ColorDot
import com.powermax.patchstudio.ui.components.NavigationRow
import com.powermax.patchstudio.ui.components.PrimaryButton
import com.powermax.patchstudio.ui.components.RowDivider
import com.powermax.patchstudio.ui.components.SecondaryTextButton
import com.powermax.patchstudio.ui.components.SectionFooter
import com.powermax.patchstudio.ui.components.SectionHeader
import com.powermax.patchstudio.ui.components.SegmentedControl
import com.powermax.patchstudio.ui.components.SettingsGroup
import com.powermax.patchstudio.ui.components.SettingsRow
import com.powermax.patchstudio.ui.components.SliderRow
import com.powermax.patchstudio.ui.components.SwitchRow
import com.powermax.patchstudio.ui.components.ThemeTile
import com.powermax.patchstudio.ui.components.ThinSlider
import com.powermax.patchstudio.ui.components.hueBrush
import com.powermax.patchstudio.ui.motion.PsMotion
import com.powermax.patchstudio.ui.theme.PsColors
import com.powermax.patchstudio.ui.theme.PsType
import java.util.Locale
import kotlinx.coroutines.launch

private fun entranceLabel(e: EntranceAnimation): String = when (e) {
    EntranceAnimation.NONE -> "Aucune"
    EntranceAnimation.FADE -> "Fondu"
    EntranceAnimation.SLIDE_UP -> "Glisser"
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

/** Même famille système que celle appliquée dans l'appli cible. */
private fun fontFamilyFor(f: FontChoice): FontFamily {
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

private data class NamedColor(val name: String, val color: Color)

private val AccentChoices = listOf(
    NamedColor("Violet", Color(0xFF8B5CF6)),
    NamedColor("Indigo", Color(0xFF6366F1)),
    NamedColor("Bleu", Color(0xFF3B82F6)),
    NamedColor("Cyan", Color(0xFF06B6D4)),
    NamedColor("Turquoise", Color(0xFF14B8A6)),
    NamedColor("Vert", Color(0xFF22C55E)),
    NamedColor("Jaune", Color(0xFFEAB308)),
    NamedColor("Orange", Color(0xFFF97316)),
    NamedColor("Rouge", Color(0xFFEF4444)),
    NamedColor("Rose", Color(0xFFEC4899)),
)

private val BackgroundChoices = listOf(
    NamedColor("Noir", Color(0xFF000000)),
    NamedColor("Graphite", Color(0xFF111114)),
    NamedColor("Marine", Color(0xFF0B1220)),
    NamedColor("Prune", Color(0xFF140B1E)),
    NamedColor("Forêt", Color(0xFF0A140F)),
    NamedColor("Bordeaux", Color(0xFF1A0A0E)),
    NamedColor("Brun", Color(0xFF16110C)),
)

private val OriginalTileBackground = Color(0xFF121216)
private val OriginalTileSurface = Color(0xFF26262C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(appId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember(context) { ConfigRepository.get(context) }
    val target = TargetApps.byId(appId) ?: TargetApps.DEEZER
    val config by repo.configFlow(appId).collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()

    var showFontSheet by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }

    fun update(transform: (StyleConfig) -> StyleConfig) = repo.update(appId, transform)
    fun message(text: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = PsColors.Background,
        contentColor = PsColors.TextPrimary,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = target.displayName,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp),
                    )
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
                    actionIconContentColor = PsColors.TextSecondary,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(PsColors.Background)
                    .navigationBarsPadding(),
            ) {
                HorizontalDivider(thickness = 1.dp, color = PsColors.Hairline)
                Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)) {
                    PrimaryButton(
                        text = "Appliquer et ouvrir ${target.displayName}",
                        icon = Icons.Filled.PlayArrow,
                        onClick = {
                            if (!repo.launchTarget(context, appId)) {
                                message("${target.displayName} n'est pas installé")
                            }
                        },
                    )
                    SecondaryTextButton(
                        text = "Appliquer sans ouvrir",
                        onClick = {
                            repo.pushToTarget(appId)
                            message("Réglages envoyés à ${target.displayName}")
                        },
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(12.dp),
                    containerColor = PsColors.SurfaceHigh,
                    contentColor = PsColors.TextPrimary,
                    actionColor = PsColors.Accent,
                )
            }
        },
    ) { inner ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = inner.calculateTopPadding() + 8.dp,
                bottom = inner.calculateBottomPadding() + 32.dp,
            ),
        ) {
            // Aperçu
            item(key = "preview") {
                Column {
                    SectionHeader("Aperçu")
                    PlayerPreview(config = config, target = target)
                }
            }

            // Interrupteur général
            item(key = "master") {
                Column(Modifier.padding(top = 24.dp)) {
                    SettingsGroup {
                        SwitchRow(
                            title = "Activer Patch Studio",
                            subtitle = if (config.enabled) {
                                "Les réglages ci-dessous s'appliquent à ${target.displayName}"
                            } else {
                                "${target.displayName} garde son apparence d'origine"
                            },
                            icon = Icons.Outlined.PowerSettingsNew,
                            checked = config.enabled,
                            onCheckedChange = { v -> update { it.copy(enabled = v) } },
                        )
                    }
                }
            }

            // Thème
            item(key = "theme") {
                Column(Modifier.padding(top = 32.dp)) {
                    SectionHeader("Thème")
                    SettingsGroup {
                        LazyRow(
                            modifier = Modifier.selectableGroup(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(ThemePresets.ALL, key = { it.id }) { preset ->
                                val isCustom = preset.id == ThemePresets.CUSTOM_ID
                                val palette = if (isCustom) {
                                    ThemePresets.paletteOf(config.customAccent, config.customBackground)
                                } else {
                                    preset.palette
                                }
                                ThemeTile(
                                    name = preset.name,
                                    background = palette?.let { Color(it.background) } ?: OriginalTileBackground,
                                    surface = palette?.let { Color(it.surface) } ?: OriginalTileSurface,
                                    accent = palette?.let { Color(it.accent) } ?: Color(target.brandColor),
                                    selected = config.themeId == preset.id,
                                    onClick = { update { it.copy(themeId = preset.id) } },
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = config.themeId == ThemePresets.CUSTOM_ID,
                            enter = PsMotion.Expand,
                            exit = PsMotion.Collapse,
                        ) {
                            CustomColors(
                                accent = config.customAccent,
                                background = config.customBackground,
                                onAccent = { c -> update { it.copy(customAccent = c) } },
                                onBackground = { c -> update { it.copy(customBackground = c) } },
                            )
                        }
                    }
                    SectionFooter(ThemePresets.byId(config.themeId).description)
                }
            }

            // Animations
            item(key = "anim") {
                Column(Modifier.padding(top = 32.dp)) {
                    SectionHeader("Animations")
                    SettingsGroup {
                        SettingsRow(title = "Entrée des éléments", icon = Icons.Outlined.Animation)
                        SegmentedControl(
                            options = EntranceAnimation.values().toList(),
                            selected = config.entrance,
                            label = { entranceLabel(it) },
                            onSelect = { e -> update { it.copy(entrance = e) } },
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                        )
                        RowDivider()
                        SwitchRow(
                            title = "Transitions d'écran",
                            icon = Icons.Outlined.SwapHoriz,
                            checked = config.screenTransitions,
                            onCheckedChange = { v -> update { it.copy(screenTransitions = v) } },
                        )
                        RowDivider()
                        SwitchRow(
                            title = "Animation des listes",
                            icon = Icons.Outlined.Reorder,
                            checked = config.listAnimations,
                            onCheckedChange = { v -> update { it.copy(listAnimations = v) } },
                        )
                        RowDivider()
                        SwitchRow(
                            title = "Rebond au toucher",
                            icon = Icons.Outlined.TouchApp,
                            checked = config.pressBounce,
                            onCheckedChange = { v -> update { it.copy(pressBounce = v) } },
                        )
                        RowDivider()
                        SwitchRow(
                            title = "Vibrations",
                            subtitle = "Léger retour haptique",
                            icon = Icons.Outlined.Vibration,
                            checked = config.haptics,
                            onCheckedChange = { v -> update { it.copy(haptics = v) } },
                        )
                        RowDivider()
                        SliderRow(
                            title = "Vitesse",
                            icon = Icons.Outlined.Speed,
                            value = config.animationSpeed,
                            valueRange = 0.5f..2f,
                            onValueChange = { v -> update { it.copy(animationSpeed = v) } },
                            valueLabel = { String.format(Locale.FRANCE, "×%.1f", it) },
                        )
                    }
                }
            }

            // Style
            item(key = "style") {
                Column(Modifier.padding(top = 32.dp)) {
                    SectionHeader("Style")
                    SettingsGroup {
                        NavigationRow(
                            title = "Police",
                            icon = Icons.Outlined.TextFields,
                            value = fontLabel(config.font),
                            onClick = { showFontSheet = true },
                        )
                        RowDivider()
                        SliderRow(
                            title = "Coins arrondis",
                            icon = Icons.Outlined.RoundedCorner,
                            value = config.cornerRadiusDp.toFloat(),
                            valueRange = 0f..32f,
                            onValueChange = { v -> update { it.copy(cornerRadiusDp = v.toInt()) } },
                            valueLabel = { if (it.toInt() == 0) "Désactivé" else "${it.toInt()} dp" },
                        )
                        RowDivider()
                        SwitchRow(
                            title = "Aurore",
                            subtitle = "Halo coloré en arrière-plan",
                            icon = Icons.Outlined.BlurOn,
                            checked = config.aurora,
                            onCheckedChange = { v -> update { it.copy(aurora = v) } },
                        )
                        AnimatedVisibility(
                            visible = config.aurora,
                            enter = PsMotion.Expand,
                            exit = PsMotion.Collapse,
                        ) {
                            Column {
                                RowDivider()
                                SliderRow(
                                    title = "Intensité",
                                    value = config.auroraIntensity,
                                    valueRange = 0.05f..0.6f,
                                    onValueChange = { v -> update { it.copy(auroraIntensity = v) } },
                                    valueLabel = { "${(it * 100).toInt()} %" },
                                    modifier = Modifier.padding(start = 48.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Système
            item(key = "system") {
                Column(Modifier.padding(top = 32.dp)) {
                    SectionHeader("Système")
                    SettingsGroup {
                        SwitchRow(
                            title = "Teinter les barres système",
                            subtitle = "Barre d'état et de navigation aux couleurs du thème",
                            icon = Icons.Outlined.Smartphone,
                            checked = config.tintSystemBars,
                            onCheckedChange = { v -> update { it.copy(tintSystemBars = v) } },
                        )
                        RowDivider()
                        SettingsRow(
                            title = "Réinitialiser les réglages",
                            icon = Icons.Outlined.RestartAlt,
                            titleColor = PsColors.Destructive,
                            rowModifier = Modifier.clickable(role = Role.Button) { showResetDialog = true },
                        )
                    }
                }
            }
        }
    }

    if (showFontSheet) {
        FontSheet(
            selected = config.font,
            onSelect = { f ->
                update { it.copy(font = f) }
                showFontSheet = false
            },
            onDismiss = { showFontSheet = false },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = PsColors.SurfaceHigh,
            titleContentColor = PsColors.TextPrimary,
            textContentColor = PsColors.TextSecondary,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Réinitialiser ?", style = PsType.RowTitle.copy(fontWeight = PsType.Button.fontWeight)) },
            text = {
                Text(
                    "Tous les réglages de ${target.displayName} reviendront à leurs valeurs par défaut.",
                    style = PsType.RowSubtitle,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    repo.reset(appId)
                    showResetDialog = false
                    message("Réglages réinitialisés")
                }) {
                    Text("Réinitialiser", color = PsColors.Destructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Annuler", color = PsColors.TextPrimary)
                }
            },
        )
    }
}

@Composable
private fun CustomColors(
    accent: Int,
    background: Int,
    onAccent: (Int) -> Unit,
    onBackground: (Int) -> Unit,
) {
    val hsl = ColorMath.toHsl(accent)
    Column(Modifier.fillMaxWidth()) {
        RowDivider(indent = 16.dp)
        Text(
            "Couleur d'accent",
            style = PsType.RowSubtitle,
            color = PsColors.TextSecondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
        )
        AccentChoices.chunked(5).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                row.forEach { choice ->
                    val argb = choice.color.toArgb()
                    ColorDot(
                        color = choice.color,
                        selected = accent == argb,
                        onClick = { onAccent(argb) },
                        contentDescription = choice.name,
                    )
                }
            }
        }
        ThinSlider(
            value = hsl[0],
            valueRange = 0f..360f,
            onValueChange = { h ->
                val s = hsl[1].coerceAtLeast(0.55f)
                val l = hsl[2].coerceIn(0.45f, 0.7f)
                onAccent(ColorMath.fromHsl(h, s, l))
            },
            trackBrush = hueBrush(),
            thumbColor = Color(accent),
            contentDescription = "Teinte de l'accent",
            stateLabel = "${hsl[0].toInt()}°",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
        )
        Text(
            "Fond",
            style = PsType.RowSubtitle,
            color = PsColors.TextSecondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 10.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BackgroundChoices.forEach { choice ->
                val argb = choice.color.toArgb()
                ColorDot(
                    color = choice.color,
                    selected = background == argb,
                    onClick = { onBackground(argb) },
                    contentDescription = "Fond ${choice.name}",
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontSheet(selected: FontChoice, onSelect: (FontChoice) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PsColors.Surface,
        contentColor = PsColors.TextPrimary,
        scrimColor = Color.Black.copy(alpha = 0.5f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .selectableGroup(),
        ) {
            Text(
                "Police",
                style = PsType.RowTitle.copy(fontWeight = PsType.Button.fontWeight),
                color = PsColors.TextPrimary,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
            )
            FontChoice.values().forEach { choice ->
                val isSelected = choice == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(choice) }
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = fontLabel(choice),
                        style = PsType.RowTitle.copy(fontFamily = fontFamilyFor(choice)),
                        color = PsColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = PsColors.Accent,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Spacer(Modifier.width(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
