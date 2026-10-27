package com.powermax.patchstudio.hook

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.powermax.patchstudio.BuildConfig
import com.powermax.patchstudio.core.ConfigProtocol
import com.powermax.patchstudio.core.StyleConfig
import com.powermax.patchstudio.core.TargetApp
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Config de style dans le process cible.
 * Sources, par ordre : cache local (prefs de la cible) -> extras de lancement -> ContentProvider de l'app -> broadcasts.
 */
class TargetConfigStore(private val target: TargetApp) {

    @Volatile
    var config: StyleConfig = StyleConfig.DEFAULT
        private set

    @Volatile
    var appContext: Context? = null
        private set

    @Volatile
    private var foreground: WeakReference<Activity>? = null

    private val listeners = CopyOnWriteArrayList<(StyleConfig) -> Unit>()
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    fun addListener(listener: (StyleConfig) -> Unit) {
        listeners += listener
    }

    fun foregroundActivity(): Activity? = foreground?.get()

    fun install() {
        Hooks.guard("Instrumentation.callApplicationOnCreate") {
            Hooks.before(Instrumentation::class.java, "callApplicationOnCreate", Application::class.java) { p ->
                (p.args[0] as? Application)?.let { onApplication(it) }
            }
        }
        Hooks.guard("Instrumentation.callActivityOnCreate") {
            Hooks.before(
                Instrumentation::class.java, "callActivityOnCreate", Activity::class.java, Bundle::class.java,
            ) { p ->
                val activity = p.args[0] as? Activity ?: return@before
                if (appContext == null) onApplication(activity.application ?: return@before)
                applyFromExtras(activity.intent?.extras)
            }
        }
        Hooks.guard("Instrumentation.callActivityOnResume") {
            Hooks.after(Instrumentation::class.java, "callActivityOnResume", Activity::class.java) { p ->
                (p.args[0] as? Activity)?.let { foreground = WeakReference(it) }
            }
        }
    }

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences("patchstudio_${target.id}", Context.MODE_PRIVATE)

    @Synchronized
    private fun onApplication(app: Application) {
        if (appContext != null) return
        val ctx: Context = app.applicationContext ?: app
        appContext = ctx
        try {
            val raw = HashMap<String, String>()
            for ((k, v) in prefs(ctx).all) if (v is String) raw[k] = v
            if (raw.isNotEmpty()) config = StyleConfig.fromMap(raw)
        } catch (t: Throwable) {
            Hooks.log("lecture du cache local impossible : $t")
        }
        notifyListeners()
        registerUpdateReceiver(ctx)
        sendHello(ctx)
        // Le provider peut démarrer le process de l'app : hors thread principal.
        Thread({ loadFromProvider(ctx) }, "PatchStudio-config").apply { isDaemon = true }.start()
    }

    private fun loadFromProvider(ctx: Context) {
        try {
            val uri = Uri.parse("content://${ConfigProtocol.PROVIDER_AUTHORITY}/${target.id}")
            val raw = HashMap<String, String>()
            ctx.contentResolver.query(uri, arrayOf("key", "value"), null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val k = c.getString(0) ?: continue
                    val v = c.getString(1) ?: continue
                    raw[k] = v
                }
            }
            if (raw.isEmpty()) return
            val fresh = StyleConfig.fromMap(raw)
            if (fresh != config) mainHandler.post { update(fresh, recreate = true, toast = false) }
        } catch (t: Throwable) {
            Hooks.log("provider indisponible (visibilité de paquets ?) : $t")
        }
    }

    /** Applique la config transmise dans les extras d'un Intent (lancement depuis Patch Studio). */
    fun applyFromExtras(extras: Bundle?): Boolean {
        if (extras == null) return false
        return try {
            val map = HashMap<String, String?>()
            for (key in extras.keySet()) {
                if (key.startsWith(ConfigProtocol.EXTRA_CONFIG_PREFIX)) map[key] = extras.getString(key)
            }
            val decoded = ConfigProtocol.decode(map) ?: return false
            if (decoded != config) update(decoded, recreate = false, toast = false)
            true
        } catch (t: Throwable) {
            Hooks.log("extras illisibles : $t")
            false
        }
    }

    private fun update(newConfig: StyleConfig, recreate: Boolean, toast: Boolean) {
        config = newConfig
        persist(newConfig)
        notifyListeners()
        if (!recreate) return
        val activity = foregroundActivity() ?: return
        mainHandler.post {
            try {
                if (!activity.isFinishing && !activity.isDestroyed) activity.recreate()
                if (toast) Toast.makeText(activity.applicationContext, TOAST_TEXT, Toast.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                Hooks.log("recreate impossible : $t")
            }
        }
    }

    private fun persist(cfg: StyleConfig) {
        val ctx = appContext ?: return
        try {
            val editor = prefs(ctx).edit().clear()
            for ((k, v) in cfg.toMap()) editor.putString(k, v)
            editor.apply()
        } catch (t: Throwable) {
            Hooks.log("sauvegarde locale impossible : $t")
        }
    }

    private fun notifyListeners() {
        val cfg = config
        for (l in listeners) {
            try {
                l(cfg)
            } catch (t: Throwable) {
                Hooks.logOnce("listener", t)
            }
        }
    }

    private fun sendHello(ctx: Context) {
        try {
            val intent = Intent(ConfigProtocol.ACTION_HELLO)
                .setPackage(ConfigProtocol.MODULE_PACKAGE)
                .putExtra(ConfigProtocol.EXTRA_APP_ID, target.id)
                .putExtra(ConfigProtocol.EXTRA_VERSION, BuildConfig.VERSION_NAME)
            ctx.sendBroadcast(intent)
        } catch (t: Throwable) {
            Hooks.log("HELLO non envoyé : $t")
        }
    }

    private fun registerUpdateReceiver(ctx: Context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    if (intent.action != ConfigProtocol.ACTION_UPDATE) return
                    val appId = intent.getStringExtra(ConfigProtocol.EXTRA_APP_ID)
                    if (appId != null && appId != target.id) return
                    val map = HashMap<String, String?>()
                    val extras = intent.extras ?: return
                    for (key in extras.keySet()) {
                        if (key.startsWith(ConfigProtocol.EXTRA_CONFIG_PREFIX)) map[key] = extras.getString(key)
                    }
                    val decoded = ConfigProtocol.decode(map) ?: return
                    update(decoded, recreate = true, toast = true)
                } catch (t: Throwable) {
                    Hooks.log("mise à jour reçue illisible : $t")
                }
            }
        }
        try {
            val filter = IntentFilter(ConfigProtocol.ACTION_UPDATE)
            // L'émetteur (Patch Studio) doit détenir la permission signature.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ctx.registerReceiver(receiver, filter, ConfigProtocol.PERMISSION, null, Context.RECEIVER_EXPORTED)
            } else {
                ctx.registerReceiver(receiver, filter, ConfigProtocol.PERMISSION, null)
            }
        } catch (t: Throwable) {
            Hooks.log("receiver non enregistré : $t")
        }
    }

    private companion object {
        const val TOAST_TEXT = "Patch Studio : style appliqué ✨"
    }
}
