package com.amneziawg.fork.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverters
import androidx.room.Update
import com.amneziawg.fork.db.converters.RoutingRuleTypeConverter
import com.amneziawg.fork.model.AppRoutingProfile
import com.amneziawg.fork.model.DomainRoutingProfile
import com.amneziawg.fork.model.RoutingConfig
import com.amneziawg.fork.model.RoutingRule
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "routing_configs",
    indices = [androidx.room.Index(value = ["name"], unique = true)]
)
@TypeConverters(RoutingRuleTypeConverter::class)
data class RoutingConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val wireguardConfigJson: String, // Serialized WireGuardConfig
    val rulesJson: String = "[]", // Serialized List<RoutingRule>
    val defaultAction: String = "ROUTE_VIA_VPN",
    val isActive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toModel(): RoutingConfig {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        return RoutingConfig(
            id = id,
            name = name,
            wireguardConfig = json.decodeFromString(wireguardConfigJson),
            rules = json.decodeFromString(rulesJson),
            defaultAction = com.amneziawg.fork.model.RoutingAction.valueOf(defaultAction),
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromModel(model: RoutingConfig): RoutingConfigEntity {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            return RoutingConfigEntity(
                id = model.id,
                name = model.name,
                wireguardConfigJson = json.encodeToString(model.wireguardConfig),
                rulesJson = json.encodeToString(model.rules),
                defaultAction = model.defaultAction.name,
                isActive = model.isActive,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt
            )
        }
    }
}

@Serializable
@Entity(
    tableName = "routing_rules",
    indices = [androidx.room.Index(value = ["configId", "priority"])]
)
@TypeConverters(RoutingRuleTypeConverter::class)
data class RoutingRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val configId: Long,
    val name: String,
    val type: String, // RoutingRuleType.name
    val target: String,
    val action: String, // RoutingAction.name
    val priority: Int = 0,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toModel(): RoutingRule {
        return RoutingRule(
            id = id,
            configId = configId,
            name = name,
            type = com.amneziawg.fork.model.RoutingRuleType.valueOf(type),
            target = target,
            action = com.amneziawg.fork.model.RoutingAction.valueOf(action),
            priority = priority,
            isEnabled = isEnabled,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromModel(model: RoutingRule): RoutingRuleEntity {
            return RoutingRuleEntity(
                id = model.id,
                configId = model.configId,
                name = model.name,
                type = model.type.name,
                target = model.target,
                action = model.action.name,
                priority = model.priority,
                isEnabled = model.isEnabled,
                createdAt = model.createdAt,
                updatedAt = model.updatedAt
            )
        }
    }
}

@Serializable
@Entity(tableName = "app_routing_profiles")
data class AppRoutingProfileEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val icon: String? = null,
    val assignedConfigId: Long? = null,
    val customRulesJson: String = "[]",
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toModel(): AppRoutingProfile {
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        return AppRoutingProfile(
            packageName = packageName,
            appName = appName,
            icon = icon,
            assignedConfigId = assignedConfigId,
            customRules = json.decodeFromString(customRulesJson)
        )
    }

    companion object {
        fun fromModel(model: AppRoutingProfile): AppRoutingProfileEntity {
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            return AppRoutingProfileEntity(
                packageName = model.packageName,
                appName = model.appName,
                icon = model.icon,
                assignedConfigId = model.assignedConfigId,
                customRulesJson = json.encodeToString(model.customRules)
            )
        }
    }
}

@Serializable
@Entity(tableName = "domain_routing_profiles")
data class DomainRoutingProfileEntity(
    @PrimaryKey val domain: String,
    val assignedConfigId: Long? = null,
    val action: String = "ROUTE_VIA_VPN",
    val isRegex: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toModel(): DomainRoutingProfile {
        return DomainRoutingProfile(
            domain = domain,
            assignedConfigId = assignedConfigId,
            action = com.amneziawg.fork.model.RoutingAction.valueOf(action),
            isRegex = isRegex
        )
    }

    companion object {
        fun fromModel(model: DomainRoutingProfile): DomainRoutingProfileEntity {
            return DomainRoutingProfileEntity(
                domain = model.domain,
                assignedConfigId = model.assignedConfigId,
                action = model.action.name,
                isRegex = model.isRegex
            )
        }
    }
}

@Serializable
@Entity(tableName = "vpn_connection_logs")
data class VpnConnectionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val configId: Long,
    val configName: String,
    val connectedAt: Long,
    val disconnectedAt: Long? = null,
    val bytesSent: Long = 0,
    val bytesReceived: Long = 0,
    val durationMs: Long = 0,
    val disconnectReason: String? = null
)