package org.amnezia.awg.engine

import org.amnezia.awg.config.Config
import org.json.JSONArray
import org.json.JSONObject
import java.util.Optional

/**
 * Converts the app's AmneziaWG configs and split-tunnelling rules into a sing-box-lx config.
 *
 * sing-box keeps one TUN and any number of WireGuard endpoints, so every active tunnel is an
 * endpoint and route rules choose the endpoint per package/domain/IP.
 */
object SingBoxConfigBuilder {
    private const val TUN_IPV4 = "172.19.0.1/30"
    private const val TUN_IPV6 = "fdfe:dcba:9876::1/126"
    private const val DNS_REMOTE = "dns-remote"

    fun build(active: Map<String, Config>, settings: RoutingSettings): String {
        require(active.isNotEmpty()) { "no active tunnels" }

        val tags = active.keys.associateWith { tunnelTag(it) }
        // A config with only IPv4 addresses cannot carry IPv6 through its WireGuard endpoint.
        // Keep such a profile IPv4-only instead of failing every AAAA connection with
        // "missing IPv6 local address".
        val hasIpv6 = active.values.any { config ->
            config.getInterface().getAddresses().any { it.getAddress() is java.net.Inet6Address }
        }

        // The old tunnel editor stores per-app include/exclude lists in the .conf file. Keep
        // those lists meaningful by turning them into routing rules, with explicit rules first.
        val configRules = active.flatMap { (name, config) ->
            val iface = config.getInterface()
            iface.getIncludedApplications().map {
                RoutingRule(type = RoutingRuleType.APP, target = it, outbound = name)
            } + iface.getExcludedApplications().map {
                RoutingRule(type = RoutingRuleType.APP, target = it, outbound = "direct")
            }
        }
        val allRules = settings.rules + configRules

        var defaultOutbound = when (settings.defaultOutbound) {
            "direct", "block" -> settings.defaultOutbound
            else -> if (tags.containsKey(settings.defaultOutbound)) settings.defaultOutbound else "direct"
        }
        // Turning on a single tunnel without any rules should keep the familiar "everything
        // through this tunnel" behaviour.
        if (allRules.none { it.enabled } && defaultOutbound == "direct") {
            defaultOutbound = active.keys.first()
        }

        val root = JSONObject()
        root.put("log", JSONObject().apply {
            put("level", "info")
            put("timestamp", true)
        })

        root.put("dns", JSONObject().apply {
            put("servers", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "https")
                    put("tag", DNS_REMOTE)
                    put("server", "1.1.1.1")
                })
            })
            put("final", DNS_REMOTE)
            put("strategy", "prefer_ipv4")
        })

        val captureAll = allRules.any { it.enabled && it.type != RoutingRuleType.APP } ||
            defaultOutbound != "direct"
        val tunnelApps = linkedSetOf<String>()
        val directApps = linkedSetOf<String>()

        val routeRules = JSONArray()
        // Sniff before matching so domain rules also work for HTTPS/QUIC and plain HTTP.
        routeRules.put(JSONObject().put("action", "sniff"))
        if (!hasIpv6) {
            routeRules.put(JSONObject().apply {
                put("ip_version", 6)
                put("action", "reject")
            })
        }

        for (rule in allRules) {
            if (!rule.enabled) continue
            val target = rule.target.trim()
            if (target.isEmpty()) continue

            when (rule.type) {
                RoutingRuleType.APP -> {
                    if (rule.outbound == "direct") {
                        directApps += target
                        routeRules.put(routeRule(
                            JSONObject().put("package_name", JSONArray().put(target)),
                            rule.outbound,
                        ))
                    } else {
                        tunnelApps += target
                        if (rule.outbound == "block" || tags.containsKey(rule.outbound)) {
                            routeRules.put(routeRule(
                                JSONObject().put("package_name", JSONArray().put(target)),
                                rule.outbound,
                            ))
                        }
                    }
                }

                RoutingRuleType.DOMAIN -> {
                    if (rule.outbound == "block" || rule.outbound == "direct" ||
                        tags.containsKey(rule.outbound)
                    ) {
                        routeRules.put(routeRule(domainMatcher(target), rule.outbound))
                    }
                }

                RoutingRuleType.IP -> {
                    if (rule.outbound == "block" || rule.outbound == "direct" ||
                        tags.containsKey(rule.outbound)
                    ) {
                        routeRules.put(routeRule(
                            JSONObject().put("ip_cidr", JSONArray().put(target)),
                            rule.outbound,
                        ))
                    }
                }
            }
        }

        val includePackages = if (captureAll) {
            emptyList()
        } else {
            tunnelApps.toList()
        }
        val excludePackages = if (captureAll) {
            (directApps - tunnelApps).toList()
        } else {
            emptyList()
        }

        val inbound = JSONObject().apply {
            put("type", "tun")
            put("tag", "tun-in")
            put("address", JSONArray().apply {
                put(TUN_IPV4)
                if (hasIpv6) put(TUN_IPV6)
            })
            put("mtu", 1500)
            put("auto_route", true)
            put("strict_route", false)
            put("stack", "gvisor")
            put("dns_mode", "hijack")
            if (includePackages.isNotEmpty()) {
                put("include_package", JSONArray(includePackages))
            }
            if (excludePackages.isNotEmpty()) {
                put("exclude_package", JSONArray(excludePackages))
            }
        }
        root.put("inbounds", JSONArray().put(inbound))

        val endpoints = JSONArray()
        for ((name, config) in active) {
            endpoints.put(endpoint(tags.getValue(name), config, hasIpv6))
        }
        root.put("endpoints", endpoints)
        root.put("outbounds", JSONArray().apply {
            put(JSONObject().apply {
                put("type", "direct")
                put("tag", "direct")
            })
            put(JSONObject().apply {
                put("type", "block")
                put("tag", "block")
            })
        })

        val finalTag = when (defaultOutbound) {
            "direct", "block" -> defaultOutbound
            else -> tags[defaultOutbound] ?: "direct"
        }
        root.put("route", JSONObject().apply {
            put("rules", routeRules)
            put("final", finalTag)
            put("auto_detect_interface", true)
        })

        return root.toString(2)
    }

    private fun routeRule(matcher: JSONObject, outbound: String): JSONObject {
        val rule = JSONObject(matcher.toString())
        if (outbound == "block") {
            rule.put("action", "reject")
        } else {
            rule.put("action", "route")
            rule.put("outbound", if (outbound == "direct") "direct" else tunnelTag(outbound))
        }
        return rule
    }

    private fun domainMatcher(target: String): JSONObject {
        val matcher = JSONObject()
        val value = target.lowercase()
        when {
            value.startsWith("*.") -> {
                matcher.put("domain_suffix", JSONArray().put(value.removePrefix("*")))
            }
            value.startsWith(".") -> {
                matcher.put("domain_suffix", JSONArray().put(value))
            }
            else -> {
                matcher.put("domain", JSONArray().put(value))
                matcher.put("domain_suffix", JSONArray().put(".$value"))
            }
        }
        return matcher
    }

    private fun endpoint(tag: String, config: Config, allowIpv6: Boolean): JSONObject {
        val iface = config.getInterface()
        val endpoint = JSONObject()
        endpoint.put("type", "wireguard")
        endpoint.put("tag", tag)

        val addresses = JSONArray()
        for (address in iface.getAddresses()) addresses.put(address.toString())
        endpoint.put("address", addresses)
        endpoint.put("private_key", iface.getKeyPair().getPrivateKey().toBase64())
        endpoint.put("mtu", iface.getMtu().orElse(1420))
        if (iface.getListenPort().isPresent) {
            endpoint.put("listen_port", iface.getListenPort().get())
        }

        val peers = JSONArray()
        for (peer in config.getPeers()) {
            val peerEndpoint = peer.getEndpoint()
            if (!peerEndpoint.isPresent) continue
            val socket = peerEndpoint.get()
            val json = JSONObject()
            json.put("address", socket.getHost())
            json.put("port", socket.getPort())
            json.put("public_key", peer.getPublicKey().toBase64())
            if (peer.getPreSharedKey().isPresent) {
                json.put("pre_shared_key", peer.getPreSharedKey().get().toBase64())
            }
            val allowed = peer.getAllowedIps().map { it.toString() }
                .filter { allowIpv6 || !it.contains(':') }
                .ifEmpty { if (allowIpv6) listOf("0.0.0.0/0", "::/0") else listOf("0.0.0.0/0") }
            json.put("allowed_ips", JSONArray(allowed))
            peer.getPersistentKeepalive().ifPresent { keepalive ->
                if (keepalive.contains('-')) {
                    json.put("persistent_keepalive_interval", keepalive)
                } else {
                    keepalive.toIntOrNull()?.let {
                        json.put("persistent_keepalive_interval", it)
                    }
                }
            }
            peers.put(json)
        }
        endpoint.put("peers", peers)

        // AmneziaWG 1.x / 2.0 / 3.x parameters.
        putInt(endpoint, "jc", iface.getJunkPacketCount())
        putInt(endpoint, "jmin", iface.getJunkPacketMinSize())
        putInt(endpoint, "jmax", iface.getJunkPacketMaxSize())
        putInt(endpoint, "s1", iface.getInitPacketJunkSize())
        putInt(endpoint, "s2", iface.getResponsePacketJunkSize())
        putInt(endpoint, "s3", iface.getCookieReplyPacketJunkSize())
        putInt(endpoint, "s4", iface.getTransportPacketJunkSize())
        putMagic(endpoint, "h1", iface.getInitPacketMagicHeader())
        putMagic(endpoint, "h2", iface.getResponsePacketMagicHeader())
        putMagic(endpoint, "h3", iface.getUnderloadPacketMagicHeader())
        putMagic(endpoint, "h4", iface.getTransportPacketMagicHeader())
        iface.getSpecialJunkI1().ifPresent { endpoint.put("i1", it) }
        iface.getSpecialJunkI2().ifPresent { endpoint.put("i2", it) }
        iface.getSpecialJunkI3().ifPresent { endpoint.put("i3", it) }
        iface.getSpecialJunkI4().ifPresent { endpoint.put("i4", it) }
        iface.getSpecialJunkI5().ifPresent { endpoint.put("i5", it) }
        iface.getHeaderProtectionKey().ifPresent {
            endpoint.put("header_protection_key", it.toBase64())
        }
        iface.getContentPaddingAddition().ifPresent {
            endpoint.put("content_padding_addition", it.toIntOrNull() ?: it)
        }
        iface.getRandomTrailers().ifPresent {
            endpoint.put("random_trailers", parseBool(it))
        }
        iface.getDisableCookies().ifPresent {
            endpoint.put("disable_cookies", parseBool(it))
        }
        iface.getRekeyAfterTime().ifPresent { endpoint.put("rekey_after_time", it) }
        iface.getRekeyTimeout().ifPresent { endpoint.put("rekey_timeout", it) }
        iface.getRejectAfterTime().ifPresent { endpoint.put("reject_after_time", it) }
        iface.getKeepaliveTimeout().ifPresent { endpoint.put("keepalive_timeout", it) }
        iface.getMaxHandshakeAttempts().ifPresent {
            endpoint.put("max_handshake_attempts", it.toIntOrNull() ?: it)
        }
        return endpoint
    }

    private fun putInt(json: JSONObject, key: String, value: Optional<Int>) {
        if (value.isPresent) json.put(key, value.get())
    }

    private fun putMagic(json: JSONObject, key: String, value: Optional<String>) {
        if (!value.isPresent) return
        val raw = value.get()
        val number = raw.toIntOrNull()
        if (number != null) json.put(key, number) else json.put(key, raw)
    }

    private fun parseBool(raw: String): Boolean =
        raw == "1" || raw.equals("true", ignoreCase = true) || raw.equals("yes", ignoreCase = true)

    private fun tunnelTag(name: String): String =
        "tunnel_" + name.replace(Regex("[^A-Za-z0-9_-]"), "_")
}
