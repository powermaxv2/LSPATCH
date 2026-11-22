package com.powermax.patchstudio.ui.app

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Animation
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Gradient
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.RoundedCorner
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.powermax.patchstudio.core.ColorMath
import com.powermax.patchstudio.core.EntranceAnimation
import com.powermax.patchstudio.core.FontChoice
import com.powermax.patchstudio.core.LyricsAlignment
import com.powermax.patchstudio.core.LyricsBackground
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
import com.powermax.patchstudio.ui.components.StatusDot
import com.powermax.patchstudio.ui.components.SwitchRow
import com.powermax.patchstudio.ui.components.ThemeTile
import com.powermax.patchstudio.ui.components.ThinSlider
import com.powermax.patchstudio.ui.components.hueBrush
import com.powermax.patchstudio.ui.motion.PsMotion
import com.powermax.patchstudio.ui.theme.PsColors
import com.powermax.patchstudio.ui.theme.PsShapes
import com.powermax.patchstudio.ui.theme.PsType
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private fun lyricsBackgroundLabel(b: LyricsBackground): String = when (b) {
    LyricsBackground.NONE -> "Aucun"
    LyricsBackground.GRADIENT -> "Dégradé"
    LyricsBackground.DARK -> "Sombre"
}

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

private fun roundStep(value: Float, step: Float): Float =
    ((value / step).roundToInt() * step)

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
private val WarningAmber = Color(0xFFF59E0B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(appId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val repo = remember(context) { ConfigRepository.get(context) }
    val target = TargetApps.byId(appId) ?: TargetApps.DEEZER
    val config by repo.configFlow(appId).collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()

    var showFontSheet by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showShareDialog by rememberSaveable { mutableStateOf(false) }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    var hexEditTarget by rememberSaveable { mutableStateOf<String?>(null) } // "accent" or "bg"

    fun update(transform: (StyleConfig) -> StyleConfig) = repo.update(appId, transform)
    fun message(text: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }

    val dependentAlpha = if (config.enabled) 1f else 0.45f

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
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Partager la configuration")
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
                            if (repo.pushToTarget(appId)) {
                                message("Réglages envoyés à ${target.displayName}")
                            } else {
                                message("${target.displayName} n'est pas installé")
                            }
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
                Column(Modifier.padding(top = 32.dp).alpha(dependentAlpha)) {
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
                                onEditAccentHex = { hexEditTarget = "accent" },
                                onEditBackgroundHex = { hexEditTarget = "bg" },
                            )
                        }
                    }
                    SectionFooter(ThemePresets.byId(config.themeId).description)
                }
            }

            // Animations
            item(key = "anim") {
                Column(Modifier.padding(top = 32.dp).alpha(dependentAlpha)) {
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
                            valueRange = StyleConfig.SPEED_RANGE,
                            steps = 14,
                            onValueChange = { v ->
                                update { it.copy(animationSpeed = roundStep(v, 0.1f).coerceIn(0.5f, 2f)) }
                            },
                            valueLabel = { String.format(Locale.FRANCE, "×%.1f", it) },
                        )
                    }
                }
            }

            // Style
            item(key = "style") {
                Column(Modifier.padding(top = 32.dp).alpha(dependentAlpha)) {
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
                            onValueChange = { v -> update { it.copy(cornerRadiusDp = v.roundToInt()) } },
                            valueLabel = { if (it.roundToInt() == 0) "Désactivé" else "${it.roundToInt()} dp" },
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
                                    valueRange = StyleConfig.AURORA_RANGE,
                                    steps = 10,
                                    onValueChange = { v ->
                                        update { it.copy(auroraIntensity = roundStep(v, 0.05f).coerceIn(0.05f, 0.6f)) }
                                    },
                                    valueLabel = { "${(it * 100).roundToInt()} %" },
                                    modifier = Modifier.padding(start = 48.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Paroles
            item(key = "lyrics") {
                Column(Modifier.padding(top = 32.dp).alpha(dependentAlpha)) {
                    SectionHeader("Paroles")
                    SettingsGroup {
                        SwitchRow(
                            title = "Paroles embellies",
                            subtitle = "Texte plus grand, ligne en cours mise en avant",
                            icon = Icons.Outlined.MusicNote,
                            checked = config.lyricsEnhanced,
                            onCheckedChange = { v -> update { it.copy(lyricsEnhanced = v) } },
                        )
                        AnimatedVisibility(
                            visible = config.lyricsEnhanced,
                            enter = PsMotion.Expand,
                            exit = PsMotion.Collapse,
                        ) {
                            Column {
                                RowDivider()
                                SettingsRow(title = "Fond", icon = Icons.Outlined.Gradient)
                                SegmentedControl(
                                    options = LyricsBackground.values().toList(),
                                    selected = config.lyricsBackground,
                                    label = { lyricsBackgroundLabel(it) },
                                    onSelect = { b -> update { it.copy(lyricsBackground = b) } },
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                                )
                                RowDivider()
                                SettingsRow(title = "Alignement", icon = Icons.AutoMirrored.Outlined.FormatAlignLeft)
                                SegmentedControl(
                                    options = LyricsAlignment.values().toList(),
                                    selected = config.lyricsAlignment,
                                    label = { if (it == LyricsAlignment.LEFT) "Gauche" else "Centré" },
                                    onSelect = { a -> update { it.copy(lyricsAlignment = a) } },
                                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                                )
                                RowDivider()
                                SliderRow(
                                    title = "Taille du texte",
                                    icon = Icons.Outlined.FormatSize,
                                    value = config.lyricsTextScale,
                                    valueRange = StyleConfig.LYRICS_SCALE_RANGE,
                                    steps = 13,
                                    onValueChange = { v ->
                                        update { it.copy(lyricsTextScale = roundStep(v, 0.05f).coerceIn(0.9f, 1.6f)) }
                                    },
                                    valueLabel = { "${(it * 100).roundToInt()} %" },
                                )
                                RowDivider()
                                SwitchRow(
                                    title = "Zoom sur la ligne en cours",
                                    icon = Icons.Outlined.ZoomIn,
                                    checked = config.lyricsActiveZoom,
                                    onCheckedChange = { v -> update { it.copy(lyricsActiveZoom = v) } },
                                )
                                RowDivider()
                                SwitchRow(
                                    title = "Ligne active en couleur d'accent",
                                    subtitle = "Teinte la ligne chantée avec l'accent du thème",
                                    icon = Icons.Outlined.Palette,
                                    checked = config.lyricsAccentActive,
                                    onCheckedChange = { v -> update { it.copy(lyricsAccentActive = v) } },
                                )
                                RowDivider()
                                SwitchRow(
                                    title = "Fondu en haut et en bas",
                                    icon = Icons.Outlined.Opacity,
                                    checked = config.lyricsFadeEdges,
                                    onCheckedChange = { v -> update { it.copy(lyricsFadeEdges = v) } },
                                )
                            }
                        }
                    }
                }
            }

            // Système & partage
            item(key = "system") {
                Column(Modifier.padding(top = 32.dp)) {
                    SectionHeader("Système")
                    SettingsGroup {
                        SwitchRow(
                            title = "Teinter les barres système",
                            subtitle = "Barre d'état et de navigation aux couleurs du thème",
                            icon = Icons.Outlined.Smartphone,
                            checked = config.tintSystemBars,
                            enabled = config.enabled,
                            onCheckedChange = { v -> update { it.copy(tintSystemBars = v) } },
                        )
                        RowDivider()
                        NavigationRow(
                            title = "Partager la configuration",
                            subtitle = "Copier ou envoyer un code de style",
                            icon = Icons.Outlined.Share,
                            onClick = { showShareDialog = true },
                        )
                        RowDivider()
                        NavigationRow(
                            title = "Importer une configuration",
                            subtitle = "Coller un code PS1:…",
                            icon = Icons.Outlined.Download,
                            onClick = { showImportDialog = true },
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

    hexEditTarget?.let { which ->
        val isAccent = which == "accent"
        HexColorDialog(
            title = if (isAccent) "Couleur d'accent" else "Couleur de fond",
            initialColor = if (isAccent) config.customAccent else config.customBackground,
            onConfirm = { parsed ->
                update { if (isAccent) it.copy(customAccent = parsed) else it.copy(customBackground = parsed) }
                hexEditTarget = null
            },
            onDismiss = { hexEditTarget = null },
        )
    }

    if (showShareDialog) {
        val code = remember(config) { config.toShareCode() }
        ShareConfigDialog(
            code = code,
            onCopy = {
                clipboard.setText(AnnotatedString(code))
                showShareDialog = false
                message("Code copié dans le presse-papiers")
            },
            onShare = {
                showShareDialog = false
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Style Patch Studio (${target.displayName})")
                    putExtra(Intent.EXTRA_TEXT, code)
                }
                try {
                    context.startActivity(Intent.createChooser(send, "Partager le style"))
                } catch (_: Exception) {
                    clipboard.setText(AnnotatedString(code))
                    message("Code copié dans le presse-papiers")
                }
            },
            onDismiss = { showShareDialog = false },
        )
    }

    if (showImportDialog) {
        ImportConfigDialog(
            onPasteFromClipboard = { clipboard.getText()?.text ?: "" },
            onImport = { imported ->
                repo.replace(appId, imported)
                showImportDialog = false
                message("Configuration importée (${ThemePresets.byId(imported.themeId).name})")
            },
            onDismiss = { showImportDialog = false },
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
                    val previous = config
                    repo.reset(appId)
                    showResetDialog = false
                    scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        val res = snackbar.showSnackbar(
                            message = "Réglages réinitialisés",
                            actionLabel = "Annuler",
                            duration = SnackbarDuration.Long,
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            repo.replace(appId, previous)
                        }
                    }
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
    onEditAccentHex: () -> Unit,
    onEditBackgroundHex: () -> Unit,
) {
    val hsl = ColorMath.toHsl(accent)
    val contrast = ColorMath.contrastRatio(accent, background)
    val contrastColor = when {
        contrast >= 4.5f -> PsColors.Positive
        contrast >= 3.0f -> WarningAmber
        else -> PsColors.Destructive
    }
    val contrastLabel = when {
        contrast >= 4.5f -> "Contraste optimal"
        contrast >= 3.0f -> "Contraste correct"
        else -> "Contraste faible"
    }

    Column(Modifier.fillMaxWidth()) {
        RowDivider(indent = 16.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Couleur d'accent", style = PsType.RowSubtitle, color = PsColors.TextSecondary, modifier = Modifier.weight(1f))
            HexChip(color = accent, onClick = onEditAccentHex)
        }
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
                val s = hsl[1].coerceAtLeast(0.35f)
                val l = hsl[2].coerceIn(0.35f, 0.78f)
                onAccent(ColorMath.fromHsl(h, s, l))
            },
            trackBrush = hueBrush(),
            thumbColor = Color(accent),
            contentDescription = "Teinte de l'accent",
            stateLabel = "${hsl[0].roundToInt()}°",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Saturation", style = PsType.Caption, color = PsColors.TextTertiary, modifier = Modifier.weight(1f))
            Text("${(hsl[1] * 100).roundToInt()} %", style = PsType.Caption, color = PsColors.TextSecondary)
        }
        ThinSlider(
            value = hsl[1],
            valueRange = 0.15f..1f,
            onValueChange = { s -> onAccent(ColorMath.fromHsl(hsl[0], s, hsl[2])) },
            activeColor = Color(accent),
            contentDescription = "Saturation de l'accent",
            stateLabel = "${(hsl[1] * 100).roundToInt()} %",
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Luminosité", style = PsType.Caption, color = PsColors.TextTertiary, modifier = Modifier.weight(1f))
            Text("${(hsl[2] * 100).roundToInt()} %", style = PsType.Caption, color = PsColors.TextSecondary)
        }
        ThinSlider(
            value = hsl[2],
            valueRange = 0.25f..0.82f,
            onValueChange = { l -> onAccent(ColorMath.fromHsl(hsl[0], hsl[1], l)) },
            activeColor = Color(accent),
            contentDescription = "Luminosité de l'accent",
            stateLabel = "${(hsl[2] * 100).roundToInt()} %",
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Fond", style = PsType.RowSubtitle, color = PsColors.TextSecondary, modifier = Modifier.weight(1f))
            HexChip(color = background, onClick = onEditBackgroundHex)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot(color = contrastColor)
            Spacer(Modifier.width(8.dp))
            Text(
                text = String.format(Locale.FRANCE, "%s (%.1f:1)", contrastLabel, contrast),
                style = PsType.Caption,
                color = if (contrast < 3.0f) PsColors.Destructive else PsColors.TextSecondary,
            )
        }
    }
}

@Composable
private fun HexChip(color: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(PsShapes.SegmentItem)
            .background(PsColors.SurfaceHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(color))
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = StyleConfig.hexRgb(color),
            style = PsType.Caption.copy(fontFamily = FontFamily.Monospace),
            color = PsColors.TextPrimary,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Outlined.Edit,
            contentDescription = "Modifier le code hexadécimal",
            tint = PsColors.TextTertiary,
            modifier = Modifier.size(13.dp),
        )
    }
}

@Composable
private fun HexColorDialog(
    title: String,
    initialColor: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf(StyleConfig.hexRgb(initialColor)) }
    val parsed = StyleConfig.parseHex(input)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PsColors.SurfaceHigh,
        titleContentColor = PsColors.TextPrimary,
        textContentColor = PsColors.TextSecondary,
        shape = RoundedCornerShape(20.dp),
        title = { Text(title, style = PsType.RowTitle.copy(fontWeight = FontWeight.SemiBold)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= 9) input = it.uppercase(Locale.ROOT) },
                    singleLine = true,
                    label = { Text("Code hexadécimal (#RRGGBB)") },
                    isError = input.isNotBlank() && parsed == null,
                    leadingIcon = {
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(Color(parsed ?: initialColor))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsColors.Accent,
                        unfocusedBorderColor = PsColors.Control,
                        focusedTextColor = PsColors.TextPrimary,
                        unfocusedTextColor = PsColors.TextPrimary,
                        focusedLabelColor = PsColors.Accent,
                        unfocusedLabelColor = PsColors.TextSecondary,
                        cursorColor = PsColors.Accent,
                    ),
                )
                if (input.isNotBlank() && parsed == null) {
                    Text("Format attendu : #RRGGBB (ex. #00E5FF)", style = PsType.Caption, color = PsColors.Destructive)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = parsed != null, onClick = { parsed?.let(onConfirm) }) {
                Text("Valider", color = if (parsed != null) PsColors.Accent else PsColors.TextTertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = PsColors.TextPrimary)
            }
        },
    )
}

@Composable
private fun ShareConfigDialog(
    code: String,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PsColors.SurfaceHigh,
        titleContentColor = PsColors.TextPrimary,
        textContentColor = PsColors.TextSecondary,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Partager le style", style = PsType.RowTitle.copy(fontWeight = FontWeight.SemiBold)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Ce code contient tous tes réglages actuels et peut être réimporté sur n'importe quel appareil.",
                    style = PsType.RowSubtitle,
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(PsShapes.Segment)
                        .background(PsColors.Background)
                        .padding(12.dp),
                ) {
                    Text(
                        text = code,
                        style = PsType.Caption.copy(fontFamily = FontFamily.Monospace),
                        color = PsColors.TextPrimary,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onCopy) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, tint = PsColors.TextPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copier", color = PsColors.TextPrimary)
                }
                TextButton(onClick = onShare) {
                    Text("Partager", color = PsColors.Accent)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = PsColors.TextSecondary)
            }
        },
    )
}

@Composable
private fun ImportConfigDialog(
    onPasteFromClipboard: () -> String,
    onImport: (StyleConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val decoded = remember(input) { StyleConfig.fromShareCode(input) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PsColors.SurfaceHigh,
        titleContentColor = PsColors.TextPrimary,
        textContentColor = PsColors.TextSecondary,
        shape = RoundedCornerShape(20.dp),
        title = { Text("Importer un style", style = PsType.RowTitle.copy(fontWeight = FontWeight.SemiBold)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    maxLines = 4,
                    label = { Text("Code PS1:…") },
                    isError = input.isNotBlank() && decoded == null,
                    trailingIcon = {
                        IconButton(onClick = { input = onPasteFromClipboard() }) {
                            Icon(Icons.Outlined.ContentPaste, contentDescription = "Coller", tint = PsColors.TextSecondary)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PsColors.Accent,
                        unfocusedBorderColor = PsColors.Control,
                        focusedTextColor = PsColors.TextPrimary,
                        unfocusedTextColor = PsColors.TextPrimary,
                        focusedLabelColor = PsColors.Accent,
                        unfocusedLabelColor = PsColors.TextSecondary,
                        cursorColor = PsColors.Accent,
                    ),
                )
                when {
                    decoded != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(color = PsColors.Positive)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Thème ${ThemePresets.byId(decoded.themeId).name} · Police ${fontLabel(decoded.font)}",
                            style = PsType.Caption,
                            color = PsColors.Positive,
                        )
                    }
                    input.isNotBlank() -> Text(
                        "Code invalide. Il doit commencer par « PS1: ».",
                        style = PsType.Caption,
                        color = PsColors.Destructive,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = decoded != null, onClick = { decoded?.let(onImport) }) {
                Text("Appliquer", color = if (decoded != null) PsColors.Accent else PsColors.TextTertiary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler", color = PsColors.TextPrimary)
            }
        },
    )
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
