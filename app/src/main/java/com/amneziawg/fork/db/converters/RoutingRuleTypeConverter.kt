package com.amneziawg.fork.db.converters

import androidx.room.TypeConverter
import com.amneziawg.fork.model.RoutingAction
import com.amneziawg.fork.model.RoutingRule
import com.amneziawg.fork.model.RoutingRuleType
import com.amneziawg.fork.model.WireGuardConfig
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

class RoutingRuleTypeConverter {

    @TypeConverter
    fun fromRoutingRuleType(value: String?): RoutingRuleType? {
        return value?.let { RoutingRuleType.valueOf(it) }
    }

    @TypeConverter
    fun toRoutingRuleType(value: RoutingRuleType?): String? {
        return value?.name
    }

    @TypeConverter
    fun fromRoutingAction(value: String?): RoutingAction? {
        return value?.let { RoutingAction.valueOf(it) }
    }

    @TypeConverter
    fun toRoutingAction(value: RoutingAction?): String? {
        return value?.name
    }

    @TypeConverter
    fun fromWireGuardConfig(value: String?): WireGuardConfig? {
        return value?.let { json.decodeFromString<WireGuardConfig>(it) }
    }

    @TypeConverter
    fun toWireGuardConfig(value: WireGuardConfig?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun fromRoutingRuleList(value: String?): List<RoutingRule> {
        return value?.let { json.decodeFromString<List<RoutingRule>>(it) } ?: emptyList()
    }

    @TypeConverter
    fun toRoutingRuleList(value: List<RoutingRule>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.let { json.decodeFromString<List<String>>(it) } ?: emptyList()
    }

    @TypeConverter
    fun toStringList(value: List<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun fromLongList(value: String?): List<Long> {
        return value?.let { json.decodeFromString<List<Long>>(it) } ?: emptyList()
    }

    @TypeConverter
    fun toLongList(value: List<Long>?): String? {
        return value?.let { json.encodeToString(it) }
    }
}