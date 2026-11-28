package org.amnezia.awg.engine

import org.amnezia.awg.config.Config
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.StringReader

class SingBoxConfigBuilderTest {
    private val sample = """
        [Interface]
        Address = 192.0.2.2/32,2001:db8:ffff:ffff:ffff:ffff:ffff:ffff/128
        DNS = 192.0.2.0
        PrivateKey = TFlmmEUC7V7VtiDYLKsbP5rySTKLIZq1yn8lMqK83wo=
        MTU = 1420
        Jc = 4
        Jmin = 10
        Jmax = 50
        S1 = 55
        S2 = 42
        S3 = 40
        S4 = 12
        H1 = 1
        H2 = 2
        H3 = 3
        H4 = 4
        I1 = dGVzdC1qdW5r
        [Peer]
        AllowedIPs = 0.0.0.0/0, ::0/0
        Endpoint = 192.0.2.1:51820
        PersistentKeepalive = 25
        PublicKey = vBN7qyUTb5lJtWYJ8LhbPio1Z4RcyBPGnqFBGn6O6Qg=
    """.trimIndent()

    @Test
    fun buildsMultipleEndpointsAndSplitRules() {
        val config = Config.parse(BufferedReader(StringReader(sample)))
        val settings = RoutingSettings(
            defaultOutbound = "direct",
            rules = listOf(
                RoutingRule(type = RoutingRuleType.APP, target = "com.example.app", outbound = "work"),
                RoutingRule(type = RoutingRuleType.DOMAIN, target = "example.com", outbound = "home"),
                RoutingRule(type = RoutingRuleType.IP, target = "10.0.0.0/8", outbound = "direct"),
            ),
        )

        val json = SingBoxConfigBuilder.build(
            mapOf("home" to config, "work" to config),
            settings,
        )
        val root = JSONObject(json)

        assertEquals(2, root.getJSONArray("endpoints").length())
        assertEquals(1, root.getJSONArray("inbounds").length())
        val rules = root.getJSONObject("route").getJSONArray("rules")
        assertTrue(rules.length() >= 4) // sniff + app + domain + ip
        assertEquals("direct", root.getJSONObject("route").getString("final"))

        val endpoint = root.getJSONArray("endpoints").getJSONObject(0)
        assertEquals("wireguard", endpoint.getString("type"))
        assertEquals(4, endpoint.getInt("jc"))
        assertEquals(1, endpoint.getInt("h1"))
        assertEquals("dGVzdC1qdW5r", endpoint.getString("i1"))

        File("build/sample-singbox.json").writeText(json)
    }
}
