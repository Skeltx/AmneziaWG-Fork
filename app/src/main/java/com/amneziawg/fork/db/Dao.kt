package com.amneziawg.fork.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.amneziawg.fork.model.AppRoutingProfile
import com.amneziawg.fork.model.DomainRoutingProfile
import com.amneziawg.fork.model.RoutingConfig
import com.amneziawg.fork.model.RoutingRule
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutingConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: RoutingConfigEntity): Long

    @Update
    suspend fun update(config: RoutingConfigEntity): Int

    @Delete
    suspend fun delete(config: RoutingConfigEntity)

    @Query("DELETE FROM routing_configs WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM routing_configs ORDER BY createdAt DESC")
    fun getAll(): Flow<List<RoutingConfigEntity>>

    @Query("SELECT * FROM routing_configs WHERE id = :id")
    fun getById(id: Long): Flow<RoutingConfigEntity?>

    @Query("SELECT * FROM routing_configs WHERE isActive = 1")
    fun getActive(): Flow<RoutingConfigEntity?>

    @Query("SELECT * FROM routing_configs WHERE name = :name")
    fun getByName(name: String): Flow<RoutingConfigEntity?>

    @Transaction
    @Query("SELECT * FROM routing_configs WHERE id = :id")
    suspend fun getWithRules(id: Long): RoutingConfigEntity?

    @Query("UPDATE routing_configs SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAll(): Int

    @Query("UPDATE routing_configs SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean): Int
}

@Dao
interface RoutingRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: RoutingRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<RoutingRuleEntity>): List<Long>

    @Update
    suspend fun update(rule: RoutingRuleEntity): Int

    @Delete
    suspend fun delete(rule: RoutingRuleEntity)

    @Query("DELETE FROM routing_rules WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM routing_rules WHERE configId = :configId")
    suspend fun deleteByConfigId(configId: Long): Int

    @Query("SELECT * FROM routing_rules WHERE configId = :configId ORDER BY priority DESC, createdAt ASC")
    fun getByConfigId(configId: Long): Flow<List<RoutingRuleEntity>>

    @Query("SELECT * FROM routing_rules WHERE configId = :configId AND isEnabled = 1 ORDER BY priority DESC, createdAt ASC")
    fun getEnabledByConfigId(configId: Long): Flow<List<RoutingRuleEntity>>

    @Query("SELECT * FROM routing_rules WHERE id = :id")
    fun getById(id: Long): Flow<RoutingRuleEntity?>

    @Query("SELECT * FROM routing_rules WHERE configId = :configId AND type = :type AND target = :target")
    fun getByConfigAndTarget(configId: Long, type: String, target: String): Flow<RoutingRuleEntity?>

    @Transaction
    suspend fun replaceRulesForConfig(configId: Long, rules: List<RoutingRuleEntity>) {
        deleteByConfigId(configId)
        if (rules.isNotEmpty()) {
            insertAll(rules)
        }
    }
}

@Dao
interface AppRoutingProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: AppRoutingProfileEntity): Long

    @Update
    suspend fun update(profile: AppRoutingProfileEntity): Int

    @Delete
    suspend fun delete(profile: AppRoutingProfileEntity)

    @Query("DELETE FROM app_routing_profiles WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String): Int

    @Query("SELECT * FROM app_routing_profiles ORDER BY appName ASC")
    fun getAll(): Flow<List<AppRoutingProfileEntity>>

    @Query("SELECT * FROM app_routing_profiles WHERE packageName = :packageName")
    fun getByPackageName(packageName: String): Flow<AppRoutingProfileEntity?>

    @Query("SELECT * FROM app_routing_profiles WHERE assignedConfigId = :configId")
    fun getByConfigId(configId: Long): Flow<List<AppRoutingProfileEntity>>

    @Query("SELECT * FROM app_routing_profiles WHERE assignedConfigId IS NULL")
    fun getWithoutConfig(): Flow<List<AppRoutingProfileEntity>>

    @Transaction
    suspend fun upsertProfile(profile: AppRoutingProfile) {
        insert(AppRoutingProfileEntity.fromModel(profile))
    }

    @Transaction
    suspend fun upsertProfiles(profiles: List<AppRoutingProfile>) {
        profiles.forEach { insert(AppRoutingProfileEntity.fromModel(it)) }
    }
}

@Dao
interface DomainRoutingProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: DomainRoutingProfileEntity): Long

    @Update
    suspend fun update(profile: DomainRoutingProfileEntity): Int

    @Delete
    suspend fun delete(profile: DomainRoutingProfileEntity)

    @Query("DELETE FROM domain_routing_profiles WHERE domain = :domain")
    suspend fun deleteByDomain(domain: String): Int

    @Query("SELECT * FROM domain_routing_profiles ORDER BY domain ASC")
    fun getAll(): Flow<List<DomainRoutingProfileEntity>>

    @Query("SELECT * FROM domain_routing_profiles WHERE domain = :domain")
    fun getByDomain(domain: String): Flow<DomainRoutingProfileEntity?>

    @Query("SELECT * FROM domain_routing_profiles WHERE assignedConfigId = :configId")
    fun getByConfigId(configId: Long): Flow<List<DomainRoutingProfileEntity>>

    @Query("SELECT * FROM domain_routing_profiles WHERE isRegex = 1")
    fun getRegexProfiles(): Flow<List<DomainRoutingProfileEntity>>

    @Transaction
    suspend fun upsertProfile(profile: DomainRoutingProfile) {
        insert(DomainRoutingProfileEntity.fromModel(profile))
    }

    @Transaction
    suspend fun upsertProfiles(profiles: List<DomainRoutingProfile>) {
        profiles.forEach { insert(DomainRoutingProfileEntity.fromModel(it)) }
    }
}

@Dao
interface VpnConnectionLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: VpnConnectionLogEntity): Long

    @Update
    suspend fun update(log: VpnConnectionLogEntity): Int

    @Query("SELECT * FROM vpn_connection_logs WHERE configId = :configId ORDER BY connectedAt DESC LIMIT :limit")
    fun getByConfigId(configId: Long, limit: Int = 50): Flow<List<VpnConnectionLogEntity>>

    @Query("SELECT * FROM vpn_connection_logs ORDER BY connectedAt DESC LIMIT :limit")
    fun getAll(limit: Int = 100): Flow<List<VpnConnectionLogEntity>>

    @Query("SELECT * FROM vpn_connection_logs WHERE id = :id")
    fun getById(id: Long): Flow<VpnConnectionLogEntity?>

    @Query("UPDATE vpn_connection_logs SET disconnectedAt = :disconnectedAt, bytesSent = :bytesSent, bytesReceived = :bytesReceived, durationMs = :durationMs, disconnectReason = :reason WHERE id = :id")
    suspend fun updateDisconnect(
        id: Long,
        disconnectedAt: Long,
        bytesSent: Long,
        bytesReceived: Long,
        durationMs: Long,
        reason: String?
    ): Int

    @Query("DELETE FROM vpn_connection_logs WHERE connectedAt < :beforeTimestamp")
    suspend fun deleteOldLogs(beforeTimestamp: Long): Int
}