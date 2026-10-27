package com.powermax.patchstudio.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.powermax.patchstudio.core.ConfigProtocol
import com.powermax.patchstudio.core.TargetApps

/** Reçoit le HELLO envoyé par le module depuis l'appli cible. */
class StatusReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ConfigProtocol.ACTION_HELLO) return
        val appId = intent.getStringExtra(ConfigProtocol.EXTRA_APP_ID) ?: return
        if (TargetApps.byId(appId) == null) return
        ConfigRepository.get(context).recordHello(appId, System.currentTimeMillis())
    }
}
