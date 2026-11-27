package com.amneziawg.fork.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.amneziawg.fork.db.converters.RoutingRuleTypeConverter
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "routing_rules")
data class RoutingRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val configId: Long, // Reference to WireGuard config
    val name: String,
    val type: RoutingRuleType,
    val target: String, // Package name, domain, IP range, etc.
    val action: RoutingAction,
    val priority: Int = 0,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class RoutingRuleType(val displayName: String) {
    APP_PACKAGE("Приложение (пакет)"),
    DOMAIN("Домен"),
    IP_CIDR("IP/CIDR"),
    PROCESS_UID("UID процесса"),
    DNS_NAME("DNS имя"),
    GEO_IP("Гео-IP (страна)")
}

enum class RoutingAction(val displayName: String) {
    ROUTE_VIA_VPN("Через VPN"),
    ROUTE_DIRECT("Напрямую (обход VPN)"),
    BLOCK("Блокировать"),
    ROUTE_VIA_CONFIG("Через другой конфиг") // For multi-config support
}

@Serializable
data class RoutingRuleDto(
    val name: String,
    val type: RoutingRuleType,
    val target: String,
    val action: RoutingAction,
    val priority: Int = 0,
    val isEnabled: Boolean = true
) {
    fun toEntity(configId: Long): RoutingRule = RoutingRule(
        configId = configId,
        name = name,
        type = type,
        target = target,
        action = action,
        priority = priority,
        isEnabled = isEnabled
    )
}

@TypeConverters(RoutingRuleTypeConverter::class)
@Serializable
data class RoutingConfig(
    val id: Long = 0,
    val name: String,
    val wireguardConfig: WireGuardConfig, // Embedded WireGuard config
    val rules: List<RoutingRule> = emptyList(),
    val defaultAction: RoutingAction = RoutingAction.ROUTE_VIA_VPN,
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun withUpdatedRules(newRules: List<RoutingRule>): RoutingConfig = copy(
        rules = newRules,
        updatedAt = System.currentTimeMillis()
    )

    fun withActive(active: Boolean): RoutingConfig = copy(
        isActive = active,
        updatedAt = System.currentTimeMillis()
    )
}

@Serializable
data class WireGuardConfig(
    val name: String,
    val privateKey: String,
    val publicKey: String,
    val address: List<String>, // CIDR notation
    val dns: List<String> = emptyList(),
    val endpoint: String, // host:port
    val allowedIPs: List<String> = listOf("0.0.0.0/0", "::/0"),
    val persistentKeepalive: Int = 25,
    val mtu: Int = 1420,
    val preUp: String = "",
    val postUp: String = "",
    val preDown: String = "",
    val postDown: String = ""
)

@Serializable
data class AppRoutingProfile(
    val packageName: String,
    val appName: String,
    val icon: String?, // Base64 or resource reference
    val assignedConfigId: Long?, // Null = use default
    val customRules: List<RoutingRule> = emptyList()
)

@Serializable
data class DomainRoutingProfile(
    val domain: String,
    val assignedConfigId: Long?,
    val action: RoutingAction = RoutingAction.ROUTE_VIA_VPN,
    val isRegex: Boolean = false
)