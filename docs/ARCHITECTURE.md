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
enum class LyricsBackground { NONE, GRADIENT, DARK } // GRADIENT = dégradé lent dérivé de l'accent ; DARK = uni très sombre
enum class LyricsAlignment { LEFT, CENTER }
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
    // Écran des paroles (détecté via les noms d'id de ressource contenant "lyric")
    val lyricsEnhanced: Boolean = true,
    val lyricsBackground: LyricsBackground = LyricsBackground.GRADIENT,
    val lyricsTextScale: Float = 1.15f,   // 0.9..1.6 (LYRICS_SCALE_RANGE)
    val lyricsAlignment: LyricsAlignment = LyricsAlignment.LEFT,
    val lyricsActiveZoom: Boolean = true, // la ligne en cours grossit légèrement (ressort)
    val lyricsFadeEdges: Boolean = true,  // fondu en haut/bas de la liste
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
- `LyricsStyler` (branché par `UiStyler`, même parcours d'arbre throttlé) :
  conteneur de paroles = vue dont `getResourceEntryName(id)` contient « lyric »
  (aucun repli heuristique). Agrandit/graisse/aligne les TextView, zoom de la
  ligne active via `TextView.setTextColor` (couleur plus vive ⇒ ligne active),
  fondu des bords des listes, fond `LyricsGradientDrawable` (dégradé) ou uni.
  Log une fois par nom d'id détecté (`PatchStudio: lyrics id …`).
- Moteur générique `UiStyler` réutilisable pour toute future app ; chaque
  app = un `TargetApp` dans `:core` + éventuellement un `AppHooker` dédié.

## Contrat Design system (packages `ui.theme`, `ui.components`, `ui.motion`)

Direction : sobre, type Réglages iOS / Linear. Fond plat, surfaces neutres,
**un seul accent** (violet `#8B5CF6`) réservé aux switchs actifs, à la
sélection et au bouton principal. Aucun dégradé décoratif, halo, néon ni
animation en boucle. Typo système, marges 20dp, rythme 8dp.

```kotlin
// ui/theme
@Composable fun PatchStudioTheme(content: @Composable () -> Unit)   // toujours sombre
object PsColors { Background #0E0E10; Surface #18181B; SurfaceHigh #222226; Hairline #2A2A2F;
                  Control #3F3F46; TextPrimary #F4F4F5; TextSecondary #A1A1AA; TextTertiary #71717A;
                  Accent #8B5CF6; OnAccent; Positive; Neutral; Destructive }
object PsType   { ScreenTitle (32sp SemiBold, -0.5); SectionLabel (13sp Medium, capitales);
                  RowTitle (16sp); RowSubtitle (14sp); Caption (13sp); Button }
object PsShapes { Group (16dp); IconBadge (8dp); Button (14dp); Tile; Segment; SegmentItem }

// ui/motion
object PsMotion { DurationShort = 150; DurationMedium = 300; Standard = FastOutSlowIn
                  fun sharedAxisX(forward: Boolean, offsetPx: Int): ContentTransform
                  val Expand: EnterTransition; val Collapse: ExitTransition }
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.98f): Modifier

// ui/components
SectionHeader(title) · SectionFooter(text) · SettingsGroup { … } · RowDivider(indent)
IconBadge(icon) · StatusDot(color) · SettingsRow(title, subtitle, icon|leading, trailing)
NavigationRow(title, onClick, value?) · SwitchRow(title, checked, onCheckedChange)
SliderRow(title, value, valueRange, onValueChange, valueLabel) · ThinSlider(…) · hueBrush()
SegmentedControl(options, selected, label, onSelect) · ColorDot(color, selected, onClick)
ThemeTile(name, background, surface, accent, selected, onClick) · TextLink(text, onClick)
PrimaryButton(text, onClick, icon?) · SecondaryTextButton(text, onClick)
AppIcon(packageName, label, fallbackColor)   // vraie icône via PackageManager, sinon initiale
```

## Écrans (UI dev)

- `MainActivity` : edge-to-edge (barres système transparentes, icônes claires),
  `PatchStudioTheme { AppRoot() }`.
- `AppRoot` : navigation par état (`sealed interface Screen { Home; AppSettings(appId); Guide }`),
  `AnimatedContent` en shared axis X (fondu + glissement 30dp, 300ms),
  `SaveableStateHolder` pour garder le scroll, `BackHandler`.
- `ui/home/HomeScreen` : grand titre, carte statut (point vert/gris selon
  `lastHelloFlow`, lien vers le guide), groupe « Applications » (icône réelle,
  thème actif, chevron ; ligne grisée « D'autres applications arrivent »),
  groupe « Aide » (guide), version en pied de page.
- `ui/app/AppSettingsScreen(appId)` : `LargeTopAppBar` repliable, `PlayerPreview`
  (mini lecteur dans la palette choisie, couleurs animées), interrupteur
  général, sections Thème (vignettes + couleurs perso), Animations, Style
  (police via feuille modale, coins, aurore), Système (barres, réinitialiser
  avec confirmation). Bouton fixe « Appliquer et ouvrir Deezer » +
  « Appliquer sans ouvrir », snackbar standard.
- `ui/guide/GuideScreen` : liste numérotée des étapes LSPatch (installer Patch
  Studio, installer LSPatch, patcher Deezer en mode local avec signature
  bypass niveau 2, activer le module, choisir son style) + astuce.
