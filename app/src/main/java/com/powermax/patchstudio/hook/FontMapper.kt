package com.powermax.patchstudio.hook

import android.graphics.Typeface
import com.powermax.patchstudio.core.FontChoice
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap

/** Remplace les polices par une famille système, en conservant le style (gras/italique). */
internal object FontMapper {

    @Volatile
    var choice: FontChoice = FontChoice.DEFAULT
        set(value) {
            if (field != value) {
                field = value
                byStyle.clear()
            }
        }

    private val byStyle = ConcurrentHashMap<Int, Typeface>()
    private val produced: MutableSet<Typeface> = Collections.synchronizedSet(
        Collections.newSetFromMap(IdentityHashMap<Typeface, Boolean>()),
    )

    private fun family(c: FontChoice): String? = when (c) {
        FontChoice.DEFAULT -> null
        FontChoice.CONDENSED -> "sans-serif-condensed"
        FontChoice.LIGHT -> "sans-serif-light"
        FontChoice.MEDIUM -> "sans-serif-medium"
        FontChoice.SERIF -> "serif"
        FontChoice.MONO -> "monospace"
        FontChoice.CASUAL -> "casual"
    }

    fun map(tf: Typeface?): Typeface? {
        val c = choice
        val fam = family(c) ?: return tf
        if (tf != null && produced.contains(tf)) return tf
        val style = tf?.style ?: Typeface.NORMAL
        return byStyle.getOrPut(style) {
            Typeface.create(fam, style).also { produced.add(it) }
        }
    }

    /** Les polices d'icônes utilisent la zone Unicode privée : ne jamais les remplacer. */
    fun isIconText(text: CharSequence?): Boolean {
        if (text.isNullOrEmpty()) return false
        for (ch in text) if (ch in '\uE000'..'\uF8FF') return true
        return false
    }
}
