package org.amnezia.awg.engine

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Small facade around the foreground [SingBoxService]. The backend writes the generated config
 * first and then asks the service to (re)load it, so the service never needs a Binder API.
 */
object EngineController {
    private const val TAG = "AmneziaWG/Engine"

    const val ACTION_START = "org.amnezia.awg.engine.action.START"
    const val ACTION_STOP = "org.amnezia.awg.engine.action.STOP"
    const val ACTION_RELOAD = "org.amnezia.awg.engine.action.RELOAD"
    const val CONFIG_FILE = "config.json"

    @Volatile
    var running: Boolean = false
        private set

    @Volatile
    var lastError: String? = null
        private set

    fun start(context: Context, config: String) {
        val dir = File(context.filesDir, "engine").apply { mkdirs() }
        File(dir, CONFIG_FILE).writeText(config)
        lastError = null
        send(context, ACTION_START)
    }

    fun reload(context: Context) = send(context, ACTION_RELOAD)

    fun stop(context: Context) = send(context, ACTION_STOP)

    fun onStarted() {
        running = true
        lastError = null
    }

    fun onStopped() {
        running = false
    }

    fun onError(error: Throwable) {
        running = false
        lastError = error.message ?: error.javaClass.simpleName
        Log.e(TAG, "engine error", error)
    }

    private fun send(context: Context, action: String) {
        val intent = Intent(context, SingBoxService::class.java).setAction(action)
        runCatching { ContextCompat.startForegroundService(context, intent) }
            .onFailure { Log.e(TAG, "cannot send $action", it) }
    }
}
