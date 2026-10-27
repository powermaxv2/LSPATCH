package com.powermax.patchstudio.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import java.util.concurrent.ConcurrentHashMap

/** Petits utilitaires pour poser des hooks qui ne peuvent jamais faire crasher l'app hôte. */
internal object Hooks {
    private val reported = ConcurrentHashMap.newKeySet<String>()

    fun log(msg: String) {
        try {
            XposedBridge.log("PatchStudio: $msg")
        } catch (_: Throwable) {
        }
    }

    /** Log une seule fois par clé (évite d'inonder les logs depuis un hook appelé en boucle). */
    fun logOnce(key: String, t: Throwable) {
        if (reported.add(key)) log("$key a échoué : $t")
    }

    inline fun guard(name: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            log("$name : installation impossible ($t)")
        }
    }

    private class SafeHook(
        private val name: String,
        private val before: ((MethodHookParam) -> Unit)?,
        private val after: ((MethodHookParam) -> Unit)?,
    ) : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val b = before ?: return
            try {
                b(param)
            } catch (t: Throwable) {
                logOnce(name, t)
            }
        }

        override fun afterHookedMethod(param: MethodHookParam) {
            val a = after ?: return
            try {
                a(param)
            } catch (t: Throwable) {
                logOnce(name, t)
            }
        }
    }

    fun before(clazz: Class<*>, method: String, vararg types: Any, body: (MethodHookParam) -> Unit) {
        hook(clazz, method, types, SafeHook("$method.before", body, null))
    }

    fun after(clazz: Class<*>, method: String, vararg types: Any, body: (MethodHookParam) -> Unit) {
        hook(clazz, method, types, SafeHook("$method.after", null, body))
    }

    fun beforeConstructor(clazz: Class<*>, vararg types: Any, body: (MethodHookParam) -> Unit) {
        val args = arrayOf<Any>(*types, SafeHook("${clazz.simpleName}.<init>", body, null))
        XposedHelpers.findAndHookConstructor(clazz, *args)
    }

    private fun hook(clazz: Class<*>, method: String, types: Array<out Any>, cb: XC_MethodHook) {
        val args = arrayOf<Any>(*types, cb)
        XposedHelpers.findAndHookMethod(clazz, method, *args)
    }
}
