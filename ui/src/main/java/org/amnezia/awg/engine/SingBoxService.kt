package org.amnezia.awg.engine

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.IpPrefix
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.Process
import android.system.OsConstants
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.ConnectionOwner
import io.nekohasekai.libbox.InterfaceUpdateListener
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.NetworkInterfaceIterator
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.PlatformInterface
import io.nekohasekai.libbox.RoutePrefix
import io.nekohasekai.libbox.RoutePrefixIterator
import io.nekohasekai.libbox.StringIterator
import io.nekohasekai.libbox.SystemProxyStatus
import io.nekohasekai.libbox.TunOptions
import io.nekohasekai.libbox.WIFIState
import io.nekohasekai.libbox.NetworkInterface as LibboxNetworkInterface
import io.nekohasekai.libbox.Notification as LibboxNotification
import org.amnezia.awg.R
import org.amnezia.awg.activity.MainActivity
import java.io.File
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface
import java.util.concurrent.Executors

/**
 * One Android VpnService for all tunnels. sing-box owns the TUN and its own userspace routing
 * table; each active WireGuard endpoint is a separate sing-box endpoint.
 */
class SingBoxService : VpnService(), PlatformInterface, CommandServerHandler {
    companion object {
        private const val TAG = "AmneziaWG/SingBox"
        private const val CHANNEL_ID = "amneziawg-vpn"
        private const val NOTIFICATION_ID = 1
        private const val CONFIG_FILE = "engine/config.json"
    }

    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "singbox-engine")
    }
    private val networkHandler by lazy {
        Handler(HandlerThread("singbox-network").apply { start() }.looper)
    }
    private val connectivity: ConnectivityManager by lazy {
        getSystemService(ConnectivityManager::class.java)
    }

    private var commandServer: CommandServer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var networkListener: InterfaceUpdateListener? = null
    private var starting = false
    private var stopping = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNow(getString(R.string.app_name))
        when (intent?.action) {
            EngineController.ACTION_STOP -> stopEngine()
            EngineController.ACTION_START, EngineController.ACTION_RELOAD, null -> startOrReload()
            else -> startOrReload()
        }
        return START_STICKY
    }

    override fun onRevoke() {
        stopEngine()
    }

    override fun onDestroy() {
        stopping = true
        runCatching { commandServer?.closeService() }
        runCatching { commandServer?.close() }
        commandServer = null
        runCatching { fileDescriptor?.close() }
        fileDescriptor = null
        unregisterNetworkCallback()
        EngineController.onStopped()
        worker.shutdownNow()
        super.onDestroy()
    }

    // ---------------------------------------------------------------------
    // Engine lifecycle
    // ---------------------------------------------------------------------

    private fun startOrReload() {
        if (starting || stopping) return
        starting = true
        val configFile = File(filesDir, CONFIG_FILE)
        worker.execute {
            try {
                val config = configFile.readText()
                require(config.isNotBlank()) { "empty engine config" }
                val server = commandServer ?: CommandServer(this, this).also {
                    it.start()
                    commandServer = it
                }
                server.startOrReloadService(config, OverrideOptions().apply {
                    autoRedirect = false
                    // Empty iterators mean "no override"; the TUN config carries the lists.
                    includePackage = StringArray(emptyList<String>().iterator())
                    excludePackage = StringArray(emptyList<String>().iterator())
                })
                EngineController.onStarted()
                updateNotification(getString(R.string.app_name))
            } catch (e: Throwable) {
                EngineController.onError(e)
                updateNotification("Ошибка: ${e.message ?: e.javaClass.simpleName}")
                stopSelfSafely()
            } finally {
                starting = false
            }
        }
    }

    private fun stopEngine() {
        if (stopping) return
        stopping = true
        worker.execute {
            runCatching { commandServer?.closeService() }
            runCatching { commandServer?.close() }
            commandServer = null
            runCatching { fileDescriptor?.close() }
            fileDescriptor = null
            unregisterNetworkCallback()
            EngineController.onStopped()
            stopForegroundCompat()
            stopSelf()
            stopping = false
        }
    }

    private fun stopSelfSafely() {
        runCatching { stopEngine() }
    }

    // ---------------------------------------------------------------------
    // PlatformInterface
    // ---------------------------------------------------------------------

    override fun usePlatformAutoDetectInterfaceControl(): Boolean = true

    override fun autoDetectInterfaceControl(fd: Int) {
        protect(fd)
    }

    override fun openTun(options: TunOptions): Int {
        if (prepare(this) != null) error("android: missing vpn permission")

        val builder = Builder()
            .setSession("AmneziaWG")
            .setMtu(options.mtu)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        forEachPrefix(options.inet4Address) { builder.addAddress(it.address(), it.prefix()) }
        forEachPrefix(options.inet6Address) { builder.addAddress(it.address(), it.prefix()) }

        val dns = options.dnsServerAddress
        while (dns.hasNext()) {
            builder.addDnsServer(dns.next())
        }

        if (options.autoRoute) {
            var ipv4Routes = 0
            forEachPrefix(options.inet4RouteRange) {
                builder.addRoute(it.address(), it.prefix())
                ipv4Routes++
            }
            if (ipv4Routes == 0 && options.inet4Address.hasNext()) {
                builder.addRoute("0.0.0.0", 0)
            }

            var ipv6Routes = 0
            forEachPrefix(options.inet6RouteRange) {
                builder.addRoute(it.address(), it.prefix())
                ipv6Routes++
            }
            if (ipv6Routes == 0 && options.inet6Address.hasNext()) {
                builder.addRoute("::", 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                forEachPrefix(options.inet4RouteExcludeAddress) {
                    runCatching { builder.excludeRoute(it.toIpPrefix()) }
                }
                forEachPrefix(options.inet6RouteExcludeAddress) {
                    runCatching { builder.excludeRoute(it.toIpPrefix()) }
                }
            }
        }

        applyPackages(builder, options)

        val pfd = builder.establish()
            ?: error("android: the application is not prepared or is revoked")
        fileDescriptor = pfd
        return pfd.fd
    }

    private fun applyPackages(builder: Builder, options: TunOptions) {
        val include = options.includePackage
        if (include.hasNext()) {
            while (include.hasNext()) {
                try {
                    builder.addAllowedApplication(include.next())
                } catch (_: NameNotFoundException) {
                }
            }
        } else {
            val exclude = options.excludePackage
            while (exclude.hasNext()) {
                try {
                    builder.addDisallowedApplication(exclude.next())
                } catch (_: NameNotFoundException) {
                }
            }
        }
    }

    override fun findConnectionOwner(
        ipProtocol: Int,
        sourceAddress: String,
        sourcePort: Int,
        destinationAddress: String,
        destinationPort: Int,
    ): ConnectionOwner {
        val uid = runCatching {
            connectivity.getConnectionOwnerUid(
                ipProtocol,
                InetSocketAddress(sourceAddress, sourcePort),
                InetSocketAddress(destinationAddress, destinationPort),
            )
        }.getOrDefault(Process.INVALID_UID)

        val owner = ConnectionOwner()
        if (uid == Process.INVALID_UID) {
            owner.userId = Process.INVALID_UID
            return owner
        }
        owner.userId = uid
        val packages = runCatching { packageManager.getPackagesForUid(uid)?.toList() }
            .getOrDefault(emptyList())
        owner.userName = packages?.firstOrNull() ?: ""
        owner.setAndroidPackageNames(StringArray((packages ?: emptyList()).iterator()))
        return owner
    }

    override fun startDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        unregisterNetworkCallback()
        networkListener = listener
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = notifyInterface(network, listener)
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) =
                notifyInterface(network, listener)

            override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) =
                notifyInterface(network, listener)
        }
        networkCallback = callback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            connectivity.registerBestMatchingNetworkCallback(request, callback, networkHandler)
        } else {
            connectivity.registerDefaultNetworkCallback(callback, networkHandler)
        }
    }

    override fun closeDefaultInterfaceMonitor(listener: InterfaceUpdateListener) {
        if (networkListener === listener) unregisterNetworkCallback()
    }

    private fun unregisterNetworkCallback() {
        val callback = networkCallback ?: return
        networkCallback = null
        networkListener = null
        runCatching { connectivity.unregisterNetworkCallback(callback) }
    }

    private fun notifyInterface(network: Network, listener: InterfaceUpdateListener) {
        val lp = runCatching { connectivity.getLinkProperties(network) }.getOrNull() ?: return
        val caps = runCatching { connectivity.getNetworkCapabilities(network) }.getOrNull() ?: return
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return
        val iface = NetworkInterface.getByName(lp.interfaceName) ?: return
        val path = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }
        listener.updateDefaultInterface(
            lp.interfaceName,
            iface.index,
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
            false,
        )
        listener.updateNetworkPath(path)
    }

    override fun getInterfaces(): NetworkInterfaceIterator {
        val result = mutableListOf<LibboxNetworkInterface>()
        val systemInterfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        for (network in connectivity.allNetworks) {
            val lp = runCatching { connectivity.getLinkProperties(network) }.getOrNull() ?: continue
            val caps = runCatching { connectivity.getNetworkCapabilities(network) }.getOrNull()
                ?: continue
            val ni = systemInterfaces.firstOrNull { it.name == lp.interfaceName } ?: continue
            val box = LibboxNetworkInterface()
            box.name = lp.interfaceName
            box.dnsServer = StringArray(lp.dnsServers.mapNotNull { it.hostAddress }.iterator())
            box.gateway = StringArray(
                lp.routes
                    .filter { it.destination.prefixLength == 0 }
                    .mapNotNull { it.gateway }
                    .filterNot { it.isAnyLocalAddress }
                    .mapNotNull { it.hostAddress }
                    .iterator(),
            )
            box.type = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Libbox.InterfaceTypeWIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Libbox.InterfaceTypeCellular
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> Libbox.InterfaceTypeEthernet
                else -> Libbox.InterfaceTypeOther
            }
            box.index = ni.index
            runCatching { box.mtu = ni.mtu }
            box.addresses = StringArray(
                ni.interfaceAddresses.map { it.toPrefix() }.iterator(),
            )
            var flags = 0
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                flags = OsConstants.IFF_UP or OsConstants.IFF_RUNNING
            }
            if (ni.isLoopback) flags = flags or OsConstants.IFF_LOOPBACK
            if (ni.isPointToPoint) flags = flags or OsConstants.IFF_POINTOPOINT
            if (ni.supportsMulticast()) flags = flags or OsConstants.IFF_MULTICAST
            box.flags = flags
            box.metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            result += box
        }
        return InterfaceArray(result.iterator())
    }

    override fun underNetworkExtension(): Boolean = false

    override fun includeAllNetworks(): Boolean = false

    override fun readWIFIState(): WIFIState? = null

    override fun clearDNSCache() = Unit

    override fun localDNSTransport() = null

    override fun registerMyInterface(name: String) = Unit

    override fun startNeighborMonitor(listener: io.nekohasekai.libbox.NeighborUpdateListener) = Unit

    override fun closeNeighborMonitor(listener: io.nekohasekai.libbox.NeighborUpdateListener) = Unit

    override fun sendNotification(notification: LibboxNotification) = Unit

    override fun cancelNotification(identifier: String, typeID: Int) = Unit

    override fun usePlatformShell(): Boolean = false

    override fun checkPlatformShell() {
        throw UnsupportedOperationException("platform shell is not available")
    }

    override fun openShellSession(
        user: io.nekohasekai.libbox.PlatformUser,
        command: String,
        environ: StringIterator,
        term: String,
        rows: Int,
        cols: Int,
    ) = throw UnsupportedOperationException("platform shell is not available")

    override fun lookupUser(username: String) =
        throw UnsupportedOperationException("platform users are not available")

    override fun lookupSFTPServer() =
        throw UnsupportedOperationException("sftp is not available")

    override fun readSystemSSHHostKey() =
        throw UnsupportedOperationException("ssh host key is not available")

    override fun tailscaleHostname(): String = ""

    override fun usePlatformBridge(): Boolean = false

    override fun createBridge(options: io.nekohasekai.libbox.BridgeOptions) =
        throw UnsupportedOperationException("bridge is not available")

    // ---------------------------------------------------------------------
    // CommandServerHandler
    // ---------------------------------------------------------------------

    override fun serviceStop() {
        if (!stopping) stopEngine()
    }

    override fun serviceReload() {
        startOrReload()
    }

    override fun getSystemProxyStatus(): SystemProxyStatus = SystemProxyStatus().apply {
        available = false
        enabled = false
    }

    override fun setSystemProxyEnabled(enabled: Boolean) = Unit

    override fun connectSSHAgent(): Int =
        throw UnsupportedOperationException("ssh agent is not available")

    override fun triggerNativeCrash() =
        throw UnsupportedOperationException("native crash trigger is disabled")

    override fun writeDebugMessage(message: String) {
        Log.d(TAG, message)
    }

    // ---------------------------------------------------------------------
    // Notification and helpers
    // ---------------------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "AmneziaWG",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    private fun buildNotification(text: String): android.app.Notification {
        val intent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(intent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun startForegroundNow(text: String) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(text), type)
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun stopForegroundCompat() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun forEachPrefix(iterator: RoutePrefixIterator, block: (RoutePrefix) -> Unit) {
        while (iterator.hasNext()) block(iterator.next())
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun RoutePrefix.toIpPrefix(): IpPrefix =
        IpPrefix(InetAddress.getByName(address()), prefix())

    private fun InterfaceAddress.toPrefix(): String = if (address is Inet6Address) {
        "${Inet6Address.getByAddress(address.address).hostAddress}/$networkPrefixLength"
    } else {
        "${address.hostAddress}/$networkPrefixLength"
    }

    private class StringArray(private val iterator: Iterator<String>) : StringIterator {
        override fun len(): Int = 0
        override fun hasNext(): Boolean = iterator.hasNext()
        override fun next(): String = iterator.next()
    }

    private class InterfaceArray(
        private val iterator: Iterator<LibboxNetworkInterface>,
    ) : NetworkInterfaceIterator {
        override fun hasNext(): Boolean = iterator.hasNext()
        override fun next(): LibboxNetworkInterface = iterator.next()
    }
}
