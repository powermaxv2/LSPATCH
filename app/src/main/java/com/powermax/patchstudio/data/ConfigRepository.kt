package com.powermax.patchstudio.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import com.powermax.patchstudio.core.ConfigProtocol
import com.powermax.patchstudio.core.StyleConfig
import com.powermax.patchstudio.core.TargetApps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/** Source de vérité côté app : config par appli cible + statut du module. */
class ConfigRepository private constructor(context: Context) {

    private val appContext: Context = context.applicationContext
    private val statusPrefs: SharedPreferences =
        appContext.getSharedPreferences(STATUS_PREFS, Context.MODE_PRIVATE)
    private val configFlows = ConcurrentHashMap<String, MutableStateFlow<StyleConfig>>()
    private val helloFlows = ConcurrentHashMap<String, MutableStateFlow<Long>>()
    private val helloVersionFlows = ConcurrentHashMap<String, MutableStateFlow<String?>>()

    private fun prefs(appId: String): SharedPreferences =
        appContext.getSharedPreferences("ps_config_$appId", Context.MODE_PRIVATE)

    private fun load(appId: String): StyleConfig {
        val raw = HashMap<String, String>()
        for ((k, v) in prefs(appId).all) if (v is String) raw[k] = v
        return if (raw.isEmpty()) StyleConfig.DEFAULT else StyleConfig.fromMap(raw)
    }

    private fun flowFor(appId: String): MutableStateFlow<StyleConfig> =
        configFlows.getOrPut(appId) { MutableStateFlow(load(appId)) }

    private fun helloFor(appId: String): MutableStateFlow<Long> =
        helloFlows.getOrPut(appId) { MutableStateFlow(statusPrefs.getLong(helloKey(appId), 0L)) }

    private fun helloVersionFor(appId: String): MutableStateFlow<String?> =
        helloVersionFlows.getOrPut(appId) { MutableStateFlow(statusPrefs.getString(helloVersionKey(appId), null)) }

    fun configFlow(appId: String): StateFlow<StyleConfig> = flowFor(appId).asStateFlow()

    fun current(appId: String): StyleConfig = flowFor(appId).value

    @Synchronized
    fun update(appId: String, transform: (StyleConfig) -> StyleConfig) {
        val flow = flowFor(appId)
        val next = transform(flow.value)
        save(appId, next)
        flow.value = next
    }

    @Synchronized
    fun replace(appId: String, config: StyleConfig) {
        save(appId, config)
        flowFor(appId).value = config
    }

    @Synchronized
    fun reset(appId: String) {
        prefs(appId).edit().clear().apply()
        flowFor(appId).value = StyleConfig.DEFAULT
    }

    private fun save(appId: String, config: StyleConfig) {
        val editor = prefs(appId).edit().clear()
        for ((k, v) in config.toMap()) editor.putString(k, v)
        editor.apply()
    }

    /**
     * Broadcast explicite vers la cible. Pas de receiverPermission (la cible ne la détient pas) :
     * c'est le receveur côté hook qui exige que l'émetteur détienne [ConfigProtocol.PERMISSION].
     * Renvoie false si l'application cible n'est pas installée.
     */
    fun pushToTarget(appId: String): Boolean {
        val target = TargetApps.byId(appId) ?: return false
        if (!isInstalled(appId)) return false
        return try {
            val intent = Intent(ConfigProtocol.ACTION_UPDATE)
                .setPackage(target.packageName)
                .putExtra(ConfigProtocol.EXTRA_APP_ID, appId)
            for ((k, v) in ConfigProtocol.encode(current(appId))) intent.putExtra(k, v)
            appContext.sendBroadcast(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun launchTarget(context: Context, appId: String): Boolean {
        val target = TargetApps.byId(appId) ?: return false
        if (!isInstalled(appId)) return false
        val intent = context.packageManager.getLaunchIntentForPackage(target.packageName) ?: return false
        intent.putExtra(ConfigProtocol.EXTRA_APP_ID, appId)
        for ((k, v) in ConfigProtocol.encode(current(appId))) intent.putExtra(k, v)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        pushToTarget(appId)
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun isInstalled(appId: String): Boolean = installedVersion(appId) != null

    /** Nom de version de l'app cible, ou null si non installée. */
    fun installedVersion(appId: String): String? {
        val target = TargetApps.byId(appId) ?: return null
        return packageVersion(target.packageName)
    }

    /** Nom du paquet LSPatch installé sur l'appareil, ou null si absent. */
    fun findLspatchPackage(): String? =
        LSPATCH_PACKAGES.firstOrNull { packageVersion(it) != null }

    fun launchPackage(context: Context, packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun packageVersion(packageName: String): String? = try {
        appContext.packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    } catch (_: PackageManager.NameNotFoundException) {
        null
    } catch (_: Exception) {
        null
    }

    fun lastHelloFlow(appId: String): StateFlow<Long> = helloFor(appId).asStateFlow()

    /** Dernière version du module signalée par [StatusReceiver], ou null. */
    fun lastHelloVersionFlow(appId: String): StateFlow<String?> = helloVersionFor(appId).asStateFlow()

    /** Appelé par [StatusReceiver]. */
    fun recordHello(appId: String, timestamp: Long = System.currentTimeMillis(), version: String? = null) {
        val editor = statusPrefs.edit().putLong(helloKey(appId), timestamp)
        val cleanVersion = version?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanVersion != null) editor.putString(helloVersionKey(appId), cleanVersion)
        editor.apply()
        helloFor(appId).value = timestamp
        if (cleanVersion != null) helloVersionFor(appId).value = cleanVersion
    }

    private fun helloKey(appId: String) = "hello_$appId"
    private fun helloVersionKey(appId: String) = "hello_ver_$appId"

    companion object {
        private const val STATUS_PREFS = "ps_status"
        private val LSPATCH_PACKAGES = listOf("org.lsposed.lspatch", "io.github.lsposed.lspatch")

        @Volatile
        private var instance: ConfigRepository? = null

        fun get(context: Context): ConfigRepository =
            instance ?: synchronized(this) {
                instance ?: ConfigRepository(context).also { instance = it }
            }
    }
}
