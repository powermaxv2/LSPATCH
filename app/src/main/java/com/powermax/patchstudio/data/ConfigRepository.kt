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
     */
    fun pushToTarget(appId: String) {
        val target = TargetApps.byId(appId) ?: return
        try {
            val intent = Intent(ConfigProtocol.ACTION_UPDATE)
                .setPackage(target.packageName)
                .putExtra(ConfigProtocol.EXTRA_APP_ID, appId)
            for ((k, v) in ConfigProtocol.encode(current(appId))) intent.putExtra(k, v)
            appContext.sendBroadcast(intent)
        } catch (_: Exception) {
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

    fun isInstalled(appId: String): Boolean {
        val target = TargetApps.byId(appId) ?: return false
        return try {
            appContext.packageManager.getPackageInfo(target.packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun lastHelloFlow(appId: String): StateFlow<Long> = helloFor(appId).asStateFlow()

    /** Appelé par [StatusReceiver]. */
    fun recordHello(appId: String, timestamp: Long = System.currentTimeMillis()) {
        statusPrefs.edit().putLong(helloKey(appId), timestamp).apply()
        helloFor(appId).value = timestamp
    }

    private fun helloKey(appId: String) = "hello_$appId"

    companion object {
        private const val STATUS_PREFS = "ps_status"

        @Volatile
        private var instance: ConfigRepository? = null

        fun get(context: Context): ConfigRepository =
            instance ?: synchronized(this) {
                instance ?: ConfigRepository(context).also { instance = it }
            }
    }
}
