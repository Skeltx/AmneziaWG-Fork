package com.amneziawg.fork.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class VpnStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("VpnStateReceiver: ${intent.action}")

        // Handle VPN state changes
        // Could be used for UI updates, notifications, etc.
    }
}