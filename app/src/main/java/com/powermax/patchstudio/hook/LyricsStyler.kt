package com.powermax.patchstudio.hook

import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.AbsListView
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import com.powermax.patchstudio.core.ColorMath
import com.powermax.patchstudio.core.LyricsAlignment
import com.powermax.patchstudio.core.LyricsBackground
import com.powermax.patchstudio.core.StyleConfig
import com.powermax.patchstudio.core.TargetApp
import java.lang.ref.WeakReference
import java.util.ArrayDeque
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Embellit l'écran des paroles. Purement cosmétique, uniquement via le framework Android :
 * un "conteneur de paroles" est une vue dont le nom d'id de ressource contient "lyric"
 * (aucune autre heuristique). Appelé par le parcours d'arbre throttlé d'[UiStyler]
 * (thread principal) et par un hook sur `TextView.setTextColor`.
 */
internal class LyricsStyler(
    private val target: TargetApp,
    private val config: () -> StyleConfig,
) {

    private class TextState(val origPx: Float, val appliedPx: Float)

    private class LineState(val list: WeakReference<View>, var vivid: Float, var state: Int)

    private class Levels {
        var min = Float.MAX_VALUE
        var max = -1f
    }

    // Toutes ces structures ne sont touchées que depuis le thread principal.
    private val idNames = HashMap<Int, String>()
    private val loggedNames = HashSet<String>()
    private val containers = WeakHashMap<View, Boolean>()
    private val standaloneTexts = WeakHashMap<View, Boolean>()
    private val textStates = WeakHashMap<TextView, TextState>()
    private val styledTexts = WeakHashMap<TextView, Boolean>()
    private val fadeDone = WeakHashMap<View, Boolean>()
    private val backgrounds = WeakHashMap<View, Drawable>()
    private val bgAttempts = WeakHashMap<View, Int>()
    private val lines = WeakHashMap<TextView, LineState>()
    private val levels = WeakHashMap<View, Levels>()
    private val scrollClassCache = HashMap<Class<*>, Boolean>()

    private val mainLooper: Looper? = Looper.getMainLooper()

    fun install() {
        val int = Int::class.javaPrimitiveType!!
        Hooks.guard("TextView.setTextColor(int)") {
            Hooks.after(TextView::class.java, "setTextColor", int) { p ->
                val tv = p.thisObject as? TextView ?: return@after
                val c = p.args[0] as? Int ?: return@after
                onTextColor(tv, c)
            }
        }
        Hooks.guard("TextView.setTextColor(ColorStateList)") {
            Hooks.after(TextView::class.java, "setTextColor", ColorStateList::class.java) { p ->
                val tv = p.thisObject as? TextView ?: return@after
                val csl = p.args[0] as? ColorStateList ?: return@after
                onTextColor(tv, csl.defaultColor)
            }
        }
    }

    private fun active(c: StyleConfig): Boolean = c.enabled && c.lyricsEnhanced

    private fun View.dp(v: Float): Float = v * resources.displayMetrics.density

    /** Échelle de repos d'une ligne (utilisée par le rebond d'appui pour ne pas annuler le zoom). */
    fun restScale(v: View): Float? {
        if (lines.isEmpty()) return null
        val tv = v as? TextView ?: return null
        return if (lines[tv]?.state == STATE_ACTIVE) ACTIVE_SCALE else null
    }

    // ---------------------------------------------------------------- Détection

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

    private fun isLyricName(name: String): Boolean = name.contains("lyric", ignoreCase = true)

    private fun logName(name: String, v: View, what: String) {
        if (loggedNames.add(name)) Hooks.log("lyrics id '$name' ($what, ${v.javaClass.name})")
    }

    private fun isScrollable(v: View): Boolean {
        if (v !is ViewGroup) return false
        val byClass = scrollClassCache.getOrPut(v.javaClass) {
            var k: Class<*>? = v.javaClass
            var found = v is AbsListView || v is ScrollView
            while (!found && k != null && k != ViewGroup::class.java) {
                val n = k.name
                if (n.contains("RecyclerView") || n.contains("ScrollView")) found = true
                k = k.superclass
            }
            found
        }
        return byClass || v.canScrollVertically(1) || v.canScrollVertically(-1)
    }

    private fun hasContainerAncestor(v: View): Boolean {
        var p = v.parent
        var depth = 0
        while (p is View && depth < 40) {
            if (containers.containsKey(p)) return true
            p = p.parent
            depth++
        }
        return false
    }

    private fun scrollableAncestor(v: View): View? {
        var p = v.parent
        var depth = 0
        while (p is View && depth < 12) {
            if (isScrollable(p)) return p
            p = p.parent
            depth++
        }
        return null
    }

    /** Appelé pour chaque vue par le parcours d'arbre d'UiStyler (thread principal). */
    fun onWalk(v: View, c: StyleConfig) {
        if (!active(c)) return
        if (v.id == View.NO_ID) return
        val name = idName(v)
        if (name.isEmpty() || !isLyricName(name)) return
        if (v is ViewGroup) {
            val w = v.width
            val h = v.height
            if (w <= 0 || h <= 0) return // pas encore mesurée : on réessaiera
            val dm = v.resources.displayMetrics
            if (w < dm.widthPixels * 0.5f || h < dm.heightPixels * 0.3f) {
                logName(name, v, "ignoré : trop petit")
                return
            }
            if (hasContainerAncestor(v)) return // déjà couvert par un conteneur parent
            if (containers.put(v, true) == null) logName(name, v, "conteneur")
            styleContainer(v, c)
            applyBackground(v, c)
        } else if (v is TextView) {
            if (hasContainerAncestor(v)) return
            // Ligne isolée : seulement si elle est dans une liste (évite boutons/libellés).
            if (scrollableAncestor(v) == null) return
            if (standaloneTexts.put(v, true) == null) logName(name, v, "ligne")
            styleText(v, c)
        }
    }

    // ---------------------------------------------------------------- Texte & bords

    private fun styleContainer(root: ViewGroup, c: StyleConfig) {
        val stack = ArrayDeque<View>()
        stack.push(root)
        while (stack.isNotEmpty()) {
            val v = stack.pop()
            if (v is ViewGroup) {
                for (i in 0 until v.childCount) v.getChildAt(i)?.let { stack.push(it) }
                if (c.lyricsFadeEdges && !fadeDone.containsKey(v) && isScrollable(v)) {
                    fadeDone[v] = true
                    v.isVerticalFadingEdgeEnabled = true
                    v.setFadingEdgeLength(v.dp(64f).toInt())
                }
            } else if (v is TextView) {
                styleText(v, c)
            }
        }
    }

    private fun styleText(tv: TextView, c: StyleConfig) {
        if (tv is Button || tv is EditText) return
        if (FontMapper.isIconText(tv.text)) return

        // Taille : une seule fois par taille d'origine (réappliquée si l'app la réinitialise).
        if (tv.autoSizeTextType == TextView.AUTO_SIZE_TEXT_TYPE_NONE) {
            val scale = c.lyricsTextScale.coerceIn(0.9f, 1.6f)
            val current = tv.textSize
            val st = textStates[tv]
            if (st == null || abs(current - st.appliedPx) > 0.5f) {
                val orig = if (st != null && abs(current - st.origPx) <= 0.5f) st.origPx else current
                val target = orig * scale
                textStates[tv] = TextState(orig, target)
                if (abs(target - current) > 0.5f) tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, target)
            }
        }

        if (styledTexts.containsKey(tv)) return
        styledTexts[tv] = true

        val tf = tv.typeface
        if (tf == null || !tf.isBold) {
            val bold = Typeface.create(tf, Typeface.BOLD)
            tv.typeface = FontMapper.map(bold) ?: bold
        }
        val vertical = tv.gravity and Gravity.VERTICAL_GRAVITY_MASK
        val horizontal = if (c.lyricsAlignment == LyricsAlignment.CENTER) Gravity.CENTER_HORIZONTAL else Gravity.START
        tv.textAlignment = View.TEXT_ALIGNMENT_GRAVITY
        tv.gravity = horizontal or vertical
        tv.setLineSpacing(tv.lineSpacingExtra, max(tv.lineSpacingMultiplier, 1.12f))
        tv.letterSpacing = -0.01f
    }

    // ---------------------------------------------------------------- Ligne active

    private fun vividness(color: Int): Float {
        val a = ColorMath.alpha(color) / 255f
        val lum = ColorMath.luminance(color).coerceIn(0f, 1f)
        return a * (0.15f + 0.85f * sqrt(lum))
    }

    private fun onTextColor(tv: TextView, color: Int) {
        if (containers.isEmpty() && standaloneTexts.isEmpty()) return // chemin rapide hors paroles
        if (Looper.myLooper() !== mainLooper) return
        val c = config()
        if (!active(c) || !c.lyricsActiveZoom) return
        if (tv is Button || tv is EditText) return

        // Contexte : un conteneur de paroles parmi les ancêtres (ou ligne isolée), et une liste.
        var list: View? = null
        var inLyrics = standaloneTexts.containsKey(tv)
        var p = tv.parent
        var depth = 0
        while (p is View && depth < 40) {
            if (list == null && isScrollable(p)) list = p
            if (containers.containsKey(p)) {
                inLyrics = true
                break
            }
            p = p.parent
            depth++
        }
        if (!inLyrics || list == null) return

        val v = vividness(color)
        val line = lines[tv]
        if (line == null) {
            lines[tv] = LineState(WeakReference(list), v, STATE_NEUTRAL)
        } else {
            line.vivid = v
            if (line.list.get() !== list) lines[tv] = LineState(WeakReference(list), v, line.state)
        }
        val lv = levels.getOrPut(list) { Levels() }
        val oldMin = lv.min
        val oldMax = lv.max
        if (v < lv.min) lv.min = v
        if (v > lv.max) lv.max = v
        if (abs(lv.min - oldMin) > 0.02f || abs(lv.max - oldMax) > 0.02f) {
            // Les niveaux ont changé : réévalue toutes les lignes connues de cette liste.
            for ((t, s) in lines.entries.toList()) {
                if (s.list.get() === list) applyLine(t, s, lv, c)
            }
        } else {
            lines[tv]?.let { applyLine(tv, it, lv, c) }
        }
    }

    private fun applyLine(tv: TextView, s: LineState, lv: Levels, c: StyleConfig) {
        val want = when {
            lv.max - lv.min < 0.12f -> STATE_NEUTRAL // paroles non synchronisées : on ne touche à rien
            s.vivid >= lv.max - 0.06f -> STATE_ACTIVE
            else -> STATE_INACTIVE
        }
        if (want == s.state) return
        s.state = want
        val speed = c.animationSpeed.coerceIn(0.5f, 2f)
        tv.pivotX = if (c.lyricsAlignment == LyricsAlignment.CENTER) tv.width / 2f else 0f
        tv.pivotY = tv.height / 2f
        val anim = tv.animate().setStartDelay(0)
        when (want) {
            STATE_ACTIVE -> anim.scaleX(ACTIVE_SCALE).scaleY(ACTIVE_SCALE).alpha(1f)
                .setDuration((260f / speed).toLong()).setInterpolator(OvershootInterpolator(1.4f))
            STATE_INACTIVE -> anim.scaleX(1f).scaleY(1f).alpha(INACTIVE_ALPHA)
                .setDuration((320f / speed).toLong()).setInterpolator(DecelerateInterpolator())
            else -> anim.scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration((200f / speed).toLong()).setInterpolator(DecelerateInterpolator())
        }
        anim.start()
    }

    // ---------------------------------------------------------------- Fond

    private fun applyBackground(v: View, c: StyleConfig) {
        if (c.lyricsBackground == LyricsBackground.NONE) return
        val ours = backgrounds[v]
        if (ours != null && v.background === ours) return
        // Si l'app remplace notre fond, on réessaie quelques fois seulement (pas de ping-pong).
        val attempts = bgAttempts[v] ?: 0
        if (attempts >= 4) return
        bgAttempts[v] = attempts + 1

        val palette = c.resolvedPalette()
        val base = palette?.background ?: DARK_FALLBACK
        val drawable: Drawable = when (c.lyricsBackground) {
            LyricsBackground.DARK -> ColorDrawable(base)
            LyricsBackground.GRADIENT -> {
                val accent = palette?.accent ?: target.brandColor
                val hsl = ColorMath.toHsl(accent)
                // Accent assombri à 35 % de luminosité, légèrement désaturé pour rester sobre.
                val deep = ColorMath.fromHsl(hsl[0], hsl[1] * 0.85f, 0.35f)
                val gradient = LyricsGradientDrawable(deep, base)
                bindLifecycle(v, gradient)
                val original = v.background
                if (original == null || original is ColorDrawable) {
                    gradient
                } else {
                    gradient.alpha = 210
                    LayerDrawable(arrayOf(original, gradient))
                }
            }
            LyricsBackground.NONE -> return
        }
        v.background = drawable
        backgrounds[v] = drawable
    }

    private fun bindLifecycle(v: View, d: LyricsGradientDrawable) {
        v.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                try {
                    d.start()
                } catch (_: Throwable) {
                }
            }

            override fun onViewDetachedFromWindow(view: View) {
                try {
                    d.stop()
                } catch (_: Throwable) {
                }
            }
        })
        if (v.isAttachedToWindow) d.start()
    }

    private companion object {
        const val STATE_NEUTRAL = 0
        const val STATE_ACTIVE = 1
        const val STATE_INACTIVE = 2
        const val ACTIVE_SCALE = 1.06f
        const val INACTIVE_ALPHA = 0.55f
        const val DARK_FALLBACK = 0xFF0B0B0D.toInt()
    }
}

/** Dégradé vertical sobre (accent profond → fond) qui glisse lentement de haut en bas. */
internal class LyricsGradientDrawable(private val top: Int, private val bottom: Int) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val matrix = Matrix()
    private var phase = 0f
    private var globalAlpha = 255

    private val animator: ValueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 9_000L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidateSelf()
        }
    }

    fun start() {
        if (animator.isPaused) animator.resume() else if (!animator.isStarted) animator.start()
    }

    fun stop() {
        animator.cancel()
    }

    override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
        val changed = super.setVisible(visible, restart)
        try {
            if (visible) {
                if (animator.isPaused) animator.resume()
            } else if (animator.isRunning) {
                animator.pause()
            }
        } catch (_: Throwable) {
        }
        return changed
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val h = bounds.height().toFloat()
        if (h <= 0f) return
        paint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(top, ColorMath.blend(top, bottom, 0.55f), bottom),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val shader = paint.shader ?: return
        // Glisse de -12 % à +18 % de la hauteur : mouvement à peine perceptible.
        matrix.setTranslate(b.left.toFloat(), b.top + b.height() * (-0.12f + 0.30f * phase))
        shader.setLocalMatrix(matrix)
        paint.alpha = globalAlpha
        canvas.drawRect(b, paint)
    }

    override fun setAlpha(alpha: Int) {
        globalAlpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun getAlpha(): Int = globalAlpha

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = if (globalAlpha == 255) PixelFormat.OPAQUE else PixelFormat.TRANSLUCENT
}
