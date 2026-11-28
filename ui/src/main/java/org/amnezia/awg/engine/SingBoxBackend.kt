package org.amnezia.awg.engine

import org.amnezia.awg.BuildConfig
import org.amnezia.awg.backend.Backend
import org.amnezia.awg.backend.Statistics
import org.amnezia.awg.backend.StatusCallback
import org.amnezia.awg.backend.Tunnel
import android.os.Handler
import android.os.Looper
import org.amnezia.awg.config.Config
import java.util.LinkedHashMap

/**
 * [Backend] implementation backed by sing-box-lx. Unlike the old wg-go backend it can keep
 * several tunnels (endpoints) up at the same time; routing rules decide which endpoint a
 * package/domain/IP uses.
 */
class SingBoxBackend(private val context: android.content.Context) : Backend {
    private val active = LinkedHashMap<String, Config>()
    private val rulesStore = RoutingRulesStore(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var statusCallback: StatusCallback? = null

    @Synchronized
    override fun getRunningTunnelNames(): Set<String> = active.keys.toSet()

    @Synchronized
    override fun getState(tunnel: Tunnel): Tunnel.State =
        if (active.containsKey(tunnel.getName())) Tunnel.State.UP else Tunnel.State.DOWN

    @Synchronized
    override fun getLastHandshake(tunnel: Tunnel): Long =
        if (active.containsKey(tunnel.getName())) 0L else -3L

    @Synchronized
    override fun getStatistics(tunnel: Tunnel): Statistics = Statistics()

    override fun getVersion(): String = "sing-box-lx ${BuildConfig.SINGBOX_VERSION}"

    @Synchronized
    override fun setState(tunnel: Tunnel, state: Tunnel.State, config: Config?): Tunnel.State {
        val name = tunnel.getName()
        val previous = LinkedHashMap(active)
        try {
            when (state) {
                Tunnel.State.UP -> {
                    val cfg = config ?: throw IllegalArgumentException("missing config for $name")
                    active[name] = cfg
                }
                Tunnel.State.DOWN -> active.remove(name)
                Tunnel.State.TOGGLE -> {
                    if (active.containsKey(name)) {
                        active.remove(name)
                    } else {
                        val cfg = config ?: throw IllegalArgumentException("missing config for $name")
                        active[name] = cfg
                    }
                }
            }
            applyConfig()
        } catch (e: Throwable) {
            active.clear()
            active.putAll(previous)
            throw e
        }
        return if (active.containsKey(name)) Tunnel.State.UP else Tunnel.State.DOWN
    }

    override fun setStatusCallback(callback: StatusCallback?) {
        statusCallback = callback
        if (callback != null && EngineController.running) notifyStatus(true)
    }

    /** Re-applies the current rules without touching the set of active tunnels. */
    @Synchronized
    fun reloadRouting() {
        if (active.isNotEmpty()) applyConfig()
    }

    /**
     * Rebuilds the sing-box config from the active tunnels and current rules, then asks the
     * service to start or hot-reload. Called on the backend's IO coroutine.
     */
    @Synchronized
    private fun applyConfig() {
        if (active.isEmpty()) {
            EngineController.stop(context)
            notifyStatus(false)
            return
        }
        val settings = rulesStore.load()
        val json = SingBoxConfigBuilder.build(active, settings)
        EngineController.start(context, json)
        notifyStatus(true)
    }

    /**
     * The UI updates the tunnel state after [setState] returns, so report status on the next
     * main-loop turn instead of synchronously (the tunnel would still be DOWN at that point).
     */
    private fun notifyStatus(connected: Boolean) {
        mainHandler.postDelayed({
            if (connected && !EngineController.running) return@postDelayed
            statusCallback?.onStatusChanged(connected)
        }, if (connected) 1200L else 0L)
    }
}
