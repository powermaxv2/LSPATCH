package com.powermax.patchstudio.core

/** Protocole app <-> process hooké. */
object ConfigProtocol {
    const val MODULE_PACKAGE = "com.powermax.patchstudio"
    const val ACTION_UPDATE = "com.powermax.patchstudio.action.UPDATE_CONFIG"
    const val ACTION_HELLO = "com.powermax.patchstudio.action.HELLO"
    const val PERMISSION = "com.powermax.patchstudio.permission.CONFIG"
    const val EXTRA_APP_ID = "ps_app_id"
    const val EXTRA_CONFIG_PREFIX = "ps_cfg_"
    const val EXTRA_VERSION = "ps_version"
    const val PROVIDER_AUTHORITY = "com.powermax.patchstudio.config"

    fun encode(config: StyleConfig): Map<String, String> =
        config.toMap().mapKeys { EXTRA_CONFIG_PREFIX + it.key }

    fun decode(extras: Map<String, String?>): StyleConfig? {
        val raw = HashMap<String, String>()
        for ((k, v) in extras) {
            if (k.startsWith(EXTRA_CONFIG_PREFIX) && v != null) raw[k.removePrefix(EXTRA_CONFIG_PREFIX)] = v
        }
        return if (raw.isEmpty()) null else StyleConfig.fromMap(raw)
    }
}
