# Patch Studio — Architecture & contrats d'équipe

Module Xposed (API legacy 82, compatible **LSPatch** et LSPosed) + application
« launcher » Compose. Cible actuelle : **Deezer** (`deezer.android.app`).
Uniquement du **cosmétique** (thèmes, couleurs, polices, animations, coins
arrondis). **Interdit** : tout contournement d'abonnement, paywall, pub, DRM,
téléchargements, vérifications de licence. Ne jamais toucher aux classes
métier/réseau/lecteur de l'application cible.

Aucun SDK Android n'est disponible dans le conteneur de dev : le build est
vérifié par GitHub Actions. Écrire du code qui compile du premier coup
(imports explicites, API stables, pas d'API expérimentales non annotées).

## Stack (versions figées — ne pas changer)

| Élément | Version |
|---|---|
| Gradle wrapper | 8.11.1 |
| AGP | 8.7.3 |
| Kotlin (JVM + Android + compose plugin `org.jetbrains.kotlin.plugin.compose`) | 2.0.21 |
| Compose BOM | 2024.12.01 |
| activity-compose | 1.9.3 |
| core-ktx | 1.15.0 |
| lifecycle-runtime-compose / lifecycle-viewmodel-compose | 2.8.7 |
| JDK / jvmTarget | 17 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 28 |
| JUnit | 4.13.2 |

Versions centralisées dans `gradle/libs.versions.toml`.

## Modules & propriété des fichiers

Chaque rôle n'écrit **que** dans ses fichiers. Les contrats ci‑dessous sont la
seule source de vérité entre rôles.

```
settings.gradle.kts, build.gradle.kts, gradle.properties,
gradle/libs.versions.toml, gradle/wrapper/*, gradlew*         -> Build
.github/workflows/build.yml, keystore/, .gitignore             -> Build
xposed-stubs/                                                   -> Build
app/build.gradle.kts, app/proguard-rules.pro                    -> Build
app/src/main/AndroidManifest.xml, app/src/main/res/**           -> Build
  (sauf res/values/strings.xml : Build crée, tous peuvent ajouter)
app/src/main/assets/xposed_init                                 -> Build

core/  (module Kotlin JVM pur, testé en JUnit)                  -> Hook engineer
app/src/main/java/com/powermax/patchstudio/hook/**              -> Hook engineer
app/src/main/java/com/powermax/patchstudio/data/**              -> Hook engineer

app/src/main/java/com/powermax/patchstudio/ui/theme/**          -> Designer / motion
app/src/main/java/com/powermax/patchstudio/ui/components/**     -> Designer / motion
app/src/main/java/com/powermax/patchstudio/ui/motion/**         -> Designer / motion

app/src/main/java/com/powermax/patchstudio/MainActivity.kt      -> UI dev
app/src/main/java/com/powermax/patchstudio/ui/AppRoot.kt        -> UI dev
app/src/main/java/com/powermax/patchstudio/ui/home/**           -> UI dev
app/src/main/java/com/powermax/patchstudio/ui/app/**            -> UI dev
app/src/main/java/com/powermax/patchstudio/ui/guide/**          -> UI dev
```

- `applicationId` / namespace : `com.powermax.patchstudio`
- Nom affiché : **Patch Studio**
- Module `:xposed-stubs` : `java-library` contenant des stubs de
  `de.robv.android.xposed.*` (IXposedHookLoadPackage, XC_MethodHook (+
  MethodHookParam, Unhook), XC_MethodReplacement, XposedBridge,
  XposedHelpers, callbacks.XC_LoadPackage (+ LoadPackageParam)). Consommé en
  `compileOnly(project(":xposed-stubs"))` par `:app` → jamais empaqueté.
- Module `:core` : `org.jetbrains.kotlin.jvm`, consommé en
  `implementation(project(":core"))` par `:app`.

## Contrat `:core` (package `com.powermax.patchstudio.core`)

Couleurs = `Int` ARGB (comme `android.graphics.Color`). Aucune dépendance Android.

```kotlin
// TargetApps.kt
data class TargetApp(
    val id: String,                 // "deezer"
    val packageName: String,        // "deezer.android.app"
    val displayName: String,        // "Deezer"
    val tagline: String,            // texte court FR
    val brandColor: Int,            // 0xFFA238FF.toInt()
    val brandHueRange: ClosedFloatingPointRange<Float>, // teintes "accent" d'origine, ex 250f..300f
)
object TargetApps {
    val DEEZER: TargetApp
    val ALL: List<TargetApp>
    fun byId(id: String): TargetApp?
    fun byPackage(pkg: String): TargetApp?
}

// Themes.kt
data class Palette(val accent: Int, val background: Int, val surface: Int, val onAccent: Int)
data class ThemePreset(val id: String, val name: String, val description: String, val palette: Palette?)
// palette == null  => thème "original" (aucun remappage couleur)
object ThemePresets {
    val ORIGINAL: ThemePreset        // id "original"
    val ALL: List<ThemePreset>       // original, amoled, synthwave, ocean, foret, sunset, sakura, custom
    fun byId(id: String): ThemePreset // fallback ORIGINAL
    const val CUSTOM_ID = "custom"
}

// StyleConfig.kt
enum class EntranceAnimation { NONE, FADE, SLIDE_UP, ZOOM, BOUNCE }
enum class FontChoice { DEFAULT, CONDENSED, LIGHT, MEDIUM, SERIF, MONO, CASUAL }
data class StyleConfig(
    val enabled: Boolean = true,
    val themeId: String = "amoled",
    val customAccent: Int = 0xFF00E5FF.toInt(),
    val customBackground: Int = 0xFF05070D.toInt(),
    val tintSystemBars: Boolean = true,
    val entrance: EntranceAnimation = EntranceAnimation.SLIDE_UP,
    val screenTransitions: Boolean = true,
    val animationSpeed: Float = 1f,       // 0.5 (lent) .. 2.0 (rapide) ; durée = base / speed
    val listAnimations: Boolean = true,
    val pressBounce: Boolean = true,
    val haptics: Boolean = false,
    val cornerRadiusDp: Int = 14,         // 0..32, 0 = désactivé
    val font: FontChoice = FontChoice.DEFAULT,
    val aurora: Boolean = false,
    val auroraIntensity: Float = 0.25f,   // 0.05..0.6
) {
    /** null si thème original ou si !enabled */
    fun resolvedPalette(): Palette?
    fun toMap(): Map<String, String>
    companion object {
        fun fromMap(map: Map<String, String>): StyleConfig  // tolérant : clés absentes/invalides => défaut
        val DEFAULT: StyleConfig
    }
}

// ColorMath.kt — HSL utilitaires purs
object ColorMath {
    fun toHsl(color: Int): FloatArray      // [h 0..360, s 0..1, l 0..1]
    fun fromHsl(h: Float, s: Float, l: Float, alpha: Int = 255): Int
    fun blend(a: Int, b: Int, t: Float): Int
    fun luminance(color: Int): Float
    fun onColorFor(background: Int): Int   // noir ou blanc lisible
}

// ColorRemapper.kt — thread-safe, cache interne
class ColorRemapper(palette: Palette, brandHueRange: ClosedFloatingPointRange<Float>) {
    fun map(color: Int): Int
}

// ConfigProtocol.kt — protocole app <-> process hooké
object ConfigProtocol {
    const val MODULE_PACKAGE = "com.powermax.patchstudio"
    const val ACTION_UPDATE = "com.powermax.patchstudio.action.UPDATE_CONFIG"   // app -> cible (broadcast, setPackage(cible))
    const val ACTION_HELLO  = "com.powermax.patchstudio.action.HELLO"          // cible -> app (broadcast, setPackage(MODULE_PACKAGE))
    const val PERMISSION    = "com.powermax.patchstudio.permission.CONFIG"     // signature
    const val EXTRA_APP_ID = "ps_app_id"
    const val EXTRA_CONFIG_PREFIX = "ps_cfg_"   // chaque entrée de toMap() => extra String "ps_cfg_<key>"
    const val EXTRA_VERSION = "ps_version"
    const val PROVIDER_AUTHORITY = "com.powermax.patchstudio.config"
    // content://<authority>/<appId> -> curseur colonnes ("key","value")
    fun encode(config: StyleConfig): Map<String, String>        // clés préfixées
    fun decode(extras: Map<String, String?>): StyleConfig?        // null si aucune clé préfixée
}
```

## Contrat `data` (package `com.powermax.patchstudio.data`, côté app)

```kotlin
class ConfigRepository private constructor(context: Context) {
    fun configFlow(appId: String): StateFlow<StyleConfig>
    fun current(appId: String): StyleConfig
    fun update(appId: String, transform: (StyleConfig) -> StyleConfig)
    fun reset(appId: String)
    /** Envoie la config au process cible (broadcast) si lancé. */
    fun pushToTarget(appId: String)
    /** Lance l'app cible avec la config en extras. Renvoie false si non installée. */
    fun launchTarget(context: Context, appId: String): Boolean
    fun isInstalled(appId: String): Boolean
    /** Timestamp (ms) du dernier HELLO reçu du module dans l'app cible, 0 si jamais. */
    fun lastHelloFlow(appId: String): StateFlow<Long>
    companion object { fun get(context: Context): ConfigRepository }
}
// ConfigProvider : ContentProvider (lecture seule) exposant la config
// StatusReceiver : BroadcastReceiver ACTION_HELLO -> enregistre le timestamp
```
Manifest (Build) déclare : `com.powermax.patchstudio.data.ConfigProvider`
(authority `com.powermax.patchstudio.config`, exported=true) et
`com.powermax.patchstudio.data.StatusReceiver` (exported=true, action HELLO),
`<queries><package android:name="deezer.android.app"/></queries>`, la
permission signature `com.powermax.patchstudio.permission.CONFIG` (définie +
uses-permission).

## Contrat hook (package `com.powermax.patchstudio.hook`)

- Point d'entrée : `com.powermax.patchstudio.hook.HookEntry` (implémente
  `IXposedHookLoadPackage`), référencé dans `assets/xposed_init`.
- `res/values/arrays.xml` : `xposed_scope` = [`deezer.android.app`].
- Moteur générique `UiStyler` réutilisable pour toute future app ; chaque
  app = un `TargetApp` dans `:core` + éventuellement un `AppHooker` dédié.

## Contrat Design system (packages `ui.theme`, `ui.components`, `ui.motion`)

Toutes ces API sont `@Composable` publiques ; le UI dev ne doit utiliser que
celles-ci (plus Material3 standard).

```kotlin
// ui/theme
@Composable fun PatchStudioTheme(content: @Composable () -> Unit)   // toujours sombre
object PsColors { val Background: Color; val Surface: Color; val SurfaceHigh: Color;
                  val Primary: Color; val Secondary: Color; val Tertiary: Color;
                  val TextPrimary: Color; val TextSecondary: Color; val Outline: Color }
object PsShapes { val Card: Shape; val Pill: Shape; val Sheet: Shape }
// typo via MaterialTheme.typography (display/headline/title/body/label configurés)

// ui/motion
object PsMotion {
    val Spring: SpringSpec<Float>           // ressort par défaut, bouncy doux
    val SpringSnappy: SpringSpec<Float>
    const val StaggerMs: Int                 // ~45
    fun <T> tween(durationMs: Int = 420): TweenSpec<T>   // easing emphasized
    val EmphasizedEasing: Easing
}
/** Apparition décalée (fade + slide + scale) quand index entre en composition. */
fun Modifier.staggeredEntrance(index: Int, baseDelayMs: Int = 0): Modifier
/** Effet de pression ressort (scale 0.95) + haptique optionnelle, conserve clickable. */
fun Modifier.bounceClick(enabled: Boolean = true, haptic: Boolean = true, onClick: () -> Unit): Modifier
/** Halo lumineux animé autour de l'élément. */
fun Modifier.glow(color: Color, radius: Dp = 24.dp, animated: Boolean = true): Modifier

// ui/components
@Composable fun AuroraBackground(modifier: Modifier = Modifier, colors: List<Color> = <défaut>, content: @Composable BoxScope.() -> Unit)
@Composable fun GlassCard(modifier: Modifier = Modifier, accent: Color = PsColors.Primary, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)
@Composable fun ShimmerText(text: String, style: TextStyle, modifier: Modifier = Modifier, colors: List<Color> = <défaut>)
@Composable fun PulsingDot(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp)
@Composable fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier)
@Composable fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit, icon: ImageVector? = null, modifier: Modifier = Modifier)
@Composable fun SliderRow(title: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit, valueLabel: (Float) -> String, modifier: Modifier = Modifier, steps: Int = 0)
@Composable fun HueSlider(color: Color, onColorChange: (Color) -> Unit, modifier: Modifier = Modifier)   // choix de teinte + luminosité
@Composable fun <T> ChipSelector(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier)  // indicateur animé qui glisse
@Composable fun ThemeSwatch(name: String, accent: Color, background: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)  // vignette animée
@Composable fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, colors: List<Color> = <défaut>)  // dégradé animé + shimmer
@Composable fun EqualizerBars(color: Color, modifier: Modifier = Modifier, bars: Int = 5, playing: Boolean = true)
@Composable fun SpinningVinyl(accent: Color, modifier: Modifier = Modifier, spinning: Boolean = true)
@Composable fun AnimatedLogo(modifier: Modifier = Modifier, size: Dp = 96.dp)   // logo Patch Studio animé (anneaux, rotation, morph)
```

## Écrans (UI dev)

- `MainActivity` : edge-to-edge, `PatchStudioTheme { AppRoot() }`.
- `AppRoot` : navigation par état (`sealed interface Screen { Home; AppSettings(appId); Guide }`),
  `AnimatedContent` avec transitions riches (scale+fade+slide), `BackHandler`.
- `ui/home/HomeScreen` : AuroraBackground, AnimatedLogo + ShimmerText titre,
  statut module (PulsingDot selon `lastHelloFlow`), cartes des apps (`TargetApps.ALL`)
  en entrée décalée, carte « Bientôt d'autres applis », accès au guide.
- `ui/app/AppSettingsScreen(appId)` : en-tête avec `TargetPreview` (faux écran de
  l'app cible rendu avec la palette choisie : vinyle qui tourne, égaliseur,
  liste qui s'anime selon les réglages d'animation), sections Thème
  (ThemeSwatch carrousel + HueSlider si custom), Animations, Style
  (police, coins, aurora), Système ; `GradientButton` « Appliquer et ouvrir Deezer »
  → `ConfigRepository.update/launchTarget`, snackbar animée.
- `ui/guide/GuideScreen` : étapes illustrées et animées pour LSPatch
  (installer Patch Studio, patcher Deezer en mode local, signature bypass
  niveau 2, activer le module, ouvrir Patch Studio).
