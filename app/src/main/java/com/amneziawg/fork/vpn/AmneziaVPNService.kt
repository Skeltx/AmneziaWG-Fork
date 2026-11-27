package com.amneziawg.fork.vpn

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.ContextCompat
import java.net.InetSocketAddress
import com.amneziawg.fork.db.AmneziaDatabase
import com.amneziawg.fork.model.RoutingAction
import com.amneziawg.fork.model.RoutingConfig
import com.amneziawg.fork.model.RoutingRule
import com.amneziawg.fork.model.RoutingRuleType
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.WgQuickBackend
import com.wireguard.config.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections
import java.util.concurrent.Executors

class AmneziaVPNService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var backend: GoBackend? = null
    private var vpnTunnel: VpnTunnel? = null
    private var currentConfig: RoutingConfig? = null
    private var tunnelFd: ParcelFileDescriptor? = null
    private val connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionStateFlow = connectionState.asStateFlow()

    private val packetProcessor = PacketProcessor()
    private var routingRules: List<RoutingRule> = emptyList()
    private var appProfiles: Map<String, Long> = emptyMap()
    private var domainProfiles: Map<String, DomainRoutingProfile> = emptyMap()

    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING, ERROR
    }

    data class DomainRoutingProfile(
        val domain: String,
        val assignedConfigId: Long?,
        val action: RoutingAction,
        val isRegex: Boolean
    )

    override fun onCreate() {
        super.onCreate()
        Timber.tag("AmneziaVPNService")
        backend = GoBackend(this)
        packetProcessor.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand: ${intent?.action}")

        when (intent?.action) {
            "CONNECT" -> {
                val configId = intent.getLongExtra("config_id", -1)
                if (configId != -1L) {
                    scope.launch { connect(configId) }
                }
            }
            "DISCONNECT" -> {
                scope.launch { disconnect() }
            }
            "RECONNECT" -> {
                scope.launch { reconnect() }
            }
        }

        return START_STICKY
    }

    private suspend fun connect(configId: Long) {
        connectionState.value = ConnectionState.CONNECTING

        try {
            val db = AmneziaDatabase.getInstance(this)
            val configEntity = db.routingConfigDao().getWithRules(configId)

            configEntity?.let { entity ->
                currentConfig = entity.toModel()
                routingRules = currentConfig?.rules ?: emptyList()

                // Load app and domain profiles
                loadProfiles()

                // Build WireGuard config
                val wgConfig = buildWireGuardConfig(currentConfig!!)

                // Establish VPN tunnel
                establishVpnTunnel(wgConfig)

                connectionState.value = ConnectionState.CONNECTED
                logConnectionStart(configId)

                // Start keep-alive monitoring
                startKeepAlive()
            } ?: run {
                Timber.e("Config not found: $configId")
                connectionState.value = ConnectionState.ERROR
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to connect")
            connectionState.value = ConnectionState.ERROR
            stopSelf()
        }
    }

    private fun buildWireGuardConfig(config: RoutingConfig): Config {
        val wg = config.wireguardConfig
        val text = buildString {
            appendLine("[Interface]")
            appendLine("PrivateKey = ${wg.privateKey}")
            wg.address.forEach { appendLine("Address = $it") }
            if (wg.dns.isNotEmpty()) appendLine("DNS = ${wg.dns.joinToString(", ")}")
            if (wg.mtu > 0) appendLine("MTU = ${wg.mtu}")
            appendLine()
            appendLine("[Peer]")
            appendLine("PublicKey = ${wg.publicKey}")
            appendLine("AllowedIPs = ${wg.allowedIPs.joinToString(", ")}")
            if (wg.endpoint.isNotBlank()) appendLine("Endpoint = ${wg.endpoint}")
            if (wg.persistentKeepalive > 0) appendLine("PersistentKeepalive = ${wg.persistentKeepalive}")
        }
        return Config.parse(java.io.BufferedReader(java.io.StringReader(text)))
    }

    private fun parseIpPrefix(cidr: String): Pair<InetAddress, Int> {
        val parts = cidr.split("/")
        val address = InetAddress.getByName(parts[0])
        val prefixLength = if (parts.size > 1) parts[1].toInt() else if (address is java.net.Inet6Address) 128 else 32
        return address to prefixLength
    }

    private fun parseEndpoint(endpoint: String): InetSocketAddress {
        val parts = endpoint.split(":")
        val host = parts[0]
        val port = if (parts.size > 1) parts[1].toInt() else 51820
        return java.net.InetSocketAddress(host, port)
    }

    private fun establishVpnTunnel(config: Config) {
        // The WireGuard backend establishes and manages the tun device itself.
        vpnTunnel = VpnTunnel("amneziawg", config, backend!!, Executors.newSingleThreadExecutor())
        vpnTunnel?.start()
    }

    private fun addDefaultExclusions(builder: android.net.VpnService.Builder) {
        // Exclude local networks
        builder.addRoute("10.0.0.0", 8)
        builder.addRoute("172.16.0.0", 12)
        builder.addRoute("192.168.0.0", 16)
        builder.addRoute("127.0.0.0", 8)
        builder.addRoute("169.254.0.0", 16)
        builder.addRoute("224.0.0.0", 4)
        builder.addRoute("240.0.0.0", 4)

        // IPv6
        builder.addRoute("::1", 128)
        builder.addRoute("fe80::", 10)
        builder.addRoute("fc00::", 7)
        builder.addRoute("ff00::", 8)
    }

    private fun loadProfiles() {
        val db = AmneziaDatabase.getInstance(this)

        scope.launch {
            appProfiles = db.appRoutingProfileDao().getAll().first().associate { it.packageName to (it.assignedConfigId ?: -1L) }
            domainProfiles = db.domainRoutingProfileDao().getAll().first().associate { it.domain to DomainRoutingProfile(
                domain = it.domain,
                assignedConfigId = it.assignedConfigId,
                action = com.amneziawg.fork.model.RoutingAction.valueOf(it.action),
                isRegex = it.isRegex
            ) }
        }
    }

    private fun startKeepAlive() {
        scope.launch {
            while (connectionState.value == ConnectionState.CONNECTED) {
                // Send keepalive
                vpnTunnel?.sendKeepalive()
                kotlinx.coroutines.delay(25_000) // 25 seconds
            }
        }
    }

    private suspend fun reconnect() {
        disconnect()
        currentConfig?.id?.let { connect(it) }
    }

    suspend fun disconnect() {
        connectionState.value = ConnectionState.DISCONNECTING

        vpnTunnel?.close()
        vpnTunnel = null
        tunnelFd?.close()
        tunnelFd = null

        logConnectionEnd()
        connectionState.value = ConnectionState.DISCONNECTED
        stopSelf()
    }

    private fun logConnectionStart(configId: Long) {
        val db = AmneziaDatabase.getInstance(this)
        val log = VpnConnectionLogEntity(
            configId = configId,
            configName = currentConfig?.name ?: "Unknown",
            connectedAt = System.currentTimeMillis()
        )
        scope.launch { db.vpnConnectionLogDao().insert(log) }
    }

    private fun logConnectionEnd() {
        // Update last log entry with disconnect info
        // Implementation depends on tracking the log ID
    }

    override fun onDestroy() {
        scope.coroutineContext.cancelChildren()
        backend?.close()
        packetProcessor.stop()
        super.onDestroy()
        Timber.d("Service destroyed")
    }

    // API for external control
    companion object {
        private const val ACTION_CONNECT = "com.amneziawg.fork.CONNECT"
        private const val ACTION_DISCONNECT = "com.amneziawg.fork.DISCONNECT"
        private const val ACTION_RECONNECT = "com.amneziawg.fork.RECONNECT"
        private const val EXTRA_CONFIG_ID = "config_id"

        fun startConnection(context: Context, configId: Long) {
            val intent = Intent(context, AmneziaVPNService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_CONFIG_ID, configId)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopConnection(context: Context) {
            val intent = Intent(context, AmneziaVPNService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }

        fun reconnect(context: Context) {
            val intent = Intent(context, AmneziaVPNService::class.java).apply {
                action = ACTION_RECONNECT
            }
            context.startService(intent)
        }

        fun prepareVpn(context: Context): Intent? {
            return VpnService.prepare(context)
        }
    }
}

// Packet processor for per-app/per-domain routing decisions
class PacketProcessor {
    private val job = Job()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    private val packetChannel = Channel<ByteArray>(1000)

    fun start() {
        scope.launch {
            while (isActive) {
                val packet = packetChannel.receive()
                processPacket(packet)
            }
        }
    }

    fun stop() {
        job.cancel()
    }

    fun submitPacket(packet: ByteArray) {
        packetChannel.trySend(packet)
    }

    private fun processPacket(packet: ByteArray) {
        // Parse packet headers to determine routing
        // This is a simplified version - real implementation would parse IP/TCP/UDP headers
        // and match against routing rules
    }
}

// End of file