package com.amneziawg.fork.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.amneziawg.fork.vpn.AmneziaVPNService
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("BootReceiver: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_QUICKBOOT_POWERON -> {
                // Check if we should auto-reconnect
                val prefs = context.getSharedPreferences("amneziawg_prefs", Context.MODE_PRIVATE)
                val autoReconnect = prefs.getBoolean("auto_reconnect", true)
                val alwaysOnVpn = prefs.getBoolean("always_on_vpn", false)

                if (autoReconnect || alwaysOnVpn) {
                    // Find the last active config
                    // This would need database access - for now just log
                    Timber.d("Auto-reconnect enabled, would restore VPN connection")
                }
            }
        }
    }
}