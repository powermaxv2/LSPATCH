package com.powermax.patchstudio.hook

import android.app.Activity
import android.app.Instrumentation
import android.content.res.ColorStateList
import android.content.res.Resources
import android.content.res.TypedArray
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.SystemClock
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import android.widget.AbsListView
import android.widget.ImageView
import android.widget.TextView
import com.powermax.patchstudio.core.ColorRemapper
import com.powermax.patchstudio.core.EntranceAnimation
import com.powermax.patchstudio.core.FontChoice
import com.powermax.patchstudio.core.StyleConfig
import com.powermax.patchstudio.core.TargetApp
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import java.util.ArrayDeque
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Moteur de style générique : uniquement des hooks sur des classes du framework Android
 * (aucune classe de l'app cible). Chaque callback est protégé et respecte `config.enabled`.
 */
class UiStyler(private val target: TargetApp, private val store: TargetConfigStore) {

    @Volatile
    private var remapper: ColorRemapper? = null

    private val cfg: StyleConfig get() = store.config

    // État UI (thread principal uniquement).
    private val entrancePending = WeakHashMap<Activity, Boolean>()
    private val listState = WeakHashMap<ViewGroup, LongArray>() // [dernier ajout ms, index stagger]
    private val pressState = WeakHashMap<View, Boolean>()
    private val processed = WeakHashMap<View, Boolean>()
    private val walkers = WeakHashMap<View, TreeWalker>()
    private val auroras = WeakHashMap<Activity, AuroraDrawable>()
    private val auroraLayout = WeakHashMap<View, Boolean>()
    private val listClassCache = ConcurrentHashMap<Class<*>, Boolean>()
    private val pagerClassCache = ConcurrentHashMap<Class<*>, Boolean>()

    private val emphasized = PathInterpolator(0.2f, 0f, 0f, 1f)

    private val lyrics = LyricsStyler(target) { store.config }

    fun install() {
        onConfig(store.config)
        store.addListener { onConfig(it) }
        installColorHooks()
        installActivityHooks()
        installViewHooks()
        lyrics.install()
    }

    private fun onConfig(c: StyleConfig) {
        val palette = c.resolvedPalette()
        remapper = if (palette == null) null else ColorRemapper(palette, target.brandHueRange)
        FontMapper.choice = if (c.enabled) c.font else FontChoice.DEFAULT
    }

    private fun remap(color: Int): Int {
        if (!cfg.enabled) return color
        return remapper?.map(color) ?: color
    }

    private fun speed(): Float = cfg.animationSpeed.coerceIn(0.5f, 2f)

    private fun View.dp(v: Float): Float = v * resources.displayMetrics.density

    // ---------------------------------------------------------------- 1. Couleurs

    private fun installColorHooks() {
        val int = Int::class.javaPrimitiveType!!
        val remapResult: (MethodHookParam) -> Unit = { p ->
            val r = p.result
            if (r is Int) {
                val m = remap(r)
                if (m != r) p.result = m
            }
        }
        Hooks.guard("Resources.getColor") {
            Hooks.after(Resources::class.java, "getColor", int, Resources.Theme::class.java, body = remapResult)
        }
        Hooks.guard("ColorStateList.getColorForState") {
            Hooks.after(ColorStateList::class.java, "getColorForState", IntArray::class.java, int, body = remapResult)
        }
        Hooks.guard("ColorStateList.getDefaultColor") {
            Hooks.after(ColorStateList::class.java, "getDefaultColor", body = remapResult)
        }
        Hooks.guard("TypedArray.getColor") {
            Hooks.after(TypedArray::class.java, "getColor", int, int, body = remapResult)
        }
        Hooks.guard("Theme.resolveAttribute") {
            Hooks.after(
                Resources.Theme::class.java, "resolveAttribute",
                int, TypedValue::class.java, Boolean::class.javaPrimitiveType!!,
            ) { p ->
                if (p.result != true) return@after
                val tv = p.args[1] as? TypedValue ?: return@after
                if (tv.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT) tv.data = remap(tv.data)
            }
        }
        val remapArg0: (MethodHookParam) -> Unit = { p ->
            val c = p.args[0]
            if (c is Int) p.args[0] = remap(c)
        }
        Hooks.guard("ColorDrawable(int)") {
            Hooks.beforeConstructor(ColorDrawable::class.java, int, body = remapArg0)
        }
        Hooks.guard("ColorDrawable.setColor") {
            Hooks.before(ColorDrawable::class.java, "setColor", int, body = remapArg0)
        }
    }

    // ---------------------------------------------------------------- Activités

    private fun installActivityHooks() {
        Hooks.guard("callActivityOnCreate") {
            Hooks.after(Instrumentation::class.java, "callActivityOnCreate", Activity::class.java, Bundle::class.java) { p ->
                val a = p.args[0] as? Activity ?: return@after
                entrancePending[a] = true
            }
        }
        Hooks.guard("callActivityOnResume") {
            Hooks.after(Instrumentation::class.java, "callActivityOnResume", Activity::class.java) { p ->
                val a = p.args[0] as? Activity ?: return@after
                onResume(a)
            }
        }
        Hooks.guard("callActivityOnPause") {
            Hooks.after(Instrumentation::class.java, "callActivityOnPause", Activity::class.java) { p ->
                (p.args[0] as? Activity)?.let { auroras[it]?.pause() }
            }
        }
    }

    private fun onResume(a: Activity) {
        val c = cfg
        val window = a.window ?: return
        val decor = window.decorView
        if (c.enabled) {
            safe("systemBars") { tintSystemBars(a, c) }
            if (entrancePending.remove(a) == true) safe("entrance") { playEntrance(a, c) }
            safe("walker") { ensureWalker(decor) }
        }
        safe("aurora") { updateAurora(a, decor, c) }
    }

    @Suppress("DEPRECATION")
    private fun tintSystemBars(a: Activity, c: StyleConfig) {
        if (!c.tintSystemBars) return
        val palette = c.resolvedPalette() ?: return
        a.window.statusBarColor = palette.background
        a.window.navigationBarColor = palette.background
    }

    // ---------------------------------------------------------------- 3. Entrée d'écran

    private fun playEntrance(a: Activity, c: StyleConfig) {
        if (c.entrance == EntranceAnimation.NONE) return
        val content = a.findViewById<View>(android.R.id.content) ?: return
        val dur = (420f / speed()).toLong()
        content.animate().cancel()
        val anim = content.animate().setStartDelay(0).setDuration(dur).setInterpolator(emphasized)
        when (c.entrance) {
            EntranceAnimation.FADE -> {
                content.alpha = 0f
                anim.alpha(1f)
            }
            EntranceAnimation.SLIDE_UP -> {
                content.alpha = 0f
                content.translationY = content.dp(48f)
                anim.alpha(1f).translationY(0f)
            }
            EntranceAnimation.ZOOM -> {
                content.alpha = 0f
                content.scaleX = 0.92f
                content.scaleY = 0.92f
                anim.alpha(1f).scaleX(1f).scaleY(1f)
            }
            EntranceAnimation.BOUNCE -> {
                content.alpha = 0f
                content.scaleX = 0.85f
                content.scaleY = 0.85f
                anim.alpha(1f).scaleX(1f).scaleY(1f).setInterpolator(OvershootInterpolator(1.6f))
            }
            EntranceAnimation.NONE -> Unit
        }
        anim.start()
    }

    // ---------------------------------------------------------------- 4/5/6/7. Vues

    private fun installViewHooks() {
        Hooks.guard("ViewGroup.addView") {
            Hooks.after(
                ViewGroup::class.java, "addView",
                View::class.java, Int::class.javaPrimitiveType!!, ViewGroup.LayoutParams::class.java,
            ) { p ->
                val parent = p.thisObject as? ViewGroup ?: return@after
                val child = p.args[0] as? View ?: return@after
                onChildAdded(parent, child)
            }
        }
        Hooks.guard("View.setPressed") {
            Hooks.after(View::class.java, "setPressed", Boolean::class.javaPrimitiveType!!) { p ->
                val v = p.thisObject as? View ?: return@after
                val pressed = p.args[0] as? Boolean ?: return@after
                onPressed(v, pressed)
            }
        }
        Hooks.guard("TextView.setTypeface") {
            Hooks.before(TextView::class.java, "setTypeface", Typeface::class.java) { p ->
                if (!cfg.enabled) return@before
                if (FontMapper.isIconText((p.thisObject as TextView).text)) return@before
                val tf = p.args[0] as Typeface?
                val mapped = FontMapper.map(tf)
                if (mapped !== tf) p.args[0] = mapped
            }
        }
    }

    private fun onChildAdded(parent: ViewGroup, child: View) {
        val c = cfg
        if (!c.enabled || !parent.isAttachedToWindow) return
        if (isList(parent)) {
            if (c.listAnimations) animateListItem(parent, child)
            return
        }
        if (!c.screenTransitions || parent.width <= 0 || isPager(parent)) return
        val lp = child.layoutParams ?: return
        if (lp.width != ViewGroup.LayoutParams.MATCH_PARENT || lp.height != ViewGroup.LayoutParams.MATCH_PARENT) return
        val dur = (260f / speed()).toLong()
        child.alpha = 0f
        val anim = child.animate().setStartDelay(0).setDuration(dur).setInterpolator(emphasized).alpha(1f)
        if (c.entrance == EntranceAnimation.SLIDE_UP) {
            child.translationY = child.dp(24f)
            anim.translationY(0f)
        } else {
            child.translationX = child.dp(24f)
            anim.translationX(0f)
        }
        anim.start()
    }

    private fun animateListItem(parent: ViewGroup, child: View) {
        val now = SystemClock.uptimeMillis()
        val state = listState.getOrPut(parent) { longArrayOf(0L, 0L) }
        state[1] = if (now - state[0] < 80L) state[1] + 1 else 0L
        state[0] = now
        val delay = (state[1] * 35L).coerceAtMost(280L)
        child.alpha = 0f
        child.translationY = child.dp(28f)
        child.animate()
            .setStartDelay(delay)
            .setDuration((320f / speed()).toLong())
            .setInterpolator(DecelerateInterpolator())
            .alpha(1f)
            .translationY(0f)
            .start()
    }

    private fun hierarchyNameContains(cls: Class<*>, token: String): Boolean {
        var k: Class<*>? = cls
        while (k != null && k != ViewGroup::class.java) {
            if (k.name.contains(token)) return true
            k = k.superclass
        }
        return false
    }

    private fun isList(v: ViewGroup): Boolean = listClassCache.getOrPut(v.javaClass) {
        v is AbsListView || hierarchyNameContains(v.javaClass, "RecyclerView")
    }

    private fun isPager(v: ViewGroup): Boolean = pagerClassCache.getOrPut(v.javaClass) {
        hierarchyNameContains(v.javaClass, "ViewPager")
    }

    private fun onPressed(v: View, pressed: Boolean) {
        val c = cfg
        val wasPressed = pressState[v] == true
        if (wasPressed == pressed) return
        if (!pressed) {
            // Relâché : on ne revient à 1.0 que si on avait réduit la vue.
            pressState.remove(v)
        } else {
            if (!c.enabled || !c.pressBounce || !v.isClickable) return
            pressState[v] = true
        }
        if (pressed) {
            val wide = v.width > v.resources.displayMetrics.widthPixels * 0.8f
            val s = if (wide) 0.985f else 0.96f
            v.animate().setStartDelay(0).setDuration(90L).setInterpolator(DecelerateInterpolator())
                .scaleX(s).scaleY(s).start()
            if (c.haptics) v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        } else {
            val rest = try {
                lyrics.restScale(v) ?: 1f
            } catch (_: Throwable) {
                1f
            }
            v.animate().setStartDelay(0).setDuration(260L).setInterpolator(OvershootInterpolator())
                .scaleX(rest).scaleY(rest).start()
        }
    }

    // ---------------------------------------------------------------- 8. Coins arrondis + polices existantes

    private fun ensureWalker(decor: View) {
        if (walkers.containsKey(decor)) {
            walkers[decor]?.schedule()
            return
        }
        val walker = TreeWalker(decor)
        walkers[decor] = walker
        decor.viewTreeObserver.addOnGlobalLayoutListener(walker)
        walker.schedule()
    }

    private inner class TreeWalker(private val root: View) : ViewTreeObserver.OnGlobalLayoutListener, Runnable {
        private var pending = false
        private var lastRun = 0L

        override fun onGlobalLayout() = schedule()

        fun schedule() {
            if (pending) return
            pending = true
            val wait = (120L - (SystemClock.uptimeMillis() - lastRun)).coerceAtLeast(0L)
            root.postDelayed(this, wait)
        }

        override fun run() {
            pending = false
            lastRun = SystemClock.uptimeMillis()
            try {
                walk()
            } catch (t: Throwable) {
                Hooks.logOnce("treeWalker", t)
            }
        }

        private fun walk() {
            val c = cfg
            if (!c.enabled) return
            val radiusPx = root.dp(c.cornerRadiusDp.toFloat())
            val minSize = root.dp(40f)
            val stack = ArrayDeque<View>()
            stack.push(root)
            while (stack.isNotEmpty()) {
                val v = stack.pop()
                if (v is ViewGroup) for (i in 0 until v.childCount) v.getChildAt(i)?.let { stack.push(it) }
                try {
                    lyrics.onWalk(v, c)
                } catch (t: Throwable) {
                    Hooks.logOnce("lyrics", t)
                }
                if (processed.containsKey(v)) continue
                when (v) {
                    is ImageView -> {
                        val w = v.width
                        val h = v.height
                        if (w <= 0 || h <= 0) continue // pas encore mesurée : on réessaiera
                        processed[v] = true
                        if (c.cornerRadiusDp <= 0 || w < minSize || h < minSize) continue
                        val ratio = w.toFloat() / h
                        if (ratio < 0.75f || ratio > 1.33f) continue
                        if (v.clipToOutline || v.outlineProvider !== ViewOutlineProvider.BACKGROUND) continue
                        v.outlineProvider = RoundedOutline(radiusPx)
                        v.clipToOutline = true
                    }
                    is TextView -> {
                        processed[v] = true
                        if (FontMapper.isIconText(v.text)) continue
                        val mapped = FontMapper.map(v.typeface)
                        if (mapped != null && mapped !== v.typeface) v.typeface = mapped
                    }
                    else -> Unit
                }
            }
        }
    }

    private class RoundedOutline(private val radius: Float) : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }

    // ---------------------------------------------------------------- 9. Aurora

    private fun updateAurora(a: Activity, decor: View, c: StyleConfig) {
        val existing = auroras[a]
        if (!c.enabled || !c.aurora) {
            if (existing != null) {
                existing.stop()
                decor.overlay.remove(existing)
                auroras.remove(a)
            }
            return
        }
        val accent = c.resolvedPalette()?.accent ?: target.brandColor
        val drawable = existing ?: AuroraDrawable(accent, c.auroraIntensity).also {
            it.setBounds(0, 0, decor.width, decor.height)
            decor.overlay.add(it)
            auroras[a] = it
        }
        if (auroraLayout[decor] != true) {
            auroraLayout[decor] = true
            decor.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                try {
                    auroras[a]?.setBounds(0, 0, v.width, v.height)
                } catch (_: Throwable) {
                }
            }
        }
        drawable.start()
    }

    private inline fun safe(name: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Hooks.logOnce(name, t)
        }
    }
}
