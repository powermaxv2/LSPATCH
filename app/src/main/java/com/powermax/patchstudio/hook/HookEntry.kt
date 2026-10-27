package com.powermax.patchstudio.hook

import com.powermax.patchstudio.core.TargetApps
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.concurrent.atomic.AtomicBoolean

/** Point d'entrée Xposed (assets/xposed_init). Cosmétique uniquement : hooks sur le framework Android. */
class HookEntry : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val target = TargetApps.byPackage(lpparam.packageName ?: return) ?: return
            if (!installed.compareAndSet(false, true)) return
            Hooks.log("chargé dans ${lpparam.packageName} (${lpparam.processName})")
            val store = TargetConfigStore(target)
            store.install()
            UiStyler(target, store).install()
        } catch (t: Throwable) {
            Hooks.log("échec d'initialisation : $t")
        }
    }

    private companion object {
        val installed = AtomicBoolean(false)
    }
}
