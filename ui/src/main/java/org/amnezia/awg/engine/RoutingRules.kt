package org.amnezia.awg.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class RoutingRuleType {
    APP,
    DOMAIN,
    IP
}

/**
 * A single split-tunnelling rule.
 *
 * [outbound] is either a tunnel name, "direct" or "block".
 */
data class RoutingRule(
    val id: String = UUID.randomUUID().toString(),
    val type: RoutingRuleType,
    val target: String,
    val outbound: String,
    val enabled: Boolean = true,
)

data class RoutingSettings(
    val defaultOutbound: String = "direct",
    val rules: List<RoutingRule> = emptyList(),
)

/**
 * Small JSON-backed store. It is deliberately independent from the UI so the engine can be
 * started from a quick tile or a deep link before any activity has been opened.
 */
class RoutingRulesStore(context: Context) {
    private val file = File(context.filesDir, "routing.json")
    private val lock = Any()

    fun load(): RoutingSettings = synchronized(lock) {
        if (!file.isFile) return RoutingSettings()
        runCatching {
            val root = JSONObject(file.readText())
            val rules = mutableListOf<RoutingRule>()
            val array = root.optJSONArray("rules") ?: JSONArray()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                rules += RoutingRule(
                    id = item.optString("id", UUID.randomUUID().toString()),
                    type = RoutingRuleType.valueOf(item.optString("type", "APP")),
                    target = item.getString("target"),
                    outbound = item.optString("outbound", "direct"),
                    enabled = item.optBoolean("enabled", true),
                )
            }
            RoutingSettings(
                defaultOutbound = root.optString("defaultOutbound", "direct"),
                rules = rules,
            )
        }.getOrElse { RoutingSettings() }
    }

    fun save(settings: RoutingSettings) = synchronized(lock) {
        val root = JSONObject()
        root.put("defaultOutbound", settings.defaultOutbound)
        val array = JSONArray()
        for (rule in settings.rules) {
            array.put(JSONObject().apply {
                put("id", rule.id)
                put("type", rule.type.name)
                put("target", rule.target)
                put("outbound", rule.outbound)
                put("enabled", rule.enabled)
            })
        }
        root.put("rules", array)
        file.parentFile?.mkdirs()
        file.writeText(root.toString(2))
    }
}
