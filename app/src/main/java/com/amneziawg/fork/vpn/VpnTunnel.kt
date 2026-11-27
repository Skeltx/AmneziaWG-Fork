package com.amneziawg.fork.vpn

import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.util.concurrent.ExecutorService

/**
 * Thin wrapper around the WireGuard backend so the service can start/stop a tunnel
 * without depending on a non-existent library class.
 */
class VpnTunnel(
    private val tunnelName: String,
    private val config: Config,
    private val backend: Backend,
    private val executor: ExecutorService
) {
    private val tunnel = object : Tunnel {
        override fun getName(): String = tunnelName
        override fun onStateChange(newState: Tunnel.State) = Unit
    }

    fun start() {
        try {
            backend.setState(tunnel, Tunnel.State.UP, config)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to start WireGuard tunnel", e)
        }
    }

    fun sendKeepalive() {
        // Keepalive is configured through PersistentKeepalive in the WireGuard config.
    }

    fun close() {
        try {
            backend.setState(tunnel, Tunnel.State.DOWN, null)
        } catch (_: Exception) {
            // Tunnel already down or backend unavailable.
        } finally {
            executor.shutdown()
        }
    }
}
