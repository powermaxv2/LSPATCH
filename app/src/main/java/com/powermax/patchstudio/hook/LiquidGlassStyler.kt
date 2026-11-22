package com.powermax.patchstudio.hook

import android.app.Activity
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toolbar
import com.powermax.patchstudio.core.StyleConfig
import com.powermax.patchstudio.core.TargetApp
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * Applique un habillage "Liquid Glass" (verre translucide à reflets spéculaires) aux barres de navigation,
 * mini-lecteur flottant, barres d'en-tête, cartes et fenêtres flottantes — uniquement via le framework Android.
 *
 * Toutes les méthodes d'arbre s'exécutent sur le thread principal depuis le parcours throttlé d'[UiStyler].
 */
internal class LiquidGlassStyler(private val target: TargetApp) {

    private class GlassRecord(
        val drawable: LiquidGlassDrawable,
        val style: LiquidGlassDrawable.GlassStyle,
        val origBackground: Drawable?,
        val origElevation: Float,
        val origOutline: ViewOutlineProvider?,
        val origClip: Boolean,
    )

    private val applied = WeakHashMap<View, GlassRecord>()
    private val idNames = HashMap<Int, String>()
    private val navClassCache = ConcurrentHashMap<Class<*>, Boolean>()
    private val toolbarClassCache = ConcurrentHashMap<Class<*>, Boolean>()
    private val cardClassCache = ConcurrentHashMap<Class<*>, Boolean>()
    private val loc = IntArray(2)

    private fun active(c: StyleConfig): Boolean = c.enabled && c.liquidGlass

    private fun View.dp(v: Float): Float = v * resources.displayMetrics.density

    private fun surfaceFor(c: StyleConfig): Int =
        c.resolvedPalette()?.surface ?: DEFAULT_GLASS_SURFACE

    private fun accentFor(c: StyleConfig): Int =
        c.resolvedPalette()?.accent ?: target.brandColor

    private fun cardRadiusDp(c: StyleConfig): Float =
        c.cornerRadiusDp.coerceAtLeast(16).toFloat()

    /** Active un flou d'arrière-plan matériel (Android 12+) sur les fenêtres flottantes / dialogues. */
    fun onActivityResume(a: Activity, c: StyleConfig) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val window = a.window ?: return
        try {
            if (!active(c)) {
                if (window.isFloating) {
                    window.setBackgroundBlurRadius(0)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                }
                return
            }
            if (window.isFloating) {
                val density = a.resources.displayMetrics.density
                val radius = (22f * density * c.liquidGlassIntensity).toInt().coerceIn(8, 80)
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.setBackgroundBlurRadius(radius)
            }
        } catch (_: Throwable) {
        }
    }

    /** Met à jour ou restaure toutes les surfaces Liquid Glass en direct (thread principal). */
    fun onConfigChanged(c: StyleConfig) {
        if (!active(c)) {
            for ((v, rec) in applied.entries.toList()) {
                try {
                    if (v.background === rec.drawable) v.background = rec.origBackground
                    v.elevation = rec.origElevation
                    if (v.outlineProvider is GlassOutline) {
                        v.outlineProvider = rec.origOutline ?: ViewOutlineProvider.BACKGROUND
                        v.clipToOutline = rec.origClip
                    }
                } catch (_: Throwable) {
                }
            }
            applied.clear()
            return
        }
        val surface = surfaceFor(c)
        val accent = accentFor(c)
        for ((v, rec) in applied.entries.toList()) {
            try {
                val radiusPx = when (rec.style) {
                    LiquidGlassDrawable.GlassStyle.HEADER -> 0f
                    LiquidGlassDrawable.GlassStyle.CAPSULE -> v.dp(26f)
                    LiquidGlassDrawable.GlassStyle.CARD -> v.dp(cardRadiusDp(c))
                }
                rec.drawable.updateStyle(surface, accent, c.liquidGlassIntensity, c.liquidGlassTint, radiusPx)
                if (rec.style == LiquidGlassDrawable.GlassStyle.CARD && v.outlineProvider is GlassOutline) {
                    v.outlineProvider = GlassOutline(radiusPx)
                }
            } catch (_: Throwable) {
            }
        }
    }

    /** Appelé pour chaque vue lors du parcours throttlé d'[UiStyler] (thread principal). */
    fun onWalk(v: View, c: StyleConfig) {
        if (!active(c)) return
        if (v !is ViewGroup) return
        if (v.visibility != View.VISIBLE) return
        val w = v.width
        val h = v.height
        if (w <= 0 || h <= 0) return

        // Déjà stylée : on vérifie simplement que l'app n'a pas écrasé notre drawable.
        applied[v]?.let { rec ->
            val radiusPx = when (rec.style) {
                LiquidGlassDrawable.GlassStyle.HEADER -> 0f
                LiquidGlassDrawable.GlassStyle.CAPSULE -> v.dp(26f)
                LiquidGlassDrawable.GlassStyle.CARD -> v.dp(cardRadiusDp(c))
            }
            rec.drawable.updateStyle(
                surfaceFor(c), accentFor(c), c.liquidGlassIntensity, c.liquidGlassTint, radiusPx,
            )
            return
        }

        // Ne jamais toucher à l'écran des paroles (géré par LyricsStyler) ni à un enfant d'une vue déjà en verre.
        val name = idName(v)
        if (name.contains("lyric", ignoreCase = true)) return
        if (hasGlassAncestor(v)) return

        val dm = v.resources.displayMetrics
        val screenW = dm.widthPixels
        val screenH = dm.heightPixels

        // 1. Barre de navigation inférieure ou mini-lecteur flottant -> Capsule Liquid Glass.
        if (isBottomBarOrMiniPlayer(v, w, h, screenW, screenH, name)) {
            applyGlass(
                v = v,
                c = c,
                style = LiquidGlassDrawable.GlassStyle.CAPSULE,
                radiusPx = min(v.dp(26f), h / 2f),
                insetHPx = v.dp(8f),
                insetVPx = v.dp(3f),
                elevationPx = v.dp(10f),
                clipOutline = false,
            )
            return
        }

        // 2. Barre d'en-tête / Toolbar -> Bandeau verre givré avec reflet inférieur.
        if (isTopToolbar(v, h, screenH)) {
            applyGlass(
                v = v,
                c = c,
                style = LiquidGlassDrawable.GlassStyle.HEADER,
                radiusPx = 0f,
                insetHPx = 0f,
                insetVPx = 0f,
                elevationPx = v.elevation,
                clipOutline = false,
            )
            return
        }

        // 3. Cartes, chips et conteneurs arrondis -> Carte Liquid Glass.
        if (isGlassCardCandidate(v, w, h, screenW, screenH)) {
            applyGlass(
                v = v,
                c = c,
                style = LiquidGlassDrawable.GlassStyle.CARD,
                radiusPx = v.dp(cardRadiusDp(c)),
                insetHPx = 0f,
                insetVPx = 0f,
                elevationPx = maxOf(v.elevation, v.dp(4f)),
                clipOutline = true,
            )
        }
    }

    private fun applyGlass(
        v: View,
        c: StyleConfig,
        style: LiquidGlassDrawable.GlassStyle,
        radiusPx: Float,
        insetHPx: Float,
        insetVPx: Float,
        elevationPx: Float,
        clipOutline: Boolean,
    ) {
        val density = v.resources.displayMetrics.density
        val glass = LiquidGlassDrawable(
            surfaceColor = surfaceFor(c),
            accentColor = accentFor(c),
            intensity = c.liquidGlassIntensity,
            tinted = c.liquidGlassTint,
            style = style,
            cornerRadiusPx = radiusPx,
            density = density,
            insetHPx = insetHPx,
            insetVPx = insetVPx,
        )
        val record = GlassRecord(
            drawable = glass,
            style = style,
            origBackground = v.background,
            origElevation = v.elevation,
            origOutline = v.outlineProvider,
            origClip = v.clipToOutline,
        )
        applied[v] = record
        v.background = glass
        v.elevation = elevationPx
        if (clipOutline && radiusPx > 0f) {
            v.outlineProvider = GlassOutline(radiusPx)
            v.clipToOutline = true
        }
    }

    private fun isBottomBarOrMiniPlayer(
        v: ViewGroup,
        w: Int,
        h: Int,
        screenW: Int,
        screenH: Int,
        idName: String,
    ): Boolean {
        if (h < v.dp(44f) || h > v.dp(104f)) return false
        if (w < screenW * 0.85f) return false
        if (isBottomNavClass(v.javaClass)) return true
        if (!hasVisibleBackground(v)) return false
        v.getLocationInWindow(loc)
        val topY = loc[1]
        if (topY < screenH * 0.68f) return false
        // Soit identifié par un id générique de barre/mini-lecteur, soit élevé / docké en bas avec contenu.
        val hint = idName.contains("mini", ignoreCase = true) ||
            idName.contains("bottom", ignoreCase = true) ||
            idName.contains("nav", ignoreCase = true) ||
            idName.contains("tab", ignoreCase = true) ||
            idName.contains("bar", ignoreCase = true)
        if (hint) return true
        return v.elevation > 0f && hasInteractiveChildren(v)
    }

    private fun isTopToolbar(v: ViewGroup, h: Int, screenH: Int): Boolean {
        if (h < v.dp(44f) || h > v.dp(88f)) return false
        if (!isToolbarClass(v.javaClass)) return false
        v.getLocationInWindow(loc)
        return loc[1] <= screenH * 0.16f
    }

    private fun isGlassCardCandidate(
        v: ViewGroup,
        w: Int,
        h: Int,
        screenW: Int,
        screenH: Int,
    ): Boolean {
        if (w < v.dp(76f) || h < v.dp(40f)) return false
        // Exclure les conteneurs plein écran.
        if (w >= screenW * 0.96f && h >= screenH * 0.55f) return false
        if (isCardClass(v.javaClass)) return true
        val bg = v.background
        if (bg is GradientDrawable && bg.alpha > 20) {
            // Évite les simples lignes de liste pleines largeurs sans coins arrondis.
            val hasRadius = try {
                bg.cornerRadius >= v.dp(6f) || (bg.cornerRadii?.any { it >= v.dp(6f) } == true)
            } catch (_: Throwable) {
                false
            }
            if (hasRadius && h <= screenH * 0.45f) return true
        }
        return false
    }

    private fun hasVisibleBackground(v: View): Boolean {
        val bg = v.background ?: return false
        if (bg is ColorDrawable) return Color.alpha(bg.color) > 30
        return bg.alpha > 30
    }

    private fun hasInteractiveChildren(v: ViewGroup): Boolean {
        var count = 0
        for (i in 0 until v.childCount) {
            val child = v.getChildAt(i) ?: continue
            if (child is TextView || child is ImageView || child is ViewGroup) count++
            if (count >= 2) return true
        }
        return false
    }

    private fun hasGlassAncestor(v: View): Boolean {
        var p = v.parent
        var depth = 0
        while (p is View && depth < 16) {
            if (applied.containsKey(p)) return true
            p = p.parent
            depth++
        }
        return false
    }

    private fun idName(v: View): String {
        val id = v.id
        if (id == View.NO_ID) return ""
        idNames[id]?.let { return it }
        val name = try {
            v.resources.getResourceEntryName(id) ?: ""
        } catch (_: Throwable) {
            ""
        }
        idNames[id] = name
        return name
    }

    private fun hierarchyContains(cls: Class<*>, vararg tokens: String): Boolean {
        var k: Class<*>? = cls
        while (k != null && k != ViewGroup::class.java) {
            val n = k.name
            for (t in tokens) if (n.contains(t)) return true
            k = k.superclass
        }
        return false
    }

    private fun isBottomNavClass(cls: Class<*>): Boolean = navClassCache.getOrPut(cls) {
        hierarchyContains(cls, "BottomNavigationView", "NavigationBarView")
    }

    private fun isToolbarClass(cls: Class<*>): Boolean = toolbarClassCache.getOrPut(cls) {
        Toolbar::class.java.isAssignableFrom(cls) || hierarchyContains(cls, "Toolbar")
    }

    private fun isCardClass(cls: Class<*>): Boolean = cardClassCache.getOrPut(cls) {
        hierarchyContains(cls, "CardView", "Chip")
    }

    private class GlassOutline(private val radius: Float) : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }

    private companion object {
        const val DEFAULT_GLASS_SURFACE = 0xFF151C28.toInt()
    }
}
