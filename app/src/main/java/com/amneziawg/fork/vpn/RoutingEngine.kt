package com.amneziawg.fork.vpn

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import android.os.Process
import android.util.Log
import com.amneziawg.fork.db.AmneziaDatabase
import com.amneziawg.fork.model.AppRoutingProfile
import com.amneziawg.fork.model.DomainRoutingProfile
import com.amneziawg.fork.model.RoutingAction
import com.amneziawg.fork.model.RoutingConfig
import com.amneziawg.fork.model.RoutingRule
import com.amneziawg.fork.model.RoutingRuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.regex.Pattern

/**
 * Core routing engine that decides how each packet/connection should be routed:
 * - Through VPN
 * - Direct (bypass VPN)
 * - Blocked
 * - Through a different WireGuard config
 */
class RoutingEngine(
    private val context: Context,
    private val activeConfig: RoutingConfig
) {

    private val db = AmneziaDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // Cached routing decisions for performance
    private val packageDecisionCache = ConcurrentHashMap<String, RoutingDecision>()
    private val domainDecisionCache = ConcurrentHashMap<String, RoutingDecision>()
    private val ipDecisionCache = ConcurrentHashMap<String, RoutingDecision>()

    // UID to package mapping
    private val uidToPackage = ConcurrentHashMap<Int, String>()
    private val packageToUid = ConcurrentHashMap<String, Int>()

    // Compiled regex patterns for domain matching
    private val domainRegexPatterns = mutableMapOf<String, Pattern>()

    // Stats
    private val stats = RoutingStats()

    data class RoutingDecision(
        val action: RoutingAction,
        val configId: Long?, // Target config if action is ROUTE_VIA_CONFIG
        val matchedRule: RoutingRule?,
        val reason: String
    )

    data class RoutingStats(
        var vpnPackets: Long = 0,
        var directPackets: Long = 0,
        var blockedPackets: Long = 0,
        var otherConfigPackets: Long = 0,
        var totalBytesVpn: Long = 0,
        var totalBytesDirect: Long = 0
    )

    init {
        loadRoutingData()
        startCacheCleanup()
    }

    private fun loadRoutingData() {
        scope.launch {
            // Load app profiles
            val appProfiles = db.appRoutingProfileDao().getAll().firstOrNull() ?: emptyList()
            appProfiles.forEach { profile ->
                packageToUid[profile.packageName] = getUidForPackage(profile.packageName) ?: -1
                profile.assignedConfigId?.let { configId ->
                    packageDecisionCache[profile.packageName] = RoutingDecision(
                        action = RoutingAction.ROUTE_VIA_CONFIG,
                        configId = configId,
                        matchedRule = null,
                        reason = "App profile: ${profile.appName}"
                    )
                }
                profile.customRules.forEach { rule ->
                    if (rule.isEnabled) {
                        packageDecisionCache["${profile.packageName}:${rule.target}"] = RoutingDecision(
                            action = rule.action,
                            configId = null,
                            matchedRule = rule,
                            reason = "Custom rule: ${rule.name}"
                        )
                    }
                }
            }

            // Load domain profiles
            val domainProfiles = db.domainRoutingProfileDao().getAll().firstOrNull() ?: emptyList()
            domainProfiles.forEach { profile ->
                if (profile.isRegex) {
                    try {
                        domainRegexPatterns[profile.domain] = Pattern.compile(profile.domain)
                    } catch (e: Exception) {
                        Timber.e(e, "Invalid regex pattern: ${profile.domain}")
                    }
                }
                domainDecisionCache[profile.domain] = RoutingDecision(
                    action = profile.action,
                    configId = profile.assignedConfigId,
                    matchedRule = null,
                    reason = "Domain profile: ${profile.domain}"
                )
            }

            // Load routing rules from active config
            activeConfig.rules.filter { it.isEnabled }.forEach { rule ->
                when (rule.type) {
                    RoutingRuleType.APP_PACKAGE -> {
                        packageDecisionCache[rule.target] = RoutingDecision(
                            action = rule.action,
                            configId = null,
                            matchedRule = rule,
                            reason = "Rule: ${rule.name}"
                        )
                    }
                    RoutingRuleType.DOMAIN, RoutingRuleType.DNS_NAME -> {
                        domainDecisionCache[rule.target] = RoutingDecision(
                            action = rule.action,
                            configId = null,
                            matchedRule = rule,
                            reason = "Rule: ${rule.name}"
                        )
                    }
                    RoutingRuleType.IP_CIDR -> {
                        ipDecisionCache[rule.target] = RoutingDecision(
                            action = rule.action,
                            configId = null,
                            matchedRule = rule,
                            reason = "Rule: ${rule.name}"
                        )
                    }
                    RoutingRuleType.PROCESS_UID -> {
                        rule.target.toIntOrNull()?.let { uid ->
                            uidToPackage[uid] = rule.target
                            packageDecisionCache["uid:$uid"] = RoutingDecision(
                                action = rule.action,
                                configId = null,
                                matchedRule = rule,
                                reason = "Rule: ${rule.name}"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startCacheCleanup() {
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(5 * 60 * 1000) // 5 minutes
                cleanupCaches()
            }
        }
    }

    private fun cleanupCaches() {
        // Keep only recent entries, remove stale ones
        if (packageDecisionCache.size > 1000) {
            val toRemove = packageDecisionCache.keys.take(packageDecisionCache.size - 800)
            toRemove.forEach { packageDecisionCache.remove(it) }
        }
        if (domainDecisionCache.size > 1000) {
            val toRemove = domainDecisionCache.keys.take(domainDecisionCache.size - 800)
            toRemove.forEach { domainDecisionCache.remove(it) }
        }
        if (ipDecisionCache.size > 1000) {
            val toRemove = ipDecisionCache.keys.take(ipDecisionCache.size - 800)
            toRemove.forEach { ipDecisionCache.remove(it) }
        }
    }

    /**
     * Determine routing for an outgoing connection by package name
     */
    fun decideByPackage(packageName: String): RoutingDecision {
        return packageDecisionCache.getOrPut(packageName) {
            // Check custom rules for this package
            val customRules = activeConfig.rules.filter {
                it.isEnabled && it.type == RoutingRuleType.APP_PACKAGE && it.target == packageName
            }.maxByOrNull { it.priority }

            customRules?.let { rule ->
                RoutingDecision(rule.action, null, rule, "Rule: ${rule.name}")
            } ?: run {
                // Check app profile
                val profile = db.appRoutingProfileDao().getByPackageName(packageName).firstOrNull()?.toModel()
                profile?.assignedConfigId?.let { configId ->
                    RoutingDecision(RoutingAction.ROUTE_VIA_CONFIG, configId, null, "App profile")
                } ?: run {
                    // Default action from config
                    RoutingDecision(activeConfig.defaultAction, null, null, "Default: ${activeConfig.defaultAction.displayName}")
                }
            }
        }
    }

    /**
     * Determine routing for a connection by destination domain
     */
    fun decideByDomain(domain: String): RoutingDecision {
        return domainDecisionCache.getOrPut(domain) {
            // Exact match first
            val exactRule = activeConfig.rules.filter {
                it.isEnabled && (it.type == RoutingRuleType.DOMAIN || it.type == RoutingRuleType.DNS_NAME) && it.target == domain
            }.maxByOrNull { it.priority }

            exactRule?.let { rule ->
                RoutingDecision(rule.action, null, rule, "Rule: ${rule.name}")
            } ?: run {
                // Check regex patterns
                val regexMatch = domainRegexPatterns.entries.find { (pattern, _) ->
                    pattern.matcher(domain).matches()
                }
                regexMatch?.let { (_, pattern) ->
                    val rule = activeConfig.rules.find { it.target == pattern.pattern() }
                    rule?.let { r ->
                        RoutingDecision(r.action, null, r, "Regex rule: ${r.name}")
                    } ?: RoutingDecision(activeConfig.defaultAction, null, null, "Default")
                } ?: run {
                    // Check domain profile
                    val profile = db.domainRoutingProfileDao().getByDomain(domain).firstOrNull()?.toModel()
                    profile?.let { p ->
                        RoutingDecision(p.action, p.assignedConfigId, null, "Domain profile")
                    } ?: run {
                        // Check parent domains (e.g., sub.example.com -> example.com)
                        val parts = domain.split(".")
                        for (i in 1 until parts.size) {
                            val parentDomain = parts.drop(i).joinToString(".")
                            val parentDecision = domainDecisionCache[parentDomain]
                            if (parentDecision != null) {
                                return@getOrPut RoutingDecision(
                                    parentDecision.action,
                                    parentDecision.configId,
                                    parentDecision.matchedRule,
                                    "Inherited from $parentDomain"
                                )
                            }
                        }
                        RoutingDecision(activeConfig.defaultAction, null, null, "Default")
                    }
                }
            }
        }
    }

    /**
     * Determine routing by destination IP address
     */
    fun decideByIp(ipAddress: String): RoutingDecision {
        return ipDecisionCache.getOrPut(ipAddress) {
            // Check CIDR rules
            val matchingRule = activeConfig.rules.filter { rule ->
                rule.isEnabled && rule.type == RoutingRuleType.IP_CIDR && matchesCidr(ipAddress, rule.target)
            }.maxByOrNull { it.priority }

            matchingRule?.let { rule ->
                RoutingDecision(rule.action, null, rule, "CIDR Rule: ${rule.name}")
            } ?: run {
                // Check if it's a local/private IP
                if (isPrivateIp(ipAddress)) {
                    RoutingDecision(RoutingAction.ROUTE_DIRECT, null, null, "Private IP")
                } else {
                    RoutingDecision(activeConfig.defaultAction, null, null, "Default")
                }
            }
        }
    }

    /**
     * Determine routing by UID (process owner)
     */
    fun decideByUid(uid: Int): RoutingDecision {
        return packageDecisionCache.getOrPut("uid:$uid") {
            val packageName = uidToPackage[uid] ?: getPackageNameForUid(uid)
            packageName?.let {
                decideByPackage(it)
            } ?: run {
                // Check UID-specific rules
                val rule = activeConfig.rules.filter {
                    it.isEnabled && it.type == RoutingRuleType.PROCESS_UID && it.target == uid.toString()
                }.maxByOrNull { it.priority }

                rule?.let { r ->
                    RoutingDecision(r.action, null, r, "UID Rule: ${r.name}")
                } ?: RoutingDecision(activeConfig.defaultAction, null, null, "Default")
            }
        }
    }

    /**
     * Main routing decision function - combines all factors
     */
    fun decide(
        packageName: String? = null,
        domain: String? = null,
        ipAddress: String? = null,
        uid: Int? = null
    ): RoutingDecision {
        // Priority order: explicit rules > app profile > domain profile > CIDR > default

        // 1. Check package-specific rules (highest priority for app-based routing)
        packageName?.let { pkg ->
            val decision = decideByPackage(pkg)
            if (decision.matchedRule != null || decision.action != activeConfig.defaultAction) {
                recordDecision(decision)
                return decision
            }
        }

        // 2. Check UID-specific rules
        uid?.let { u ->
            val decision = decideByUid(u)
            if (decision.matchedRule != null) {
                recordDecision(decision)
                return decision
            }
        }

        // 3. Check domain rules
        domain?.let { dom ->
            val decision = decideByDomain(dom)
            if (decision.matchedRule != null || decision.action != activeConfig.defaultAction) {
                recordDecision(decision)
                return decision
            }
        }

        // 4. Check IP/CIDR rules
        ipAddress?.let { ip ->
            val decision = decideByIp(ip)
            if (decision.matchedRule != null || decision.action != activeConfig.defaultAction) {
                recordDecision(decision)
                return decision
            }
        }

        // 5. Default action
        val defaultDecision = RoutingDecision(activeConfig.defaultAction, null, null, "Default")
        recordDecision(defaultDecision)
        return defaultDecision
    }

    private fun recordDecision(decision: RoutingDecision) {
        when (decision.action) {
            RoutingAction.ROUTE_VIA_VPN -> stats.vpnPackets++
            RoutingAction.ROUTE_DIRECT -> stats.directPackets++
            RoutingAction.BLOCK -> stats.blockedPackets++
            RoutingAction.ROUTE_VIA_CONFIG -> stats.otherConfigPackets++
        }
    }

    fun getStats(): RoutingStats = stats

    fun refreshRules() {
        packageDecisionCache.clear()
        domainDecisionCache.clear()
        ipDecisionCache.clear()
        domainRegexPatterns.clear()
        loadRoutingData()
    }

    fun updateActiveConfig(newConfig: RoutingConfig) {
        // This would be called when switching configs
        // For now, just refresh
        refreshRules()
    }

    // Helper functions

    private fun getUidForPackage(packageName: String): Int? {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            appInfo.uid
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun getPackageNameForUid(uid: Int): String? {
        val pm = context.packageManager
        val packages = pm.getPackagesForUid(uid)
        return packages?.firstOrNull()
    }

    private fun matchesCidr(ip: String, cidr: String): Boolean {
        try {
            val parts = cidr.split("/")
            val baseIp = InetAddress.getByName(parts[0])
            val prefixLength = parts[1].toInt()
            val targetIp = InetAddress.getByName(ip)

            val baseBytes = baseIp.address
            val targetBytes = targetIp.address

            if (baseBytes.size != targetBytes.size) return false

            val fullBytes = prefixLength / 8
            val remainingBits = prefixLength % 8

            for (i in 0 until fullBytes) {
                if (baseBytes[i] != targetBytes[i]) return false
            }

            if (remainingBits > 0 && fullBytes < baseBytes.size) {
                val mask = (0xFF shl (8 - remainingBits)) and 0xFF
                if ((baseBytes[fullBytes] and mask) != (targetBytes[fullBytes] and mask)) return false
            }

            return true
        } catch (e: Exception) {
            return false
        }
    }

    private fun isPrivateIp(ip: String): Boolean {
        return try {
            val addr = InetAddress.getByName(ip)
            addr.isSiteLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress ||
                    (addr is java.net.Inet4Address && ip.startsWith("10.")) ||
                    (addr is java.net.Inet4Address && ip.startsWith("172.") && ip.substringAfter("172.").substringBefore(".").toIntOrNull()?.inRange(16, 31) == true) ||
                    (addr is java.net.Inet4Address && ip.startsWith("192.168.")) ||
                    (addr is java.net.Inet6Address && (ip.startsWith("fc") || ip.startsWith("fd") || ip.startsWith("fe80")))
        } catch (e: UnknownHostException) {
            false
        }
    }
}

/**
 * Network callback to monitor connectivity and apply routing
 */
class RoutingNetworkCallback(
    private val routingEngine: RoutingEngine,
    private val vpnService: AmneziaVPNService
) : ConnectivityManager.NetworkCallback() {

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        Timber.d("Network available: $network")
        // Apply routing rules to this network
    }

    override fun onLost(network: Network) {
        super.onLost(network)
        Timber.d("Network lost: $network")
    }

    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
        super.onCapabilitiesChanged(network, networkCapabilities)
        Timber.d("Capabilities changed: $networkCapabilities")
    }
}